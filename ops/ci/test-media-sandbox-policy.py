#!/usr/bin/env python3
"""Source contract checks; real policy loading and codec execution remain separate remote boundaries."""
import importlib.util
import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch

ROOT = Path(__file__).resolve().parents[2]
SPEC = importlib.util.spec_from_file_location('media_policy', ROOT / 'ops/bin/media-sandbox-policy.py')
POLICY = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(POLICY)


class MediaSandboxPolicyTest(unittest.TestCase):
    def test_default_deny_and_exact_namespace_arguments(self):
        files, metadata = POLICY.validate_source()
        profile = json.loads(files['media-seccomp.json'])
        self.assertEqual('SCMP_ACT_ERRNO', profile['defaultAction'])
        additions = metadata['addedRules']
        self.assertEqual([0x7e020011, 0x7c020011], [rule['args'][0]['value'] for rule in additions[:2]])
        self.assertTrue(all(rule['args'][0]['op'] == 'SCMP_CMP_EQ' for rule in additions[:3]))
        self.assertEqual(0x10000000, additions[2]['args'][0]['value'])
        self.assertEqual(['mount', 'umount2', 'pivot_root'], additions[3]['names'])
        self.assertNotIn('setns', additions[3]['names'])

    def test_policy_byte_changes_fail_closed(self):
        files, _ = POLICY.validate_source()
        with tempfile.TemporaryDirectory() as directory:
            target = Path(directory)
            for name, content in files.items():
                (target / name).write_bytes(content)
            (target / 'media-sandbox-policy.json').write_bytes(
                (POLICY.SECURITY / 'media-sandbox-policy.json').read_bytes())
            (target / 'media-seccomp.json').write_bytes(files['media-seccomp.json'] + b' ')
            with patch.object(POLICY, 'SECURITY', target):
                with self.assertRaisesRegex(ValueError, 'versioned hashes'):
                    POLICY.validate_source()

    def test_codec_stacks_a_stricter_profile_and_compose_applies_only_to_media(self):
        files, _ = POLICY.validate_source()
        text = files['media-apparmor'].decode()
        self.assertIn('/usr/bin/prlimit px -> &spoolin-media-codec,', text)
        codec = text.split('profile spoolin-media-codec ', 1)[1]
        for denied in ('network', 'capability', 'mount', 'umount', 'pivot_root', 'userns', 'ptrace'):
            self.assertIn('deny ' + denied + ',', codec)
        self.assertNotIn('\n  mount,', text)
        compose = (ROOT / 'ops/compose.yaml').read_text()
        media = compose.split('  media:\n', 1)[1].split('  media-migrator:\n', 1)[0]
        self.assertIn('cap_drop: [ALL]', media)
        self.assertIn('no-new-privileges:true', media)
        self.assertIn('read_only: true', media)
        self.assertEqual(1, compose.count('apparmor=spoolin-media'))
        self.assertEqual(1, compose.count('seccomp=/etc/spoolin/security/media-seccomp.json'))


if __name__ == '__main__':
    unittest.main()
