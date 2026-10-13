import json
from pathlib import Path
import tempfile
import unittest

from receipt_ocr.dictionary import load_dictionary
from receipt_ocr.extract import extract
from receipt_ocr.model import Token

ROOT = Path(__file__).resolve().parents[1]


def tokens(*lines, confidence=0.99):
    return [Token(line, confidence, (10, index * 30, 500, index * 30 + 20))
            for index, line in enumerate(lines)]


class ExtractionTests(unittest.TestCase):
    def setUp(self):
        self.dictionary = load_dictionary(ROOT / "data/drinks.md")

    def read(self, *lines, confidence=0.99):
        return extract(tokens(*lines, confidence=confidence), self.dictionary)

    def test_example_has_total_two_drinks_and_confirmation(self):
        records = json.loads((ROOT / "examples/receipt-tokens.json").read_text(encoding="utf-8"))
        result = extract([Token(record["text"], record["confidence"], tuple(record["box"])) for record in records], self.dictionary)
        self.assertEqual(50000, result.total)
        self.assertEqual([(5000, 3, 15000), (6000, 2, 12000)],
                         [(drink.unit_price, drink.quantity, drink.line_amount) for drink in result.drinks])
        self.assertEqual(["UNKNOWN", "UNKNOWN"], [drink.unit for drink in result.drinks])
        self.assertEqual([None, None], [drink.bottle_count for drink in result.drinks])
        self.assertIn("ITEMS_NEED_REVIEW", result.warnings)
        self.assertTrue(result.to_dict()["requiresConfirmation"])

    def test_quantity_before_unit_price_follows_header(self):
        result = self.read("품목 수량 단가 금액", "참이슬 3 5,000 15,000", "합계 20,000")
        self.assertEqual((3, 5000), (result.drinks[0].quantity, result.drinks[0].unit_price))

    def test_separate_boxes_are_sorted_by_coordinates_not_ocr_order(self):
        row_tokens = [Token("15,000", .99, (350, 40, 440, 60)), Token("3", .99, (280, 39, 300, 59)),
                      Token("참이슬", .99, (10, 40, 120, 60)), Token("5,000", .99, (160, 41, 240, 61))]
        result = extract(tokens("품목 단가 수량 금액") + row_tokens + [Token("합계 20,000", .99, (10, 80, 440, 100))], self.dictionary)
        self.assertEqual(3, result.drinks[0].quantity)

    def test_no_header_does_not_invent_columns(self):
        drink = self.read("참이슬 5,000 3 15,000", "합계 20,000").drinks[0]
        self.assertEqual((None, None, None), (drink.unit_price, drink.quantity, drink.line_amount))
        self.assertIn("COLUMN_HEADER_MISSING", drink.review_reasons)

    def test_missing_quantity_does_not_infer_from_price(self):
        drink = self.read("품목 단가 수량 금액", "참이슬 5,000 15,000").drinks[0]
        self.assertIsNone(drink.quantity)
        self.assertIn("ITEM_VALUES_MISSING", drink.review_reasons)

    def test_line_mismatch_requires_review(self):
        drink = self.read("품목 단가 수량 금액", "참이슬 5,000 3 12,000").drinks[0]
        self.assertIn("ITEM_AMOUNT_MISMATCH", drink.review_reasons)

    def test_explicit_glass_does_not_turn_into_bottle(self):
        drink = self.read("품목 단가 수량 금액", "테라 5,000원 2잔 10,000원").drinks[0]
        self.assertEqual("GLASS", drink.unit)
        self.assertEqual(0, drink.bottle_count)
        self.assertIn("UNIT_REQUIRES_MAPPING", drink.review_reasons)

    def test_only_line_amount_remains_incomplete(self):
        drink = self.read("품목 단가 수량 금액", "참이슬 15,000").drinks[0]
        self.assertIsNone(drink.quantity)
        self.assertIn("ITEM_VALUES_MISSING", drink.review_reasons)

    def test_zero_and_excessive_quantity_are_not_usable(self):
        for quantity in (0, 1001):
            with self.subTest(quantity=quantity):
                drink = self.read("품목 단가 수량 금액", f"참이슬 5,000 {quantity} 15,000").drinks[0]
                self.assertIsNone(drink.quantity)
                self.assertIn("ITEM_OUT_OF_RANGE", drink.review_reasons)

    def test_decimal_negative_and_bad_separator_are_not_repaired(self):
        for amount in ("5.000", "5,00", "-5,000", "1,000,000,001"):
            with self.subTest(amount=amount):
                self.assertIsNone(self.read(f"합계 {amount}").total)

    def test_conflicting_totals_are_left_empty(self):
        result = self.read("합계 50,000", "결제금액 45,000")
        self.assertIsNone(result.total)
        self.assertIn("TOTAL_CONFLICT", result.warnings)

    def test_repeated_identical_total_is_allowed(self):
        self.assertEqual(50000, self.read("합계 50,000", "결제금액 50,000").total)

    def test_tax_and_cash_received_are_not_totals(self):
        result = self.read("공급가액 45,455", "부가세 4,545", "받은돈 100,000", "거스름돈 50,000")
        self.assertIsNone(result.total)

    def test_total_smaller_than_drinks_is_flagged(self):
        result = self.read("품목 단가 수량 금액", "참이슬 5,000 3 15,000", "합계 10,000")
        self.assertIn("DRINK_AMOUNT_EXCEEDS_TOTAL", result.warnings)

    def test_low_confidence_total_is_not_filled(self):
        self.assertIsNone(self.read("합계 50,000", confidence=.5).total)

    def test_low_confidence_item_is_flagged(self):
        self.assertIn("LOW_CONFIDENCE", self.read("품목 단가 수량 금액", "참이슬 5,000 3 15,000", confidence=.5).drinks[0].review_reasons)

    def test_volume_in_name_is_not_quantity(self):
        drink = self.read("품목 단가 수량 금액", "생맥주 500cc 5,000 2 10,000").drinks[0]
        self.assertEqual(2, drink.quantity)
        self.assertEqual("UNKNOWN", drink.unit)
        self.assertEqual("DRAFT", drink.serving_type)
        self.assertEqual(500, drink.volume_ml)
        self.assertEqual(0, drink.bottle_count)
        self.assertIn("UNKNOWN_CONTAINER", drink.review_reasons)

    def test_non_alcohol_and_unknown_names_are_not_guessed(self):
        result = self.read("품목 단가 수량 금액", "카스 0.0 5,000 2 10,000", "테라무알콜 5,000 2 10,000")
        self.assertEqual([], result.drinks)
        self.assertIn("NO_DRINK_ITEMS", result.warnings)

    def test_separate_purchases_are_not_deduplicated(self):
        result = self.read("품목 단가 수량 금액", "참이슬 5,000 2 10,000", "참이슬 5,000 1 5,000")
        self.assertEqual([2, 1], [drink.quantity for drink in result.drinks])

    def test_private_receipt_details_are_not_returned(self):
        result = self.read("카드번호 1234-5678-1234-5678", "승인번호 12345678", "합계 50,000")
        encoded = json.dumps(result.to_dict())
        self.assertNotIn("12345678", encoded)
        self.assertNotIn("카드번호", encoded)

    def test_empty_ocr_is_reviewable(self):
        self.assertIn("OCR_NO_TEXT", extract([], self.dictionary).warnings)


class DictionaryTests(unittest.TestCase):
    def read(self, content):
        with tempfile.TemporaryDirectory() as folder:
            path = Path(folder) / "dictionary.md"
            path.write_text(content, encoding="utf-8")
            return load_dictionary(path)

    def test_spaces_and_case_are_normalized(self):
        entries = self.read("| 영수증 표기 | 주류 종류 | 표시 이름 | 단위 |\n|---|---|---|---|\n| T E R A | 맥주 | 테라 | 미상 |")
        self.assertEqual("BEER", entries["tera"].category)

    def test_conflicting_aliases_fail_fast(self):
        with self.assertRaises(ValueError):
            self.read("| 영수증 표기 | 주류 종류 | 표시 이름 | 단위 |\n| 테라 | 맥주 | 테라 | 미상 |\n| 테 라 | 비주류 | 테라 | 미상 |")

    def test_brand_container_assumptions_are_rejected_even_in_custom_dictionary(self):
        with self.assertRaisesRegex(ValueError, "브랜드의 용기"):
            self.read("| 영수증 표기 | 주류 종류 | 표시 이름 | 단위 |\n| 테라 | 맥주 | 테라 | 병 |")

    def test_brand_draft_product_name_is_not_a_tap_serving(self):
        with self.assertRaisesRegex(ValueError, "일반 생맥"):
            self.read("| 영수증 표기 | 주류 종류 | 표시 이름 | 단위 | 제공 방식 |\n| 클라우드 생드래프트 | 맥주 | 클라우드 생드래프트 | 미상 | 생맥 |")

    def test_extended_table_and_following_document_table_are_separate(self):
        entries = self.read("| 영수증 표기 | 주류 종류 | 표시 이름 | 단위 | 제공 방식 |\n| 생맥주 | 맥주 | 생맥주 | 미상 | 생맥 |\n\n## 설명\n| 필드 | 설명 |\n|---|---|\n| 예시 | 아무 값 |")
        self.assertEqual("DRAFT", entries["생맥주"].serving_type)
        self.assertEqual(1, len(entries))

    def test_exact_alias_does_not_hide_its_explicit_quantity(self):
        entries = self.read("| 영수증 표기 | 주류 종류 | 표시 이름 | 단위 |\n| 소주2병 | 소주 | 소주 | 병 |")
        drink = extract(tokens("소주2병"), entries).drinks[0]
        self.assertEqual((2, 2), (drink.quantity, drink.bottle_count))

    def test_invalid_category_and_empty_dictionary_fail_fast(self):
        for content in ("", "| 영수증 표기 | 주류 종류 | 표시 이름 | 단위 |\n| 테라 | 아무거나 | 테라 | 병 |"):
            with self.subTest(content=content), self.assertRaises(ValueError):
                self.read(content)


if __name__ == "__main__":
    unittest.main()
