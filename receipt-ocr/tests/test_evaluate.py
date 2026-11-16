from io import BytesIO
import json
from pathlib import Path
import tempfile
import unittest
from PIL import Image
from receipt_ocr.evaluate import evaluate
from test_extract import tokens


class EvaluationTests(unittest.TestCase):
    def test_failed_images_remain_in_accuracy_denominator(self):
        class Engine:
            def recognize(self, image):
                return tokens("품목 단가 수량 금액", "참이슬 5,000 3 15,000", "합계 50,000")
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            buffer = BytesIO()
            with Image.new("RGB", (32, 32)) as image:
                image.save(buffer, format="PNG")
            (root / "good.png").write_bytes(buffer.getvalue())
            expected = {"total": 50000, "drinks": [{"name": "참이슬 (세부형 미상)", "category": "SOJU", "unit": "UNKNOWN",
                         "servingType": "UNKNOWN", "unitPrice": 5000, "quantity": 3, "lineAmount": 15000,
                         "volumeMl": None, "bottleCount": None}]}
            manifest = root / "manifest.json"
            manifest.write_text(json.dumps([{**expected, "image": "good.png"}, {**expected, "image": "missing.png"}]), encoding="utf-8")
            dictionary = Path(__file__).resolve().parents[1] / "data/drinks.md"
            result = evaluate(manifest, dictionary, Engine())
            self.assertEqual(2, result["samples"])
            self.assertEqual(1, result["failedSamples"])
            self.assertEqual(.5, result["totalExactRate"])
            self.assertEqual(.5, result["allDrinkFieldsExactRate"])
            self.assertNotIn("image", result)

    def test_bottle_count_and_volume_are_part_of_exact_accuracy(self):
        class Engine:
            def recognize(self, image):
                return tokens("품목 단가 수량 금액", "테라 500ml 5,000 2 10,000", "합계 10,000")
        expected = {"name": "테라", "category": "BEER", "unit": "UNKNOWN", "servingType": "UNKNOWN",
                    "unitPrice": 5000, "quantity": 2, "lineAmount": 10000, "volumeMl": 500, "bottleCount": None}
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            with Image.new("RGB", (32, 32)) as image:
                image.save(root / "good.png")
            manifest = root / "manifest.json"
            for altered_field in ({}, {"bottleCount": 2}, {"volumeMl": 50}):
                with self.subTest(altered_field=altered_field):
                    manifest.write_text(json.dumps([{"image": "good.png", "total": 10000,
                                         "drinks": [{**expected, **altered_field}]}]), encoding="utf-8")
                    result = evaluate(manifest, Path(__file__).resolve().parents[1] / "data/drinks.md", Engine())
                    self.assertEqual(0 if altered_field else 1, result["allDrinkFieldsExactRate"])
