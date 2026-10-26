"""Adapter behavior tests; no application suites or dashboard mutation."""
import importlib.util
import io
from contextlib import redirect_stdout
import json
from pathlib import Path
import tempfile
import sys
import unittest
from unittest.mock import patch

REPO = Path(__file__).resolve().parents[2]
CONFIG = json.loads((REPO / '.ai/config.json').read_text())
sys.path.insert(0, str(REPO / '.ai' / CONFIG['bundle']))
import agent_workflow.quality

SPEC = importlib.util.spec_from_file_location('movie_quality', Path(__file__).with_name('quality-proof.py'))
ADAPTER = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(ADAPTER)


class AdapterTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.output = self.root / '.local/unit-coverage'
        self.directory = self.output / 'app'
        self.directory.mkdir(parents=True)
        (self.root / 'src').mkdir()
        (self.root / 'src/app.ts').write_text('export const value = 1;\n')
        (self.root / 'config.json').write_text('{}')
        (self.root / 'docker.lock').write_text('image@sha256:' + 'a' * 64)
        self.contract = dict(id='app', inputs=['src/*.ts', 'config.json'], production=['src/*.ts'], roots=['src'],
                             toolchain_files=['docker.lock'], commands=[['make', 'coverage']],
                             unit_report={'format': 'lcov', 'reports': ['.local/unit-coverage/app/lcov.info']},
                             test_report={'format': 'vitest', 'glob': '.local/unit-coverage/app/tests.json'})
        self.registry = {'quality': {'components': [self.contract]}}
        for name, value in [('ROOT', self.root), ('OUTPUT', self.output)]:
            context = patch.object(ADAPTER, name, value)
            context.start()
            self.addCleanup(context.stop)
        with patch.object(ADAPTER.subprocess, 'check_output', return_value='revision\n'):
            ADAPTER.capture(self.registry)
        (self.directory / 'lcov.info').write_text('SF:src/app.ts\nDA:1,1\nend_of_record\n')
        (self.directory / 'tests.json').write_text(json.dumps({'numPassedTests': 1, 'numFailedTests': 0, 'success': True}))
        ADAPTER.finish(self.registry)
        self.captured = ADAPTER.read(self.output / 'producer-inputs.json')
        self.summary = {'execution': {'runtime': 'docker', 'images': ['image@sha256:' + 'a' * 64],
                                     'memory_bytes': 1024, 'workers': 1, 'tests_separate_from_analysis': True},
                        'components': [{'component': 'app', 'command': 0, 'status': 'passed'}]}
        receipt = ADAPTER.read(self.directory / 'producer.json')
        self.analysis = dict(classifications=[], classified_issue_ids=[], component='app', status='passed', executionStatus='SUCCESS', analysisId='analysis', gate='OK',
                             covered=8, total=10, conditions={'covered': 8, 'total': 10},
                             quality=dict(securityIssues=0, reliabilityIssues=0, duplicatedLines=0, duplicatedBlocks=0),
                             input_sha256=receipt['input_sha256'],
                             producer_sha256=ADAPTER.gate_selection.hash_paths(self.root, ['.local/unit-coverage/app/producer.json'])['.local/unit-coverage/app/producer.json'])
        for name in ('summary.json', 'sonar-results.json'):
            (self.output / name).write_text('{}')
        context = patch.object(ADAPTER.gate_reports, 'changed_lines', return_value={'src/app.ts': {1}})
        context.start()
        self.addCleanup(context.stop)

    def build(self):
        return ADAPTER.component_proof(self.contract, self.captured, self.summary, {'components': [self.analysis]}, 'base')

    def test_valid_adapter_proof_and_configuration_invalidation(self):
        proof = self.build()
        self.assertEqual(proof['unit']['lines'], {'covered': 1, 'total': 1})
        (self.root / 'config.json').write_text('{"changed":true}')
        with self.assertRaisesRegex(ValueError, 'changed after measurement'):
            self.build()

    def test_mutation_during_tests_names_changed_input(self):
        (self.root / 'config.json').write_text('{"changed":true}')
        with self.assertRaisesRegex(ValueError, 'config.json'):
            ADAPTER.finish(self.registry)

    def test_contract_inputs_include_maintained_contracts_without_build_outputs(self):
        registry = json.loads((REPO / '.ai/gates.json').read_text())
        tracked = ADAPTER.subprocess.check_output(
            ['git', 'ls-files', 'contracts'], cwd=REPO, text=True).splitlines()
        for component in registry['quality']['components']:
            inputs = agent_workflow.quality.snapshot(REPO, component['inputs'])
            self.assertTrue(set(tracked) <= set(inputs))
            self.assertFalse(any('/build/' in path for path in inputs if path.startswith('contracts/')))
        runner = (REPO / 'ops/unit-coverage/run.mjs').read_text()
        self.assertLess(runner.index('execute("production-inputs"'), runner.index('summary.fingerprint = fingerprint()'))
        self.assertLess(runner.index('summary.fingerprint = fingerprint()'), runner.index('execute("quality-inputs"'))
        self.assertLess(runner.index('execute("quality-inputs"'), runner.index('runComponent(component) : setup'))

    def test_failed_suite_and_unfinished_analysis_block(self):
        self.summary['components'][0]['command'] = 1
        with self.assertRaises(ValueError):
            self.build()
        self.summary['components'][0]['command'] = 0
        self.analysis['executionStatus'] = 'PENDING'
        with self.assertRaises(ValueError):
            self.build()

    def test_missing_measurement_receipt_blocks(self):
        (self.directory / 'producer.json').unlink()
        with self.assertRaises(OSError):
            self.build()

    def test_modified_raw_report_blocks(self):
        (self.directory / 'lcov.info').write_text('SF:src/app.ts\nDA:1,0\nend_of_record\n')
        with self.assertRaises(ValueError):
            self.build()


class BootstrapTests(unittest.TestCase):
    def load_bootstrap(self):
        spec = importlib.util.spec_from_file_location('ci_bootstrap', Path(__file__).with_name('sonar-ci-bootstrap.py'))
        module = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(module)
        return module

    def test_bootstrap_rejects_non_ci_without_network(self):
        module = self.load_bootstrap()
        with patch.dict(module.os.environ, {}, clear=True), patch.object(module, 'post') as post:
            with self.assertRaises(ValueError):
                module.main()
            post.assert_not_called()

    def test_fresh_ci_credentials_are_database_local_and_masked(self):
        module = self.load_bootstrap()
        with tempfile.TemporaryDirectory() as directory:
            env = Path(directory) / 'environment'
            with patch.dict(module.os.environ, {'GITHUB_ACTIONS': 'true', 'GITHUB_ENV': str(env)}), \
                    patch.object(module.secrets, 'token_urlsafe', return_value='fixture-password'), \
                    patch.object(module, 'post', side_effect=[{}, {'token': 'fixture-token'}]) as post, \
                    redirect_stdout(io.StringIO()) as output:
                module.main()
            self.assertEqual(post.call_args_list[0].args[2], 'admin')
            self.assertEqual(post.call_args_list[1].args[2], 'fixture-password')
            self.assertEqual(env.read_text(), 'SONAR_TOKEN=fixture-token\n')
            self.assertIn('::add-mask::fixture-token', output.getvalue())


if __name__ == '__main__':
    unittest.main()
