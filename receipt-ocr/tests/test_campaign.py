from copy import deepcopy
import hashlib
from pathlib import Path
import tempfile
import unittest
from tools.validate_capture_campaign import compare_result, inspect_case


class CampaignTests(unittest.TestCase):
    def result(self, **values):
        return {"requiresConfirmation": True, "total": None, "drinks": [], "warnings": [], **values}

    def test_missing_field_is_not_equal_to_labelled_null(self):
        case = {"drinks": [{"quantity": None}]}
        self.assertFalse(compare_result(case, self.result(drinks=[{}]))["drink0.quantity"])
        self.assertTrue(compare_result(case, self.result(drinks=[{"quantity": None}]))["drink0.quantity"])

    def test_extra_and_missing_items_fail_count_without_changing_truth(self):
        case = {"drinks": [{"quantity": 1}, {"quantity": 2}]}
        original = deepcopy(case)
        missing = compare_result(case, self.result(drinks=[{"quantity": 1}]))
        self.assertFalse(missing["drinkCount"])
        self.assertFalse(missing["drink1.quantity"])
        extra = compare_result(case, self.result(drinks=[{"quantity": 1}, {"quantity": 2}, {"quantity": 3}]))
        self.assertFalse(extra["drinkCount"])
        self.assertEqual(original, case)

    def test_unlabelled_fields_are_not_reported_as_full_accuracy(self):
        checks = compare_result({"drinks": [{"quantity": 2}]}, self.result(drinks=[{"quantity": 2}]))
        self.assertTrue(all(checks.values()))
        self.assertNotIn("drink0.name", checks)
        self.assertNotIn("total", checks)

    def test_blurred_prices_control_rejects_any_invented_money(self):
        case = {"noInventedPrices": True}
        partial = self.result(drinks=[{"unitPrice": None, "lineAmount": None}])
        self.assertTrue(compare_result(case, partial)["noInventedPrices"])
        for result in (self.result(total=100), self.result(drinks=[{"unitPrice": 100, "lineAmount": None}]),
                       self.result(drinks=[{"unitPrice": None, "lineAmount": 100}])):
            self.assertFalse(compare_result(case, result)["noInventedPrices"])

    def test_changed_original_is_recorded_as_failure_before_recognition(self):
        class Engine:
            def recognize(self, image):
                raise AssertionError("changed source must not reach OCR")
        with tempfile.TemporaryDirectory() as folder:
            samples = Path(folder)
            (samples / "changed.jpg").write_bytes(b"changed source")
            case = {"id": "changed", "cohort": "new", "family": "one", "image": "changed.jpg",
                    "sha256": hashlib.sha256(b"labelled source").hexdigest()}
            record = inspect_case(case, samples, None, Engine())
            self.assertEqual("ValueError", record["errorType"])
            self.assertNotIn("checks", record)
            self.assertNotIn("result", record)

    def test_missing_file_stays_a_failed_observation_without_exception_message(self):
        with tempfile.TemporaryDirectory() as folder:
            case = {"id": "missing", "cohort": "new", "family": "one", "image": "missing.jpg", "sha256": "unused"}
            record = inspect_case(case, Path(folder), None, type("Engine", (), {})())
            self.assertEqual("missing", record["sample"])
            self.assertEqual("FileNotFoundError", record["errorType"])
            self.assertNotIn("checks", record)
            self.assertNotIn("errorMessage", record)
