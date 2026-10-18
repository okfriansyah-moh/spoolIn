#!/usr/bin/env python3
"""Regression tests for isolated local setup and destructive operations."""
import copy
import importlib.util
import json
from pathlib import Path
import subprocess
import tempfile
import unittest
from unittest.mock import patch

SPEC = importlib.util.spec_from_file_location('local_runner', Path(__file__).with_name('run.py'))
RUNNER = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(RUNNER)


class LocalSetupTest(unittest.TestCase):
    def test_configuration_preserves_security_and_excludes_external_services(self):
        service = {'image': 'production', 'read_only': True, 'cap_drop': ['ALL'],
                   'security_opt': ['no-new-privileges:true'], 'networks': ['private']}
        document = {'services': {name: copy.deepcopy(service) for name in (*RUNNER.SERVICES, 'mcp', 'grafana')}}
        result = RUNNER.local_configuration(document)
        self.assertNotIn('mcp', result['services'])
        self.assertNotIn('grafana', result['services'])
        for name in RUNNER.SERVICES:
            self.assertTrue(result['services'][name]['read_only'])
            self.assertEqual(['ALL'], result['services'][name]['cap_drop'])
            self.assertIn('no-new-privileges:true', result['services'][name]['security_opt'])
            self.assertTrue(result['services'][name]['image'].startswith('spoolin-local-'))
        self.assertIn('systempaths=unconfined', result['services']['media']['security_opt'])
        self.assertNotIn('systempaths=unconfined', result['services']['core']['security_opt'])
        self.assertIn(f'seccomp={RUNNER.ROOT / "ops/security/media-seccomp.json"}', result['services']['media']['security_opt'])

    def test_repeat_setup_preserves_existing_environment(self):
        with tempfile.TemporaryDirectory() as temporary:
            state = Path(temporary)
            original = state / 'environment.json'
            original.write_text('{"secret-path":"unchanged"}')
            with patch.object(RUNNER, 'STATE', state), patch.object(RUNNER, 'run') as command, patch.object(RUNNER, 'render_compose'):
                RUNNER.setup()
            command.assert_not_called()
            self.assertEqual('{"secret-path":"unchanged"}', original.read_text())
            self.assertEqual(0o700, state.stat().st_mode & 0o777)

    def test_partial_setup_does_not_replace_secrets(self):
        with tempfile.TemporaryDirectory() as temporary:
            state = Path(temporary)
            (state / 'runtime').mkdir()
            with patch.object(RUNNER, 'STATE', state), patch.object(RUNNER, 'run') as command:
                with self.assertRaisesRegex(RuntimeError, 'Incomplete provisioning'):
                    RUNNER.setup()
            command.assert_not_called()

    def test_reset_requires_exact_confirmation(self):
        with patch('builtins.input', return_value='yes'), patch.object(RUNNER, 'compose') as command, patch.object(RUNNER, 'tooling') as tool:
            with self.assertRaisesRegex(RuntimeError, 'cancelled'):
                RUNNER.reset()
            command.assert_not_called()
            tool.assert_not_called()

    def test_reset_removes_only_local_compose_volumes_and_directory(self):
        with tempfile.TemporaryDirectory() as temporary:
            state = Path(temporary)
            (state / 'compose.json').write_text('{}')
            with patch.object(RUNNER, 'STATE', state), patch('builtins.input', return_value='DELETE'), patch.object(RUNNER, 'compose') as command, patch.object(RUNNER, 'tooling') as tool, patch.object(RUNNER, 'run'):
                RUNNER.reset()
            command.assert_called_once_with('down', '--volumes', '--remove-orphans')
            self.assertIn('shutil.rmtree("/workspace/.local")', tool.call_args.args[2])

    def test_migration_revokes_membership_after_failure(self):
        with patch.object(RUNNER, 'compose', side_effect=[None, RuntimeError('migration failed'), None]) as command:
            with self.assertRaisesRegex(RuntimeError, 'migration failed'):
                RUNNER.migrate('core')
            self.assertEqual(('exec', '-T', 'postgres', '/opt/spoolin/membership.sh', 'revoke', 'core'), command.call_args.args)

    def test_codec_failure_never_reports_success(self):
        with patch.object(RUNNER, 'compose', side_effect=subprocess.CalledProcessError(1, 'bwrap')):
            with self.assertRaisesRegex(RuntimeError, 'No sandbox bypass'):
                RUNNER.codec_probe()

    def test_readiness_waits_for_private_service_probes(self):
        with patch.object(RUNNER, 'compose', side_effect=[subprocess.CalledProcessError(1, 'ready'), None]) as command, patch.object(RUNNER.time, 'sleep'):
            RUNNER.ready()
            self.assertEqual(2, command.call_count)
            self.assertIn('local-readiness', command.call_args.args)

    def test_render_does_not_inherit_deployment_ports(self):
        with tempfile.TemporaryDirectory() as temporary:
            state = Path(temporary)
            (state / 'environment.json').write_text('{}')
            document = {'services': {name: {'image': 'production'} for name in RUNNER.SERVICES}}
            process = subprocess.CompletedProcess([], 0, stdout=json.dumps(document))
            with patch.object(RUNNER, 'STATE', state), patch.dict(RUNNER.os.environ, {'CORE_HTTPS_PORT': '9999', 'LOG_LEVEL': 'TRACE'}), patch.object(RUNNER, 'run', return_value=process) as command:
                RUNNER.render_compose()
            self.assertEqual('8443', command.call_args.kwargs['env']['CORE_HTTPS_PORT'])
            self.assertNotIn('LOG_LEVEL', command.call_args.kwargs['env'])
            self.assertIn('--env-file', command.call_args.args)
            self.assertIn('/dev/null', command.call_args.args)
            self.assertEqual(0o600, (state / 'compose.json').stat().st_mode & 0o777)



class LocalCommandsTest(unittest.TestCase):
    def test_tooling_uses_host_identity_and_workspace(self):
        with patch.object(RUNNER, 'run') as command:
            RUNNER.tooling('npm', 'ci')
        self.assertIn(f'{RUNNER.os.getuid()}:{RUNNER.os.getgid()}', command.call_args.args)
        self.assertIn(f'{RUNNER.ROOT}:/workspace', command.call_args.args)

    def test_setup_provisions_only_when_uninitialized(self):
        with tempfile.TemporaryDirectory() as temporary:
            with patch.object(RUNNER, 'STATE', Path(temporary)), patch.object(RUNNER, 'run') as command, patch.object(RUNNER, 'tooling') as tool, patch.object(RUNNER, 'render_compose') as render:
                RUNNER.setup()
            command.assert_called_once()
            tool.assert_called_once()
            render.assert_called_once()

    def test_start_orders_initialization_before_services(self):
        with patch.object(RUNNER, 'setup'), patch.object(RUNNER, 'build_sources'), patch.object(RUNNER, 'codec_probe'), patch.object(RUNNER, 'migrate') as migration, patch.object(RUNNER, 'ready') as ready, patch.object(RUNNER, 'compose') as command:
            RUNNER.start()
        self.assertEqual([unittest.mock.call(owner) for owner in RUNNER.OWNERS], migration.call_args_list)
        self.assertIn(unittest.mock.call('up', '-d', '--wait', '--wait-timeout', '120', 'postgres'), command.call_args_list)
        self.assertIn(unittest.mock.call('run', '--rm', '--no-deps', 'core', '--initialize-accounts=/run/spoolin-config/account-initialization.json'), command.call_args_list)
        self.assertEqual(unittest.mock.call('up', '-d', *RUNNER.OWNERS), command.call_args_list[-1])
        ready.assert_called_once()

    def test_source_checks_include_types_unit_tests_and_backend_tests(self):
        with tempfile.TemporaryDirectory() as temporary:
            with patch.object(RUNNER, 'STATE', Path(temporary)), patch.object(RUNNER, 'run'), patch.object(RUNNER, 'tooling') as command:
                RUNNER.build_sources(test=True)
        self.assertIn(unittest.mock.call('npm', 'run', 'check'), command.call_args_list)
        self.assertIn(unittest.mock.call('npm', 'run', 'test:coverage', '--workspace', 'frontend'), command.call_args_list)
        self.assertEqual(unittest.mock.call('./gradlew', '--no-daemon', '--console', 'plain', 'test'), command.call_args_list[-1])

    def test_status_checks_readiness_and_codec(self):
        with tempfile.TemporaryDirectory() as temporary:
            state = Path(temporary)
            (state / 'compose.json').write_text('{}')
            with patch.object(RUNNER, 'STATE', state), patch.object(RUNNER, 'compose') as command, patch.object(RUNNER, 'ready') as ready, patch.object(RUNNER, 'codec_probe') as codec:
                RUNNER.dispatch('status')
            command.assert_called_once_with('ps', '--all')
            ready.assert_called_once()
            codec.assert_called_once()

    def test_missing_setup_fails_before_service_commands(self):
        with tempfile.TemporaryDirectory() as temporary:
            with patch.object(RUNNER, 'STATE', Path(temporary)), patch.object(RUNNER, 'compose') as command:
                with self.assertRaisesRegex(RuntimeError, 'local-setup'):
                    RUNNER.dispatch('status')
            command.assert_not_called()

    def test_logs_are_bounded_and_stop_preserves_volumes(self):
        with tempfile.TemporaryDirectory() as temporary:
            state = Path(temporary)
            (state / 'compose.json').write_text('{}')
            with patch.object(RUNNER, 'STATE', state), patch.object(RUNNER, 'compose') as command:
                RUNNER.dispatch('logs')
                RUNNER.dispatch('stop')
            self.assertEqual(unittest.mock.call('logs', '--tail', '200', *RUNNER.OWNERS, 'postgres'), command.call_args_list[0])
            self.assertEqual(unittest.mock.call('down', '--remove-orphans'), command.call_args_list[1])

    def test_action_lock_rejects_concurrent_command(self):
        with tempfile.TemporaryDirectory() as temporary:
            state = Path(temporary)
            (state / 'action.lock').mkdir()
            with patch.object(RUNNER, 'STATE', state), patch.object(RUNNER, 'check_tools'), patch.object(RUNNER.sys, 'argv', ['run.py', 'setup']), patch.object(RUNNER, 'dispatch') as dispatch:
                with self.assertRaisesRegex(RuntimeError, 'Another local command'):
                    RUNNER.main()
                dispatch.assert_not_called()

    def test_action_lock_releases_after_command_failure(self):
        with tempfile.TemporaryDirectory() as temporary:
            state = Path(temporary)
            with patch.object(RUNNER, 'STATE', state), patch.object(RUNNER, 'check_tools'), patch.object(RUNNER.sys, 'argv', ['run.py', 'start']), patch.object(RUNNER, 'dispatch', side_effect=RuntimeError('failed')):
                with self.assertRaisesRegex(RuntimeError, 'failed'):
                    RUNNER.main()
            self.assertFalse((state / 'action.lock').exists())


class LocalPathSafetyTest(unittest.TestCase):
    def test_setup_rejects_symlink_before_chmod_or_provisioning(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            target = root / 'outside'
            target.mkdir(mode=0o755)
            link = root / 'local'
            link.symlink_to(target, target_is_directory=True)
            with patch.object(RUNNER, 'STATE', link), patch.object(RUNNER, 'run') as command:
                with self.assertRaisesRegex(RuntimeError, 'Refusing symlink'):
                    RUNNER.setup()
            self.assertEqual(0o755, target.stat().st_mode & 0o777)
            command.assert_not_called()

    def test_reset_rejects_symlink_before_confirmation(self):
        with tempfile.TemporaryDirectory() as temporary:
            link = Path(temporary) / 'local'
            link.symlink_to(Path(temporary) / 'missing')
            with patch.object(RUNNER, 'STATE', link), patch('builtins.input') as prompt:
                with self.assertRaisesRegex(RuntimeError, 'Refusing symlink'):
                    RUNNER.reset()
            prompt.assert_not_called()


class LocalCacheResetTest(unittest.TestCase):
    def test_reset_without_build_cache_does_not_fail_volume_removal(self):
        with tempfile.TemporaryDirectory() as temporary:
            state = Path(temporary)
            with patch.object(RUNNER, 'STATE', state), patch('builtins.input', return_value='DELETE'), patch.object(RUNNER, 'tooling'), patch.object(RUNNER, 'run', return_value=subprocess.CompletedProcess([], 0, stdout='')) as command:
                RUNNER.reset()
            self.assertEqual(1, command.call_count)
            self.assertEqual(('docker', 'volume', 'ls'), command.call_args.args[:3])

    def test_reset_removes_existing_build_cache(self):
        with tempfile.TemporaryDirectory() as temporary:
            state = Path(temporary)
            with patch.object(RUNNER, 'STATE', state), patch('builtins.input', return_value='DELETE'), patch.object(RUNNER, 'tooling'), patch.object(RUNNER, 'run', return_value=subprocess.CompletedProcess([], 0, stdout='spoolin-local-gradle\n')) as command:
                RUNNER.reset()
            self.assertEqual(('docker', 'volume', 'rm', 'spoolin-local-gradle'), command.call_args.args)


if __name__ == '__main__':
    unittest.main()
