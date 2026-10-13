from typing import Protocol
from PIL import Image
from .model import Token


class OcrEngine(Protocol):
    def recognize(self, image: Image.Image) -> list[Token]: ...


class PaddleEngine:
    def __init__(self):
        from paddleocr import PaddleOCR
        # 기본 인식 모델의 언어 범위에 기대지 않고 한국어 모델을 고정한다.
        self.pipeline = PaddleOCR(
            text_detection_model_name="PP-OCRv5_mobile_det",
            text_recognition_model_name="korean_PP-OCRv5_mobile_rec",
            use_doc_orientation_classify=False,
            use_doc_unwarping=False,
            use_textline_orientation=False,
            device="cpu", cpu_threads=2, enable_mkldnn=False,
        )

    def recognize(self, image: Image.Image) -> list[Token]:
        import numpy as np
        tokens = []
        for result in self.pipeline.predict(np.asarray(image)):
            texts, scores, boxes = result["rec_texts"], result["rec_scores"], result["rec_boxes"]
            if not (len(texts) == len(scores) == len(boxes)):
                raise ValueError("OCR output lengths differ")
            for text, confidence, box in zip(texts, scores, boxes):
                if str(text).strip():
                    tokens.append(Token(str(text), float(confidence), tuple(float(value) for value in box)))
        return tokens
