from contextlib import asynccontextmanager
import hmac
import os
from pathlib import Path
from threading import BoundedSemaphore

from fastapi import FastAPI, HTTPException, Request
from starlette.concurrency import run_in_threadpool

from .dictionary import load_dictionary
from .extract import extract
from .images import decode_image, InvalidImage, MAX_BYTES
from .ocr import OcrEngine, PaddleEngine


def create_app(*, dictionary_path: Path, service_key: str, engine: OcrEngine | None = None) -> FastAPI:
    if len(service_key) < 32 or not service_key.isascii():
        raise ValueError("RECEIPT_SERVICE_KEY must be at least 32 ASCII characters")
    dictionary = load_dictionary(dictionary_path)
    admission = BoundedSemaphore(1)

    @asynccontextmanager
    async def lifespan(app: FastAPI):
        # 시작 실패는 가짜 정상 응답 대신 기동 실패로 드러낸다. 요청마다 모델을 로딩하지 않는다.
        app.state.engine = engine if engine is not None else await run_in_threadpool(PaddleEngine)
        yield

    app = FastAPI(title="정산어택 영수증 추출", version="0.1.0", lifespan=lifespan,
                  docs_url=None, redoc_url=None, openapi_url=None)

    @app.get("/health")
    def health():
        return {"status": "ready"}

    @app.post("/api/v1/receipts/extract")
    async def extract_receipt(request: Request):
        received_key = request.headers.get("X-Receipt-Service-Key", "")
        if not hmac.compare_digest(received_key.encode("utf-8"), service_key.encode("ascii")):
            raise HTTPException(401, detail={"code": "SERVICE_AUTH_REQUIRED"})
        if request.headers.get("content-type", "").split(";", 1)[0].strip().lower() not in {"image/jpeg", "image/png"}:
            raise HTTPException(415, detail={"code": "IMAGE_FORMAT_UNSUPPORTED"})
        if not admission.acquire(blocking=False):
            raise HTTPException(503, detail={"code": "OCR_BUSY"}, headers={"Retry-After": "2"})
        try:
            payload = bytearray()
            async for chunk in request.stream():
                if len(payload) + len(chunk) > MAX_BYTES:
                    raise HTTPException(413, detail={"code": "IMAGE_TOO_LARGE"})
                payload.extend(chunk)

            def process():
                image = decode_image(bytes(payload))
                try:
                    tokens = app.state.engine.recognize(image)
                    return extract(tokens, dictionary).to_dict()
                finally:
                    image.close()

            return await run_in_threadpool(process)
        except InvalidImage as error:
            raise HTTPException(400, detail={"code": str(error)}) from error
        except HTTPException:
            raise
        except Exception as error:
            # 인식 결과·원본 사진·외부 라이브러리 오류 메시지를 HTTP/로그에 내보내지 않는다.
            raise HTTPException(503, detail={"code": "OCR_UNAVAILABLE"}) from error
        finally:
            admission.release()

    return app


def application() -> FastAPI:
    dictionary_path = Path(os.environ.get("RECEIPT_DICTIONARY_PATH", str(Path(__file__).resolve().parents[1] / "data/drinks.md")))
    return create_app(dictionary_path=dictionary_path, service_key=os.environ["RECEIPT_SERVICE_KEY"])
