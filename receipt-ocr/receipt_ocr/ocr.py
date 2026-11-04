from typing import Protocol
from PIL import Image
from .model import Token


class OcrEngine(Protocol):
    def recognize(self, image: Image.Image) -> list[Token]: ...


class PaddleEngine:
    def __init__(self, *, cpu_threads: int = 2, enable_mkldnn: bool = False,
                 detection_max_edge: int | None = 1280):
        if type(cpu_threads) is not int or not 1 <= cpu_threads <= 64:
            raise ValueError("cpu_threads must be an integer between 1 and 64")
        if type(enable_mkldnn) is not bool:
            raise ValueError("enable_mkldnn must be a boolean")
        if detection_max_edge is not None and (type(detection_max_edge) is not int
                                                or not 32 <= detection_max_edge <= 4000):
            raise ValueError("detection_max_edge must be an integer between 32 and 4000")
        from paddleocr import PaddleOCR, TextRecognition
        # 검출 크기를 명시해 큰 원본의 CPU 비용을 제한한다. None은 이전 설정과의 대조용이다.
        detection_options = {} if detection_max_edge is None else {
            "text_det_limit_side_len": detection_max_edge, "text_det_limit_type": "max",
        }
        # 기본 인식 모델의 언어 범위에 기대지 않고 한국어 모델을 고정한다.
        self.pipeline = PaddleOCR(
            text_detection_model_name="PP-OCRv5_mobile_det",
            text_recognition_model_name="korean_PP-OCRv5_mobile_rec",
            use_doc_orientation_classify=False,
            use_doc_unwarping=False,
            use_textline_orientation=False,
            device="cpu", cpu_threads=cpu_threads, enable_mkldnn=enable_mkldnn,
            **detection_options,
        )
        # 숫자 칸은 위치를 이미 알므로 검출기를 다시 거치지 않는다. 공개 API로 같은 가중치를 읽는다.
        # 전체 OCR과 별도 인식 인스턴스를 두어 라이브러리의 비공개 내부 객체에는 의존하지 않는다.
        self.numeric_recognizer = TextRecognition(
            model_name="korean_PP-OCRv5_mobile_rec", device="cpu", cpu_threads=cpu_threads,
            enable_mkldnn=enable_mkldnn,
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
