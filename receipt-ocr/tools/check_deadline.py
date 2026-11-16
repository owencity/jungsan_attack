"""자기 컨테이너에 끝나지 않는 가짜 업로드를 보내 watchdog의 실제 종료를 확인한다."""
import os
import socket
import time


if __name__ == "__main__":
    started = time.monotonic()
    key = os.environ["RECEIPT_SERVICE_KEY"]
    headers = (
        "POST /api/v1/receipts/extract HTTP/1.1\r\n"
        "Host: localhost\r\n"
        "Content-Type: image/png\r\n"
        "Content-Length: 1000\r\n"
        f"X-Receipt-Service-Key: {key}\r\n\r\n"
    ).encode("ascii")
    with socket.create_connection(("127.0.0.1", 8091), timeout=5) as connection:
        connection.settimeout(50)
        connection.sendall(headers + b"x")
        try:
            result = connection.recv(1024)
            if result:
                raise SystemExit("Expected watchdog termination, received a response instead")
        except ConnectionResetError:
            pass
    elapsed = time.monotonic() - started
    if elapsed < 35:
        raise SystemExit("Server stopped before the configured deadline")
    print("OCR deadline terminated the stalled server")
