"""네이티브 모델 없이 공개 인식 모듈과의 경계만 검증한다."""
import sys
from types import SimpleNamespace
import unittest
from unittest.mock import patch

from PIL import Image
from receipt_ocr.ocr import PaddleEngine


class RecognitionAdapterTests(unittest.TestCase):
    def test_initialization_pins_korean_model_for_both_public_interfaces(self):
        configurations = []
        def pipeline(**options):
            configurations.append(("pipeline", options))
            return object()
        def recognition(**options):
            configurations.append(("recognition", options))
            return object()
        with patch.dict(sys.modules, {"paddleocr": SimpleNamespace(PaddleOCR=pipeline, TextRecognition=recognition)}):
            PaddleEngine()
        self.assertEqual("korean_PP-OCRv5_mobile_rec", configurations[0][1]["text_recognition_model_name"])
        self.assertEqual("korean_PP-OCRv5_mobile_rec", configurations[1][1]["model_name"])
        self.assertEqual(["cpu", "cpu"], [configuration[1]["device"] for configuration in configurations])
        self.assertEqual([2, 2], [configuration[1]["cpu_threads"] for configuration in configurations])

    def test_numeric_patch_uses_recognition_without_a_detection_pipeline(self):
        seen = []
        def predict(array):
            seen.append(array)
            return [{"rec_text": " 12 ", "rec_score": .98}]
        engine = PaddleEngine.__new__(PaddleEngine)
        engine.numeric_recognizer = SimpleNamespace(predict=predict)
        with Image.new("RGB", (40, 20)) as image:
            with patch.dict(sys.modules, {"numpy": SimpleNamespace(asarray=lambda value: value.size)}):
                tokens = engine._recognize_numeric(image)
        self.assertEqual([(40, 20)], seen)
        self.assertEqual(("12", .98, (0, 0, 40, 20)), (tokens[0].text, tokens[0].confidence, tokens[0].box))

    def test_empty_and_multiple_results_do_not_become_one_combined_number(self):
        engine = PaddleEngine.__new__(PaddleEngine)
        engine.numeric_recognizer = SimpleNamespace(predict=lambda array: [
            {"rec_text": "", "rec_score": .99}, {"rec_text": "1", "rec_score": .99},
            {"rec_text": "2", "rec_score": .99}])
        with Image.new("RGB", (40, 20)) as image:
            with patch.dict(sys.modules, {"numpy": SimpleNamespace(asarray=lambda value: value.size)}):
                self.assertEqual(["1", "2"], [token.text for token in engine._recognize_numeric(image)])

    def test_invalid_model_score_propagates_instead_of_returning_a_trusted_number(self):
        engine = PaddleEngine.__new__(PaddleEngine)
        engine.numeric_recognizer = SimpleNamespace(predict=lambda array: [{"rec_text": "1", "rec_score": float("nan")}])
        with Image.new("RGB", (40, 20)) as image:
            with patch.dict(sys.modules, {"numpy": SimpleNamespace(asarray=lambda value: value.size)}):
                with self.assertRaises(ValueError):
                    engine._recognize_numeric(image)

    def test_recognition_initialization_failure_is_not_hidden(self):
        def fail(**options):
            raise RuntimeError("model failed")
        with patch.dict(sys.modules, {"paddleocr": SimpleNamespace(PaddleOCR=lambda **options: object(), TextRecognition=fail)}):
            with self.assertRaises(RuntimeError):
                PaddleEngine()
