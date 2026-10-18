#!/usr/bin/env python3
"""Remote-only parsing/redaction regressions; synthetic inputs do not prove application readiness."""
import importlib.util
import json
import os
from pathlib import Path
import unittest

spec = importlib.util.spec_from_file_location("readiness", Path(__file__).resolve().parents[1] / "bin/readiness.py")
readiness = importlib.util.module_from_spec(spec)
spec.loader.exec_module(readiness)


class ReadinessDiagnosticTest(unittest.TestCase):
    def test_compose_formats_have_identical_service_observations(self):
        rows = [{"Service": name, "State": "running", "Health": "healthy", "unrelated": "private-value"}
                for name in readiness.SERVICES]
        for encoded in (json.dumps(rows), "\n".join(json.dumps(row) for row in rows)):
            observed = readiness.container_observations(readiness.compose_rows(encoded))
            self.assertEqual(8, len(observed))
            self.assertTrue(all(row["result"] == "PASS" for row in observed))
            self.assertNotIn("private-value", json.dumps(observed))
        self.assertEqual([rows[0]], readiness.compose_rows(json.dumps(rows[0])))

    def test_missing_unhealthy_duplicate_and_stopped_services_fail(self):
        rows = [{"Service": name, "State": "running", "Health": "healthy"} for name in readiness.SERVICES]
        for broken in (rows[1:], rows + [rows[0]],
                       [dict(row, Health="unhealthy") if row["Service"] == "mcp" else row for row in rows],
                       [dict(row, State="exited") if row["Service"] == "core" else row for row in rows]):
            self.assertTrue(any(row["result"] == "FAIL" for row in readiness.container_observations(broken)))

    def test_health_observations_allowlist_fields_statuses_and_components(self):
        payload = {"status": "DOWN", "details": {"password": "never-publish"},
                   "components": {"schema": {"status": "DOWN", "details": "never-publish"},
                                  "private-value": {"status": "UP"}}}
        result = readiness.health_observation("core", json.dumps(payload), ("status", "UP"))
        self.assertEqual("FAIL", result["result"])
        self.assertEqual({"schema": "DOWN"}, result["components"])
        self.assertNotIn("never-publish", json.dumps(result))
        self.assertNotIn("private-value", json.dumps(result))
        self.assertEqual("unavailable", readiness.safe_status("never-publish"))

    def test_readiness_requires_exact_success_and_valid_json(self):
        for value in ("[]", "not-json", '{"status":"DOWN"}', '{"status":"up"}', '{}'):
            self.assertEqual("FAIL", readiness.health_observation("core", value, ("status", "UP"))["result"])
        self.assertEqual("PASS", readiness.health_observation("core", '{"status":"UP"}', ("status", "UP"))["result"])
        self.assertEqual("PASS", readiness.health_observation("grafana", '{"database":"ok"}', ("database", "ok"))["result"])


if __name__ == "__main__":
    if os.environ.get("GITHUB_ACTIONS") != "true" or os.environ.get("RUNNER_ENVIRONMENT") != "github-hosted":
        raise SystemExit("Requires a verified GitHub-hosted runner; local execution refused")
    unittest.main()
