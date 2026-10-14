from pathlib import Path
import unittest

from PIL import Image, ImageDraw

from receipt_ocr.dictionary import load_dictionary
from receipt_ocr.extract import extract
from receipt_ocr.model import Token
from receipt_ocr.numeric_regions import MAX_RECOVERY_CELLS, recover_numeric_regions
from receipt_ocr.ocr import PaddleEngine


def fixture(*, discount=True, row_count=1, ink=True):
    image = Image.new("RGB", (500, 100+row_count*32), "white")
    header = [Token("품명", .99, (10, 10, 80, 30)),
              Token("수량 할인" if discount else "수량", .99, (210, 10, 310 if discount else 250, 30)),
              Token("금액", .99, (380, 10, 430, 30))]
    tokens = list(header)
    draw = ImageDraw.Draw(image)
    for index in range(row_count):
        top = 50+index*32
        tokens.extend([Token("소주", .99, (10, top, 80, top+20)), Token("10,000", .99, (380, top, 440, top+20))])
        if ink:
            draw.rectangle((230, top+4, 233, top+15), fill="black")
            if discount:
                draw.rectangle((284, top+4, 292, top+15), fill="black")
    return image, tokens


class PatchReader:
    def __init__(self, texts, confidence=.99):
        self.texts = iter(texts)
        self.confidence = confidence
        self.calls = []

    def __call__(self, image):
        self.calls.append(image.size)
        return [Token(next(self.texts), self.confidence, (1, 1, image.width-1, image.height-1))]


class NumericRegionTests(unittest.TestCase):
    def extract(self, tokens):
        return extract(tokens, load_dictionary(Path(__file__).resolve().parents[1] / "data/drinks.md"))

    def test_missing_quantity_and_discount_are_read_from_pixels(self):
        image, tokens = fixture()
        original = image.tobytes()
        original_tokens = list(tokens)
        reader = PatchReader(["1", "1", "0", "0"])
        with image:
            result = recover_numeric_regions(image, tokens, reader)
            self.assertEqual(original, image.tobytes())
        self.assertEqual(original_tokens, tokens)
        self.assertEqual(["1", "0"], [token.text for token in result if token.recovered_numeric])
        self.assertEqual(4, len(reader.calls))
        self.assertNotEqual(reader.calls[0], reader.calls[1])
        dictionary = load_dictionary(Path(__file__).resolve().parents[1] / "data/drinks.md")
        receipt = extract(result, dictionary)
        self.assertEqual((1, None, 10000), (receipt.drinks[0].quantity, receipt.drinks[0].unit_price, receipt.drinks[0].line_amount))
        self.assertIn("NUMERIC_REGION_RECOGNIZED", receipt.drinks[0].review_reasons)
        self.assertTrue(receipt.to_dict()["requiresConfirmation"])

    def test_blank_or_uniform_dark_cells_are_not_sent_to_ocr(self):
        for color in ("white", "gray", "black"):
            image, tokens = fixture(ink=False)
            image.paste(color, (0, 0, image.width, image.height))
            reader = PatchReader([])
            with image:
                self.assertEqual(tokens, recover_numeric_regions(image, tokens, reader))
            self.assertEqual([], reader.calls)

    def test_disagreeing_views_are_not_accepted(self):
        image, tokens = fixture(discount=False)
        with image:
            self.assertEqual(tokens, recover_numeric_regions(image, tokens, PatchReader(["1", "2"])))

    def test_unreadable_discount_does_not_erase_aligned_quantity_and_amount(self):
        image, tokens = fixture()
        with image:
            result = recover_numeric_regions(image, tokens, PatchReader(["1", "1", "bad"]))
        drink = self.extract(result).drinks[0]
        self.assertEqual((1, None, 10000), (drink.quantity, drink.unit_price, drink.line_amount))
        self.assertIn("ITEM_VALUES_MISSING", drink.review_reasons)
        self.assertIn("NUMERIC_REGION_RECOGNIZED", drink.review_reasons)

    def test_zero_quantity_is_not_created_when_only_discount_is_recovered(self):
        image, tokens = fixture()
        with image:
            result = recover_numeric_regions(image, tokens, PatchReader(["0", "0", "0"]))
        drink = self.extract(result).drinks[0]
        self.assertIsNone(drink.quantity)
        self.assertEqual(10000, drink.line_amount)

    def test_partial_zero_amount_and_conflicting_explicit_quantity_are_rejected(self):
        for condition in ("amount", "quantity"):
            image, tokens = fixture()
            if condition == "amount":
                tokens[-1] = Token("0", .99, (380, 50, 440, 70))
            else:
                tokens[-2] = Token("소주 2병", .99, (10, 50, 80, 70))
            with image:
                result = recover_numeric_regions(image, tokens, PatchReader(["1", "1", "bad"]))
            drink = self.extract(result).drinks[0]
            self.assertIsNone(drink.quantity)
            self.assertIsNone(drink.bottle_count)

    def test_duplicate_partial_quantity_annotations_are_not_combined(self):
        image, tokens = fixture()
        image.close()
        tokens[-1] = Token("10,000", .99, (380, 50, 440, 70), numeric_column="금액")
        tokens.extend([Token("1", .99, (220, 50, 230, 70), True, "수량"),
                       Token("2", .99, (235, 50, 245, 70), True, "수량")])
        # 숫자 개수가 우연히 헤더와 같아도 같은 수량 칸에 두 근거가 생기면 거절한다.
        drink = self.extract(tokens).drinks[0]
        self.assertIsNone(drink.quantity)
        self.assertIn("QUANTITY_INVALID_OR_CONFLICT", drink.review_reasons)

    def test_missing_detection_in_second_view_does_not_erase_confident_pixel_reading(self):
        image, tokens = fixture(discount=False)
        calls = []
        def reader(patch):
            calls.append(1)
            return [Token("1", .99, (1, 1, 20, 20))] if len(calls) == 1 else []
        with image:
            result = recover_numeric_regions(image, tokens, reader)
            self.assertEqual("1", result[-1].text)
            self.assertTrue(result[-1].recovered_numeric)

    def test_bad_numbers_are_not_repaired_or_coerced(self):
        for text in ("1.5", "-2", "+2", "1,00", "1001", "0", "O", "I", "1 2"):
            with self.subTest(text=text):
                image, tokens = fixture(discount=False)
                reader = PatchReader([text, text])
                with image:
                    self.assertEqual(tokens, recover_numeric_regions(image, tokens, reader))

    def test_confident_invalid_second_view_vetoes_valid_first_view(self):
        for text in ("-2", "1.5", "0", "1001"):
            image, tokens = fixture(discount=False)
            with image:
                self.assertEqual(tokens, recover_numeric_regions(image, tokens, PatchReader(["1", text])))

    def test_later_incomplete_header_resets_recovery_and_large_rows_are_skipped(self):
        for condition in ("header", "height"):
            image, tokens = fixture(discount=False)
            if condition == "header":
                tokens.insert(3, Token("품명 금액", .99, (10, 34, 430, 44)))
            else:
                tokens[-1] = Token("10,000", .99, (380, 50, 440, 140))
            reader = PatchReader([])
            with image:
                self.assertEqual(tokens, recover_numeric_regions(image, tokens, reader))

    def test_low_confidence_and_multiple_results_are_rejected(self):
        for reader in (PatchReader(["1", "1"], confidence=.89),
                       lambda image: [Token("1", .99, (1, 1, 10, 20)), Token("2", .99, (12, 1, 22, 20))]):
            image, tokens = fixture(discount=False)
            with image:
                self.assertEqual(tokens, recover_numeric_regions(image, tokens, reader))

    def test_existing_negative_or_uncertain_quantity_is_never_overwritten(self):
        for text, confidence in (("-2", .99), ("1.5", .99), ("2", .5)):
            image, tokens = fixture(discount=False)
            tokens.append(Token(text, confidence, (220, 50, 245, 70)))
            reader = PatchReader([])
            with image:
                self.assertEqual(tokens, recover_numeric_regions(image, tokens, reader))
            self.assertEqual([], reader.calls)

    def test_no_header_or_low_confidence_header_does_not_recover(self):
        for header_kind in ("missing", "low", "whole", "incomplete", "parallel"):
            image, tokens = fixture(discount=False)
            rows = tokens[3:]
            if header_kind == "missing":
                tokens = rows
            elif header_kind == "low":
                tokens[1] = Token("수량", .5, (210, 10, 250, 30))
            elif header_kind == "whole":
                tokens = [Token("품명 수량 금액", .99, (10, 10, 430, 30))] + rows
            elif header_kind == "incomplete":
                tokens = [tokens[0], tokens[2]] + rows
            else:
                tokens = [Token("품명 수량 금액 품명 수량 금액", .99, (10, 10, 490, 30))] + rows
            reader = PatchReader([])
            with image:
                self.assertEqual(tokens, recover_numeric_regions(image, tokens, reader))
            self.assertEqual([], reader.calls)

    def test_footer_ends_table_before_more_name_and_amount_rows(self):
        image, tokens = fixture(discount=False)
        tokens.insert(3, Token("합계 10,000", .99, (10, 35, 440, 45)))
        reader = PatchReader([])
        with image:
            self.assertEqual(tokens, recover_numeric_regions(image, tokens, reader))

    def test_name_overlapping_cell_and_missing_amount_do_not_recover(self):
        for condition in ("name", "money"):
            image, tokens = fixture(discount=False)
            if condition == "name":
                tokens[3] = Token("매우긴상품명", .99, (10, 50, 240, 70))
            else:
                tokens.pop()
            reader = PatchReader([])
            with image:
                self.assertEqual(tokens, recover_numeric_regions(image, tokens, reader))

    def test_ink_touching_crop_edge_or_horizontal_rule_is_rejected(self):
        for shape in ((206, 54, 215, 65), (215, 58, 245, 59)):
            image, tokens = fixture(discount=False, ink=False)
            ImageDraw.Draw(image).rectangle(shape, fill="black")
            reader = PatchReader([])
            with image:
                self.assertEqual(tokens, recover_numeric_regions(image, tokens, reader))

    def test_recovery_has_a_fixed_inference_budget(self):
        image, tokens = fixture(discount=False, row_count=20)
        reader = PatchReader(["2"] * (MAX_RECOVERY_CELLS*2))
        with image:
            result = recover_numeric_regions(image, tokens, reader)
        self.assertEqual(MAX_RECOVERY_CELLS, len([token for token in result if token.recovered_numeric]))
        self.assertEqual(MAX_RECOVERY_CELLS*2, len(reader.calls))

    def test_engine_uses_one_base_pass_and_does_not_recurse(self):
        image, tokens = fixture(discount=False)
        class Engine(PaddleEngine):
            def __init__(self):
                self.calls = 0
            def _recognize_once(self, patch):
                self.calls += 1
                return tokens if self.calls == 1 else [Token("1", .99, (1, 1, 20, 20))]
        engine = Engine()
        with image:
            result = engine.recognize(image)
        self.assertEqual(3, engine.calls)
        self.assertEqual("1", result[-1].text)

    def test_patch_failure_propagates_without_mutating_image_or_tokens(self):
        image, tokens = fixture(discount=False)
        before = list(tokens)
        def failing(patch):
            raise RuntimeError("local OCR failure")
        with image:
            with self.assertRaises(RuntimeError):
                recover_numeric_regions(image, tokens, failing)
            self.assertEqual(image.size, (500, 132))
            self.assertEqual(before, tokens)
            self.assertEqual("1", recover_numeric_regions(image, tokens, PatchReader(["1", "1"]))[-1].text)
