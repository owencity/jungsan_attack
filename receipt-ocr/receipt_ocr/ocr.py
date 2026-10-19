from typing import Protocol
from PIL import Image
from .model import Token


class OcrEngine(Protocol):
    def recognize(self, image: Image.Image) -> list[Token]: ...


class PaddleEngine:
    def __init__(self):
        from paddleocr import PaddleOCR, TextRecognition
        # 기본 인식 모델의 언어 범위에 기대지 않고 한국어 모델을 고정한다.
        self.pipeline = PaddleOCR(
            text_detection_model_name="PP-OCRv5_mobile_det",
            text_recognition_model_name="korean_PP-OCRv5_mobile_rec",
            use_doc_orientation_classify=False,
            use_doc_unwarping=False,
            use_textline_orientation=False,
            device="cpu", cpu_threads=2, enable_mkldnn=False,
        )
        # 숫자 칸은 위치를 이미 알므로 검출기를 다시 거치지 않는다. 공개 API로 같은 가중치를 읽는다.
        # 전체 OCR과 별도 인식 인스턴스를 두어 라이브러리의 비공개 내부 객체에는 의존하지 않는다.
        self.numeric_recognizer = TextRecognition(
            model_name="korean_PP-OCRv5_mobile_rec", device="cpu", cpu_threads=2, enable_mkldnn=False,
        )

    def recognize(self, image: Image.Image) -> list[Token]:
        from .numeric_regions import recover_numeric_regions
        tokens = self._recognize_once(image)
        return recover_numeric_regions(image, tokens, self._recognize_numeric)

    def _recognize_numeric(self, image: Image.Image) -> list[Token]:
        import numpy as np
        tokens = []
        for result in self.numeric_recognizer.predict(np.asarray(image)):
            text = str(result["rec_text"]).strip()
            if text:
                tokens.append(Token(text, float(result["rec_score"]), (0, 0, image.width, image.height)))
        return tokens

    def _recognize_once(self, image: Image.Image) -> list[Token]:
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
