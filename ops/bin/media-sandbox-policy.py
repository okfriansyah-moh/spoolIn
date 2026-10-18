#!/usr/bin/env python3
"""Provision only the versioned Media sandbox profiles; never alter global controls."""
import fcntl
import hashlib
import json
import os
import selectors
from pathlib import Path
import stat
import subprocess
import sys
import time

ROOT = Path(__file__).resolve().parents[2]
SECURITY = ROOT / 'ops/security'
DESTINATION = Path('/etc/spoolin/security')
APPARMOR = Path('/etc/apparmor.d/spoolin-media')
PROFILES = ('spoolin-media', 'spoolin-media-codec')


def require(condition, message):
    if not condition:
        raise ValueError(message)


def read(path, maximum=131072):
    require(not path.is_symlink() and path.is_file(), 'Regular policy file required')
    with path.open('rb') as source:
        content = source.read(maximum + 1)
    require(len(content) <= maximum, 'Policy inspection bound exceeded')
    return content


def digest(content):
    return hashlib.sha256(content).hexdigest()


def structure(value):
    return json.dumps(value, sort_keys=True, separators=(',', ':')).encode()


def validate_source():
    manifest = json.loads(read(SECURITY / 'media-sandbox-policy.json'))
    files = {name: read(SECURITY / name) for name in ('media-seccomp.json', 'media-apparmor')}
    require(all(digest(content) == manifest['files'][name] for name, content in files.items()),
            'Policy bytes differ from versioned hashes')
    seccomp = json.loads(files['media-seccomp.json'])
    additions = seccomp['syscalls'][-4:]
    base = dict(seccomp, syscalls=seccomp['syscalls'][:-4])
    require(digest(structure(base)) == manifest['dockerDefaultStructureSha256'], 'Docker default policy changed')
    require(additions == manifest['addedRules'], 'Unexpected Media syscall permission')
    require(seccomp['defaultAction'] == 'SCMP_ACT_ERRNO' and seccomp['defaultErrnoRet'] == 1,
            'Default deny is required')
    return files, manifest


def protected(path, mode):
    info = path.lstat()
    require(not path.is_symlink() and info.st_uid == info.st_gid == 0 and stat.S_IMODE(info.st_mode) == mode,
            'Protected root-owned policy path required')


def loaded():
    text = read(Path('/sys/kernel/security/apparmor/profiles'), 1048576).decode()
    selected = [line for line in text.splitlines() if line.split(' ', 1)[0] in PROFILES]
    return selected


def check_loaded():
    require(sorted(loaded()) == sorted(name + ' (enforce)' for name in PROFILES),
            'Both Media profiles must be loaded exactly once in enforce mode')


def check_installed(files):
    for directory in (Path('/etc/spoolin'), DESTINATION, APPARMOR.parent):
        protected(directory, 0o755)
    for name, target in (('media-seccomp.json', DESTINATION / 'media-seccomp.json'), ('media-apparmor', APPARMOR)):
        protected(target, 0o644)
        require(read(target) == files[name], 'Installed policy differs; explicit operator migration required')
    check_loaded()


def install_file(target, content):
    if target.exists() or target.is_symlink():
        protected(target, 0o644)
        require(read(target) == content, 'Refusing to replace another policy revision')
        return
    descriptor = os.open(target, os.O_WRONLY | os.O_CREAT | os.O_EXCL | os.O_NOFOLLOW, 0o644)
    with os.fdopen(descriptor, 'wb') as output:
        os.fchmod(output.fileno(), 0o644)
        output.write(content)
        output.flush()
        os.fsync(output.fileno())


def load_profile():
    retained = bytearray()
    observed = 0
    timed_out = False
    with subprocess.Popen(['/sbin/apparmor_parser', '--add', str(APPARMOR)],
                          stdout=subprocess.DEVNULL, stderr=subprocess.PIPE) as process:
        os.set_blocking(process.stderr.fileno(), False)
        deadline = time.monotonic() + 30
        with selectors.DefaultSelector() as selector:
            selector.register(process.stderr, selectors.EVENT_READ)
            while selector.get_map():
                remaining = deadline - time.monotonic()
                if remaining <= 0:
                    timed_out = True
                    process.kill()
                    break
                if not selector.select(min(remaining, 0.25)):
                    continue
                chunk = os.read(process.stderr.fileno(), 4096)
                if not chunk:
                    selector.unregister(process.stderr)
                    break
                observed += len(chunk)
                retained.extend(chunk[:max(0, 4096 - len(retained))])
        try:
            code = process.wait(timeout=max(0.01, deadline - time.monotonic()))
        except subprocess.TimeoutExpired:
            timed_out = True
            process.kill()
            code = process.wait(timeout=5)
    if code != 0 or timed_out:
        print(json.dumps({'boundary': 'Media AppArmor profile loading', 'exit': code,
                          'timedOut': timed_out, 'diagnostic': retained.decode('utf-8', errors='replace'),
                          'observedBytes': observed, 'retainedByteLimit': 4096,
                          'truncated': observed > len(retained)}), file=sys.stderr)
    require(code == 0 and not timed_out, 'Media AppArmor compilation or loading failed; profiles remain unverified')


def install(files):
    require(os.geteuid() == 0, 'Root is required for explicit host policy provisioning')
    for directory in (Path('/etc/spoolin'), DESTINATION):
        if not directory.exists() and not directory.is_symlink():
            directory.mkdir(mode=0o755)
            os.chmod(directory, 0o755, follow_symlinks=False)
        protected(directory, 0o755)
    protected(APPARMOR.parent, 0o755)
    descriptor = os.open(DESTINATION / '.media-policy.lock', os.O_RDWR | os.O_CREAT | os.O_NOFOLLOW, 0o600)
    with os.fdopen(descriptor, 'r+b') as lock:
        protected(DESTINATION / '.media-policy.lock', 0o600)
        fcntl.flock(lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
        install_file(DESTINATION / 'media-seccomp.json', files['media-seccomp.json'])
        install_file(APPARMOR, files['media-apparmor'])
        if not loaded():
            load_profile()
        check_installed(files)


def main():
    # Accept only these exact operator switches, without dynamic paths or profile names.
    require(len(sys.argv) == 2 and sys.argv[1] in ('--install', '--check', '--validate-source'),
            'Use exactly --install, --check or --validate-source')
    action = sys.argv[1]
    files, manifest = validate_source()
    if action == '--install':
        install(files)
    elif action == '--check':
        check_installed(files)
    print(json.dumps({'boundary': 'Media host policy provisioning', 'action': action,
                      'policyFiles': manifest['files'], 'codecExecution': 'Pending separate production container probe'}))


if __name__ == '__main__':
    try:
        main()
    except (ValueError, OSError, subprocess.SubprocessError, KeyError, json.JSONDecodeError):
        raise SystemExit('Media sandbox policy prerequisite failed; no global controls were changed')
