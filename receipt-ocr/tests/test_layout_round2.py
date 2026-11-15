"""원근 표와 겹친 상품명에서 숫자를 다른 품목/표로 빌리지 않는지 확인한다."""
from pathlib import Path
import unittest

from receipt_ocr.dictionary import load_dictionary
from receipt_ocr.extract import extract
from receipt_ocr.model import Token


class CoordinateLayoutTests(unittest.TestCase):
    def setUp(self):
        self.dictionary = load_dictionary(Path(__file__).resolve().parents[1] / "data/drinks.md")

    def two_columns(self, *, quantity="1개", header_confidence=.99):
        return [Token("주문내역", .99, (10, 6, 110, 37)),
                Token("상품명", header_confidence, (40, 30, 100, 56)),
                Token("수량", header_confidence, (180, 26, 220, 53)),
                Token("금액", header_confidence, (300, 16, 360, 44)),
                Token("식사메뉴", .99, (10, 55, 100, 85)),
                Token("8,000", .99, (300, 58, 360, 86)),
                Token("1개", .99, (180, 73, 210, 97)),
                Token("8,000", .99, (10, 76, 70, 102)),
                Token("소주", .99, (10, 86, 70, 114)),
                Token("5,000", .99, (300, 88, 360, 116)),
                Token(quantity, .99, (180, 108, 210, 132)),
                Token("5,000", .99, (10, 107, 70, 133))]

    def three_columns(self, *, price_confidence=.99):
        return [Token("상품명", .99, (40, 10, 100, 30)),
                Token("단가", .99, (180, 10, 230, 30)),
                Token("수량", .99, (260, 10, 300, 30)),
                Token("금액", .99, (350, 10, 410, 30)),
                Token("식사메뉴(소)", .99, (10, 55, 120, 105)),
                Token("30,000", .99, (180, 82, 230, 104)),
                Token("30,000", .99, (350, 82, 410, 104)),
                Token("소주", .99, (10, 80, 65, 112)),
                Token("4,000", price_confidence, (180, 104, 230, 126)),
                Token("3", .99, (270, 106, 290, 124)),
                Token("12,000", .99, (350, 101, 410, 129))]

    def drink(self, source):
        result = extract(source, self.dictionary)
        return next((drink for drink in result.drinks if drink.category == "SOJU"), None)

    def assert_no_count(self, source):
        drink = self.drink(source)
        self.assertTrue(drink is None or drink.quantity is None)
        self.assertTrue(drink is None or drink.bottle_count is None)

    def test_split_header_and_explicit_count_do_not_create_unit_price_or_bottles(self):
        result = extract(self.two_columns(), self.dictionary)
        self.assertEqual(1, len(result.drinks))
        drink = result.drinks[0]
        self.assertEqual((1, None, 5000, "UNKNOWN", None),
                         (drink.quantity, drink.unit_price, drink.line_amount, drink.unit, drink.bottle_count))
        self.assertTrue(result.to_dict()["requiresConfirmation"])

    def test_tall_previous_name_keeps_its_own_amount_and_does_not_swallow_next_item(self):
        drink = self.drink(self.three_columns())
        self.assertEqual((3, 4000, 12000), (drink.quantity, drink.unit_price, drink.line_amount))
        self.assertIsNone(drink.bottle_count)

    def test_low_confidence_numeric_field_keeps_warning(self):
        drink = self.drink(self.three_columns(price_confidence=.8))
        self.assertEqual(3, drink.quantity)
        self.assertIn("LOW_CONFIDENCE", drink.review_reasons)
        self.assertIsNone(drink.bottle_count)

    def test_missing_and_low_confidence_headers_do_not_lend_columns(self):
        self.assert_no_count(self.two_columns()[4:])
        self.assert_no_count(self.two_columns(header_confidence=.5))

    def test_incomplete_next_header_resets_the_coordinate_table(self):
        source = self.two_columns()
        source.append(Token("품명 단가 금액", .99, (10, 75, 360, 82)))
        self.assert_no_count(source)

    def test_footer_stops_reuse_of_previous_header(self):
        for label in ("합계", "부가세", "카드결제", "환불", "취소"):
            source = self.two_columns()
            source.append(Token(label, .99, (80, 76, 150, 84)))
            self.assert_no_count(source)

    def test_duplicate_quantity_or_amount_is_ambiguous(self):
        for extra in (Token("2개", .99, (215, 108, 240, 132)),
                      Token("9,000", .99, (362, 88, 390, 116))):
            self.assert_no_count([*self.two_columns(), extra])

    def test_number_crossing_column_boundary_is_not_assigned_by_center(self):
        source = self.two_columns()
        source[-2] = Token("1개", .99, (250, 108, 290, 132))
        self.assert_no_count(source)

    def test_missing_quantity_is_not_default_one(self):
        source = self.two_columns()
        del source[-2]
        self.assert_no_count(source)

    def test_signed_fractional_malformed_and_zero_counts_are_rejected(self):
        for value in ("-1개", "+1개", "1.5개", "1,00개", "0개", "1,001개"):
            with self.subTest(value=value):
                self.assert_no_count(self.two_columns(quantity=value))

    def test_unknown_quantity_units_are_not_discarded(self):
        for value in ("1박스", "1원", "1회"):
            self.assert_no_count(self.two_columns(quantity=value))

    def test_qualifier_after_name_is_not_skipped(self):
        for qualifier in ("무알콜", "토닉워터", "(미등록)", "서비스", "취소", "잔"):
            source = self.two_columns()
            source.append(Token(qualifier, .99, (10, 112, 90, 132)))
            self.assert_no_count(source)

    def test_qualifier_in_numeric_area_is_not_skipped(self):
        source = self.two_columns()
        source.append(Token("무알콜", .99, (240, 105, 295, 125)))
        self.assert_no_count(source)

    def test_earlier_qualifier_is_not_treated_as_food_even_with_its_own_amount(self):
        for qualifier in ("무알콜", "논알코올", "제로", "(미등록)", "서비스", "환불", "잔"):
            source = self.three_columns()
            source[4] = Token(qualifier, .99, (10, 55, 120, 105))
            self.assert_no_count(source)

    def test_one_previous_amount_is_not_enough_to_discard_an_unknown_name_part(self):
        source = self.three_columns()
        del source[5]
        self.assert_no_count(source)

    def test_staggered_other_table_cannot_lend_its_numbers_to_a_far_left_name(self):
        source = [Token("상품명", .99, (10, 10, 100, 30)),
                  Token("단가", .99, (150, 10, 200, 30)),
                  Token("수량", .99, (250, 10, 300, 30)),
                  Token("금액", .99, (350, 10, 400, 30)),
                  Token("상품명", .99, (500, 35, 600, 55)),
                  Token("단가", .99, (650, 35, 700, 55)),
                  Token("수량", .99, (750, 35, 790, 55)),
                  Token("금액", .99, (850, 35, 900, 55)),
                  Token("소주", .99, (10, 80, 90, 100)),
                  Token("5,000", .99, (650, 105, 700, 125)),
                  Token("2", .99, (750, 105, 790, 125)),
                  Token("10,000", .99, (850, 105, 895, 125))]
        self.assert_no_count(source)

    def test_input_token_objects_and_coordinates_are_unchanged(self):
        source = self.two_columns()
        original = [(id(token), token.box, token.text) for token in source]
        extract(source, self.dictionary)
        self.assertEqual(original, [(id(token), token.box, token.text) for token in source])

    def test_numeric_non_alcohol_qualifier_is_not_dropped_from_product_column(self):
        for qualifier in ("0.0", "0.00", "0,0", "0"):
            with self.subTest(qualifier=qualifier):
                source = self.three_columns()
                source[7] = Token("카스", .99, (10, 80, 65, 112))
                source.append(Token(qualifier, .99, (70, 80, 120, 100)))
                result = extract(source, self.dictionary)
                self.assertFalse(any(drink.category == "BEER" and drink.quantity is not None
                                     for drink in result.drinks))

    def test_earlier_numeric_name_part_cannot_escape_through_previous_item_values(self):
        for qualifier in ("0,0", "0", "25", "-1", "+1", "1.5", "1,00", "0.0", "0.00"):
            with self.subTest(qualifier=qualifier):
                source = self.three_columns()
                source[5] = Token("30,000", .99, (180, 81, 230, 103))
                source[6] = Token("30,000", .99, (350, 81, 410, 103))
                source[7] = Token("카스", .99, (10, 80, 65, 112))
                source.append(Token(qualifier, .99, (70, 80, 120, 100)))
                result = extract(source, self.dictionary)
                self.assertFalse(any(drink.category == "BEER" and drink.quantity is not None
                                     for drink in result.drinks))

    def test_unlisted_name_qualifier_with_inconsistent_row_offset_is_not_discarded(self):
        for qualifier in ("알코올무첨가", "새로운한정문구"):
            with self.subTest(qualifier=qualifier):
                source = self.three_columns()
                source[5] = Token("30,000", .99, (180, 81, 230, 103))
                source[6] = Token("30,000", .99, (350, 81, 410, 103))
                source[7] = Token("카스", .99, (10, 80, 65, 112))
                source.append(Token(qualifier, .99, (70, 80, 120, 100)))
                result = extract(source, self.dictionary)
                self.assertFalse(any(drink.category == "BEER" and drink.quantity is not None
                                     for drink in result.drinks))

    def test_consistent_previous_food_row_does_not_need_a_dictionary_entry(self):
        source = self.three_columns()
        source[4] = Token("처음보는요리(소)", .99, (10, 55, 120, 105))
        drink = self.drink(source)
        self.assertEqual((3, 4000, 12000), (drink.quantity, drink.unit_price, drink.line_amount))

    def test_inconsistent_food_row_is_also_left_unresolved(self):
        source = self.three_columns()
        source[5] = Token("30,000", .99, (180, 78, 230, 100))
        source[6] = Token("30,000", .99, (350, 78, 410, 100))
        self.assert_no_count(source)


if __name__ == "__main__":
    unittest.main()
