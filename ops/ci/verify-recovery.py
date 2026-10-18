#!/usr/bin/env python3
"""Exercise actual encrypted checkpoint and isolated restore after browser/lifecycle work."""
import hashlib
import json
import os
from pathlib import Path
import subprocess

ROOT = Path(__file__).resolve().parents[2]


def invoke(label, arguments, environment, reports):
    with (reports / (label + '.log')).open('xb') as output:
        result = subprocess.run(arguments, cwd=ROOT, env=environment, stdout=output, stderr=subprocess.STDOUT)
    if result.returncode:
        raise RuntimeError(label + ' failed with exit ' + str(result.returncode))


def exports(path):
    values = {}
    for line in path.read_text().splitlines():
        key, separator, value = line.partition('=')
        if not separator or not key.replace('_', '').isalnum() or '\x00' in value:
            raise ValueError('Invalid protected environment export')
        values[key] = value
    return values


def main():
    if os.environ.get('GITHUB_ACTIONS') != 'true' or os.environ.get('RUNNER_ENVIRONMENT') != 'github-hosted':
        raise SystemExit('Recovery verification requires a GitHub-hosted runner')
    expected = 'ci-' + os.environ['GITHUB_RUN_ID'] + '-' + os.environ['GITHUB_RUN_ATTEMPT']
    if os.environ['SPOOLIN_INSTALLATION'] != expected:
        raise SystemExit('Only the uniquely owned synthetic CI installation is supported')
    reports = ROOT / 'artifacts/recovery'
    reports.mkdir(parents=True, exist_ok=False)
    private = Path(os.environ['RUNNER_TEMP']) / ('spoolin-recovery-' + expected)
    private.mkdir(mode=0o700)
    source = dict(os.environ)
    invoke('backup', ['bash', 'ops/bin/backup.sh', '--execute', '--output', str(private / 'checkpoint')], source, reports)
    envelope = json.loads((private / 'checkpoint/encrypted-files.json').read_text())
    for name in ('checkpoint.cms', 'keys.cms'):
        actual = hashlib.sha256((private / 'checkpoint' / name).read_bytes()).hexdigest()
        if envelope.get(name) != actual:
            raise RuntimeError('Encrypted backup artifact integrity differs')
    invoke('stop-source', ['make', 'stop'], source, reports)
    environment_file = private / 'destination.env'
    environment_file.touch(mode=0o600)
    preparing = {**source, 'GITHUB_ENV': str(environment_file)}
    invoke('prepare-destination', ['python3', 'ops/ci/prepare-environment.py', '--namespace', 'restore'], preparing, reports)
    destination = {**preparing, **exports(environment_file),
                   'SPOOLIN_BACKUP_RECIPIENT_CERT_FILE': source['SPOOLIN_OPERATOR_CERT_FILE'],
                   'SPOOLIN_BACKUP_PRIVATE_KEY_FILE': source['SPOOLIN_OPERATOR_KEY_FILE'],
                   'SPOOLIN_RESTORE_CANARY_EMAIL': source['SPOOLIN_RESTORE_CANARY_EMAIL'],
                   'SPOOLIN_RESTORE_CANARY_PASSWORD_FILE': source['SPOOLIN_RESTORE_CANARY_PASSWORD_FILE']}
    invoke('restore', ['bash', 'ops/bin/restore.sh', '--execute', '--input', str(private / 'checkpoint'),
                      '--report', str(reports / 'restore.json')], destination, reports)
    destination.update(exports(environment_file))
    destination['SPOOLIN_RESTORE_CANARY_PASSWORD_FILE'] = source['SPOOLIN_RESTORE_CANARY_PASSWORD_FILE']
    invoke('restored-restart', ['make', 'restart'], destination, reports)
    invoke('restored-status', ['make', 'status'], destination, reports)
    # Preserve completed restore evidence, then free its resources before resuming the source.
    invoke('stop-restored', ['make', 'stop'], destination, reports)
    invoke('resume-source', ['make', 'start'], source, reports)
    record = {'testedSha': subprocess.check_output(['git', 'rev-parse', 'HEAD'], text=True).strip(),
              'runId': source['GITHUB_RUN_ID'], 'attempt': source['GITHUB_RUN_ATTEMPT'],
              'runner': source['RUNNER_ENVIRONMENT'], 'runnerOS': source['RUNNER_OS'],
              'sourceInstallation': source['SPOOLIN_INSTALLATION'],
              'restoredInstallation': destination['SPOOLIN_INSTALLATION'],
              'encryptedArchiveHashes': envelope, 'result': 'passed',
              'proof': 'Actual coordinated checkpoint, protected key recovery, destination application API validation and documented restart/status'}
    (reports / 'summary.json').write_text(json.dumps(record, indent=2) + '\n')


if __name__ == '__main__':
    main()
