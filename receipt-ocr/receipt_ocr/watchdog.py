"""CPU 추론이 멈춰도 별도 프로세스가 OCR 서버만 종료해 컨테이너 재시작을 허용한다."""
from contextlib import contextmanager
import multiprocessing
import os
import signal
import sys
import time


def _monitor(connection, parent_pid: int):
    deadline = None
    stage = "unknown"
    try:
        while True:
            remaining = None if deadline is None else max(0, deadline - time.monotonic())
            if not connection.poll(remaining):
                # 사진·인식 내용·요청 헤더는 이 프로세스에 전달하지 않는다.
                print(f"RECEIPT_DEADLINE_EXCEEDED stage={stage}", file=sys.stderr, flush=True)
                try:
                    os.kill(parent_pid, getattr(signal, "SIGKILL", signal.SIGTERM))
                except ProcessLookupError:
                    pass
                return
            command = connection.recv()
            if command[0] == "stop":
                return
            if command[0] == "start":
                deadline, stage = command[1:]
            elif command[0] == "cancel":
                deadline = None
    except EOFError:
        # 서버가 먼저 종료되면 감시 프로세스도 남기지 않는다.
        return
    finally:
        connection.close()


class ProcessWatchdog:
    def __init__(self):
        if os.getpid() == 1:
            raise RuntimeError("OCR container requires an init process (docker --init / compose init: true)")
        context = multiprocessing.get_context("spawn")
        self.connection, receiver = context.Pipe()
        self.process = context.Process(target=_monitor, args=(receiver, os.getpid()), daemon=True)
        self.process.start()
        receiver.close()

    @contextmanager
    def deadline(self, seconds: float, stage: str):
        if seconds <= 0 or stage not in {"startup", "request"}:
            raise ValueError("Invalid OCR deadline")
        if not self.process.is_alive():
            raise RuntimeError("OCR watchdog unavailable")
        self.connection.send(("start", time.monotonic() + seconds, stage))
        try:
            yield
        finally:
            self.connection.send(("cancel",))

    def close(self):
        try:
            self.connection.send(("stop",))
        except (BrokenPipeError, EOFError):
            pass
        self.connection.close()
        self.process.join(timeout=3)
        if self.process.is_alive():
            self.process.terminate()
            self.process.join(timeout=3)
