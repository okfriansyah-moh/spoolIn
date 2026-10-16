"""Exercise identity, dependency, coverage and evidence preservation boundaries."""
import hashlib
import importlib.util
import json
from pathlib import Path
import subprocess
import tempfile
import unittest
import yaml

SPEC = importlib.util.spec_from_file_location('plan_numbering', Path(__file__).with_name('verify.py'))
audit = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(audit)


def rendered(meta, body=''):
    return '---\n' + yaml.safe_dump(meta, sort_keys=False) + '---\n' + body


class MigrationAuditTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.git('init', '-q')
        self.rows = []
        groups = []
        for n in range(1, 4):
            tid = f'T{n:03}'
            gid = f'G{n:02}'
            path = f'docs/plan/{n:02}-feature/tasks/{tid}-result.md'
            meta = {'kind': 'task', 'id': tid, 'group': gid, 'depends_on': [] if n == 1 else ['T001'], 'status': 'Done' if n == 1 else 'In progress'}
            body = '[Feature group](../group.md)\n\n## Evidence\n\nExisting observation at original revision.\n\n## Completion\n\nPending.\n'
            self.write(path, rendered(meta, body))
            gp = f'plan/{n:02}-feature/group.md'
            self.write('docs/' + gp, rendered({'id': gid, 'tasks': [tid], 'integration_owner': tid}))
            groups.append({'id': gid, 'path': gp})
            self.rows.append({'old_id': tid, 'new_id': tid, 'old_group': gid, 'new_group': gid, 'old_path': path, 'new_path': path, 'status_before': meta['status'], 'depends_on_before': meta['depends_on'], 'depends_on_after': meta['depends_on'], 'evidence_sha256': hashlib.sha256(audit.evidence(body).encode()).hexdigest()})
        self.plan_meta = {'sources': [], 'groups': groups}
        self.plan_body = '| ATDD-001 | ATDD | exact locator | Covered | T002 | T003 | reason |\n'
        self.write('docs/06-PLAN.md', rendered(self.plan_meta, self.plan_body))
        self.git('add', '.')
        self.git('-c', 'user.name=Fixture', '-c', 'user.email=fixture@example.invalid', 'commit', '-qm', 'source')
        revision = self.git('rev-parse', 'HEAD').strip()
        self.plan_meta['numbering'] = 'sequential'
        self.write('docs/06-PLAN.md', rendered(self.plan_meta, self.plan_body))
        self.data = {'schema_version': 1, 'target_numbering': 'sequential', 'source_revision': revision, 'source_plan': 'docs/06-PLAN.md', 'tasks': self.rows}

    def git(self, *args):
        return subprocess.check_output(['git', *args], cwd=self.root, text=True)

    def write(self, name, content):
        p = self.root / name
        p.parent.mkdir(parents=True, exist_ok=True)
        p.write_text(content)

    def verify(self):
        p = self.root / 'docs/map.json'
        p.write_text(json.dumps(self.data))
        return audit.verify(self.root, p)

    def replace_task(self, n, transform):
        path = self.rows[n]['new_path']
        self.write(path, transform((self.root / path).read_text()))

    def test_valid_migration_preserves_done_and_unfinished(self):
        result = self.verify()
        self.assertEqual(result['statuses'], {'Done': 1, 'In progress': 2})
        self.assertEqual(result['obligations'], 1)

    def test_duplicate_id_is_rejected(self):
        self.rows[1]['new_id'] = 'T001'
        with self.assertRaisesRegex(ValueError, 'one-to-one'):
            self.verify()

    def test_gap_is_rejected(self):
        self.rows[2]['new_id'] = 'T004'
        with self.assertRaisesRegex(ValueError, 'consecutive'):
            self.verify()

    def test_missing_task_is_rejected(self):
        (self.root / self.rows[2]['new_path']).unlink()
        with self.assertRaisesRegex(ValueError, 'coverage is incomplete'):
            self.verify()

    def test_false_done_is_rejected(self):
        self.replace_task(1, lambda s: s.replace('In progress', 'Done'))
        with self.assertRaisesRegex(ValueError, 'completion status'):
            self.verify()

    def test_changed_evidence_is_rejected(self):
        self.replace_task(0, lambda s: s.replace('Existing observation', 'New observation'))
        with self.assertRaisesRegex(ValueError, 'Evidence section'):
            self.verify()

    def test_existing_wrong_stage_navigation_is_rejected(self):
        self.replace_task(1, lambda s: s.replace('../group.md', '../../01-feature/group.md'))
        with self.assertRaisesRegex(ValueError, 'own stage'):
            self.verify()

    def test_wrong_digest_is_rejected(self):
        self.rows[0]['evidence_sha256'] = '0' * 64
        with self.assertRaisesRegex(ValueError, 'digest'):
            self.verify()

    def test_changed_dependencies_are_rejected(self):
        self.rows[1]['depends_on_after'] = []
        with self.assertRaisesRegex(ValueError, 'dependencies changed'):
            self.verify()

    def test_changed_obligation_is_rejected(self):
        self.write('docs/06-PLAN.md', rendered(self.plan_meta, self.plan_body.replace('T003', 'T002')))
        with self.assertRaisesRegex(ValueError, 'Obligation rows'):
            self.verify()

    def test_stage_order_is_rejected(self):
        self.plan_meta['groups'].reverse()
        self.write('docs/06-PLAN.md', rendered(self.plan_meta, self.plan_body))
        with self.assertRaisesRegex(ValueError, 'Group numbering'):
            self.verify()

    def fixture_boundary(self, pattern, producer):
        base = 'services/catalog/src/integrationTest/kotlin/com/spoolin/catalog'
        self.write(base + '/boundary/bootstrap/OwnerFixture.kt',
                   'require(evidenceName.matches(Regex("' + pattern + '")))')
        self.write(base + '/operations/FixtureTest.kt', 'OwnerFixture("' + producer + '")')

    def test_fixture_producer_passes_matching_cleanup(self):
        self.fixture_boundary(r't(?:003|007)-[a-z-]+\\.json', 't007-receipts.json')
        self.assertEqual(audit.fixture_names(self.root), 1)

    def test_encoded_old_cleanup_identity_rejects_renumbered_producer(self):
        self.fixture_boundary(r't(?:003|010)-[a-z-]+\\.json', 't007-receipts.json')
        with self.assertRaisesRegex(ValueError, 'cannot pass catalog cleanup'):
            audit.fixture_names(self.root)

    def test_interpolated_fixture_keeps_new_task_prefix(self):
        self.fixture_boundary(r't(?:003|007)-[a-z-]+\\.json', 't007-crash-$stage.json')
        self.assertEqual(audit.fixture_names(self.root), 1)


    def test_simultaneous_reused_ids_keep_correct_owners(self):
        mapping = {'T001': 'T001', 'T002': 'T003', 'T003': 'T002'}
        after = self.plan_body.replace('T002 | T003', 'T003 | T002')
        self.assertEqual(audit.coverage_preserved(self.plan_body, after, mapping), 1)


if __name__ == '__main__':
    unittest.main()
