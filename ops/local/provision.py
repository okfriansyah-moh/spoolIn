#!/usr/bin/env python3
"""Create persistent local-only identities inside the tooling container."""
import base64
import importlib.util
import json
import os
from pathlib import Path
import secrets
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[2]
BASE = ROOT / '.local'
SPEC = importlib.util.spec_from_file_location('prepare', ROOT / 'ops/ci/prepare-environment.py')
LOCAL_DEMO_PASSWORDS = {'viewer': 'SpoolInViewer123!', 'maintainer': 'SpoolInMaintainer123!'}

PREPARE = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(PREPARE)


def local_command(*args):
    args = list(args)
    if args[0] == 'sudo':
        args.pop(0)
    if '-days' in args:
        args[args.index('-days') + 1] = '365'
    if '-extfile' in args:
        extensions = Path(args[args.index('-extfile') + 1])
        extensions.write_text(extensions.read_text() +
                              'basicConstraints=critical,CA:FALSE\n'
                              'keyUsage=critical,digitalSignature,keyEncipherment\n'
                              'subjectKeyIdentifier=hash\n'
                              'authorityKeyIdentifier=keyid:always,issuer\n')
    subprocess.run(args, check=True, stdout=subprocess.DEVNULL, stderr=subprocess.PIPE)


def provision(host_root, uid, gid):
    identities = BASE / 'identities'
    runtime = BASE / 'runtime'
    identities.mkdir(mode=0o700)
    runtime.mkdir(mode=0o700)
    PREPARE.command = local_command
    local_command('openssl', 'req', '-x509', '-newkey', 'rsa:2048', '-nodes', '-days', '365',
                  '-addext', 'basicConstraints=critical,CA:TRUE',
                  '-addext', 'keyUsage=critical,keyCertSign,cRLSign',
                  '-addext', 'subjectKeyIdentifier=hash',
                  '-subj', '/CN=SpoolIn local development CA', '-keyout', str(identities / 'ca-key'),
                  '-out', str(identities / 'service-ca'))
    exports = {'SPOOLIN_INSTALLATION': 'spoolin-local', 'SPOOLIN_RUNTIME_CONFIG_DIR': str(runtime),
               'SERVICE_CA_FILE': str(identities / 'service-ca')}
    PREPARE.provision_identity_files(identities, exports)
    initialize_runtime(runtime)
    save_host_files(host_root, uid, gid, runtime, exports)
    local_command('chown', '-R', '10001:10001', str(identities), str(runtime))
    for directory in (identities, runtime):
        for file in directory.iterdir():
            file.chmod(0o600)


def initialize_runtime(runtime):
    for name in ('pii', 'draft', 'email_lookup', 'csrf', 'rate', 'operation', 'catalog-operation', 'mcp-ui'):
        PREPARE.protected(runtime / name, secrets.token_bytes(32))
    local_command('openssl', 'genpkey', '-algorithm', 'ED25519', '-outform', 'DER', '-out', str(runtime / 'actor-key'))
    public = subprocess.check_output(['openssl', 'pkey', '-inform', 'DER', '-in', str(runtime / 'actor-key'), '-pubout', '-outform', 'DER'])
    PREPARE.protected(runtime / 'actor-trust', json.dumps({'active': base64.b64encode(public).decode()}))
    PREPARE.runtime_config(runtime)
    PREPARE.prepared_accounts(runtime)
    for role, password in LOCAL_DEMO_PASSWORDS.items():
        PREPARE.protected(runtime / (role + "-password"), password)
    PREPARE.rate_agreement(runtime)


def save_host_files(host_root, uid, gid, runtime, exports):
    accounts = '\n'.join(f'{role}@example.invalid: {(runtime / (role + "-password")).read_text()}'
                         for role in ('viewer', 'maintainer')) + '\n'
    files = {'accounts.txt': accounts, 'environment.json': json.dumps({
        key: value.replace('/workspace', host_root, 1) for key, value in exports.items()})}
    for name, value in files.items():
        file = PREPARE.protected(BASE / name, value)
        os.chown(file, uid, gid)
    # This public certificate lets the browser trust only this local installation.
    certificate = BASE / 'local-ca.crt'
    certificate.write_bytes((BASE / 'identities/service-ca').read_bytes())
    os.chown(certificate, uid, gid)
    certificate.chmod(0o644)


if __name__ == '__main__':
    provision(sys.argv[1], int(sys.argv[2]), int(sys.argv[3]))
