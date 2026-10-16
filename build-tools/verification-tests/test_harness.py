import importlib.util
import io
import json
import os
from pathlib import Path
import stat
import shutil
import subprocess
import sys
import tempfile
import time
import unittest
import xml.etree.ElementTree as ET
from unittest.mock import patch
from uuid import uuid4

from fixture import SetupError, TestInstallation, preserve_failure, validate_namespace
from lock_dependencies import relocate_local_catalog
import lock_dependencies
from run_owner_integration import junit_counts
from unittest_reports import ObservedResult, test_list, write_junit
from test_scan_metadata import ScanMetadataTests

ROOT = Path(__file__).resolve().parents[2]
spec = importlib.util.spec_from_file_location("task_selector", Path(__file__).with_name("task_selector.py"))
selector = importlib.util.module_from_spec(spec)
spec.loader.exec_module(selector)


class IsolationTests(unittest.TestCase):
    def test_normal_installation_rejected_before_resources(self):
        with tempfile.TemporaryDirectory() as directory:
            sentinel = Path(directory) / "normal-sentinel"
            sentinel.write_text("untouched")
            with patch("fixture.tempfile.mkdtemp") as create_directory:
                with self.assertRaises(SetupError):
                    TestInstallation.create("spoolin")
                create_directory.assert_not_called()
            self.assertEqual(sentinel.read_text(), "untouched")
            self.assertEqual(list(Path(directory).iterdir()), [sentinel])

    def test_namespace_requires_lowercase_uuid4(self):
        values = ["atdd-no", "atdd-00000000-0000-0000-0000-000000000000", f"atdd-{str(uuid4()).upper()}"]
        for value in values:
            with self.subTest(value=value), self.assertRaises(SetupError):
                validate_namespace(value)

    def test_each_fixture_has_independent_resources_and_secure_tokens(self):
        first, second = TestInstallation.create(), TestInstallation.create()
        try:
            self.assertNotEqual(first.namespace, second.namespace)
            self.assertNotEqual(first.root, second.root)
            self.assertTrue(set(first.ports).isdisjoint(second.ports))
            for path in first.root.joinpath("private").iterdir():
                self.assertEqual(stat.S_IMODE(path.stat().st_mode), 0o600)
            self.assertEqual(first.configuration()["clock"], "2026-10-12T03:00:00Z")
            self.assertEqual(first.configuration()["databases"], ["spoolin_core", "spoolin_catalog", "spoolin_media"])
        finally:
            first.reset()
            second.reset()
        self.assertFalse(first.root.exists())
        self.assertFalse(second.root.exists())

    def test_real_services_remain_explicitly_unavailable(self):
        fixture = TestInstallation.create()
        try:
            with self.assertRaisesRegex(SetupError, "pending"):
                fixture.start_services()
        finally:
            fixture.reset()

    def test_actor_and_catalog_interfaces_require_their_real_owner_fixtures(self):
        fixture = TestInstallation.create()
        try:
            for name, method in (("F-ACTORS", fixture.actors), ("F-CATALOG", fixture.catalog)):
                with self.subTest(fixture=name), self.assertRaisesRegex(SetupError, name):
                    method()
            self.assertTrue(fixture.root.joinpath("fixture.json").exists())
        finally:
            fixture.reset()

    def test_cleanup_refuses_other_namespace(self):
        fixture = TestInstallation.create()
        try:
            fixture.root.joinpath("fixture.json").write_text(json.dumps({"namespace": f"atdd-{uuid4()}"}))
            with self.assertRaisesRegex(SetupError, "ownership"):
                fixture.reset()
            self.assertTrue(fixture.root.exists())
        finally:
            fixture.root.joinpath("fixture.json").write_text(json.dumps({"namespace": fixture.namespace}))
            fixture.reset()

    def test_original_failure_survives_cleanup_failure(self):
        def failed_cleanup():
            raise OSError("synthetic cleanup failure")
        self.assertEqual(preserve_failure(1, failed_cleanup), 1)
        self.assertEqual(preserve_failure(2, failed_cleanup), 2)
        self.assertEqual(preserve_failure(0, failed_cleanup), 2)


class SelectionTests(unittest.TestCase):
    def wrapper_fixture(self, root):
        paths = ["ops/bin/test-unit.sh", "ops/bin/test-integration.sh",
                 "build-tools/verification-tests/task_selector.py"]
        for name in paths:
            target = root / name
            target.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(ROOT / name, target)
        pending = "Isolated pending suite"
        mapping = {"tasks": {
            "T001": {"unit": [], "integration": [], "pending_reason": pending},
            "T002": {"unit": [{"id": "fixture-contracts", "argv": [sys.executable, "tests/contracts/run.py"],
                                  "report": ".ai/evidence/t002-contract-tests.json", "timeout": 5,
                                  "required_paths": ["tests/contracts/run.py"]}], "pending_reason": pending},
            "T003": {"integration": [{"id": "fixture-owner", "argv": [sys.executable, "missing.py"],
                                         "report": ".ai/evidence/t003-owner-tests.json", "timeout": 5,
                                         "required_paths": ["services/core/bootstrap/**/*.kt"]}], "pending_reason": pending},
        }}
        root.joinpath("build-tools/verification-map.json").write_text(json.dumps(mapping))

    def test_report_paths_are_specific_to_selected_task(self):
        self.assertEqual(selector.report_path("unit", ["T001"]).name, "t001-tests.json")
        self.assertEqual(selector.report_path("unit", ["T002"]).name, "t002-tests.json")
        self.assertEqual(selector.report_path("integration", ["T003"]).name, "t003-tests.json")
        self.assertEqual(selector.report_path("unit", ["../../outside"]).name, "unit-selection-tests.json")

    def test_missing_required_sources_refuse_execution(self):
        with tempfile.TemporaryDirectory() as directory:
            entry = {"argv": [sys.executable, "-c", "raise RuntimeError('must not execute')"],
                     "report": "report.json", "required_paths": ["owner/**/*.kt"], "timeout": 5}
            with patch.object(selector.subprocess, "run") as command:
                self.assertEqual(selector.execute(entry, Path(directory))[0], 2)
                command.assert_not_called()

    def test_unknown_empty_and_pending_selections_are_setup_errors(self):
        mapping = {"tasks": {"T001": {"unit": [], "pending_reason": "unavailable"}}}
        for name in ("T999", "T001"):
            with self.subTest(task=name), self.assertRaises(ValueError):
                selector.selected_entries(mapping, "unit", [name])
        with self.assertRaisesRegex(ValueError, "Empty"):
            selector.selected_entries({"tasks": {}}, "unit", None)

    def test_only_selected_level_and_task_are_returned(self):
        mapping = {"tasks": {
            "T001": {"unit": ["unit"], "integration": ["integration"]},
            "T002": {"unit": ["other-unit"]},
        }}
        self.assertEqual(selector.selected_entries(mapping, "unit", ["T001"]), ["unit"])
        self.assertEqual(selector.selected_entries(mapping, "integration", ["T001"]), ["integration"])

    def test_failing_synthetic_test_returns_one_with_diagnostics(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            script = "import json,pathlib;pathlib.Path('report.json').write_text(json.dumps({'selected':1,'executed':1,'failed':1}));raise SystemExit(1)"
            entry = {"argv": [sys.executable, "-c", script], "report": "report.json", "timeout": 5}
            code, counts = selector.execute(entry, root)
            self.assertEqual(code, 1)
            self.assertEqual(counts, {"selected": 1, "executed": 1, "failed": 1})
            self.assertTrue(root.joinpath("report.json").is_file())

    def test_missing_command_and_empty_report_return_two(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            entry = {"argv": [str(root / "missing")], "report": "report.json", "timeout": 5}
            self.assertEqual(selector.execute(entry, root)[0], 2)
            entry["argv"] = [sys.executable, "-c", "pass"]
            self.assertEqual(selector.execute(entry, root)[0], 2)

    def test_false_success_with_failed_report_is_rejected(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            script = "import json,pathlib;pathlib.Path('report.json').write_text(json.dumps({'selected':1,'executed':1,'failed':1}))"
            entry = {"argv": [sys.executable, "-c", script], "report": "report.json", "timeout": 5}
            self.assertEqual(selector.execute(entry, root)[0], 1)

    def test_malformed_and_unexecuted_reports_return_setup_error(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            for content in ("invalid", '{"selected":1,"executed":0,"failed":0}', '{"selected":true}'):
                script = f"import pathlib;pathlib.Path('report.json').write_text({content!r})"
                entry = {"argv": [sys.executable, "-c", script], "report": "report.json", "timeout": 5}
                self.assertEqual(selector.execute(entry, root)[0], 2)

    def test_public_wrappers_reject_invalid_and_unavailable_selection(self):
        cases = [("unit", "T999"), ("unit", "T002"), ("integration", "T001"), ("integration", "T003")]
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            self.wrapper_fixture(root)
            for level, task in cases:
                result = subprocess.run(["bash", f"ops/bin/test-{level}.sh", "--task", task], cwd=root, capture_output=True)
                self.assertEqual(result.returncode, 2, result.stderr.decode())
                self.assertIn(b"Setup unavailable", result.stderr)

    def test_downstream_activation_does_not_change_pending_fixture_selection(self):
        with tempfile.TemporaryDirectory() as active, tempfile.TemporaryDirectory() as pending:
            self.wrapper_fixture(Path(active))
            self.wrapper_fixture(Path(pending))
            program = Path(active) / "tests/contracts/run.py"
            program.parent.mkdir(parents=True)
            program.write_text("import json,pathlib,unittest\nclass Probe(unittest.TestCase):\n def test_probe(self): self.assertTrue(True)\nsuite=unittest.defaultTestLoader.loadTestsFromTestCase(Probe)\nselected=suite.countTestCases()\nresult=unittest.TextTestRunner().run(suite)\np=pathlib.Path('.ai/evidence/t002-contract-tests.json')\np.parent.mkdir(parents=True,exist_ok=True)\np.write_text(json.dumps({'selected':selected,'executed':result.testsRun,'failed':len(result.failures)+len(result.errors)}))\n")
            command = ["bash", "ops/bin/test-unit.sh", "--task", "T002"]
            self.assertEqual(subprocess.run(command, cwd=active, capture_output=True).returncode, 0)
            self.assertEqual(subprocess.run(command, cwd=pending, capture_output=True).returncode, 2)

    def test_invalid_integration_selection_preserves_retained_unit_report(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            self.wrapper_fixture(root)
            report = root / ".ai/evidence/t001-tests.json"
            report.parent.mkdir(parents=True)
            retained = json.dumps({"selected": 1, "executed": 1, "failed": 0, "level": "unit"})
            report.write_text(retained)
            result = subprocess.run(["bash", "ops/bin/test-integration.sh", "--task", "T001"], cwd=root, capture_output=True)
            self.assertEqual(result.returncode, 2)
            self.assertEqual(report.read_text(), retained)

    def test_absent_empty_and_unexecuted_reports_are_setup_errors_for_any_child_exit(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            for content in (None, {"selected": 0, "executed": 0, "failed": 0}, {"selected": 1, "executed": 0, "failed": 0}):
                for ending in ("raise SystemExit(3)", "raise SystemExit(5)", "import os,signal;os.kill(os.getpid(),signal.SIGTERM)"):
                    write = "" if content is None else f"import json,pathlib;pathlib.Path('report.json').write_text(json.dumps({content!r}));"
                    entry = {"argv": [sys.executable, "-c", write + ending], "report": "report.json", "timeout": 5}
                    with self.subTest(report=content, ending=ending):
                        self.assertEqual(selector.execute(entry, root)[0], 2)

    def test_executed_failures_normalize_to_one_for_any_child_exit(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            for ending in ("raise SystemExit(0)", "raise SystemExit(2)", "raise SystemExit(3)", "raise SystemExit(5)", "import os,signal;os.kill(os.getpid(),signal.SIGTERM)"):
                script = "import json,pathlib;pathlib.Path('report.json').write_text(json.dumps({'selected':1,'executed':1,'failed':1}));" + ending
                entry = {"argv": [sys.executable, "-c", script], "report": "report.json", "timeout": 5}
                with self.subTest(ending=ending):
                    self.assertEqual(selector.execute(entry, root)[0], 1)

    def test_make_passes_task_argument_without_shell_evaluation(self):
        with tempfile.TemporaryDirectory() as directory:
            sentinel = Path(directory) / "must-not-exist"
            result = subprocess.run(["make", "test-unit", f"TASK=T999; touch {sentinel}"], cwd=ROOT, capture_output=True)
            self.assertEqual(result.returncode, 2)
            self.assertFalse(sentinel.exists())


class JunitAdapterTests(unittest.TestCase):
    def test_unittest_export_records_actual_pass_failure_and_skip(self):
        class Outcomes(unittest.TestCase):
            def test_pass(self):
                self.assertTrue(True)

            def test_failure(self):
                self.fail("Intentional exporter fixture failure")

            @unittest.skip("Intentional exporter fixture skip")
            def test_skip(self):
                self.fail("Skipped method must not execute")

        suite = unittest.defaultTestLoader.loadTestsFromTestCase(Outcomes)
        tests = test_list(suite)
        result = unittest.TextTestRunner(stream=io.StringIO(), resultclass=ObservedResult).run(suite)
        with tempfile.TemporaryDirectory() as directory:
            report = Path(directory) / "unittest.xml"
            self.assertEqual(write_junit(report, tests, result), {"selected": 3, "executed": 2, "failed": 1})
            xml = ET.parse(report).getroot()
            self.assertEqual(len(xml.findall("testcase")), 3)
            self.assertEqual(len(xml.findall("testcase/failure")), 1)
            self.assertEqual(len(xml.findall("testcase/skipped")), 1)

    def test_counts_come_from_cases_and_preserve_skipped_and_failed_results(self):
        with tempfile.TemporaryDirectory() as directory:
            report = Path(directory) / "TEST-fixture.xml"
            start = time.time_ns()
            report.write_text('<testsuite tests="3"><testcase name="pass"/><testcase name="skip"><skipped/></testcase><testcase name="fail"><failure/></testcase></testsuite>')
            self.assertEqual(junit_counts([report], start), {"selected": 3, "executed": 2, "failed": 1})

    def test_empty_claimed_counts_do_not_establish_test_execution(self):
        with tempfile.TemporaryDirectory() as directory:
            report = Path(directory) / "TEST-fixture.xml"
            report.write_text('<testsuite tests="99"/>')
            with self.assertRaisesRegex(SetupError, "empty"):
                junit_counts([report], 0)

    def test_stale_and_missing_reports_cannot_pass(self):
        with self.assertRaisesRegex(SetupError, "Fresh"):
            junit_counts([], 0)
        with tempfile.TemporaryDirectory() as directory:
            report = Path(directory) / "TEST-fixture.xml"
            report.write_text('<testsuite><testcase name="pass"/></testsuite>')
            os.utime(report, ns=(1, 1))
            with self.assertRaisesRegex(SetupError, "Fresh"):
                junit_counts([report], time.time_ns())


class ProductionGenerationTests(unittest.TestCase):
    def prepare(self, directory):
        root = Path(directory)
        script = root / "build-tools/verification-tests/production_contracts.mjs"
        script.parent.mkdir(parents=True)
        shutil.copy2(ROOT / "build-tools/verification-tests/production_contracts.mjs", script)
        return root, script

    def test_absent_production_inputs_are_optional_until_explicitly_required(self):
        with tempfile.TemporaryDirectory() as directory:
            root, script = self.prepare(directory)
            optional = subprocess.run(["node", str(script)], capture_output=True)
            required = subprocess.run(["node", str(script), "--required"], capture_output=True)
            self.assertEqual(optional.returncode, 0)
            self.assertEqual(required.returncode, 2)
            self.assertIn(b"Setup unavailable", required.stderr)

    def test_production_schema_requires_generator_and_preserves_its_failure(self):
        with tempfile.TemporaryDirectory() as directory:
            root, script = self.prepare(directory)
            schema = root / "contracts/graphql/schema.graphql"
            schema.parent.mkdir(parents=True)
            schema.write_text("type Query { value: String }")
            self.assertEqual(subprocess.run(["node", str(script)], capture_output=True).returncode, 2)
            generator = root / "build-tools/generate-contracts.mjs"
            generator.write_text("process.exitCode = 1;")
            self.assertEqual(subprocess.run(["node", str(script)], capture_output=True).returncode, 1)
            generator.write_text("process.exitCode = 0;")
            self.assertEqual(subprocess.run(["node", str(script)], capture_output=True).returncode, 0)


class LockMaintenanceTests(unittest.TestCase):
    def test_failed_resolution_restores_the_previous_integrity_manifest(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            metadata = root / "gradle/verification-metadata.xml"
            metadata.parent.mkdir()
            previous = b"<verification-metadata/>"
            metadata.write_bytes(previous)
            with patch.object(lock_dependencies, "ROOT", root), patch.object(lock_dependencies.subprocess, "run", return_value=subprocess.CompletedProcess([], 1)):
                self.assertEqual(lock_dependencies.main(), 1)
            self.assertEqual(metadata.read_bytes(), previous)
            archives = list(root.glob(".ai/evidence/lock-maintenance/*.xml"))
            self.assertEqual(len(archives), 1)
            self.assertEqual(archives[0].read_bytes(), previous)

    def test_empty_local_catalog_inventory_moves_to_owned_path(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            source = root / "settings-gradle.lockfile"
            source.write_text("# Local catalog\nempty=incomingCatalogForLibs0\n")
            relocate_local_catalog(root)
            self.assertFalse(source.exists())
            self.assertEqual(root.joinpath("gradle/locks/settings/gradle.lockfile").read_text(), "# Local catalog\nempty=incomingCatalogForLibs0\n")

    def test_external_settings_coordinates_remain_visible_for_review(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            source = root / "settings-gradle.lockfile"
            source.write_text("external:module:1.0=classpath\n")
            with self.assertRaisesRegex(ValueError, "Unexpected external"):
                relocate_local_catalog(root)
            self.assertEqual(source.read_text(), "external:module:1.0=classpath\n")
            self.assertFalse(root.joinpath("gradle/locks/settings/gradle.lockfile").exists())


if __name__ == "__main__":
    suite = unittest.defaultTestLoader.loadTestsFromModule(sys.modules[__name__])
    tests = test_list(suite)
    result = unittest.TextTestRunner(verbosity=2, resultclass=ObservedResult).run(suite)
    counts = write_junit(ROOT / ".ai/evidence/t001-harness.xml", tests, result)
    report = ROOT / ".ai/evidence/t001-harness.json"
    report.parent.mkdir(parents=True, exist_ok=True)
    report.write_text(json.dumps({**counts, "cases": ["T001-A2", "T001-A3"]}, indent=2) + "\n")
    sys.exit(2 if counts["selected"] != counts["executed"] else 1 if counts["failed"] else 0)
