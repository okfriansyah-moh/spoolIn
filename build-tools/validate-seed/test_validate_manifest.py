"""Structural checks stay independent of the authoritative JVM input policy."""
import copy
import importlib.util
import json
import unittest
import tempfile
from pathlib import Path

HERE = Path(__file__).resolve().parent
SPEC = importlib.util.spec_from_file_location('seed_validator', HERE / 'validate_manifest.py')
VALIDATOR = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(VALIDATOR)


class StructuralSeedTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.original = json.loads((HERE.parent.parent / 'data/seed/manifest.json').read_text())

    def snapshot(self):
        return copy.deepcopy(self.original)

    def rehash(self, root):
        for row in root['people'] + root['records']:
            row['contentSHA256'] = VALIDATOR.checksum(row)
        root['snapshotSHA256'] = VALIDATOR.checksum(root)
        return root

    def test_structure_reports_its_actual_scope(self):
        result = VALIDATOR.validate(self.snapshot())
        self.assertEqual(100, result['movies'])
        self.assertEqual(100, result['series'])
        self.assertFalse(result['effectivePolicyVerified'])
        self.assertEqual('structure_provenance_hash', result['validationScope'])

    def test_wider_text_is_not_rejected_by_guessed_policy(self):
        root = self.snapshot()
        root['people'][0]['name'] = 'N' * 300
        root['records'][0]['normalized']['title'] = 'T' * 300
        root['records'][0]['normalized']['synopsis'] = 'S' * 6000
        self.assertFalse(VALIDATOR.validate(self.rehash(root))['effectivePolicyVerified'])

    def test_checksum_source_permission_and_artwork_controls_remain_required(self):
        mutations = [
            lambda root: root['people'][0].update(name='Changed without updating the checksum'),
            lambda root: root['records'][0]['sources'][0].update(license='unreviewed'),
            lambda root: root.update(files=[{'unreviewed': True}]),
        ]
        for mutate in mutations:
            root = self.snapshot()
            mutate(root)
            if root['people'][0]['name'] == self.original['people'][0]['name']:
                self.rehash(root)
            with self.assertRaises(ValueError):
                VALIDATOR.validate(root)

    def test_previous_snapshot_remains_readable(self):
        baseline = HERE.parent.parent / 'data/seed/baselines/wikidata-2026-10-v1.json'
        if baseline.exists():
            result = VALIDATOR.validate(json.loads(baseline.read_text()))
            self.assertEqual(0, result['artworkFiles'])

    def test_missing_or_modified_poster_is_rejected(self):
        root = self.snapshot()
        if not root['files']:
            return
        with tempfile.TemporaryDirectory() as directory:
            with self.assertRaisesRegex(ValueError, 'Missing poster'):
                VALIDATOR.validate_files(root, Path(directory))
            item = root['files'][0]
            path = Path(directory) / item['file']
            path.parent.mkdir()
            path.write_bytes(b'\xff\xd8changed\xff\xd9')
            with self.assertRaisesRegex(ValueError, 'checksum'):
                VALIDATOR.validate_files(root, Path(directory))

    def test_incomplete_package_is_rejected(self):
        root = self.snapshot()
        root['records'].pop()
        with self.assertRaisesRegex(ValueError, '100 movies'):
            VALIDATOR.validate(self.rehash(root))

    def test_structural_unicode_rejects_null(self):
        root = self.snapshot()
        root['people'][0]['name'] = 'N\x00'
        with self.assertRaises(ValueError):
            VALIDATOR.validate(self.rehash(root))


if __name__ == '__main__':
    unittest.main()
