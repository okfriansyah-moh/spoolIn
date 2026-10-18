"""Run report producers in isolation and preserve every reused legacy report."""
import importlib
import io
import json
from pathlib import Path
import sys
import tempfile
import unittest
from unittest.mock import patch

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / 'build-tools/verification-tests'))
account = importlib.import_module('run_account_protection')
policy = importlib.import_module('run_policy')
receipts = importlib.import_module('run_receipts')
selector = importlib.import_module('task_selector')
inventory = importlib.import_module('inventory')


class ReportPreservationTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.old = {}
        for number in range(1, 11):
            for level in ['unit', 'integration']:
                path = self.root / f'.ai/evidence/t{number:03}-{level}-results.json'
                path.parent.mkdir(parents=True, exist_ok=True)
                self.old[path] = f'Historical task {number}, {level}'.encode()
                path.write_bytes(self.old[path])

    def outcome(self, *args, **kwargs):
        return {'exit_code': 0}, [{'executed': True, 'failed': False}], []

    def check_reports(self, module, task, stubs):
        mapping = json.loads((ROOT / 'build-tools/verification-map.json').read_text())
        levels = [name for name in ['unit', 'integration']
                  if any(suite.get('argv', [None, None])[1:2] == [f'build-tools/verification-tests/{module.__name__}.py']
                         for suite in mapping['tasks'][task].get(name, []))]
        self.assertTrue(levels, f"No mapped report producer for {task}: {module.__name__}")
        for level in levels:
            with self.subTest(task=task, level=level), patch.object(module, 'ROOT', self.root), patch.object(
                sys, 'argv', ['runner', '--task', task, '--level', level],
            ), patch.multiple(module, **stubs):
                self.assertEqual(module.main(), 0)
            suite = next(suite for suite in mapping['tasks'][task][level]
                         if suite.get('argv', [])[1:2] == [f'build-tools/verification-tests/{module.__name__}.py'])
            report = self.root / suite['report']
            self.assertEqual(json.loads(report.read_text())['exit_code'], 0)
            self.assertIn('numbering-v1', report.parts)
        for path, original in self.old.items():
            self.assertEqual(path.read_bytes(), original)

    def test_account_report_producer_and_map_preserve_reused_id_reports(self):
        invoke = lambda *args: (0, 0, self.root, {'exit_code': 0})
        results = lambda *args: ({'selected': 1, 'executed': 1, 'failed': 0}, [], [])
        self.check_reports(account, 'T008', {'invoke': invoke, 'results': results})

    def test_policy_browser_report_producer_and_map_preserve_reused_id_reports(self):
        self.check_reports(policy, 'T010', {'backend': self.outcome, 'frontend': self.outcome, 'browser': self.outcome})

    def test_receipt_report_producer_and_map_preserve_reused_id_reports(self):
        with patch.object(policy, 'backend', self.outcome):
            self.check_reports(receipts, 'T007', {'owner': self.outcome})

    def test_policy_rules_report_matches_current_task_consumer(self):
        self.check_reports(policy, 'T006', {'backend': self.outcome, 'frontend': self.outcome})

    def test_guard_report_matches_current_task_consumer(self):
        with patch.object(policy, 'backend', self.outcome):
            self.check_reports(receipts, 'T009', {'owner': self.outcome})


    def test_inventory_initializes_fresh_numbering_namespace(self):
        (self.root / 'package-lock.json').write_text(json.dumps({'packages': {}}))
        with patch.object(inventory, 'ROOT', self.root), patch('sys.stdout', new=io.StringIO()):
            inventory.main()
        report = self.root / '.ai/evidence/numbering-v1/t001-dependency-inventory.json'
        self.assertEqual(json.loads(report.read_text())['npm'], [])
        for path, original in self.old.items():
            self.assertEqual(path.read_bytes(), original)


    def test_selector_report_is_namespaced(self):
        with patch.object(selector, 'ROOT', self.root):
            report = selector.report_path('unit', ['T008'])
        self.assertEqual(report.parent, self.root / '.ai/evidence/numbering-v1')

    def test_all_suite_reports_are_namespaced(self):
        mapping = json.loads((ROOT / 'build-tools/verification-map.json').read_text())
        for task in mapping['tasks'].values():
            for level in ['unit', 'integration']:
                for suite in task.get(level, []):
                    if suite.get('kind') == 'blocked':
                        self.assertTrue(suite['reason'])
                        self.assertTrue(suite['proof_boundary'])
                        self.assertNotIn('report', suite)
                        continue
                    if suite['id'] == 't045-measurement-rules':
                        self.assertEqual(suite['report'], 'artifacts/capacity/rules.json')
                    else:
                        self.assertTrue(suite['report'].startswith('.ai/evidence/numbering-v1/'))


if __name__ == '__main__':
    unittest.main()
