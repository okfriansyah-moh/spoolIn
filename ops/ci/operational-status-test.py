#!/usr/bin/env python3
"""Remote-only projection regressions; real owner queries remain a separate runtime boundary."""
import importlib.util
import json
import os
from pathlib import Path
import unittest

spec = importlib.util.spec_from_file_location("operational_status", Path(__file__).resolve().parents[1] / "bin/operational_status.py")
status = importlib.util.module_from_spec(spec)
spec.loader.exec_module(status)


class OperationalStatusTest(unittest.TestCase):
    def test_database_recording_and_provider_acceptance_are_projected_as_distinct_states(self):
        value = {"work": [{"state": "DONE", "count": 3, "private": "never-publish"}],
                 "notifications": [{"mode": "database", "state": "RECORDED", "count": 1},
                                   {"mode": "slack", "state": "ACCEPTED", "count": 1},
                                   {"mode": "smtp", "state": "DELIVERY_CONFIRMED", "count": 1}],
                 "payload": "never-publish"}
        observed = status.owner_observation("core", value)
        self.assertEqual("PASS", observed["result"])
        self.assertEqual(["RECORDED", "ACCEPTED", "DELIVERY_CONFIRMED"],
                         [row["state"] for row in observed["notifications"]])
        self.assertNotIn("never-publish", json.dumps(observed))

    def test_exhausted_unknown_and_failed_are_degraded(self):
        base = {"work": [], "notifications": []}
        for state in ("UNKNOWN", "FAILED"):
            observed = status.owner_observation("core", dict(base, notifications=[{"mode": "slack", "state": state, "count": 1}]))
            self.assertEqual("FAIL", observed["result"])
        self.assertEqual("FAIL", status.owner_observation("catalog", dict(base, work=[{"state": "EXHAUSTED", "count": 1}]))["result"])
        with self.assertRaises(ValueError):
            status.owner_observation("media", dict(base, work=[{"state": "PRIVATE_VALUE", "count": 1}]))

    def test_seed_requires_exact_original_receipt_manifest_and_classification_counts(self):
        manifest = {"manifestVersion": "seed-v1", "snapshotSHA256": "a" * 64}
        row = {"state": "SUCCEEDED", "version": "seed-v1", "hash": "a" * 64, "movies": 100, "series": 100}
        self.assertEqual("PASS", status.seed_observation([row], manifest)["result"])
        self.assertEqual("PASS", status.seed_observation([row, dict(row, version="retained-old-version")], manifest)["result"])
        for rows in ([], [row, row], [dict(row, state="PENDING")], [dict(row, movies=200, series=0)],
                     [dict(row, hash="b" * 64)], [dict(row, version="seed-v2")]):
            self.assertEqual("FAIL", status.seed_observation(rows, manifest)["result"])

    def test_missing_runner_never_becomes_connected_or_accepted(self):
        observed = status.tunnel_observation()
        self.assertEqual("UNVERIFIED", observed["result"])
        self.assertIsNone(observed["enabled"])
        self.assertIsNone(observed["connected"])
        self.assertEqual("PENDING", observed["actualClientAcceptance"])


if __name__ == "__main__":
    if os.environ.get("GITHUB_ACTIONS") != "true" or os.environ.get("RUNNER_ENVIRONMENT") != "github-hosted":
        raise SystemExit("Requires a verified GitHub-hosted runner; local execution refused")
    unittest.main()
