#!/usr/bin/env python3
"""Bounded read-only codec prerequisite diagnostics; never changes runner security policy."""
import argparse
from collections import Counter
import hashlib
import json
import os
from pathlib import Path
import re
import selectors
import subprocess
import time

STATUS_FIELDS = {'CapEff', 'CapBnd', 'NoNewPrivs', 'Seccomp'}
NAMESPACES = ('user', 'mnt', 'net', 'pid', 'ipc', 'uts', 'cgroup')
OPERATIONS = {'userns_create', 'capable', 'create', 'bind', 'sendmsg', 'receive', 'open', 'mount', 'pivotroot', 'change_profile'}
CAPABILITIES = set(('chown dac_override dac_read_search fowner fsetid kill setgid setuid setpcap linux_immutable '
                    'net_bind_service net_broadcast net_admin net_raw ipc_lock ipc_owner sys_module sys_rawio sys_chroot '
                    'sys_ptrace sys_pacct sys_admin sys_boot sys_nice sys_resource sys_time sys_tty_config mknod lease '
                    'audit_write audit_control setfcap mac_override mac_admin syslog wake_alarm block_suspend audit_read '
                    'perfmon bpf checkpoint_restore').split())
SYSCTLS = ('kernel/apparmor_restrict_unprivileged_userns', 'kernel/apparmor_restrict_unprivileged_unconfined',
           'kernel/unprivileged_userns_clone', 'user/max_user_namespaces')


def bounded_command(command, limit=131072, seconds=5):
    started = time.monotonic()
    try:
        process = subprocess.Popen(command, stdout=subprocess.PIPE, stderr=subprocess.DEVNULL)
    except OSError:
        return {'exit': None, 'available': False, 'truncated': False, 'timedOut': False}, ''
    content = bytearray()
    truncated = False
    timed_out = False
    with selectors.DefaultSelector() as selector:
        selector.register(process.stdout, selectors.EVENT_READ)
        while selector.get_map():
            remaining = seconds - (time.monotonic() - started)
            if remaining <= 0:
                timed_out = True
                break
            for key, _ in selector.select(min(remaining, 0.2)):
                chunk = os.read(key.fileobj.fileno(), min(4096, limit + 1 - len(content)))
                if not chunk:
                    selector.unregister(key.fileobj)
                    continue
                content.extend(chunk)
                if len(content) > limit:
                    truncated = True
                    break
            if truncated:
                break
    if truncated or timed_out:
        process.kill()
    try:
        code = process.wait(timeout=1)
    except subprocess.TimeoutExpired:
        process.kill()
        code = process.wait(timeout=1)
        timed_out = True
    process.stdout.close()
    return {'exit': code, 'available': True, 'truncated': truncated, 'timedOut': timed_out}, bytes(content[:limit]).decode('utf-8', 'replace')


def limited_read(path, limit=4096):
    try:
        with Path(path).open('rb') as source:
            value = source.read(limit + 1)
        return value if len(value) <= limit else None
    except OSError:
        return None


def status_fields(value):
    result = {}
    for line in value.splitlines():
        key, _, raw = line.partition(':')
        raw = raw.strip()
        if key in STATUS_FIELDS and re.fullmatch(r'[0-9A-Fa-f]{1,32}', raw):
            result[key] = raw
    return result


def profile_name(value):
    raw = value.strip().split(' ', 1)[0]
    if raw in ('unconfined', 'docker-default', 'unprivileged_userns'):
        return raw
    if raw in ('bwrap', '/usr/bin/bwrap', 'bubblewrap', '/usr/bin/bubblewrap'):
        return 'bwrap'
    return 'other'


def denied_fields(value):
    counts = Counter()
    for line in value.splitlines():
        if 'apparmor="DENIED"' not in line:
            continue
        fields = dict(re.findall(r'\b(operation|profile|capname)="([^"\n]{0,256})"', line))
        operation = fields.get('operation', 'other')
        operation = operation if operation in OPERATIONS else 'other'
        capability = fields.get('capname', 'none')
        capability = capability if capability in CAPABILITIES or capability == 'none' else 'other'
        counts[(operation, profile_name(fields.get('profile', '')), capability)] += 1
    ordered = sorted(counts.items())
    return {'events': [{'operation': key[0], 'profile': key[1], 'capability': key[2], 'count': count}
                       for key, count in ordered[:32]], 'additionalGroups': max(0, len(ordered) - 32)}


def process_context():
    status = limited_read('/proc/self/status', 16384)
    profile = limited_read('/proc/self/attr/current')
    namespaces = {}
    for kind in NAMESPACES:
        try:
            value = os.readlink('/proc/self/ns/' + kind)
            namespaces[kind] = value if re.fullmatch(kind + r':\[[0-9]{1,20}\]', value) else 'unrecognized'
        except OSError:
            namespaces[kind] = 'unavailable'
    return {'uid': os.getuid(), 'gid': os.getgid(), 'status': status_fields((status or b'').decode()),
            'profile': profile_name((profile or b'').decode()), 'namespaces': namespaces}


def loaded_profiles(value):
    results = set()
    for line in value.splitlines():
        name, _, mode = line.partition(' ')
        if name in ('bwrap', '/usr/bin/bwrap', 'bubblewrap', '/usr/bin/bubblewrap'):
            mode = mode.strip().strip('()')
            results.add(('bwrap', mode if mode in ('enforce', 'complain', 'unconfined') else 'other'))
    return [{'profile': name, 'mode': mode} for name, mode in sorted(results)]


def host_diagnostics(since=None):
    report = {'boundary': 'runner-host', 'context': process_context(), 'sysctls': {}, 'vendorProfiles': []}
    for name in SYSCTLS:
        value = limited_read('/proc/sys/' + name, 64)
        text = value.decode().strip() if value is not None else ''
        report['sysctls'][name] = int(text) if re.fullmatch(r'[0-9]{1,20}', text) else None
    for name in ('bwrap', 'bubblewrap', 'usr.bin.bwrap'):
        file = Path('/etc/apparmor.d') / name
        value = None if file.is_symlink() else limited_read(file, 65536)
        report['vendorProfiles'].append({'name': name, 'present': file.is_file(), 'symlink': file.is_symlink(),
                                         'sha256': hashlib.sha256(value).hexdigest() if value is not None else None})
    state, loaded = bounded_command(['sudo', '-n', 'cat', '/sys/kernel/security/apparmor/profiles'])
    report['loadedProfileRead'] = state
    report['loadedBwrapProfiles'] = loaded_profiles(loaded)
    if since is not None:
        if not re.fullmatch(r'[0-9]{4}-[0-9]{2}-[0-9]{2} [0-9]{2}:[0-9]{2}:[0-9]{2} UTC', since):
            raise ValueError('Invalid bounded diagnostic time')
        state, raw = bounded_command(['sudo', '-n', 'journalctl', '--dmesg', '--since', since, '--lines=120', '--no-pager', '-o', 'cat'])
        report['kernelDeniedRead'] = state
        report['kernelDenied'] = denied_fields(raw)
    report['interpretation'] = 'Observed context only; unavailable or absent denials do not identify or exclude a control'
    return report


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--output', required=True)
    parser.add_argument('--since')
    parser.add_argument('--probe-log')
    args = parser.parse_args()
    if os.environ.get('GITHUB_ACTIONS') != 'true' or os.environ.get('RUNNER_ENVIRONMENT') != 'github-hosted' or os.environ.get('RUNNER_OS') != 'Linux':
        raise SystemExit('Codec diagnostics require verified GitHub-hosted Linux')
    target = Path(args.output)
    target.parent.mkdir(parents=True, exist_ok=True)
    report = host_diagnostics(args.since)
    if args.probe_log:
        size = Path(args.probe_log).stat().st_size
        report['probeLog'] = {'observedBytes': size, 'retainedByteLimit': 4096, 'truncated': size > 4096}
    target.write_text(json.dumps(report, indent=2) + '\n')


if __name__ == '__main__':
    main()
