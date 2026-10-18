#!/usr/bin/env python3
"""Check local identity provisioning without disclosing generated material."""
import importlib.util
import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch

SPEC = importlib.util.spec_from_file_location('local_provision', Path(__file__).with_name('provision.py'))
PROVISION = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(PROVISION)


class ProvisionTest(unittest.TestCase):
    def test_local_leaf_certificates_support_strict_chain_validation(self):
        with tempfile.TemporaryDirectory() as temporary:
            extensions = Path(temporary) / 'core.ext'
            extensions.write_text('subjectAltName=DNS:localhost\n')
            with patch.object(PROVISION.subprocess, 'run'):
                PROVISION.local_command('openssl', 'x509', '-extfile', str(extensions))
            value = extensions.read_text()
            self.assertIn('subjectAltName=DNS:localhost', value)
            self.assertIn('basicConstraints=critical,CA:FALSE', value)
            self.assertIn('authorityKeyIdentifier=keyid:always,issuer', value)

    def test_command_extends_local_certificate_validity_and_needs_no_sudo(self):
        with patch.object(PROVISION.subprocess, 'run') as command:
            PROVISION.local_command('sudo', 'openssl', '-days', '2')
        self.assertEqual(['openssl', '-days', '365'], command.call_args.args[0])

    def test_runtime_configuration_has_random_keys_roles_and_account_files(self):
        with tempfile.TemporaryDirectory() as temporary:
            runtime = Path(temporary)
            with patch.object(PROVISION, 'local_command'), patch.object(PROVISION.subprocess, 'check_output', return_value=b'public-key'):
                PROVISION.initialize_runtime(runtime)
            for name in ('pii', 'draft', 'email_lookup', 'csrf', 'rate', 'operation', 'catalog-operation', 'mcp-ui'):
                self.assertEqual(32, len((runtime / name).read_bytes()))
                self.assertEqual(0o600, (runtime / name).stat().st_mode & 0o777)
            accounts = json.loads((runtime / 'account-initialization.json').read_text())['accounts']
            self.assertEqual(['VIEWER', 'MAINTAINER'], [account['role'] for account in accounts])
            self.assertNotEqual((runtime / 'viewer-password').read_text(), (runtime / 'maintainer-password').read_text())
            self.assertTrue((runtime / 'rate-agreement').exists())

    def test_host_files_use_host_paths_and_protected_password_copy(self):
        with tempfile.TemporaryDirectory() as temporary:
            base = Path(temporary)
            runtime = base / 'runtime'
            runtime.mkdir()
            (base / 'identities').mkdir()
            (base / 'identities/service-ca').write_text('public certificate')
            (runtime / 'viewer-password').write_text('random-viewer')
            (runtime / 'maintainer-password').write_text('random-maintainer')
            with patch.object(PROVISION, 'BASE', base), patch.object(PROVISION.os, 'chown'):
                PROVISION.save_host_files('/host/path with spaces', 501, 20, runtime,
                                          {'SERVICE_CA_FILE': '/workspace/.local/identities/service-ca'})
            environment = json.loads((base / 'environment.json').read_text())
            self.assertEqual('/host/path with spaces/.local/identities/service-ca', environment['SERVICE_CA_FILE'])
            self.assertEqual(0o600, (base / 'accounts.txt').stat().st_mode & 0o777)
            self.assertEqual(0o644, (base / 'local-ca.crt').stat().st_mode & 0o777)

    def test_provision_orders_identity_runtime_and_protected_ownership(self):
        with tempfile.TemporaryDirectory() as temporary:
            base = Path(temporary)
            with patch.object(PROVISION, 'BASE', base), patch.object(PROVISION, 'local_command') as command, patch.object(PROVISION.PREPARE, 'provision_identity_files') as identities, patch.object(PROVISION, 'initialize_runtime') as runtime, patch.object(PROVISION, 'save_host_files') as files:
                PROVISION.provision('/host', 501, 20)
            identities.assert_called_once()
            runtime.assert_called_once_with(base / 'runtime')
            files.assert_called_once()
            ca_arguments = command.call_args_list[0].args
            self.assertIn('basicConstraints=critical,CA:TRUE', ca_arguments)
            self.assertIn('keyUsage=critical,keyCertSign,cRLSign', ca_arguments)
            self.assertEqual('chown', command.call_args.args[0])
            self.assertEqual(0o700, (base / 'runtime').stat().st_mode & 0o777)


if __name__ == '__main__':
    unittest.main()
