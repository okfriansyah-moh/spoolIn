#!/usr/bin/env python3
"""Run an isolated local installation without host Java or Node."""
import argparse
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import time

ROOT = Path(__file__).resolve().parents[2]
STATE = ROOT / '.local'
TOOL = 'spoolin-local-toolchain:21.0.12.1-24.21.0'
OWNERS = ('core', 'catalog', 'media')
SERVICES = ('postgres', *OWNERS, 'core-migrator', 'catalog-migrator', 'media-migrator', 'catalog-seed')


def run(*args, capture=False, **kwargs):
    kwargs.setdefault('timeout', 1800)
    return subprocess.run(args, cwd=ROOT, check=True, text=True,
                          stdout=subprocess.PIPE if capture else None,
                          stderr=subprocess.PIPE if capture else None, **kwargs)


def compose(*args, capture=False):
    return run('docker', 'compose', '--project-name', 'spoolin-local', '-f',
               str(STATE / 'compose.json'), *args, capture=capture)


def tooling(*args, root=False, cache=True):
    mounts = ['-v', 'spoolin-local-gradle:/workspace/.local/gradle'] if cache else []
    identity = [] if root else ['--user', f'{os.getuid()}:{os.getgid()}']
    return run('docker', 'run', '--rm', *identity, '-v', f'{ROOT}:/workspace',
               *mounts, TOOL, *args)


def check_tools():
    if not shutil.which('docker'):
        raise RuntimeError('Install Docker and start its engine; see docs/local-setup.md.')
    run('docker', 'info', capture=True)
    run('docker', 'compose', 'version', capture=True)


def validate_state():
    if STATE.is_symlink():
        raise RuntimeError('Refusing symlink at .local; use a real directory inside this checkout.')


def setup():
    validate_state()
    STATE.mkdir(mode=0o700, exist_ok=True)
    STATE.chmod(0o700)
    if not (STATE / 'environment.json').exists():
        if (STATE / 'runtime').exists() or (STATE / 'identities').exists():
            raise RuntimeError('Incomplete provisioning exists in .local; inspect it before local-reset.')
        run('docker', 'build', '-t', TOOL, '-f', str(ROOT / 'ops/local/toolchain.Dockerfile'), str(ROOT / 'ops/local'))
        tooling('python3', 'ops/local/provision.py', str(ROOT), str(os.getuid()), str(os.getgid()), root=True, cache=False)
    render_compose()
    print('Local configuration ready. Credentials: .local/accounts.txt (keep this file private).')


def local_configuration(document):
    document['name'] = 'spoolin-local'
    document['services'] = {name: document['services'][name] for name in SERVICES}
    for name, service in document['services'].items():
        owner = name.split('-')[0]
        service['image'] = f'spoolin-local-{owner}:development'
    document['services']['media']['security_opt'] = [
        'no-new-privileges:true', 'systempaths=unconfined', f'seccomp={ROOT / "ops/security/media-seccomp.json"}']
    for service in document['services'].values():
        service.pop('depends_on', None)
    document['services']['local-readiness'] = {
        'image': TOOL, 'user': '10001:10001', 'read_only': True,
        'cap_drop': ['ALL'], 'security_opt': ['no-new-privileges:true'],
        'tmpfs': ['/tmp'], 'networks': ['private'], 'profiles': ['check'],
        'secrets': ['service-ca', 'core-key', 'core-cert'],
        'volumes': [f'{ROOT / "ops/local/readiness.py"}:/probe.py:ro'],
        'entrypoint': ['python3', '/probe.py'], 'mem_limit': '128m', 'cpus': 0.25}
    return document


def render_compose():
    environment = {key: value for key, value in os.environ.items()
                   if key in ('PATH', 'HOME', 'DOCKER_HOST', 'DOCKER_CONTEXT', 'DOCKER_CONFIG', 'DOCKER_CERT_PATH', 'DOCKER_TLS_VERIFY')}
    environment.update(json.loads((STATE / 'environment.json').read_text()))
    # A local installation always uses its own loopback port, independent of deployment exports.
    environment['CORE_HTTPS_PORT'] = '8443'
    rendered = run('docker', 'compose', '--env-file', '/dev/null', '-f', str(ROOT / 'ops/compose.yaml'), '--profile', 'migration', '--profile', 'seed', 'config',
                   '--format', 'json', capture=True, env=environment)
    document = local_configuration(json.loads(rendered.stdout))
    destination = STATE / 'compose.json'
    destination.write_text(json.dumps(document, indent=2) + '\n')
    destination.chmod(0o600)


def build_sources(test=False):
    (STATE / 'gradle').mkdir(exist_ok=True)
    run('docker', 'build', '-t', TOOL, '-f', str(ROOT / 'ops/local/toolchain.Dockerfile'), str(ROOT / 'ops/local'))
    tooling('python3', '-c', f'import os; os.chown("/workspace/.local/gradle", {os.getuid()}, {os.getgid()})', root=True)
    if test:
        tooling('python3', 'ops/local/test_runner.py')
    tooling('npm', 'ci')
    tooling('./gradlew', '--no-daemon', '--console', 'plain', 'generateContracts',
            ':services:core:bootJar', ':services:catalog:bootJar', ':services:media:bootJar')
    if test:
        tooling('npm', 'run', 'check')
        tooling('npm', 'run', 'test:coverage', '--workspace', 'frontend')
        tooling('npm', 'run', 'test:coverage', '--workspace', 'mcp')
        tooling('./gradlew', '--no-daemon', '--console', 'plain', 'test')


def migrate(owner):
    compose('exec', '-T', 'postgres', '/opt/spoolin/membership.sh', 'grant', owner)
    try:
        compose('--profile', 'migration', 'run', '--rm', '--no-deps', f'{owner}-migrator', '--migrate')
        compose('exec', '-T', 'postgres', '/opt/spoolin/membership.sh', 'grants', owner)
    finally:
        compose('exec', '-T', 'postgres', '/opt/spoolin/membership.sh', 'revoke', owner)


def codec_probe():
    # Run Bubblewrap under the actual Media service security settings before reporting success.
    command = ['/usr/bin/bwrap', '--unshare-all', '--die-with-parent', '--new-session',
               '--ro-bind', '/usr', '/usr', '--ro-bind', '/lib', '/lib',
               '--proc', '/proc', '--dev', '/dev', '--tmpfs', '/tmp',
               '--clearenv', '--setenv', 'PATH', '/usr/bin', '/usr/bin/true']
    try:
        compose('run', '--rm', '--no-deps', '--entrypoint', command[0], 'media', *command[1:])
    except subprocess.CalledProcessError as error:
        raise RuntimeError('Media codec isolation failed. This Docker host must permit unprivileged '
                           'user namespaces under the checked-in seccomp policy. No sandbox bypass was applied.') from error


def ready():
    deadline = time.monotonic() + 300
    while time.monotonic() < deadline:
        try:
            compose('--profile', 'check', 'run', '--rm', '--no-deps', 'local-readiness', capture=True)
            return
        except subprocess.CalledProcessError:
            time.sleep(2)
    raise RuntimeError('Readiness timed out after 300 seconds. Run make local-logs.')


def start():
    setup()
    build_sources()
    compose('build', 'postgres', *OWNERS)
    codec_probe()
    compose('up', '-d', '--wait', '--wait-timeout', '120', 'postgres')
    for owner in OWNERS:
        migrate(owner)
    compose('run', '--rm', '--no-deps', 'core', '--initialize-accounts=/run/spoolin-config/account-initialization.json')
    compose('--profile', 'seed', 'run', '--rm', '--no-deps', 'catalog-seed')
    compose('up', '-d', *OWNERS)
    ready()
    print('SpoolIn is ready at https://localhost:8443. Credentials: .local/accounts.txt')


def reset():
    validate_state()
    if input('Delete ALL local SpoolIn data and credentials? Type DELETE: ') != 'DELETE':
        raise RuntimeError('Reset cancelled; no data deleted.')
    if (STATE / 'compose.json').exists():
        compose('down', '--volumes', '--remove-orphans')
    if STATE.exists():
        # UID10001 owns protected identities; remove only this installation directory.
        tooling('python3', '-c', 'import shutil; shutil.rmtree("/workspace/.local")', root=True, cache=False)
    caches = run('docker', 'volume', 'ls', '--filter', 'name=^spoolin-local-gradle$',
                 '--format', '{{.Name}}', capture=True).stdout.splitlines()
    if 'spoolin-local-gradle' in caches:
        run('docker', 'volume', 'rm', 'spoolin-local-gradle')
    print('Local data and credentials deleted. Run make local-start for a fresh installation.')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('action', choices=('setup', 'start', 'status', 'logs', 'stop', 'test', 'reset'))
    action = parser.parse_args().action
    validate_state()
    check_tools()
    STATE.mkdir(mode=0o700, exist_ok=True)
    lock = STATE / 'action.lock'
    try:
        lock.mkdir(mode=0o700)
    except FileExistsError as error:
        raise RuntimeError('Another local command is running (.local/action.lock). If interrupted, confirm no local command remains before removing this empty directory.') from error
    try:
        dispatch(action)
    finally:
        if lock.exists():
            lock.rmdir()


def dispatch(action):
    if action in ('setup', 'start', 'reset'):
        return {'setup': setup, 'start': start, 'reset': reset}[action]()
    if not (STATE / 'compose.json').exists():
        raise RuntimeError('Run make local-setup first.')
    if action == 'status':
        compose('ps', '--all')
        ready()
        codec_probe()
    elif action == 'logs':
        compose('logs', '--tail', '200', *OWNERS, 'postgres')
    elif action == 'stop':
        compose('down', '--remove-orphans')
    elif action == 'test':
        build_sources(test=True)


if __name__ == '__main__':
    try:
        main()
    except (RuntimeError, subprocess.SubprocessError) as failure:
        print(f'local: {failure}', file=sys.stderr)
        sys.exit(1)
