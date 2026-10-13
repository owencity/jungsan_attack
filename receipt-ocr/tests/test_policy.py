from pathlib import Path
import unittest

from receipt_ocr.dictionary import load_dictionary
from receipt_ocr.extract import extract
from receipt_ocr.product import read_integer, read_volume
from test_extract import tokens

DICTIONARY = Path(__file__).resolve().parents[1] / "data/drinks.md"


class ReceiptPolicyTests(unittest.TestCase):
    def setUp(self):
        self.dictionary = load_dictionary(DICTIONARY)

    def read(self, *lines):
        return extract(tokens(*lines), self.dictionary)

    def column_row(self, line):
        return self.read("품목 단가 수량 금액", line).drinks[0]

    def test_brands_do_not_imply_containers(self):
        for name in ("참이슬", "테라", "새로", "스카치블루", "국순당 생막걸리"):
            with self.subTest(name=name):
                drink = self.column_row(f"{name} 5,000 2 10,000")
                self.assertEqual("UNKNOWN", drink.unit)
                self.assertEqual(2, drink.quantity)
                self.assertIsNone(drink.bottle_count)

    def test_explicit_quantity_without_header_is_not_default_one(self):
        for line, quantity, bottle_count in (("소주", None, None), ("소주 2병", 2, 2),
                                              ("소주 / 수량 2", 2, None), ("소주 x2", 2, None)):
            with self.subTest(line=line):
                drink = self.read(line).drinks[0]
                self.assertEqual(quantity, drink.quantity)
                self.assertEqual(bottle_count, drink.bottle_count)

    def test_decimal_negative_and_bad_separator_quantity_never_partially_matches(self):
        for quantity in ("1.5", "-2", "+2", "−2", "1,00", "0", "1001"):
            with self.subTest(quantity=quantity):
                drink = self.read(f"소주 {quantity}병").drinks[0]
                self.assertIsNone(drink.quantity)
                self.assertIsNone(drink.bottle_count)
                self.assertIn("QUANTITY_INVALID_OR_CONFLICT", drink.review_reasons)
        self.assertEqual(1000, self.read("소주 1,000병").drinks[0].quantity)

    def test_bad_numeric_column_never_becomes_positive_bottle_count(self):
        for quantity in ("1.5병", "-2병", "1,00병"):
            with self.subTest(quantity=quantity):
                drink = self.column_row(f"테라 5,000 {quantity} 10,000")
                self.assertIsNone(drink.quantity)
                self.assertIsNone(drink.bottle_count)
                self.assertIn("ITEM_OUT_OF_RANGE", drink.review_reasons)

    def test_volume_conversion_preserves_whole_number(self):
        for volume, expected in (("500cc", 500), ("500㎖", 500), ("0.5L", 500),
                                 ("1.5리터", 1500), ("12,500ml", 12500), ("1.50ml", None),
                                 ("-500ml", None), ("500,00ml", None)):
            with self.subTest(volume=volume):
                drink = self.read(f"맥주 {volume}").drinks[0]
                self.assertEqual(expected, drink.volume_ml)
                self.assertIsNone(drink.quantity)
                self.assertIsNone(drink.bottle_count)

    def test_product_numbers_and_volume_are_not_quantities(self):
        drink = self.read("화요25 375ml / 수량 2").drinks[0]
        self.assertEqual("화요25", drink.name)
        self.assertEqual((375, 2, None), (drink.volume_ml, drink.quantity, drink.bottle_count))
        drink = self.read("국순당 1000억 막걸리 / 수량 1").drinks[0]
        self.assertEqual(1, drink.quantity)
        self.assertIsNone(drink.bottle_count)

    def test_draft_and_pitcher_are_independent(self):
        drink = self.read("생맥주 피처 1700cc / 수량 1").drinks[0]
        self.assertEqual(("BEER", "DRAFT", "PITCHER", 1700, 1, 0),
                         (drink.category, drink.serving_type, drink.unit, drink.volume_ml, drink.quantity, drink.bottle_count))
        pitcher = self.read("피처 / 수량 1").drinks[0]
        self.assertEqual(("UNKNOWN", "PITCHER"), (pitcher.category, pitcher.unit))
        self.assertIsNone(pitcher.bottle_count)

    def test_raw_makgeolli_is_not_tap_draft(self):
        drink = self.read("국순당 생막걸리 750ml / 수량 1").drinks[0]
        self.assertEqual(("MAKGEOLLI", "UNKNOWN", "UNKNOWN"), (drink.category, drink.serving_type, drink.unit))
        self.assertEqual(750, drink.volume_ml)

    def test_literal_container_is_read_but_glassware_excluded(self):
        for line, unit, quantity, bottles in (("테라 병 500ml / 수량 3", "BOTTLE", 3, 3),
                                               ("테라 캔 500ml / 수량 3", "CAN", 3, 0),
                                               ("병맥주 / 수량 4", "BOTTLE", 4, 4),
                                               ("위스키 2 glass", "GLASS", 2, 0),
                                               ("위스키 2 glasses", "GLASS", 2, 0)):
            with self.subTest(line=line):
                drink = self.read(line).drinks[0]
                self.assertEqual((unit, quantity, bottles), (drink.unit, drink.quantity, drink.bottle_count))
        self.assertEqual([], self.read("소주잔 2병", "와인잔 세트 2").drinks)

    def test_count_and_container_conflicts_are_left_unresolved(self):
        drink = self.column_row("테라 2병 5,000 3 15,000")
        self.assertIsNone(drink.quantity)
        self.assertIn("QUANTITY_INVALID_OR_CONFLICT", drink.review_reasons)
        drink = self.column_row("병맥주 5,000 2캔 10,000")
        self.assertEqual("UNKNOWN", drink.unit)
        self.assertIsNone(drink.bottle_count)
        self.assertIn("UNIT_CONFLICT", drink.review_reasons)

    def test_longer_non_alcohol_names_are_never_truncated_to_alcohol(self):
        names = ("카스 0.0", "카스제로0.0", "테라 제로", "하이트제로0.00", "클라우드 논알콜릭",
                 "클라우드 클리어", "진로 토닉워터", "버진 모히토", "무알콜 하이볼", "콜라", "사이다", "생수")
        for name in names:
            with self.subTest(name=name):
                self.assertEqual([], self.read(name, f"{name} 5,000 2 10,000",
                                                "품목 단가 수량 금액", f"{name} 5,000 10,000").drinks)

    def test_zero_sugar_and_product_variants_stay_distinct(self):
        cases = (("새로 제로슈거", "SOJU", "새로"), ("테라 라이트", "BEER", "테라 라이트"),
                 ("진로골드", "SOJU", "진로골드"), ("별빛청하", "OTHER_ALCOHOL", "별빛청하"),
                 ("카스", "BEER", "카스 (세부형 미상)"), ("카스 프레시", "BEER", "카스 프레시"))
        for name, category, canonical in cases:
            with self.subTest(name=name):
                drink = self.read(f"{name} / 수량 2").drinks[0]
                self.assertEqual((category, canonical), (drink.category, drink.name))

    def test_non_alcohol_and_non_beverage_are_different_dictionary_categories(self):
        self.assertEqual("NON_ALCOHOL", self.dictionary["콜라"].category)
        self.assertEqual("NON_BEVERAGE", self.dictionary["병따개"].category)

    def test_packs_sets_and_mixed_drinks_do_not_generate_component_bottles(self):
        drink = self.read("캔맥주 6캔입 / 수량 2").drinks[0]
        self.assertEqual("PACK", drink.unit)
        self.assertEqual(2, drink.quantity)
        self.assertIsNone(drink.bottle_count)
        self.assertIn("COMBO_OR_PROMOTION", drink.review_reasons)
        for line in ("하이볼 / 수량 2", "소맥 / 수량 2", "술세트 / 수량 1"):
            with self.subTest(line=line):
                self.assertIsNone(self.read(line).drinks[0].bottle_count)

    def test_returns_and_promotions_never_appear_as_positive_purchases(self):
        for line in ("맥주 환불 -2", "소주서비스 / 수량 1", "소주 1+1", "맥주 취소 2병"):
            with self.subTest(line=line):
                self.assertFalse(any(drink.bottle_count for drink in self.read(line).drinks))

    def test_payment_only_and_unknown_partial_names_are_not_drink_rows(self):
        for line in ("카드결제 42,000원 승인번호 123456", "테라스 2병", "테라무알콜 2병",
                     "테라 맥주 소주 2병", "생맥주 500"):
            with self.subTest(line=line):
                self.assertFalse(any(drink.bottle_count for drink in self.read(line).drinks))

    def test_new_header_missing_quantity_resets_previous_columns(self):
        drink = self.read("품목 단가 수량 금액", "품목 단가 금액", "참이슬 5,000 3 15,000").drinks[0]
        self.assertIsNone(drink.quantity)

    def test_all_candidates_require_confirmation_without_original_text(self):
        result = self.read("품목 단가 수량 금액", "테라 병 5,000 2 10,000", "합계 10,000").to_dict()
        self.assertEqual(2, result["schemaVersion"])
        self.assertEqual([], result["warnings"])
        self.assertTrue(result["requiresConfirmation"])
        self.assertEqual(2, result["drinks"][0]["bottleCount"])
        self.assertFalse({"raw_text", "raw_line", "image_reference"} & result["drinks"][0].keys())

    def test_low_confidence_cannot_produce_positive_bottle_count(self):
        result = extract(tokens("품목 단가 수량 금액", "소주 2병", confidence=.5), self.dictionary)
        self.assertIsNone(result.drinks[0].bottle_count)

    def test_whole_number_parser_handles_limits_without_massive_integer_conversion(self):
        self.assertIsNone(read_integer("9" * 5000, 1000))
        self.assertIsNone(read_integer("1,00", 1000))
        self.assertEqual(1000, read_integer("1,000", 1000))
        self.assertEqual(500, read_volume("0.5", "L"))


if __name__ == "__main__":
    unittest.main()
