import hashlib
import json
from pathlib import Path
from types import SimpleNamespace
import subprocess
import sys
import tempfile
import unittest
from unittest.mock import patch

from tools.benchmark_cpu import keep_system_awake, peak_rss_mib, run_configuration, run_worker, select_cases, summarize


class BenchmarkTests(unittest.TestCase):
    def test_awake_guard_restores_previous_thread_state_when_the_comparison_fails(self):
        calls = []
        def execution_state(flags):
            calls.append(flags)
            return 0x80000000
        with patch("tools.benchmark_cpu.platform.system", return_value="Windows"), patch(
                "ctypes.WinDLL", create=True, return_value=SimpleNamespace(SetThreadExecutionState=execution_state)):
            with self.assertRaises(RuntimeError):
                with keep_system_awake() as state:
                    self.assertTrue(state["available"])
                    self.assertFalse(state["displayRequired"])
                    raise RuntimeError("comparison failed")
        self.assertEqual([0x80000001, 0x80000000], calls)
        self.assertTrue(state["restored"])

    def test_unavailable_awake_guard_is_metadata_without_changing_comparison_behavior(self):
        with patch("tools.benchmark_cpu.platform.system", return_value="Windows"), patch(
                "ctypes.WinDLL", create=True, return_value=SimpleNamespace(SetThreadExecutionState=lambda _: 0)):
            with keep_system_awake() as state:
                self.assertTrue(state["requested"])
                self.assertFalse(state["available"])
        self.assertIsNone(state["restored"])

    def test_peak_memory_units_distinguish_linux_kib_from_macos_bytes(self):
        resource = SimpleNamespace(RUSAGE_SELF=0,
            getrusage=lambda _: SimpleNamespace(ru_maxrss=1024 * 1024))
        for operating_system, expected in (("Linux", 1024.0), ("Darwin", 1.0)):
            with self.subTest(operating_system=operating_system), patch(
                    "tools.benchmark_cpu.platform.system", return_value=operating_system), patch.dict(
                    sys.modules, {"resource": resource}):
                self.assertEqual(expected, peak_rss_mib())

    def test_unsupported_peak_memory_measurement_is_unknown_instead_of_zero(self):
        with patch("tools.benchmark_cpu.platform.system", return_value="unsupported"):
            self.assertIsNone(peak_rss_mib())

    def test_selected_ids_preserve_requested_order_and_reject_unknown_or_duplicate_cases(self):
        cases = [{"id": "A"}, {"id": "B"}]
        self.assertEqual([cases[1], cases[0]], select_cases(cases, ["B", "A"]))
        for source, requested in ((cases, ["A", "A"]), (cases, ["missing"]),
                                  (cases + [cases[0]], ["A"]), (cases, [])):
            with self.subTest(requested=requested), self.assertRaises(ValueError):
                select_cases(source, requested)

    def test_semantic_failure_and_execution_failure_remain_in_denominator(self):
        observations = [
            {"repeat": 1, "seconds": 5, "checks": {"total": True, "drinkCount": True,
                                                   "drink0.quantity": True}},
            {"repeat": 2, "seconds": 3, "checks": {"total": False, "drinkCount": True,
                                                   "drink0.quantity": True}},
            {"repeat": 2, "seconds": None, "errorType": "WorkerTimeout"},
        ]
        summary = summarize(observations)
        self.assertEqual(3, summary["requests"])
        self.assertEqual(1, summary["executionFailures"])
        self.assertEqual(1, summary["allAnnotatedChecksMatch"])
        self.assertEqual(1, summary["totalExactCount"])
        self.assertEqual(2, summary["allDrinkSelectedFieldsExactCount"])
        self.assertEqual(4, summary["medianSeconds"])
        self.assertEqual(3, summary["repeatedMedianSeconds"])

    def test_timeout_retains_completed_partial_result_and_records_unfinished_repeats(self):
        cases = [{"id": "A", "cohort": "web", "family": "one"}]
        with tempfile.TemporaryDirectory() as folder:
            arguments = SimpleNamespace(samples=Path(folder), manifest=Path(folder) / "manifest.json",
                                        ids=["A"], repeats=2, timeout_seconds=10)
            def timeout(command, **options):
                worker_path = Path(command[command.index("--output") + 1])
                worker_path.write_text(json.dumps({"configuration": "baseline", "status": "started",
                    "observations": [{"sample": "A", "repeat": 1, "seconds": 5,
                                      "checks": {"total": True}}]}), encoding="utf-8")
                self.assertEqual(subprocess.DEVNULL, options["stdout"])
                self.assertEqual(subprocess.DEVNULL, options["stderr"])
                self.assertEqual(10, options["timeout"])
                raise subprocess.TimeoutExpired(command, 10, output=b"private OCR output")
            with patch("tools.benchmark_cpu.subprocess.run", side_effect=timeout):
                report = run_configuration(arguments, "baseline", cases)
        self.assertEqual("timeout", report["status"])
        self.assertEqual(2, report["summary"]["requests"])
        self.assertEqual(1, report["summary"]["executionFailures"])
        self.assertEqual(1, report["summary"]["allAnnotatedChecksMatch"])
        self.assertEqual("WorkerTimeout", report["observations"][1]["errorType"])
        self.assertNotIn("private", json.dumps(report))

    def test_native_crash_without_report_keeps_all_requests_and_sanitizes_output(self):
        cases = [{"id": "A", "cohort": "web", "family": "one"}]
        with tempfile.TemporaryDirectory() as folder:
            arguments = SimpleNamespace(samples=Path(folder), manifest=Path(folder) / "manifest.json",
                                        ids=["A"], repeats=2, timeout_seconds=10)
            with patch("tools.benchmark_cpu.subprocess.run", return_value=SimpleNamespace(returncode=-1)):
                report = run_configuration(arguments, "mkldnn", cases)
        self.assertEqual("worker-failed", report["status"])
        self.assertEqual(-1, report["workerReturnCode"])
        self.assertEqual(2, report["summary"]["requests"])
        self.assertEqual(2, report["summary"]["executionFailures"])
        self.assertEqual(0, report["summary"]["allAnnotatedChecksMatch"])
        self.assertIsNone(report["summary"]["medianSeconds"])

    def test_truth_changed_during_worker_cannot_be_reported_as_comparable_measurement(self):
        with tempfile.TemporaryDirectory() as folder:
            manifest = Path(folder) / "manifest.json"
            manifest.write_bytes(b"[]")
            manifest_hash = hashlib.sha256(manifest.read_bytes()).hexdigest()
            arguments = SimpleNamespace(worker="baseline", output=Path(folder) / "result.json",
                                        manifest=manifest, samples=Path(folder), repeats=1)
            def changed_truth(*values):
                manifest.write_bytes(b"[1]")
                return {"sample": "A", "seconds": 1, "checks": {"total": True}}
            engine = SimpleNamespace(token_sha256="token-hash", token_count=1)
            with patch("tools.benchmark_cpu.FingerprintedEngine", return_value=engine), \
                    patch("tools.benchmark_cpu.load_dictionary", return_value={}), \
                    patch("tools.benchmark_cpu.source_hashes", return_value={"code": "unchanged"}), \
                    patch("tools.benchmark_cpu.inspect_case", side_effect=changed_truth):
                exit_code = run_worker(arguments, [{"id": "A"}], manifest_hash)
            report = json.loads(arguments.output.read_text(encoding="utf-8"))
        self.assertEqual(1, exit_code)
        self.assertEqual("source-changed", report["status"])
        self.assertEqual("ManifestChangedDuringBenchmark", report["errorType"])
        self.assertEqual(manifest_hash, report["manifestSha256"])
        self.assertEqual(1, len(report["observations"]))
