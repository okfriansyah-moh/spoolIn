#!/usr/bin/env python3
"""Check host trust routing without changing any certificate stores."""
import importlib.util
from pathlib import Path
import subprocess
import tempfile
import unittest
from unittest.mock import patch

SPEC = importlib.util.spec_from_file_location('local_trust', Path(__file__).with_name('trust.py'))
TRUST = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(TRUST)


class LocalTrustTest(unittest.TestCase):
    def setUp(self):
        self.directory = tempfile.TemporaryDirectory()
        self.addCleanup(self.directory.cleanup)
        self.certificate = Path(self.directory.name) / 'local-ca.crt'
        subprocess.run(['openssl', 'req', '-x509', '-newkey', 'rsa:2048', '-nodes',
                        '-days', '1', '-subj', '/CN=SpoolIn test CA',
                        '-keyout', str(Path(self.directory.name) / 'key'),
                        '-out', str(self.certificate)], check=True, capture_output=True)

    def test_missing_certificate_does_not_change_trust(self):
        with patch.object(TRUST, 'run') as run:
            with self.assertRaisesRegex(RuntimeError, 'local-setup'):
                TRUST.trust(Path(self.directory.name) / 'missing')
            run.assert_not_called()

    def test_invalid_input_does_not_change_trust(self):
        for text in ('invalid', '-----BEGIN CERTIFICATE-----\nbad\n-----END CERTIFICATE-----',
                     self.certificate.read_text() + '\nPRIVATE KEY'):
            with self.subTest(text=text[:20]):
                self.certificate.write_text(text)
                with patch.object(TRUST, 'run') as run:
                    with self.assertRaisesRegex(RuntimeError, 'certificate|private key'):
                        TRUST.trust(self.certificate)
                    run.assert_not_called()

    def test_macos_limits_trust_to_localhost_and_login_keychain(self):
        with patch.object(TRUST.platform, 'system', return_value='Darwin'), patch.object(TRUST, 'run') as run:
            TRUST.trust(self.certificate)
            run.assert_called_once_with('security', 'add-trusted-cert', '-r', 'trustRoot',
                                        '-p', 'ssl', '-s', 'localhost', '-k',
                                        str(Path.home() / 'Library/Keychains/login.keychain-db'),
                                        str(self.certificate))

    def test_windows_uses_current_user_store(self):
        with patch.object(TRUST.platform, 'system', return_value='Windows'), patch.object(TRUST, 'run') as run:
            TRUST.trust(self.certificate)
            run.assert_called_once_with('certutil.exe', '-user', '-addstore', '-f', 'Root', str(self.certificate))

    def test_wsl_targets_windows_store_with_converted_path(self):
        with patch.object(TRUST.platform, 'system', return_value='Linux'), \
                patch.object(TRUST.platform, 'release', return_value='6.6-microsoft-standard-WSL2'), \
                patch.object(TRUST.subprocess, 'check_output', return_value='C:\\SpoolIn folder\\local-ca.crt\n') as convert, \
                patch.object(TRUST, 'run') as run:
            TRUST.trust(self.certificate)
            convert.assert_called_once_with(['wslpath', '-w', str(self.certificate)], text=True)
            run.assert_called_once_with('certutil.exe', '-user', '-addstore', '-f', 'Root',
                                        'C:\\SpoolIn folder\\local-ca.crt')

    def test_linux_uses_unique_filename_and_refreshes_store(self):
        with patch.object(TRUST.platform, 'system', return_value='Linux'), \
                patch.object(TRUST.platform, 'release', return_value='6.6-generic'), \
                patch.object(TRUST.shutil, 'which', return_value='/usr/sbin/update-ca-certificates'), \
                patch.object(TRUST, 'run') as run:
            TRUST.trust(self.certificate)
            commands = [call.args for call in run.call_args_list]
            self.assertEqual(('sudo', 'install', '-m', '0644', str(self.certificate)), commands[0][:-1])
            self.assertRegex(commands[0][-1], r'/usr/local/share/ca-certificates/spoolin-local-[a-f0-9]{64}\.crt')
            self.assertEqual(('sudo', 'update-ca-certificates'), commands[1])

    def test_unsupported_linux_fails_before_installing(self):
        with patch.object(TRUST.shutil, 'which', return_value=None), patch.object(TRUST, 'run') as run:
            with self.assertRaisesRegex(RuntimeError, 'Ubuntu/Debian'):
                TRUST.trust_linux(self.certificate, 'fingerprint')
            run.assert_not_called()

    def test_unsupported_os_does_not_change_trust(self):
        with patch.object(TRUST.platform, 'system', return_value='Other'), patch.object(TRUST, 'run') as run:
            with self.assertRaisesRegex(RuntimeError, 'unavailable'):
                TRUST.trust(self.certificate)
            run.assert_not_called()

    def test_os_failure_is_propagated(self):
        with patch.object(TRUST.platform, 'system', return_value='Darwin'), \
                patch.object(TRUST, 'run', side_effect=subprocess.CalledProcessError(1, 'security')):
            with self.assertRaises(subprocess.CalledProcessError):
                TRUST.trust(self.certificate)

    def test_command_runner_uses_checked_argument_list(self):
        with patch.object(TRUST.subprocess, 'run') as run:
            TRUST.run('example', 'path with spaces')
            run.assert_called_once_with(('example', 'path with spaces'), check=True, text=True, capture_output=False)


if __name__ == '__main__':
    unittest.main()
