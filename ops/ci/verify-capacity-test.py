#!/usr/bin/env python3
"""Remote-executable measurement-rule regressions; these do not substitute for capacity execution."""
import importlib.util
import json
import os
from pathlib import Path
import unittest
from unittest.mock import patch

from capacity_http import Response
from capacity_runtime import require_hosted
from capacity_workload import assert_scope, matching, nearest_rank

spec = importlib.util.spec_from_file_location("capacity_entry", Path(__file__).with_name("verify-capacity.py"))
entry = importlib.util.module_from_spec(spec)
spec.loader.exec_module(entry)


class CapacityEvidenceRulesTest(unittest.TestCase):
    def test_nearest_rank_is_independent_nonpooled_and_rejects_empty_samples(self):
        self.assertEqual(95, nearest_rank(list(reversed(range(1, 101)))))
        self.assertEqual(7, nearest_rank([2, 7]))
        with self.assertRaises(ValueError):
            nearest_rank([])
        with self.assertRaises(ValueError):
            nearest_rank([1], 0)

    def test_exact_budget_equality_failed_samples_and_missing_samples_never_pass(self):
        counts = {"offered": 5, "completed": 5, "http200": 5, **{"class:" + str(i): 1 for i in range(5)}}
        sample = {"durationNanos": 300000000, "outcome": "SUCCESS", "code": "OK"}
        self.assertEqual("FAIL", entry.budget([sample] * 5, counts, "reader", {})["result"])
        fast = {**sample, "durationNanos": 200000000}
        self.assertEqual("PASS", entry.budget([fast] * 5, counts, "reader", {})["result"])
        self.assertEqual("FAIL", entry.budget([fast] * 4, counts, "reader", {})["result"])
        self.assertEqual("FAIL", entry.budget([fast] * 5, {**counts, "throttled": 1}, "reader", {})["result"])
        with self.assertRaises(RuntimeError):
            entry.budget([], counts, "reader", {})

    def test_custom_event_selection_excludes_warmup_and_keeps_typed_domain_failures(self):
        def event(at, outcome="SUCCESS"):
            return {"type": "spoolin.CoreRequestTiming", "values": {"startTime": at, "operation": "titles",
                    "durationNanos": 42, "outcome": outcome, "code": "OK" if outcome == "SUCCESS" else "CONFLICT"}}
        window = {"started": "2026-10-18T01:00:00Z", "drained": "2026-10-18T01:05:01Z"}
        selected = entry.select_events([event("2026-10-18T00:59:59Z"),
                                       event("2026-10-18T01:01:00Z", "DOMAIN_REJECTED")], "reader", window)
        self.assertEqual(1, len(selected))
        self.assertEqual("DOMAIN_REJECTED", selected[0]["outcome"])
        self.assertEqual({"startTime", "operation", "durationNanos", "outcome", "code"}, set(selected[0]))
        with self.assertRaises(RuntimeError):
            entry.select_events([{**event("2026-10-18T01:01:00Z"), "type": "jdk.SystemProperties"}], "reader", window)

    def test_response_samples_exclude_messages_payloads_and_credentials(self):
        response = Response(200, {"errors": [{"message": "password=private", "extensions": {"code": "NOT_FOUND"}}]}, 12, 3, 8)
        value = response.safe_result()
        self.assertTrue(value["domainError"])
        self.assertEqual("NOT_FOUND", value["problem"])
        self.assertNotIn("private", json.dumps(value))
        self.assertNotIn("password", json.dumps(value))

    def test_literal_scope_oracle_preserves_genre_or_and_unknown_date_rules(self):
        row = {"stableUUID": "fixed", "kind": "MOVIE", "normalized": {"title": "literal%_title", "genreIds": ["one"],
                "release": {"year": 2000, "month": None, "day": None}}}
        self.assertEqual([row], matching([row], {"text": "%_", "genreIds": ["one", "two"]}))
        self.assertEqual([], matching([row], {"text": "missing"}))
        self.assertEqual([], matching([row], {"year": 2000, "month": 1}))
        self.assertFalse(assert_scope(Response(200, {"data": {"titles": {"nodes": []}}}, 1, 1, 1),
                                     {"root": "titles", "expectedIds": ["fixed"], "hasNextPage": False}))

    def test_runner_guard_rejects_mac_even_when_hosted_environment_is_forged(self):
        environment = {"GITHUB_ACTIONS": "true", "RUNNER_ENVIRONMENT": "github-hosted", "RUNNER_OS": "Linux"}
        with patch.dict(os.environ, environment, clear=True), patch("platform.system", return_value="Darwin"):
            with self.assertRaises(RuntimeError):
                require_hosted()


if __name__ == "__main__":
    require_hosted()
    suite = unittest.defaultTestLoader.loadTestsFromTestCase(CapacityEvidenceRulesTest)
    selected = suite.countTestCases()
    result = unittest.TextTestRunner(verbosity=2).run(suite)
    report = {"boundary": "measurement rule unit checks; no capacity execution claim", "selected": selected,
              "executed": result.testsRun, "failed": len(result.failures) + len(result.errors), "skipped": len(result.skipped)}
    destination = Path("artifacts/capacity/rules.json")
    destination.parent.mkdir(parents=True, exist_ok=True)
    destination.write_text(json.dumps(report, indent=2) + "\n")
    raise SystemExit(0 if selected > 0 and result.testsRun == selected and result.wasSuccessful() and not result.skipped else 1)
