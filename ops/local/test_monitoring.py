"""Behavior checks for local monitoring boundaries and preserved state."""
import copy
import importlib.util
import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch

ROOT = Path(__file__).resolve().parents[2]


def load(name, path):
    spec = importlib.util.spec_from_file_location(name, ROOT / path)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


MONITOR = load('monitoring_test_module', 'ops/local/monitoring.py')
PROXY = load('proxy_test_module', 'ops/monitoring/docker_log_proxy.py')
RUNNER = load('monitoring_runner', 'ops/local/run.py')


class MonitoringConfigurationTest(unittest.TestCase):
    def test_port_validation_and_saved_override(self):
        for value in ('0', '1023', '65536', 'invalid', '3000;exit'):
            with self.assertRaises(RuntimeError):
                MONITOR.configured_port(value)
        with tempfile.TemporaryDirectory() as directory:
            state = Path(directory)
            (state / 'monitoring-port').write_text('3300\n')
            with patch.dict(MONITOR.os.environ, {}, clear=True):
                self.assertEqual(3300, MONITOR.port_for(state))
            with patch.dict(MONITOR.os.environ, {'GRAFANA_HTTPS_PORT': '3400'}):
                self.assertEqual(3400, MONITOR.port_for(state))

    def test_optional_stack_preserves_app_and_security(self):
        # A deployment-shaped fixture avoids relying on developer credentials or a running engine.
        service = {'volumes': [], 'command': ['--config.file=original'], 'image': 'original', 'read_only': True}
        deployment = {'services': {name: copy.deepcopy(service) for name in MONITOR.SERVICES[:3]}}
        document = {'services': {'core': {'image': 'unchanged'}}, 'secrets': {}, 'volumes': {}}
        with tempfile.TemporaryDirectory() as directory:
            state = Path(directory)
            result = MONITOR.configure(document, deployment, ROOT, state, 3300)
            self.assertEqual('unchanged', result['services']['core']['image'])
            self.assertEqual('127.0.0.1', result['services']['grafana']['ports'][0]['host_ip'])
            for name in MONITOR.SERVICES:
                self.assertIn(name, result['services'])
            for name in MONITOR.SERVICES[3:]:
                self.assertNotIn('ports', result['services'][name])
            self.assertEqual(['docker-logs'], result['services']['docker-log-proxy']['networks'])
            self.assertNotIn('/var/run/docker.sock', str(result['services']['alloy']))
            self.assertIn('class="infrastructure"', (state / 'monitoring-alertmanager.yaml').read_text())
            self.assertIn('receiver: spoolin-core', (state / 'monitoring-alertmanager.yaml').read_text())
            self.assertIn('local-scrapes.yaml', (state / 'monitoring-prometheus.yaml').read_text())

    def test_stop_only_stops_monitoring_and_preserves_history(self):
        with tempfile.TemporaryDirectory() as directory:
            state = Path(directory)
            (state / 'compose.json').write_text(json.dumps({'services': {name: {} for name in MONITOR.SERVICES}}))
            with patch.object(RUNNER, 'STATE', state), patch.object(RUNNER, 'compose') as compose:
                RUNNER.dispatch('monitoring-stop')
            compose.assert_called_once_with('stop', *MONITOR.SERVICES)

    def test_start_requires_existing_running_application(self):
        with patch.object(RUNNER, 'compose') as compose, patch.object(RUNNER, 'tooling') as tooling:
            compose.return_value.stdout = 'postgres\ncore\n'
            with self.assertRaisesRegex(RuntimeError, 'Application services'):
                RUNNER.start_monitoring()
            tooling.assert_not_called()


class DockerProxyBoundaryTest(unittest.TestCase):
    def test_rejects_inspect_exec_archive_and_invalid_ids(self):
        for path in ('/containers/abc/json', '/containers/create', '/images/json', '/info',
                     '/containers/' + 'a' * 64 + '/archive', '/containers/' + 'b' * 12 + '/logs'):
            with self.assertRaises(ValueError):
                PROXY.route(path)

    def test_discovery_forces_project_filter(self):
        kind, path = PROXY.route('/v1.47/containers/json?all=true&filters=malicious')
        self.assertEqual('discovery', kind)
        self.assertIn('spoolin-local', path)
        self.assertNotIn('malicious', path)
        self.assertNotIn('all=true', path)

    def test_log_queries_are_bounded(self):
        target = '/containers/' + 'a' * 64 + '/logs'
        kind, path = PROXY.route(target + '?stdout=1&follow=true&tail=999999')
        self.assertEqual('a' * 64, kind)
        self.assertIn('tail=1000', path)
        for query in ('tail=all', 'stdout=1&stdout=0', 'details=true', 'since=bad', 'stdout=yes', 'tail=1.2'):
            with self.assertRaises(ValueError):
                PROXY.route(target + '?' + query)

    def test_discovery_strips_sensitive_metadata_and_other_projects(self):
        selected = {'Id': 'a' * 64, 'Command': 'password', 'Mounts': ['private'],
                    'Labels': {'com.docker.compose.project': 'spoolin-local',
                               'com.docker.compose.service': 'core', 'private': 'secret'},
                    'NetworkSettings': {'Networks': {'private': {'IPAddress': '172.1.0.2', 'EndpointID': 'private'}}}}
        other = {'Id': 'b' * 64, 'Labels': {'com.docker.compose.project': 'unrelated'}}
        with patch.object(PROXY, 'request', return_value=[selected, other]):
            result = PROXY.discover('/containers/json')
        self.assertEqual(1, len(result))
        self.assertNotIn('password', json.dumps(result))
        self.assertNotIn('secret', json.dumps(result))
        self.assertNotIn('EndpointID', json.dumps(result))
        self.assertEqual('172.1.0.2', result[0]['NetworkSettings']['Networks']['private']['IPAddress'])

    def test_foreign_container_logs_are_denied_before_streaming(self):
        handler = object.__new__(PROXY.Handler)
        handler.path = '/containers/' + 'a' * 64 + '/logs?stdout=1'
        with patch.object(PROXY, 'request', return_value={'Config': {'Labels': {'com.docker.compose.project': 'unrelated'}}}), patch.object(handler, 'stream') as stream:
            with self.assertRaises(ValueError):
                handler.serve_request()
            stream.assert_not_called()

class MonitoringLifecycleTest(unittest.TestCase):
    def test_start_provisions_monitoring_only_and_keeps_application_running(self):
        with tempfile.TemporaryDirectory() as directory:
            state = Path(directory)
            with patch.object(RUNNER, 'STATE', state), patch.object(RUNNER.MONITOR, 'port_for', return_value=3300), patch.object(RUNNER, 'compose') as compose, patch.object(RUNNER, 'tooling') as tooling, patch.object(RUNNER, 'render_compose') as render, patch.object(RUNNER, 'provision_monitor_role') as role:
                compose.return_value.stdout = 'postgres\ncore\ncatalog\nmedia\n'
                RUNNER.start_monitoring()
            self.assertTrue((state / 'monitoring-enabled').exists())
            self.assertEqual('3300\n', (state / 'monitoring-port').read_text())
            self.assertIn('--monitoring', tooling.call_args.args)
            render.assert_called_once_with(monitoring=True)
            role.assert_called_once()
            up = compose.call_args.args
            self.assertIn('--no-deps', up)
            self.assertNotIn('core', up)
            self.assertNotIn('postgres', up)

    def test_role_provision_uses_secret_files_and_verified_tls(self):
        with tempfile.TemporaryDirectory() as directory:
            state = Path(directory)
            (state / 'compose.json').write_text(json.dumps({'services': {'postgres': {'image': 'existing-postgres'}}, 'secrets': {'postgres-password': {'file': '/protected/admin'}}}))
            with patch.object(RUNNER, 'STATE', state), patch.object(RUNNER, 'compose') as compose, patch.object(RUNNER, 'run') as run:
                compose.return_value.stdout = 'owned-postgres-id\n'
                RUNNER.provision_monitor_role()
            arguments = run.call_args.args
            self.assertIn('container:owned-postgres-id', arguments)
            self.assertIn('PGSSLMODE=verify-full', arguments)
            self.assertIn('existing-postgres', arguments)
            self.assertIn('NOSUPERUSER', arguments[-1])
            self.assertIn('default_transaction_read_only', arguments[-1])

class DockerProxyTransportTest(unittest.TestCase):
    def test_http_boundaries_and_safe_discovery(self):
        import http.client
        import threading
        server = PROXY.ThreadingHTTPServer(('127.0.0.1', 0), PROXY.Handler)
        thread = threading.Thread(target=server.serve_forever, daemon=True)
        thread.start()
        try:
            for method, path, expected in [('GET', '/_ping', 200), ('POST', '/containers/create', 501),
                                           ('GET', '/containers/any/json', 403), ('GET', '/info', 403)]:
                connection = http.client.HTTPConnection('127.0.0.1', server.server_port, timeout=2)
                connection.request(method, path)
                response = connection.getresponse()
                self.assertEqual(expected, response.status)
                response.read()
                connection.close()
            with patch.object(PROXY, 'request', return_value={'ApiVersion': '1.47', 'MinAPIVersion': '1.24', 'private': 'secret'}):
                connection = http.client.HTTPConnection('127.0.0.1', server.server_port, timeout=2)
                connection.request('GET', '/version')
                response = connection.getresponse()
                self.assertEqual(200, response.status)
                self.assertNotIn(b'secret', response.read())
                connection.close()
            with patch.object(PROXY, 'discover', return_value=[]):
                connection = http.client.HTTPConnection('127.0.0.1', server.server_port, timeout=2)
                connection.request('GET', '/containers/json')
                self.assertEqual(b'[]', connection.getresponse().read())
                connection.close()
        finally:
            server.shutdown()
            server.server_close()
            thread.join(timeout=2)

    def test_daemon_metadata_is_bounded_and_connections_close(self):
        from unittest.mock import MagicMock
        connection = MagicMock()
        connection.getresponse.return_value.status = 200
        connection.getresponse.return_value.read.return_value = b'{"safe":true}'
        with patch.object(PROXY, 'DockerConnection', return_value=connection):
            self.assertEqual({'safe': True}, PROXY.request('/version'))
        connection.close.assert_called_once()
        connection.reset_mock()
        connection.getresponse.return_value.status = 500
        with patch.object(PROXY, 'DockerConnection', return_value=connection), self.assertRaises(ValueError):
            PROXY.request('/version')
        connection.close.assert_called_once()
        connection.getresponse.return_value.status = 200
        connection.getresponse.return_value.read.return_value = b'x' * (2 * 1024 * 1024 + 1)
        with patch.object(PROXY, 'DockerConnection', return_value=connection), self.assertRaises(ValueError):
            PROXY.request('/version')

    def test_log_stream_forwards_binary_frames_and_closes_daemon_connection(self):
        import io
        from unittest.mock import MagicMock
        handler = object.__new__(PROXY.Handler)
        handler.wfile = io.BytesIO()
        connection = MagicMock()
        response = connection.getresponse.return_value
        response.status = 200
        frame = b'\x01\x00\x00\x00\x00\x00\x00\x05hello'
        response.read1.side_effect = [frame, b'']
        with patch.object(PROXY, 'DockerConnection', return_value=connection), patch.object(handler, 'send_response'), patch.object(handler, 'send_header'), patch.object(handler, 'end_headers'):
            handler.stream('/containers/owned/logs')
        self.assertEqual(frame, handler.wfile.getvalue())
        connection.close.assert_called_once()
        response.status = 404
        with patch.object(PROXY, 'DockerConnection', return_value=connection), self.assertRaises(ValueError):
            handler.stream('/containers/owned/logs')

    def test_socket_access_is_unix_only_with_timeout(self):
        from unittest.mock import MagicMock
        sock = MagicMock()
        with patch.object(PROXY.socket, 'socket', return_value=sock):
            connection = PROXY.DockerConnection()
            connection.connect()
        sock.settimeout.assert_called_once_with(30)
        sock.connect.assert_called_once_with('/var/run/docker.sock')

    def test_busy_proxy_rejects_requests_without_opening_daemon(self):
        from unittest.mock import MagicMock
        handler = object.__new__(PROXY.Handler)
        slots = MagicMock()
        slots.acquire.return_value = False
        with patch.object(PROXY, 'SLOTS', slots), patch.object(handler, 'send_error') as error, patch.object(handler, 'serve_request') as serve:
            handler.do_GET()
        error.assert_called_once_with(503)
        serve.assert_not_called()

    def test_daemon_failure_does_not_expose_exception_details(self):
        handler = object.__new__(PROXY.Handler)
        with patch.object(handler, 'serve_request', side_effect=OSError('private path')):
            handler.do_GET()
        self.assertTrue(handler.close_connection)

class DockerNetworkDiscoveryTest(unittest.TestCase):
    def test_network_discovery_is_project_scoped_and_sanitized(self):
        kind, path = PROXY.route('/v1.47/networks?filters=other')
        self.assertEqual('networks', kind)
        self.assertIn('spoolin-local', path)
        with patch.object(PROXY, 'request', return_value=[
                {'Id': 'owned', 'Name': 'private', 'Labels': {'com.docker.compose.project': 'spoolin-local'}, 'Containers': {'secret': 'metadata'}},
                {'Id': 'foreign', 'Labels': {'com.docker.compose.project': 'another'}}]):
            result = PROXY.networks(path)
        self.assertEqual('owned', result[0]['Id'])
        self.assertEqual(1, len(result))
        self.assertNotIn('secret', json.dumps(result))

    def test_head_ping_supports_docker_api_negotiation(self):
        handler = object.__new__(PROXY.Handler)
        handler.path = '/_ping'
        with patch.object(PROXY, 'request', return_value={'ApiVersion': '1.47'}), patch.object(handler, 'send_response') as response, patch.object(handler, 'send_header') as header, patch.object(handler, 'end_headers'):
            handler.do_HEAD()
        response.assert_called_once_with(200)
        header.assert_any_call('API-Version', '1.47')


class DockerContainerStateTest(unittest.TestCase):
    def test_only_tty_and_lifecycle_survive_container_inspection(self):
        target = '/containers/' + 'a' * 64 + '/json'
        info = {'Id': 'a' * 64, 'Config': {'Tty': False, 'Env': ['PASSWORD=private'],
                'Labels': {'com.docker.compose.project': 'spoolin-local'}},
                'Mounts': ['private-path'], 'State': {'Running': True, 'FinishedAt': '0001-01-01T00:00:00Z', 'Pid': 999}}
        self.assertEqual(('inspect', target), PROXY.route(target))
        with patch.object(PROXY, 'request', return_value=info):
            result = PROXY.safe_state(target)
        self.assertEqual({'Id': 'a' * 64, 'Config': {'Tty': False},
                          'State': {'Running': True, 'FinishedAt': '0001-01-01T00:00:00Z'}}, result)
        self.assertNotIn('private', json.dumps(result))
        with patch.object(PROXY, 'request', return_value={'Config': {'Labels': {'com.docker.compose.project': 'foreign'}}}):
            with self.assertRaises(ValueError):
                PROXY.safe_state(target)
        with self.assertRaises(ValueError):
            PROXY.route(target + '?size=1')


if __name__ == '__main__':
    unittest.main()
