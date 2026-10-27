"""Expose only project discovery and bounded log streams to Alloy."""
import http.client
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
import json
import re
import socket
import threading
import time
from urllib.parse import parse_qs, urlencode, urlsplit

PROJECT = 'spoolin-local'
SLOTS = threading.BoundedSemaphore(16)


class DockerConnection(http.client.HTTPConnection):
    def __init__(self):
        super().__init__('localhost', timeout=30)

    def connect(self):
        self.sock = socket.socket(socket.AF_UNIX, socket.SOCK_STREAM)
        self.sock.settimeout(self.timeout)
        self.sock.connect('/var/run/docker.sock')


def request(path):
    connection = DockerConnection()
    try:
        connection.request('GET', path)
        response = connection.getresponse()
        data = response.read(2 * 1024 * 1024 + 1)
        if response.status != 200 or len(data) > 2 * 1024 * 1024:
            raise ValueError('Docker response unavailable or oversized')
        return json.loads(data)
    finally:
        connection.close()


def route(target):
    parsed = urlsplit(target)
    path = re.sub(r'^/v[0-9]+\.[0-9]+/', '/', parsed.path)
    if path in ('/_ping', '/version') and not parsed.query:
        return 'metadata', path
    if path in ('/containers/json', '/networks'):
        filters = json.dumps({'label': [f'com.docker.compose.project={PROJECT}']})
        return ('discovery' if path == '/containers/json' else 'networks'), path + '?' + urlencode({'filters': filters})
    state = re.fullmatch(r'/containers/([a-f0-9]{64})/json', path)
    if state and not parsed.query:
        return 'inspect', path
    match = re.fullmatch(r'/containers/([a-f0-9]{64})/logs', path)
    if not match:
        raise ValueError('Endpoint denied')
    return match[1], path + '?' + urlencode(log_parameters(parsed.query))


def log_parameters(query):
    parameters = parse_qs(query, strict_parsing=True)
    allowed = {'stdout', 'stderr', 'follow', 'timestamps', 'since', 'until', 'tail'}
    if set(parameters) - allowed or any(len(value) != 1 for value in parameters.values()):
        raise ValueError('Log options denied')
    for key, values in parameters.items():
        if not valid_option(key, values[0]):
            raise ValueError('Invalid log option')
    parameters['tail'] = [str(min(int(parameters.get('tail', ['1000'])[0]), 1000))]
    return {key: value[0] for key, value in parameters.items()}


def valid_option(key, value):
    if key in ('stdout', 'stderr', 'follow', 'timestamps'):
        return value in ('0', '1', 'true', 'false')
    pattern = r'[0-9]{1,12}' if key == 'tail' else r'[0-9]{1,12}(\.[0-9]{1,9})?'
    return bool(re.fullmatch(pattern, value))


def discover(path):
    containers = request(path)
    # Never expose mount paths, command lines, ports or unrelated labels.
    return [{'Id': item['Id'], 'Names': item.get('Names', []),
             'Labels': {key: value for key, value in item.get('Labels', {}).items()
                        if key in ('com.docker.compose.project', 'com.docker.compose.service')},
             'NetworkSettings': {'Networks': {name: {'IPAddress': network.get('IPAddress', ''), 'NetworkID': network.get('NetworkID', '')}
                                            for name, network in item.get('NetworkSettings', {}).get('Networks', {}).items()}},
             'Ports': []}
            for item in containers if item.get('Labels', {}).get('com.docker.compose.project') == PROJECT]


def networks(path):
    return [{key: item.get(key) for key in ('Id', 'Name', 'Scope', 'Internal', 'Ingress')}
            for item in request(path) if item.get('Labels', {}).get('com.docker.compose.project') == PROJECT]


def owned_info(path):
    info = request(path)
    if info.get('Config', {}).get('Labels', {}).get('com.docker.compose.project') != PROJECT:
        raise ValueError('Container denied')
    return info


def safe_state(path):
    info = owned_info(path)
    return {'Id': info['Id'], 'Config': {'Tty': info.get('Config', {}).get('Tty', False)},
            'State': {key: info['State'][key] for key in ('Running', 'FinishedAt')}}


class Handler(BaseHTTPRequestHandler):
    def log_message(self, *_args):
        pass

    def do_HEAD(self):
        if self.path != '/_ping':
            self.send_error(403)
            return
        self.send_response(200)
        self.send_header('API-Version', request('/version')['ApiVersion'])
        self.send_header('Content-Length', '0')
        self.end_headers()

    def do_GET(self):
        if not SLOTS.acquire(blocking=False):
            self.send_error(503)
            return
        try:
            self.serve_request()
        except (ValueError, KeyError, TypeError):
            self.send_error(403)
        except (OSError, http.client.HTTPException):
            self.close_connection = True
        finally:
            SLOTS.release()

    def serve_request(self):
        kind, path = route(self.path)
        if kind in ('discovery', 'networks'):
            data = json.dumps(discover(path) if kind == 'discovery' else networks(path)).encode()
            return self.reply(data, 'application/json')
        if kind == 'metadata':
            if path == '/_ping':
                return self.reply(b'OK', 'text/plain')
            version = request('/version')
            return self.reply(json.dumps({key: version[key] for key in ('ApiVersion', 'MinAPIVersion')}).encode(), 'application/json')
        if kind == 'inspect':
            return self.reply(json.dumps(safe_state(path)).encode(), 'application/json')
        owned_info('/containers/' + kind + '/json')
        self.stream(path)

    def reply(self, data, content_type):
        self.send_response(200)
        self.send_header('Content-Type', content_type)
        self.send_header('Content-Length', str(len(data)))
        self.end_headers()
        self.wfile.write(data)

    def stream(self, path):
        connection = DockerConnection()
        try:
            connection.request('GET', path)
            response = connection.getresponse()
            if response.status != 200:
                raise ValueError('Log stream unavailable')
            self.send_response(200)
            self.send_header('Content-Type', 'application/vnd.docker.raw-stream')
            self.end_headers()
            # A finite connection lifetime bounds resources; Alloy reconnects using its offset.
            total = 0
            deadline = time.monotonic() + 300
            while time.monotonic() < deadline and total < 16 * 1024 * 1024:
                chunk = response.read1(16384)
                if not chunk:
                    break
                self.wfile.write(chunk)
                self.wfile.flush()
                total += len(chunk)
        finally:
            connection.close()


if __name__ == '__main__':
    server = ThreadingHTTPServer(('0.0.0.0', 2375), Handler)
    server.daemon_threads = True
    server.serve_forever()
