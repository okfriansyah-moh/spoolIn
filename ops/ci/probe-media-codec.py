#!/usr/bin/env python3
"""Unchanged nonroot production Media container boundary; no database or UI effects."""
import hashlib
from datetime import datetime, timezone
import importlib.util
import json
import os
from pathlib import Path
import re
import shlex
import struct
import subprocess
import time
import uuid

ROOT = Path(__file__).resolve().parents[2]
SPEC = importlib.util.spec_from_file_location('codec_diagnostics', Path(__file__).with_name('codec-diagnostics.py'))
DIAG = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(DIAG)
INSPECT = '{"id":{{json .Id}},"image":{{json .Image}},"user":{{json .Config.User}},"readOnly":{{json .HostConfig.ReadonlyRootfs}},"capDrop":{{json .HostConfig.CapDrop}},"capAdd":{{json .HostConfig.CapAdd}},"privileged":{{json .HostConfig.Privileged}},"securityOpt":{{json .HostConfig.SecurityOpt}},"apparmor":{{json .AppArmorProfile}},"networkMode":{{json .HostConfig.NetworkMode}},"mounts":{{json .Mounts}}}'


STATUS_READER = """while IFS=: read -r key value; do
  case "$key" in
    CapEff|CapBnd|NoNewPrivs|Seccomp) printf '%s:%s\n' "$key" "$value" ;;
  esac
done"""


def require(value, message):
    if not value:
        raise RuntimeError(message)


def compose(*arguments):
    return ['docker', 'compose', '--project-name', os.environ['SPOOLIN_INSTALLATION'], '-f', str(ROOT / 'ops/compose.yaml'), *arguments]


class ProbeCommandFailure(RuntimeError):
    def __init__(self, boundary, state, diagnostic):
        super().__init__('Required bounded container command failed')
        self.report = {'boundary': boundary, 'state': state}
        if diagnostic:
            self.report['diagnostic'] = diagnostic[:4096]
            self.report['retainedByteLimit'] = 4096


def checked(command, limit=16384, seconds=20, boundary='bounded-command', diagnostic=False):
    state, output = DIAG.bounded_command(command, limit, seconds, include_stderr=diagnostic)
    if state['exit'] != 0 or state['truncated'] or state['timedOut']:
        raise ProbeCommandFailure(boundary, state, output if diagnostic else '')
    return output.strip()


def inspect(container):
    return json.loads(checked(['docker', 'inspect', '--format', INSPECT, container]))


def effective_configuration(primary, probe):
    for config in (primary, probe):
        require(config['user'] == '10001:10001' and config['readOnly'] is True, 'Nonroot/read-only production settings required')
        require(config['privileged'] is False and not config['capAdd'] and config['capDrop'] == ['ALL'], 'Production capability restrictions required')
        require(any(value in ('no-new-privileges', 'no-new-privileges:true') for value in config['securityOpt'] or []), 'No-new-privileges required')
        require(config['networkMode'] not in ('host', 'none', 'default', ''), 'Actual private application network required')
    require(all(primary[key] == probe[key] for key in ('image', 'user', 'readOnly', 'capDrop', 'capAdd', 'privileged', 'securityOpt', 'apparmor', 'networkMode')),
            'Probe differs from running production container security/image settings')
    media = lambda config: [mount for mount in config['mounts'] if mount['Destination'] == '/var/spoolin/media']
    source, target = media(primary), media(probe)
    require(len(source) == len(target) == 1 and source[0]['Type'] == target[0]['Type'] == 'volume'
            and source[0]['Source'] == target[0]['Source'] and source[0]['RW'] and target[0]['RW'], 'Actual owned Media volume required')
    return {key: probe[key] for key in ('image', 'user', 'readOnly', 'capDrop', 'capAdd', 'privileged', 'securityOpt', 'networkMode')} | {
        'apparmor': DIAG.profile_name(probe['apparmor']),
        'mounts': [{'type': mount['Type'], 'destination': mount['Destination'], 'writable': mount['RW']} for mount in probe['mounts']]}


def context(container):
    script = '''printf 'uid=%s\ngid=%s\n' "$(id -u)" "$(id -g)"
''' + STATUS_READER + ''' < /proc/self/status
printf 'profile='; cat /proc/self/attr/current
for kind in user mnt net pid ipc uts cgroup; do printf 'ns.%s=' "$kind"; readlink "/proc/self/ns/$kind"; done'''
    value = checked(['docker', 'exec', container, '/bin/sh', '-ec', script], limit=4096,
                    boundary='process-context-command', diagnostic=True)
    output = {'status': DIAG.status_fields(value), 'namespaces': {}}
    for line in value.splitlines():
        key, separator, raw = line.partition('=')
        if separator and key in ('uid', 'gid') and raw.isdecimal():
            output[key] = int(raw)
        elif separator and key == 'profile':
            output['profile'] = DIAG.profile_name(raw)
        elif key.startswith('ns.') and key[3:] in DIAG.NAMESPACES and re.fullmatch(key[3:] + r':\[[0-9]{1,20}\]', raw):
            output['namespaces'][key[3:]] = raw
    require(output.get('uid') == output.get('gid') == 10001, 'Effective process identity is not Media UID/GID10001')
    require(output['status'].get('NoNewPrivs') == '1' and int(output['status'].get('CapEff', '1'), 16) == 0
            and int(output['status'].get('CapBnd', '1'), 16) == 0, 'Effective process restrictions differ')
    require(set(output['namespaces']) == set(DIAG.NAMESPACES), 'Missing process namespace observations')
    return output


def sandbox(input_file, output_file, limit, arguments, namespace_file):
    # Same fixed mount/environment/resource arguments as CodecSandbox; info-fd only records created namespace IDs.
    bound_name = 'dimensions' if arguments[0].endswith('spoolin-image-header') else Path(output_file).name
    return ['/usr/bin/bwrap', '--info-fd', '3', '--unshare-all', '--die-with-parent', '--new-session',
            '--ro-bind', '/usr', '/usr', '--ro-bind', '/lib', '/lib', '--ro-bind', '/lib64', '/lib64',
            '--proc', '/proc', '--dev', '/dev', '--size', '25161728', '--tmpfs', '/tmp',
            '--ro-bind', input_file, '/input', '--dir', '/output', '--bind', output_file, '/output/' + bound_name,
            '--remount-ro', '/', '--chdir', '/output', '--clearenv', '--setenv', 'PATH', '/usr/local/bin:/usr/bin',
            '--setenv', 'LD_LIBRARY_PATH', '/usr/local/lib/x86_64-linux-gnu:/usr/local/lib/aarch64-linux-gnu',
            '--setenv', 'TMPDIR', '/tmp', '--setenv', 'VIPS_CONCURRENCY', '1', '/usr/bin/prlimit', '--as=268435456',
            '--cpu=5', '--fsize=' + str(limit), '--nofile=64', '--', *arguments], namespace_file


def pipeline(workspace):
    commands = ['set -eu', 'umask 077', 'printf "stage=versions\\n"',
                '/usr/local/bin/vips --version', '/usr/bin/bwrap --version', '/usr/bin/prlimit --version']
    stages = [('header-input', workspace + '/input', 'dimensions', 64,
               ['/usr/local/bin/spoolin-image-header', '/input', '/output/dimensions'])]
    options = {'jpeg': '[Q=85,optimize_coding=true,strip=true]', 'png': '[compression=6,strip=true]', 'webp': '[Q=85,strip=true]'}
    for kind, settings in options.items():
        name = 'encoded.' + kind
        stages.append(('encode-' + kind, workspace + '/input', name, 8388608,
                       ['/usr/local/bin/vips', 'autorot', '/input', '/output/' + name + settings,
                        '--vips-concurrency', '1', '--vips-cache-max', '0']))
        stages.append(('header-' + kind, workspace + '/' + name, 'dimensions-' + kind, 64,
                       ['/usr/local/bin/spoolin-image-header', '/input', '/output/dimensions']))
    for label, source, output, limit, args in stages:
        destination = workspace + '/' + output
        argv, info = sandbox(source, destination, limit, args, workspace + '/' + label + '.namespace')
        commands += ['printf ' + shlex.quote('stage=' + label + '\n'), ': > ' + shlex.quote(destination),
                     shlex.join(argv) + ' 3>' + shlex.quote(info) + ' 2>' + shlex.quote(workspace + '/' + label + '.log')]
    commands.append('printf "stage=completed\\n"')
    return '\n'.join(commands) + '\n', [stage[0] for stage in stages]


def media_write(workspace, file, value):
    script = 'umask 077; test "$(id -u)" = 10001; ' + ('mkdir -m 700 ' + shlex.quote(workspace) + '; ' if file == 'input' else '')
    script += 'cat > ' + shlex.quote(workspace + '/' + file)
    result = subprocess.run(compose('exec', '-T', 'media', '/bin/sh', '-ec', script), input=value,
                            stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, timeout=20)
    require(result.returncode == 0, 'Owned Media fixture write failed')


def read_file(workspace, file, limit=4096):
    return checked(compose('exec', '-T', 'media', '/bin/sh', '-ec', 'head -c ' + str(limit) + ' ' + shlex.quote(workspace + '/' + file)), limit=limit + 1)


def namespaces(raw):
    if not raw:
        return {}
    value = json.loads(raw)
    require(isinstance(value, dict) and all(re.fullmatch(r'(child-pid|[a-z]+-namespace)', key) and isinstance(item, int)
                                          and 0 < item < 2 ** 64 for key, item in value.items()), 'Invalid bounded namespace diagnostic')
    return value


def output_metadata(workspace, kind, expected_dimensions):
    file = workspace + '/encoded.' + kind
    command = 'stat -c "%s %u %g %a" ' + shlex.quote(file) + '; sha256sum ' + shlex.quote(file) + '; od -An -tx1 -N12 ' + shlex.quote(file)
    lines = checked(compose('exec', '-T', 'media', '/bin/sh', '-ec', command), limit=1024).splitlines()
    size, uid, gid, mode = lines[0].split()
    require(1 <= int(size) <= 8388608 and (uid, gid, mode) == ('10001', '10001', '600'), 'Encoded file bounds/ownership differ')
    digest = lines[1].split()[0]
    require(re.fullmatch('[0-9a-f]{64}', digest), 'Missing actual output checksum')
    magic = bytes.fromhex(''.join(lines[2:]).replace(' ', ''))
    valid = {'jpeg': magic.startswith(b'\xff\xd8\xff'), 'png': magic.startswith(b'\x89PNG\r\n\x1a\n'),
             'webp': magic.startswith(b'RIFF') and magic[8:12] == b'WEBP'}
    require(valid[kind], 'Actual encoded magic differs')
    dimensions = read_file(workspace, 'dimensions-' + kind, 64).split()
    require(len(dimensions) == 3 and tuple(map(int, dimensions[:2])) == expected_dimensions and 1 <= int(dimensions[2]) <= 4, 'Encoded output failed real bounded re-decode')
    return {'format': kind, 'bytes': int(size), 'sha256': digest, 'width': int(dimensions[0]), 'height': int(dimensions[1])}


def prerequisites():
    require(os.environ.get('GITHUB_ACTIONS') == 'true' and os.environ.get('RUNNER_ENVIRONMENT') == 'github-hosted'
            and os.environ.get('RUNNER_OS') == 'Linux', 'Requires verified GitHub-hosted Linux')
    require(os.environ['SPOOLIN_INSTALLATION'] == 'ci-' + os.environ['GITHUB_RUN_ID'] + '-' + os.environ['GITHUB_RUN_ATTEMPT'], 'Requires owned synthetic installation')
    readiness = json.loads((ROOT / 'artifacts/runtime/readiness-probes.json').read_text())
    require(readiness.get('result') == 'PASS' and readiness.get('probes') and all(row['result'] == 'PASS' for row in readiness['probes']), 'Actual make-start readiness required')
    sha = checked(['git', 'rev-parse', 'HEAD'])
    environment = dict(line.split('=', 1) for line in (ROOT / 'artifacts/runtime/environment.txt').read_text().splitlines() if '=' in line)
    require(environment.get('sha') == sha, 'Readiness startup inputs differ from assessed SHA')
    return sha


def main():
    sha = prerequisites()
    identity = uuid.uuid4().hex[:12]
    name = os.environ['SPOOLIN_INSTALLATION'] + '-codec-' + identity
    workspace = '/var/spoolin/media/codec-proof-' + identity
    reports = ROOT / 'artifacts/codec-container' / identity
    reports.mkdir(parents=True)
    record = {'boundary': 'actual-nonroot-production-media-container', 'testedSha': sha, 'runId': os.environ['GITHUB_RUN_ID'],
              'attempt': os.environ['GITHUB_RUN_ATTEMPT'], 'runnerOS': os.environ['RUNNER_OS'],
              'imageVersion': os.environ.get('ImageVersion'), 'result': 'FAIL', 'hostPrerequisiteResult': 'Separate report; no inherited pass'}
    launched = False
    stages = []
    diagnostic_since = datetime.now(timezone.utc).strftime('%Y-%m-%d %H:%M:%S UTC')
    record['stage'] = 'source-fixture'
    try:
        with (ROOT / 'docs/design-system/mockup/assets/letters.png').open('rb') as fixture:
            source = fixture.read(5242881)
        require(source.startswith(b'\x89PNG\r\n\x1a\n') and len(source) <= 5242880, 'Invalid fixed public input fixture')
        expected_dimensions = struct.unpack('>II', source[16:24])
        record['stage'] = 'running-media'
        primary = checked(compose('ps', '--status', 'running', '--quiet', 'media'))
        require(re.fullmatch('[0-9a-f]{12,64}', primary), 'Exactly one actual running Media container required')
        record['stage'] = 'fixture-files'
        media_write(workspace, 'input', source)
        script, stages = pipeline(workspace)
        media_write(workspace, 'pipeline.sh', script.encode())
        ownership = checked(compose('exec', '-T', 'media', 'stat', '-c', '%u %g %a', workspace, workspace + '/input', workspace + '/pipeline.sh'))
        actual_input = checked(compose('exec', '-T', 'media', 'sha256sum', workspace + '/input')).split()[0]
        require(actual_input == hashlib.sha256(source).hexdigest(), 'Actual mounted input differs')
        require(ownership.splitlines() == ['10001 10001 700', '10001 10001 600', '10001 10001 600'], 'Fixture directory/files ownership differs')
        wait = 'while [ ! -f ' + shlex.quote(workspace + '/go') + ' ]; do sleep 1; done; exec /usr/bin/timeout --signal=KILL 10s /bin/sh -e ' + shlex.quote(workspace + '/pipeline.sh')
        record['stage'] = 'one-off-launch'
        launched = True
        checked(compose('run', '-d', '--no-deps', '--name', name, '--entrypoint', '/bin/sh', 'media', '-ec', wait), seconds=60)
        record['stage'] = 'effective-configuration'
        record['configuration'] = effective_configuration(inspect(primary), inspect(name))
        record['stage'] = 'process-context'
        record['processContext'] = context(name)
        record['input'] = {'sha256': hashlib.sha256(source).hexdigest(), 'bytes': len(source), 'width': expected_dimensions[0], 'height': expected_dimensions[1]}
        record['stage'] = 'start-pipeline'
        media_write(workspace, 'go', b'go\n')
        record['stage'] = 'wait-pipeline'
        code = checked(['docker', 'wait', name], seconds=20)
        record['containerExit'] = int(code)
        record['stage'] = 'pipeline-output'
        pipeline_log = checked(['docker', 'logs', name], limit=4096,
                               boundary='fixed-codec-pipeline-log', diagnostic=True)
        record['pipelineDiagnostics'] = {'text': pipeline_log, 'retainedByteLimit': 4096}
        logs = pipeline_log.splitlines()
        record['versions'] = [line for line in logs if re.fullmatch(r'(vips-[0-9.]+|bubblewrap [0-9.]+|prlimit from util-linux [0-9.]+)', line)]
        record['stages'] = [line[6:] for line in logs
                            if re.fullmatch(r'stage=(versions|header-input|encode-(jpeg|png|webp)|header-(jpeg|png|webp)|completed)', line)]
        require(code == '0' and record['stages'][-1:] == ['completed'], 'Actual container sandbox/codec pipeline failed')
        require('vips-8.18.7' in record['versions'], 'Actual container codec pin differs')
        record['stage'] = 'validate-outputs'
        dimensions = read_file(workspace, 'dimensions', 64).split()
        require(len(dimensions) == 3 and tuple(map(int, dimensions[:2])) == expected_dimensions and 1 <= int(dimensions[2]) <= 4, 'Input header dimensions differ')
        record['outputs'] = [output_metadata(workspace, kind, expected_dimensions) for kind in ('jpeg', 'png', 'webp')]
        record['result'] = 'PASS'
        record['stage'] = 'completed'
    except ProbeCommandFailure as failure:
        record['failureType'] = 'bounded-command-failed'
        record['failedCommand'] = failure.report
        raise
    except (RuntimeError, ValueError, OSError, subprocess.SubprocessError) as failure:
        record['failureType'] = type(failure).__name__
        record['reason'] = 'Boundary assertion or fixture operation failed; inspect stage and sanitized kernel observations'
        raise
    finally:
        if launched:
            record['namespaceObservations'] = {}
            record['codecDiagnostics'] = {}
            for stage in stages:
                try:
                    record['namespaceObservations'][stage] = namespaces(read_file(workspace, stage + '.namespace', 4096))
                    log = read_file(workspace, stage + '.log', 4096)
                    if log:
                        size = int(checked(compose('exec', '-T', 'media', 'stat', '-c', '%s', workspace + '/' + stage + '.log'), limit=128))
                        record['codecDiagnostics'][stage] = {'text': log, 'observedBytes': size, 'retainedByteLimit': 4096, 'truncated': size > 4096}
                except (RuntimeError, ValueError):
                    record['namespaceObservations'][stage] = {'status': 'unavailable'}
            if record['result'] == 'PASS':
                outer = record['processContext']['namespaces']
                for observed in record['namespaceObservations'].values():
                    if not all(isinstance(observed.get(kind + '-namespace'), int) and
                               str(observed[kind + '-namespace']) != outer[kind].split('[', 1)[1].rstrip(']')
                               for kind in ('mnt', 'net', 'pid')):
                        record['result'] = 'FAIL'
                        record['reason'] = 'required-sandbox-namespace-proof-missing-or-unchanged'
            record['userNamespaceObservation'] = 'Upstream info-fd omits user namespace inode; stage outcomes determine whether the sandbox ran'
            removal, _ = DIAG.bounded_command(['docker', 'rm', '-f', name], limit=1024, seconds=15)
            record['containerCleanup'] = removal
            if removal['exit'] != 0 or removal['timedOut']:
                record['result'] = 'FAIL'
                record['cleanupFailure'] = 'one-off-container-removal-unconfirmed'
        cleanup, _ = DIAG.bounded_command(compose('exec', '-T', 'media', '/bin/sh', '-ec', 'rm -rf -- ' + shlex.quote(workspace)), limit=1024, seconds=15)
        record['fixtureCleanup'] = cleanup
        if cleanup['exit'] != 0 or cleanup['timedOut']:
            record['result'] = 'FAIL'
            record['cleanupFailure'] = 'owned-fixture-removal-unconfirmed'
        try:
            record['hostDiagnostics'] = DIAG.host_diagnostics(diagnostic_since)
        except (OSError, ValueError, subprocess.SubprocessError):
            record['hostDiagnostics'] = {'status': 'unavailable', 'interpretation': 'No control attributed'}
        (reports / 'summary.json').write_text(json.dumps(record, indent=2) + '\n')
    require(record['result'] == 'PASS', 'Actual Media container codec proof failed')


if __name__ == '__main__':
    try:
        main()
    except Exception:
        raise SystemExit('Media container codec proof failed; inspect bounded boundary report; no control changes made')
