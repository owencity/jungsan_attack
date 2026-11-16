FROM python:3.12-slim-bookworm

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    PADDLE_PDX_DISABLE_MODEL_SOURCE_CHECK=True \
    HF_HUB_OFFLINE=1 \
    HF_HOME=/tmp/huggingface \
    XDG_CACHE_HOME=/tmp/cache \
    PADDLE_HOME=/tmp/paddle \
    PADDLE_PDX_CACHE_HOME=/tmp/paddlex \
    RECEIPT_MODEL_DIR=/opt/receipt-models \
    RECEIPT_CPU_THREADS=1 \
    RECEIPT_REQUEST_TIMEOUT_SECONDS=40 \
    RECEIPT_STARTUP_TIMEOUT_SECONDS=180 \
    OMP_NUM_THREADS=1 \
    OPENBLAS_NUM_THREADS=1

RUN apt-get update && apt-get install -y --no-install-recommends libgl1 libglib2.0-0 libgomp1 \
    && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY requirements*.txt constraints-runtime.txt ./
# PyPI에는 3.3.1 Linux ARM wheel이 없어 공식 CPU 저장소를 사용한다.
RUN python -m pip install --no-cache-dir paddlepaddle==3.3.1 \
        --index-url https://www.paddlepaddle.org.cn/packages/stable/cpu/ \
    && python -m pip install --no-cache-dir -r requirements.txt -c constraints-runtime.txt \
    && python -m pip check
COPY receipt_ocr ./receipt_ocr
COPY data ./data
COPY tools/smoke_runtime.py ./tools/smoke_runtime.py
COPY tools/check_deadline.py ./tools/check_deadline.py
COPY models.lock.json ./
RUN python -m receipt_ocr.model_files /opt/receipt-models \
    && useradd --uid 10001 --create-home receipt \
    && chmod -R a-w /opt/receipt-models
USER 10001:10001
HEALTHCHECK --interval=15s --timeout=5s --start-period=210s --retries=3 \
    CMD python -c "import urllib.request; urllib.request.urlopen('http://127.0.0.1:8091/health', timeout=3).close()"
# CPU·메모리 한도와 restart는 compose가 적용한다. 포트는 외부에 게시하지 않는다.
CMD ["python", "-m", "uvicorn", "receipt_ocr.api:application", "--factory", "--host", "0.0.0.0", "--port", "8091", "--workers", "1", "--limit-concurrency", "8", "--timeout-keep-alive", "5", "--no-access-log"]
