"""동일 원본에서 CPU 옵션을 순차 대조한다. 네이티브 오류·정확도 실패도 보존한다."""
import argparse
from contextlib import contextmanager
from dataclasses import asdict
import hashlib
from importlib.metadata import PackageNotFoundError, version
import json
import os
from pathlib import Path
import platform
import statistics
import subprocess
import sys
import tempfile
import time

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))
from tools.validate_capture_campaign import TimedEngine, inspect_case
from receipt_ocr.dictionary import load_dictionary


BASELINE_OPTIONS = {"cpu_threads": 2, "enable_mkldnn": False, "detection_max_edge": None,
                    "recognition_mkldnn": False}
CONFIGURATIONS = {
    "baseline": dict(BASELINE_OPTIONS),
    "det1280": {**BASELINE_OPTIONS, "detection_max_edge": 1280},
    "threads4": {**BASELINE_OPTIONS, "cpu_threads": 4},
    "mkldnn": {**BASELINE_OPTIONS, "enable_mkldnn": True},
    "hybrid": {**BASELINE_OPTIONS, "detection_max_edge": 1280, "recognition_mkldnn": True},
}


@contextmanager
def keep_system_awake():
    # 비교 중 자동 절전만 막는다. 화면·전역 전원 설정은 바꾸지 않고 기존 스레드 상태를 복구한다.
    state = {"requested": platform.system() == "Windows", "available": False,
             "restored": None, "displayRequired": False}
    previous = None
    execution_state = None
    if state["requested"]:
        try:
            import ctypes
            from ctypes import wintypes
            execution_state = ctypes.WinDLL("kernel32", use_last_error=True).SetThreadExecutionState
            execution_state.argtypes = [wintypes.DWORD]
            execution_state.restype = wintypes.DWORD
            previous = execution_state(0x80000001)
            state["available"] = bool(previous)
            if not previous:
                state["errorType"] = "SetThreadExecutionStateFailed"
        except (AttributeError, OSError) as error:
            state["errorType"] = type(error).__name__
    try:
        yield state
    finally:
        if previous and execution_state is not None:
            try:
                state["restored"] = bool(execution_state(previous))
            except (AttributeError, OSError) as error:
                state.update({"restored": False, "restoreErrorType": type(error).__name__})


def peak_rss_mib():
    # 모델/oneDNN 캐시를 포함한 프로세스 최고 사용량이다. 측정 불가를 0MiB로 숨기지 않는다.
    try:
        operating_system = platform.system()
        if operating_system == "Windows":
            import ctypes
            from ctypes import wintypes
            class ProcessMemoryCounters(ctypes.Structure):
                _fields_ = [("cb", wintypes.DWORD), ("PageFaultCount", wintypes.DWORD),
                    *[(name, ctypes.c_size_t) for name in ("PeakWorkingSetSize", "WorkingSetSize",
                        "QuotaPeakPagedPoolUsage", "QuotaPagedPoolUsage", "QuotaPeakNonPagedPoolUsage",
                        "QuotaNonPagedPoolUsage", "PagefileUsage", "PeakPagefileUsage")]]
            kernel = ctypes.WinDLL("kernel32", use_last_error=True)
            memory_api = ctypes.WinDLL("psapi", use_last_error=True).GetProcessMemoryInfo
            kernel.GetCurrentProcess.restype = wintypes.HANDLE
            memory_api.argtypes = [wintypes.HANDLE, ctypes.POINTER(ProcessMemoryCounters), wintypes.DWORD]
            memory_api.restype = wintypes.BOOL
            counters = ProcessMemoryCounters()
            counters.cb = ctypes.sizeof(counters)
            if not memory_api(kernel.GetCurrentProcess(), ctypes.byref(counters), counters.cb):
                return None
            return round(counters.PeakWorkingSetSize / (1024 * 1024), 3)
        if operating_system in {"Linux", "Darwin"}:
            import resource
            highest_rss = resource.getrusage(resource.RUSAGE_SELF).ru_maxrss
            return round(highest_rss / (1024 * 1024 if operating_system == "Darwin" else 1024), 3)
    except (AttributeError, ImportError, OSError):
        return None
    return None


def source_hashes():
    paths = sorted((ROOT / "receipt_ocr").glob("*.py"))
    paths.append(ROOT / "data/drinks.md")
    paths.extend((ROOT / "tools/validate_capture_campaign.py", Path(__file__).resolve()))
    return {str(path.relative_to(ROOT)): hashlib.sha256(path.read_bytes()).hexdigest() for path in paths}


def environment_versions():
    versions = {"python": platform.python_version(), "platform": platform.system(),
                "machine": platform.machine()}
    for package in ("paddleocr", "paddlex", "paddlepaddle"):
        try:
            versions[package] = version(package)
        except PackageNotFoundError:
            versions[package] = None
    return versions


def write_report(path, report):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")


def select_cases(cases, requested_ids):
    if not requested_ids or len(requested_ids) != len(set(requested_ids)):
        raise ValueError("sample IDs must be nonempty and unique")
    by_id = {case["id"]: case for case in cases}
    if len(by_id) != len(cases) or any(sample_id not in by_id for sample_id in requested_ids):
        raise ValueError("manifest IDs must be unique and include every requested sample")
    return [by_id[sample_id] for sample_id in requested_ids]


def summarize(observations):
    completed = [record for record in observations if "checks" in record]
    seconds = [record["seconds"] for record in completed]
    repeated_seconds = [record["seconds"] for record in completed if record["repeat"] > 1]
    drink_checks = [[value for key, value in record["checks"].items() if key.startswith("drink")]
                    for record in completed]
    return {
        "requests": len(observations), "executionFailures": len(observations) - len(completed),
        "allAnnotatedChecksMatch": sum(all(record["checks"].values()) for record in completed),
        "totalExactCount": sum(record["checks"].get("total", False) for record in completed),
        "allDrinkSelectedFieldsExactCount": sum(bool(checks) and all(checks) for checks in drink_checks),
        "medianSeconds": statistics.median(seconds) if seconds else None,
        "maxSeconds": max(seconds) if seconds else None,
        "repeatedMedianSeconds": statistics.median(repeated_seconds) if repeated_seconds else None,
    }


class FingerprintedEngine(TimedEngine):
    def recognize(self, image):
        tokens = super().recognize(image)
        # 원문은 저장하지 않고 실행 설정에 따른 토큰 변화만 비교한다.
        token_bytes = json.dumps([asdict(token) for token in tokens], ensure_ascii=False,
                                 sort_keys=True, separators=(",", ":")).encode("utf-8")
        self.token_sha256 = hashlib.sha256(token_bytes).hexdigest()
        self.token_count = len(tokens)
        return tokens


def run_worker(arguments, cases, manifest_sha256):
    os.environ.setdefault("PADDLE_PDX_CACHE_HOME", str(ROOT / ".models/paddlex"))
    os.environ.setdefault("HF_HOME", str(ROOT / ".models/huggingface"))
    report = {"configuration": arguments.worker, "options": CONFIGURATIONS[arguments.worker],
              "manifestSha256": manifest_sha256, "sourceHashes": source_hashes(),
              "observations": [], "modelStartupSeconds": None}
    write_report(arguments.output, report)
    try:
        dictionary = load_dictionary(ROOT / "data/drinks.md")
        started = time.perf_counter()
        engine = FingerprintedEngine(**CONFIGURATIONS[arguments.worker])
        report["modelStartupSeconds"] = round(time.perf_counter() - started, 3)
        write_report(arguments.output, report)
        for repeat in range(1, arguments.repeats + 1):
            for case in cases:
                record = inspect_case(case, arguments.samples, dictionary, engine)
                record.update({"repeat": repeat, "firstPrediction": not report["observations"]})
                if "checks" in record:
                    record.update({"tokenSha256": engine.token_sha256, "tokenCount": engine.token_count})
                report["observations"].append(record)
                write_report(arguments.output, report)
        report["status"] = "completed"
    except Exception as error:
        report.update({"status": "worker-error", "errorType": type(error).__name__})
    report["sourceHashesAfter"] = source_hashes()
    report["peakRssMiB"] = peak_rss_mib()
    if report["sourceHashesAfter"] != report["sourceHashes"]:
        report.update({"status": "source-changed", "errorType": "SourceChangedDuringBenchmark"})
    if hashlib.sha256(arguments.manifest.read_bytes()).hexdigest() != manifest_sha256:
        report.update({"status": "source-changed", "errorType": "ManifestChangedDuringBenchmark"})
    write_report(arguments.output, report)
    return int(report["status"] != "completed")


def run_configuration(arguments, configuration, cases):
    # 설정마다 새 프로세스를 써서 이전 가속 캐시와 네이티브 오류를 다음 대조에 넘기지 않는다.
    with tempfile.TemporaryDirectory(prefix="receipt-cpu-") as temporary_folder:
        worker_path = Path(temporary_folder) / "result.json"
        command = [sys.executable, str(Path(__file__).resolve()), str(arguments.samples),
                   "--manifest", str(arguments.manifest), "--ids", ",".join(arguments.ids),
                   "--repeats", str(arguments.repeats), "--worker", configuration,
                   "--output", str(worker_path)]
        status = "completed"
        return_code = None
        try:
            completed = subprocess.run(command, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL,
                                       timeout=arguments.timeout_seconds, check=False)
            return_code = completed.returncode
            if return_code:
                status = "worker-failed"
        except subprocess.TimeoutExpired:
            status = "timeout"
        try:
            report = json.loads(worker_path.read_text(encoding="utf-8"))
        except (OSError, ValueError):
            report = {"configuration": configuration, "options": CONFIGURATIONS[configuration],
                      "modelStartupSeconds": None, "observations": [], "status": "worker-failed"}
        if status != "completed":
            report["status"] = status
        report["workerReturnCode"] = return_code
        existing = {(record["sample"], record["repeat"]) for record in report["observations"]}
        for repeat in range(1, arguments.repeats + 1):
            for case in cases:
                if (case["id"], repeat) not in existing:
                    report["observations"].append({"sample": case["id"], "cohort": case["cohort"],
                        "family": case["family"], "repeat": repeat, "seconds": None,
                        "errorType": "WorkerTimeout" if status == "timeout" else "WorkerFailed"})
        report["summary"] = summarize(report["observations"])
        return report


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("samples", type=Path)
    parser.add_argument("--manifest", type=Path, default=ROOT / "examples/manifest.web-pubs.example.json")
    parser.add_argument("--ids", default="K345,K235,C48674,C49623")
    parser.add_argument("--repeats", type=int, default=2)
    parser.add_argument("--timeout-seconds", type=int, default=180, help="설정별 전체 프로세스 시간 제한")
    parser.add_argument("--configuration", action="append", choices=CONFIGURATIONS)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--worker", choices=CONFIGURATIONS, help=argparse.SUPPRESS)
    arguments = parser.parse_args()
    if not 1 <= arguments.repeats <= 10 or not 10 <= arguments.timeout_seconds <= 900:
        parser.error("repeats must be 1..10 and timeout-seconds must be 10..900")
    arguments.samples = arguments.samples.resolve()
    arguments.manifest = arguments.manifest.resolve()
    arguments.ids = arguments.ids.split(",")
    manifest_bytes = arguments.manifest.read_bytes()
    try:
        cases = select_cases(json.loads(manifest_bytes.decode("utf-8-sig")), arguments.ids)
    except (KeyError, TypeError, ValueError) as error:
        parser.error(type(error).__name__ + ": invalid manifest or selected IDs")
    if arguments.worker:
        return run_worker(arguments, cases, hashlib.sha256(manifest_bytes).hexdigest())
    configurations = arguments.configuration or list(CONFIGURATIONS)
    if len(configurations) != len(set(configurations)):
        parser.error("configurations must be unique")
    report = {"manifestSha256": hashlib.sha256(manifest_bytes).hexdigest(),
              "selectedIds": arguments.ids, "repeats": arguments.repeats,
              "timeoutSecondsPerConfiguration": arguments.timeout_seconds,
              "sourceHashes": source_hashes(), "environment": environment_versions(), "configurations": [],
              "limitations": ["partial annotations; not full field accuracy",
                              "serial local CPU benchmark; not concurrent production latency",
                              "first prediction and model initialization are separate"]}
    with keep_system_awake() as awake_state:
        report["systemAwakeGuard"] = awake_state
        write_report(arguments.output, report)
        for configuration in configurations:
            result = run_configuration(arguments, configuration, cases)
            if (("sourceHashes" in result and result["sourceHashes"] != report["sourceHashes"])
                    or source_hashes() != report["sourceHashes"]):
                result.update({"status": "source-changed", "errorType": "SourceChangedDuringBenchmark"})
            if (("manifestSha256" in result and result["manifestSha256"] != report["manifestSha256"])
                    or hashlib.sha256(arguments.manifest.read_bytes()).hexdigest() != report["manifestSha256"]):
                result.update({"status": "source-changed", "errorType": "ManifestChangedDuringBenchmark"})
            report["configurations"].append(result)
            write_report(arguments.output, report)
            print(json.dumps({"configuration": configuration, "status": result.get("status"),
                              "summary": result["summary"]}, ensure_ascii=True), flush=True)
            if result.get("status") == "source-changed":
                break
    write_report(arguments.output, report)
    # 알려진 정확도 실패와 네이티브 오류를 성공 종료로 숨기지 않는다.
    return int(any(result.get("status") != "completed" or result["summary"]["allAnnotatedChecksMatch"]
                   != result["summary"]["requests"] for result in report["configurations"]))


if __name__ == "__main__":
    raise SystemExit(main())
