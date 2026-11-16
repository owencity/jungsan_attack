from contextlib import asynccontextmanager, nullcontext
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
from .watchdog import ProcessWatchdog


def create_app(*, dictionary_path: Path, service_key: str, engine: OcrEngine | None = None,
               model_directory: Path | None = None, cpu_threads: int = 2,
               watchdog_enabled: bool = False, request_timeout: int = 40,
               startup_timeout: int = 180) -> FastAPI:
    if len(service_key) < 32 or not service_key.isascii():
        raise ValueError("RECEIPT_SERVICE_KEY must be at least 32 ASCII characters")
    dictionary = load_dictionary(dictionary_path)
    admission = BoundedSemaphore(1)
    if not 5 <= request_timeout <= 40 or not 30 <= startup_timeout <= 240:
        raise ValueError("Invalid OCR timeout configuration")
    if type(cpu_threads) is not int or not 1 <= cpu_threads <= 2:
        raise ValueError("Invalid OCR CPU configuration")

    @asynccontextmanager
    async def lifespan(app: FastAPI):
        # 시작 실패는 가짜 정상 응답 대신 기동 실패로 드러낸다. 요청마다 모델을 로딩하지 않는다.
        watchdog = ProcessWatchdog() if watchdog_enabled else None
        app.state.watchdog = watchdog
        try:
            with watchdog.deadline(startup_timeout, "startup") if watchdog else nullcontext():
                app.state.engine = engine if engine is not None else await run_in_threadpool(
                    PaddleEngine, model_directory=model_directory, cpu_threads=cpu_threads)
            yield
        finally:
            if watchdog:
                watchdog.close()

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
            watchdog = app.state.watchdog
            with watchdog.deadline(request_timeout, "request") if watchdog else nullcontext():
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
    return create_app(
        dictionary_path=dictionary_path, service_key=os.environ["RECEIPT_SERVICE_KEY"],
        model_directory=Path(os.environ["RECEIPT_MODEL_DIR"]),
        cpu_threads=int(os.environ.get("RECEIPT_CPU_THREADS", "1")), watchdog_enabled=True,
        request_timeout=int(os.environ.get("RECEIPT_REQUEST_TIMEOUT_SECONDS", "40")),
        startup_timeout=int(os.environ.get("RECEIPT_STARTUP_TIMEOUT_SECONDS", "180")),
    )
