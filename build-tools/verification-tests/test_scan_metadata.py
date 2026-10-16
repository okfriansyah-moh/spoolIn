"""Exercise secret-scanner metadata exceptions without suppressing credentials."""
import hashlib
import json
from pathlib import Path
import secrets
import shutil
import subprocess
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[2]


class ScanMetadataTests(unittest.TestCase):
    def scan(self, files):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            for name in ['.ai/security/gitleaks.toml', '.ai/bundles/1.1.0/profiles/security/gitleaks.toml']:
                target = root / name
                target.parent.mkdir(parents=True, exist_ok=True)
                shutil.copyfile(ROOT / name, target)
            for name, content in files.items():
                target = root / name
                target.parent.mkdir(parents=True, exist_ok=True)
                target.write_text(content)
            result = subprocess.run([
                'gitleaks', 'dir', '--redact', '--no-banner', '--exit-code', '1',
                '--config', '.ai/security/gitleaks.toml', '.',
            ], cwd=root, capture_output=True, timeout=30)
            self.assertIn(result.returncode, [0, 1], result.stderr.decode(errors='replace'))
            return result.returncode

    def test_known_provenance_fields_are_not_credentials(self):
        digest = hashlib.sha256(b'independent source bytes').hexdigest()
        identifier = secrets.token_urlsafe(1200)
        fields = {
            'services/core/KeyRegistry.kt': digest,
            'repo:.agents/skills/primitive-tokens.md': digest,
            'cache_key': digest,
            'apiCallId': identifier,
        }
        document = json.dumps(fields, indent=2)
        self.assertEqual(self.scan({
            '.ai/state/account-selection.json': json.dumps({'services/core/KeyRegistry.kt': digest}, indent=2),
            '.ai/evidence/release-1.1.0/native/repo/verification.json': document,
            '.ai/state/host-verifications/example/pass-native-smoke.json':
                json.dumps({'apiCallId': identifier}, indent=2),
            '.ai/evidence/autonomy-1.1.1/sessions/session-copilot-4e102219/fresh-stdout.jsonl':
                json.dumps({'message': json.dumps({'apiCallId': identifier})}),
        }), 0)

    def test_provenance_shapes_in_application_source_still_block(self):
        digest = hashlib.sha256(b'independent source bytes').hexdigest()
        self.assertEqual(self.scan({'frontend/src/keys.json': json.dumps({
            'services/core/KeyRegistry.kt': digest, 'apiCallId': secrets.token_urlsafe(80),
        }, indent=2)}), 1)

    def test_credential_in_known_provenance_record_still_blocks(self):
        self.assertEqual(self.scan({
            '.ai/evidence/release-1.1.0/native/repo/verification.json':
                json.dumps({'api_key': secrets.token_urlsafe(40)}, indent=2),
        }), 1)

    def test_unrelated_state_key_is_not_a_provenance_exception(self):
        self.assertEqual(self.scan({'.ai/state/unrelated-keys.json': json.dumps({
            'cache_key': hashlib.sha256(b'independent source bytes').hexdigest(),
        }, indent=2)}), 1)
