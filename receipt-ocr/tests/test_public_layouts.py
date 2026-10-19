"""공개 이미지에서 발견한 양식만 재현한다. 이미지·상호·카드 정보는 테스트에 복사하지 않는다."""
import json
from pathlib import Path
import unittest

from receipt_ocr.dictionary import load_dictionary
from receipt_ocr.extract import extract, group_rows
from receipt_ocr.model import Token
from test_extract import tokens


class PublicLayoutTests(unittest.TestCase):
    def setUp(self):
        self.dictionary = load_dictionary(Path(__file__).resolve().parents[1] / "data/drinks.md")

    def read(self, *lines, confidence=.99):
        return extract(tokens(*lines, confidence=confidence), self.dictionary)

    def test_spaced_total_amount_label_and_unrelated_tax(self):
        result = self.read("합 계 금 액 18,500", "총매출액 20,000", "할인금액 1,500", "부가가치세 1,682")
        self.assertEqual(18500, result.total)
        self.assertIsNone(self.read("합계금액 18,500", confidence=.5).total)
        self.assertIsNone(self.read("합계금액 18,500", "결제금액 19,000").total)

    def test_perspective_total_uses_overlapping_unequal_height_boxes(self):
        result = extract([Token("81,800", .99, (1523, 1165, 1691, 1231)),
                          Token("합계 금액", .98, (650, 1186, 963, 1288)),
                          Token("부가세", .99, (872, 1294, 1342, 1392)),
                          Token("7,435", .99, (1530, 1282, 1698, 1350))], self.dictionary)
        self.assertEqual(81800, result.total)
        self.assertTrue(result.to_dict()["requiresConfirmation"])

    def test_separate_tax_row_is_not_borrowed_for_missing_total(self):
        result = extract([Token("합계금액", .99, (10, 10, 100, 40)),
                          Token("부가세", .99, (10, 45, 100, 75)),
                          Token("7,435", .99, (200, 45, 300, 75))], self.dictionary)
        self.assertIsNone(result.total)

    def test_two_different_totals_in_separate_rows_keep_conflict_detection(self):
        result = extract([Token("합계금액", .99, (10, 10, 100, 45)),
                          Token("81,800", .99, (200, 5, 300, 30)),
                          Token("결제금액", .99, (10, 55, 100, 90)),
                          Token("82,000", .99, (200, 50, 300, 75))], self.dictionary)
        self.assertIsNone(result.total)
        self.assertIn("TOTAL_CONFLICT", result.warnings)

    def test_row_overlap_boundary_and_input_are_preserved(self):
        first = Token("소주", .99, (10, 10, 70, 30))
        half_overlap = Token("1", .99, (150, 20, 170, 40))
        next_row = Token("맥주", .99, (10, 31, 70, 51))
        source = [next_row, half_overlap, first]
        self.assertEqual([[first, half_overlap], [next_row]], group_rows(source))
        self.assertEqual([next_row, half_overlap, first], source)
        self.assertEqual([[first], [Token("1", .99, (150, 21, 170, 41))]],
                         group_rows([first, Token("1", .99, (150, 21, 170, 41))]))

    def test_observed_beer_alias_does_not_assume_bottle_or_subtype(self):
        drink = self.read("품명 단가 수량 금액", "청도맥주 7,000 1 7,000").drinks[0]
        self.assertEqual(("청도맥주 (세부형 미상)", "BEER", 7000, 1, 7000),
                         (drink.name, drink.category, drink.unit_price, drink.quantity, drink.line_amount))
        self.assertEqual(("UNKNOWN", "UNKNOWN", None), (drink.unit, drink.serving_type, drink.bottle_count))
        for name in ("청도맥주 무알콜", "청도맥주(미등록)"):
            self.assertEqual([], self.read("품명 수량 금액", name + " 1 7,000").drinks)
        decimal = self.read("품명 수량 금액", "청도맥주 0.0 1 7,000").drinks[0]
        self.assertIsNone(decimal.quantity)
        self.assertIsNone(decimal.bottle_count)

    def test_spring_example_preserves_missing_values_and_review_reasons(self):
        expected = json.loads((Path(__file__).resolve().parents[1] /
                               "examples/response.review-required.json").read_text(encoding="utf-8"))
        result = self.read("품명 단가 수량 금액", "청도맥주 7,000 7,000", "합계금액 81,800", confidence=.9434)
        self.assertEqual(expected, result.to_dict())

    def test_split_header_characters_follow_explicit_order(self):
        drink = self.read("상 품 명 단 가 수 량 금 액", "소주 5,000 2 10,000").drinks[0]
        self.assertEqual((5000, 2, 10000), (drink.unit_price, drink.quantity, drink.line_amount))

    def test_quantity_discount_amount_does_not_invent_unit_price(self):
        drink = self.read("상품명 수량 할인 금액", "생맥주(카스300cc) 1 0 2,000").drinks[0]
        self.assertEqual((None, 1, 2000, 300), (drink.unit_price, drink.quantity, drink.line_amount, drink.volume_ml))
        self.assertEqual(("카스 (세부형 미상)", "BEER", "DRAFT", "UNKNOWN", 0),
                         (drink.name, drink.category, drink.serving_type, drink.unit, drink.bottle_count))
        self.assertIn("ITEM_UNIT_PRICE_MISSING", drink.review_reasons)

    def test_two_columns_read_only_quantity_and_amount_in_either_order(self):
        for header, values in (("품명 수량 금액", "2 9,000"), ("품명 금액 수량", "9,000 2")):
            drink = self.read(header, "소주 " + values).drinks[0]
            self.assertEqual((None, 2, 9000), (drink.unit_price, drink.quantity, drink.line_amount))

    def test_ocr_missing_small_quantity_and_zero_never_defaults_to_one(self):
        drink = self.read("상품명 수량 할인 금액", "생맥주(카스300cc) 2,000").drinks[0]
        self.assertEqual((None, None, None), (drink.unit_price, drink.quantity, drink.line_amount))
        self.assertIn("ITEM_VALUES_MISSING", drink.review_reasons)

    def test_nonzero_discount_is_flagged_without_price_reconstruction(self):
        drink = self.read("품명 수량 할인 금액", "병맥주 2 1,000 9,000").drinks[0]
        self.assertEqual((None, 2, 9000), (drink.unit_price, drink.quantity, drink.line_amount))
        self.assertIn("ITEM_DISCOUNT_PRESENT", drink.review_reasons)
        self.assertNotIn("ITEM_AMOUNT_MISMATCH", drink.review_reasons)

    def test_bad_discount_or_quantity_does_not_create_positive_bottles(self):
        for values in ("2 -500 10,000", "1.5 0 10,000", "2 0잔 10,000", "2 0 0"):
            drink = self.read("품명 수량 할인 금액", "병맥주 " + values).drinks[0]
            self.assertIsNone(drink.quantity)
            self.assertIsNone(drink.bottle_count)
            self.assertIn("ITEM_OUT_OF_RANGE", drink.review_reasons)

    def test_incomplete_or_low_confidence_header_does_not_reuse_columns(self):
        result = self.read("품명 단가 수량 금액", "품명 할인 금액", "소주 2 10,000")
        self.assertIsNone(result.drinks[0].quantity)
        low = self.read("품명 수량 금액", "소주 2 10,000", confidence=.5)
        self.assertIsNone(low.drinks[0].quantity)

    def test_parallel_receipt_tables_return_no_combined_order(self):
        result = self.read("품명 수량 단가 금액 품명 수량 단가 금액",
                           "콜라 2 1,000 2,000 소주 3 5,000 15,000", "맥주 2 5,000 10,000", "합계 27,000")
        self.assertIsNone(result.total)
        self.assertEqual([], result.drinks)
        self.assertEqual(["MULTIPLE_RECEIPTS_SUSPECTED"], result.warnings)
        self.assertTrue(result.to_dict()["requiresConfirmation"])

    def test_normal_repeated_vertical_header_is_still_supported(self):
        result = self.read("품명 수량 금액", "소주 2 10,000", "품명 수량 금액", "맥주 1 5,000")
        self.assertEqual([2, 1], [drink.quantity for drink in result.drinks])

    def test_parenthesized_name_keeps_non_alcohol_and_unknown_qualifiers_safe(self):
        for name in ("생맥주(카스 0.0 300cc)", "생맥주(테라 제로 300cc)", "생맥주(미등록맥주300cc)",
                     "생맥주(카스무알콜300cc)", "카스(생맥주300cc)", "생맥주(카스)(무알콜300cc)"):
            with self.subTest(name=name):
                self.assertEqual([], self.read("품명 수량 금액", name + " 1 2,000").drinks)
