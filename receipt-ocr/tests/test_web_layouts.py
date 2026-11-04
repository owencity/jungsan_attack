"""웹 원본의 구조만 재현한다. 사진·상호·결제정보나 사진별 예외는 넣지 않는다."""
from pathlib import Path
import unittest

from receipt_ocr.dictionary import load_dictionary
from receipt_ocr.extract import extract
from receipt_ocr.model import Token
from test_extract import tokens


class WebLayoutTests(unittest.TestCase):
    def setUp(self):
        self.dictionary = load_dictionary(Path(__file__).resolve().parents[1] / "data/drinks.md")

    def read(self, *lines, confidence=.99):
        return extract(tokens(*lines, confidence=confidence), self.dictionary)

    def coded_rows(self, name="소주", code="000016", values=("5,000", "2", "10,000"),
                   header="상품명 단가 수량 금액", code_x=10, next_y=65):
        return [Token(header, .99, (10, 10, 400, 30)),
                Token(name, .99, (10, 40, 90, 60)),
                Token(code, .99, (code_x, next_y, code_x + 75, next_y + 20)),
                *[Token(value, .99, (180 + i * 100, next_y, 240 + i * 100, next_y + 20))
                  for i, value in enumerate(values)]]

    def test_explicit_total_labels_preserve_conflicts_and_confirmation(self):
        for label in ("총 주문 액:", "총 계::"):
            with self.subTest(label=label):
                result = self.read(label + " 42,000원")
                self.assertEqual(42000, result.total)
                self.assertTrue(result.to_dict()["requiresConfirmation"])
                self.assertIsNone(self.read(label + " 42,000", "결제금액 40,000").total)
                self.assertIsNone(self.read(label + " 42,000", confidence=.5).total)

    def test_received_cash_subtotal_tax_and_discount_are_not_total_aliases(self):
        result = self.read("소계 50,000", "총매출액 50,000", "판매금액 50,000", "할인금액 5,000",
                           "받은금액 100,000", "거스름돈 55,000", "부가세 4,091")
        self.assertIsNone(result.total)

    def test_new_total_labels_do_not_repair_decimal_signed_or_split_money(self):
        for value in ("42.000", "-42,000", "+42,000", "42,00", "42. 000"):
            self.assertIsNone(self.read("총계:: " + value).total)
        self.assertIsNone(self.read("총계::: 42,000").total)

    def test_product_header_and_explicit_price_column_alias(self):
        for header in ("상품 단가 수량 금액", "메뉴 가격 수량 금액"):
            drink = self.read(header, "소주 5,000 2 10,000").drinks[0]
            self.assertEqual((5000, 2, 10000), (drink.unit_price, drink.quantity, drink.line_amount))

    def test_price_word_in_a_product_is_not_removed(self):
        self.assertEqual([], self.read("상품 가격 수량 금액", "가격좋은소주 5,000 2 10,000").drinks)

    def test_exact_observed_aliases_do_not_assume_subtype_or_bottle(self):
        for name, category in (("참이슬빨간것", "SOJU"), ("장수막걸리", "MAKGEOLLI")):
            drink = self.read("품명 단가 수량 금액", name + " 5,000 2 10,000").drinks[0]
            self.assertEqual((category, 2, "UNKNOWN", None),
                             (drink.category, drink.quantity, drink.unit, drink.bottle_count))
            for suffix in ("무알콜", "제로", "잔세트", "(미등록)"):
                self.assertEqual([], self.read("품명 단가 수량 금액",
                                              name + suffix + " 5,000 2 10,000").drinks)

    def test_order_amount_with_discount_needs_review_without_subtraction(self):
        for label in ("총주문액", "총계"):
            for discount in ("1,000", "-1,000", "1.000"):
                for lines in ((label + " 10,000", "할인금액 " + discount, "청구금액 9,000"),
                              ("할인금액 " + discount, label + " 10,000", "신용카드 9,000")):
                    result = self.read(*lines)
                    self.assertIsNone(result.total)
                    self.assertIn("TOTAL_NEEDS_REVIEW", result.warnings)
            self.assertEqual(10000, self.read(label + " 10,000", "할인금액 0").total)

    def test_unreadable_or_missing_discount_cannot_authorize_a_new_total_label(self):
        for label in ("총주문액", "총계"):
            for value in ("I,000", "", "미확인"):
                result = self.read(label + " 10,000", "할인금액 " + value, "청구금액 9,000")
                self.assertIsNone(result.total)
                self.assertIn("TOTAL_NEEDS_REVIEW", result.warnings)
            uncertain_zero = [*tokens(label + " 10,000"), Token("할인금액 0", .5, (10, 40, 200, 60))]
            self.assertIsNone(extract(uncertain_zero, self.dictionary).total)

    def test_high_confidence_zero_discount_preserves_both_new_total_labels(self):
        for total_label in ("총계", "총주문액"):
            for discount_label in ("할인", "할인액", "할인금액", "총할인", "총할인금액"):
                self.assertEqual(10000, self.read(total_label + " 10,000", discount_label + ": 0원").total)

    def test_discount_guard_does_not_change_existing_total_label_policy(self):
        self.assertEqual(9000, self.read("합계 9,000", "할인금액 1,000").total)

    def test_sales_amount_can_exclude_tax_and_is_not_an_alias_for_the_total(self):
        result = self.read("합계금액 42,000", "판매금액: 38,184", "부가세 3,816")
        self.assertEqual(42000, result.total)
        self.assertNotIn("TOTAL_CONFLICT", result.warnings)

    def test_product_code_row_is_linked_only_with_explicit_complete_columns(self):
        result = extract(self.coded_rows(), self.dictionary)
        self.assertEqual(1, len(result.drinks))
        drink = result.drinks[0]
        self.assertEqual((5000, 2, 10000), (drink.unit_price, drink.quantity, drink.line_amount))
        self.assertIsNone(drink.bottle_count)

    def test_missing_header_code_or_quantity_does_not_get_a_default(self):
        for values in (self.coded_rows(header="품명"), self.coded_rows(code="-000016"),
                       self.coded_rows(values=("5,000", "10,000"))):
            drink = extract(values, self.dictionary).drinks[0]
            self.assertIsNone(drink.quantity)
            self.assertIsNone(drink.unit_price)

    def test_code_row_must_be_near_and_left_aligned_with_the_product(self):
        for values in (self.coded_rows(next_y=100), self.coded_rows(code_x=120)):
            drink = extract(values, self.dictionary).drinks[0]
            self.assertIsNone(drink.quantity)
            self.assertIsNone(drink.line_amount)

    def test_next_table_header_and_non_alcohol_qualifiers_are_not_skipped(self):
        values = self.coded_rows()
        values.insert(2, Token("상품명 수량 금액", .99, (10, 61, 400, 64)))
        self.assertIsNone(extract(values, self.dictionary).drinks[0].quantity)
        for name in ("카스 0.0", "소주 무알콜", "소주잔", "장수막걸리 제로"):
            self.assertEqual([], extract(self.coded_rows(name=name), self.dictionary).drinks)

    def test_a_separate_qualifier_row_is_not_dropped_to_join_numbers(self):
        values = self.coded_rows()
        values.insert(2, Token("무알콜", .99, (10, 61, 85, 64)))
        drink = extract(values, self.dictionary).drinks[0]
        self.assertIsNone(drink.quantity)
        self.assertIsNone(drink.line_amount)

    def test_malformed_number_keeps_rejection_after_code_row_join(self):
        for quantity in ("-2", "+2", "2.0", "2,00"):
            drink = extract(self.coded_rows(name="병맥주", values=("5,000", quantity, "10,000")),
                            self.dictionary).drinks[0]
            self.assertIsNone(drink.quantity)
            self.assertIsNone(drink.bottle_count)
            self.assertIn("ITEM_OUT_OF_RANGE", drink.review_reasons)


if __name__ == "__main__":
    unittest.main()
