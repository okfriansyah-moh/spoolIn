#!/usr/bin/env python3
"""Remote-only orchestration regressions; these never count as application runtime evidence."""
import importlib.util
import json
import os
from pathlib import Path
import unittest
from unittest.mock import patch

import operator_fixtures
from operator_runtime import OperatorRuntime, require_owned_runner

spec = importlib.util.spec_from_file_location("operator_commands", Path(__file__).with_name("verify-operator-commands.py"))
commands = importlib.util.module_from_spec(spec)
spec.loader.exec_module(commands)


class OperatorRuntimeTest(unittest.TestCase):
    def test_failed_phase_does_not_hide_other_independent_operational_phases(self):
        observed = {}
        runtime = object.__new__(OperatorRuntime)
        runtime.compose = ["controlled-test-command"]
        runtime.record = observed.__setitem__
        runtime.migrations = lambda: (_ for _ in ()).throw(RuntimeError("private canary"))
        runtime.recovery = lambda: {"result": "PASS"}
        runtime.maintenance = lambda: {"result": "PASS"}
        runtime.degraded_status = lambda: None
        with patch("operator_runtime.subprocess.run") as run:
            run.return_value.returncode = 0
            runtime.run()
        self.assertEqual("FAIL", observed["migration"]["result"])
        self.assertEqual("PASS", observed["recovery"]["result"])
        self.assertEqual("PASS", observed["maintenance"]["result"])
        self.assertNotIn("private canary", json.dumps(observed))

    def test_failed_command_beats_pending_tunnel_and_empty_selection_never_passes(self):
        phases = {name: {"result": "PASS"} for name in ("connector", "migration", "recovery", "maintenance")}
        phases["tunnel"] = {"result": "PENDING"}
        self.assertEqual(78, commands.report_exit({"phases": phases}))
        phases["recovery"] = {"result": "FAIL"}
        self.assertEqual(1, commands.report_exit({"phases": phases}))
        self.assertEqual(1, commands.report_exit({"phases": {}}))
        self.assertEqual(1, commands.report_exit({"phases": {"finalState": {"result": "OBSERVED"}}}))

    def test_wrong_installation_cannot_reach_destructive_setup(self):
        with patch.dict(os.environ, {"GITHUB_ACTIONS": "true", "RUNNER_ENVIRONMENT": "github-hosted", "RUNNER_OS": "Linux",
                                     "GITHUB_RUN_ID": "123", "GITHUB_RUN_ATTEMPT": "1", "SPOOLIN_INSTALLATION": "normal-installation"}):
            with self.assertRaises(RuntimeError):
                require_owned_runner()

    def test_receipt_batch_is_older_than_other_terminal_setup_and_ids_are_distinct(self):
        entries, expired, sql = operator_fixtures.retention_fixture()
        self.assertEqual(101, len(set(expired)))
        self.assertEqual(4, len(set(entries.values())))
        self.assertEqual(101, sql.count("INTERVAL '200 days'"))
        self.assertIn("'EXHAUSTED'", sql)
        self.assertIn("'UNCERTAIN'", sql)
        self.assertNotIn("DELETE", sql)

    def test_notification_setup_is_known_failed_not_provider_success(self):
        operation, identifier, sql = operator_fixtures.recovery_fixture()
        self.assertIn(operation, sql)
        self.assertIn(identifier, sql)
        self.assertIn("'FAILED'", sql)
        self.assertIn("NOTIFICATION_CONNECT_FAILED", sql)
        self.assertNotIn("'ACCEPTED'", sql)
        self.assertNotIn("'DELIVERY_CONFIRMED'", sql)


if __name__ == "__main__":
    if (os.environ.get("GITHUB_ACTIONS") != "true" or os.environ.get("RUNNER_ENVIRONMENT") != "github-hosted"
            or os.environ.get("RUNNER_OS") != "Linux"):
        raise SystemExit("Requires GitHub-hosted Linux; local execution refused")
    unittest.main()
