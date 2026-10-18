"""Unit checks for argument routing and honest sweep reporting; no application acceptance claim."""

import importlib.util
import os
from pathlib import Path
import subprocess
import tempfile
import unittest
from unittest.mock import patch

ROOT = Path(__file__).resolve().parents[2]
SPEC = importlib.util.spec_from_file_location("task_sweep", ROOT / "ops/ci/run-task-sweep.py")
SWEEP = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(SWEEP)


class OperationalEntrypointsTest(unittest.TestCase):
    def test_maintenance_rejects_unknown_duplicate_missing_and_old_administration_commands(self):
        for arguments in ([], ["vacuum"], ["--surprise"], ["--service", "core"],
                          ["--service", "core", "--service", "media", "--schedule", "hourly"],
                          ["--service", "foreign", "--schedule", "daily"]):
            result = subprocess.run(["bash", str(ROOT / "ops/bin/maintenance.sh"), *arguments],
                                    capture_output=True, check=False)
            self.assertEqual(2, result.returncode, arguments)

    def test_maintenance_wrapper_routes_only_selected_owner_and_fixed_packaged_cli(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            docker = root / "docker"
            docker.write_text("#!/usr/bin/env bash\nset -eu\n"
                              "if [[ ${*: -1} == version ]]; then exit 0; fi\n"
                              "printf '%s\\n' \"$@\" > \"$CAPTURE\"\n")
            docker.chmod(0o700)
            capture = root / "arguments"
            environment = {**os.environ, "PATH": f"{root}:{os.environ['PATH']}",
                           "SPOOLIN_INSTALLATION": "maintenance-unit", "CAPTURE": str(capture)}
            environment.pop("SPOOLIN_SKIP_COMPOSE", None)
            for owner in ("core", "catalog", "media"):
                arguments = ["--service", owner, "--schedule", "hourly"]
                result = subprocess.run(["bash", str(ROOT / "ops/bin/maintenance.sh"), *arguments],
                                        env=environment, capture_output=True, check=False)
                self.assertEqual(0, result.returncode)
                routed = capture.read_text().splitlines()
                self.assertIn(f"--owner={owner}", routed)
                self.assertIn("--mode=inspect", routed)
                self.assertIn("--schedule=hourly", routed)
                self.assertIn("-Dloader.main=com.spoolin.support.maintenance.RetentionMaintenanceCli", routed)
                self.assertNotIn("psql", routed)

    def test_sweep_requires_an_identified_hosted_linux_runner(self):
        self.assertFalse(SWEEP.hosted({}))
        self.assertFalse(SWEEP.hosted({"GITHUB_ACTIONS": "true", "RUNNER_ENVIRONMENT": "self-hosted", "RUNNER_OS": "Linux"}))
        self.assertTrue(SWEEP.hosted({"GITHUB_ACTIONS": "true", "RUNNER_ENVIRONMENT": "github-hosted", "RUNNER_OS": "Linux"}))

    def test_sweep_does_not_replace_missing_selection_with_nearby_cases(self):
        mapping = {"tasks": {task: {"unit": [{"id": task}], "integration": []} for task in SWEEP.TASKS}}
        selected, unavailable = SWEEP.selections(mapping)
        self.assertEqual(21, len(selected))
        self.assertEqual(21, len(unavailable))
        self.assertEqual(set(SWEEP.TASKS), {item["task"] for item in unavailable})
        self.assertTrue(all(item["level"] == "integration" for item in unavailable))

    def test_unavailable_sweep_writes_nonzero_evidence_and_never_dispatches(self):
        with patch.object(SWEEP, "hosted", return_value=False), patch.object(SWEEP, "selector") as dispatch:
            with patch.object(SWEEP, "write_report") as report:
                self.assertEqual(2, SWEEP.main([]))
                dispatch.assert_not_called()
                self.assertEqual(2, report.call_args.args[3])
                self.assertEqual([], report.call_args.args[2])
                self.assertTrue(report.call_args.args[1])

    def test_sweep_propagates_actual_selected_suite_failure_without_running_nearby_suite(self):
        entry = {"task": "T007", "level": "unit", "entry": {"id": "selected", "report": "unit.json"}}
        with patch.object(SWEEP, "hosted", return_value=True), patch.object(SWEEP, "selections", return_value=([entry, entry], [])):
            with patch.object(SWEEP, "selector") as selector, patch.object(SWEEP, "write_report") as report:
                selector.return_value.execute.return_value = (1, {"selected": 1, "executed": 1, "failed": 1})
                with patch.object(SWEEP, "report_hash", return_value="test-report-hash"):
                    self.assertEqual(1, SWEEP.main([]))
                selector.return_value.execute.assert_called_once_with(entry["entry"])
                self.assertEqual(1, report.call_args.args[3])
                self.assertEqual(1, report.call_args.args[2][0]["failed"])


if __name__ == "__main__":
    unittest.main()
