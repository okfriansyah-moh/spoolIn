#!/usr/bin/env python3
"""Trust this checkout's public local CA in the host certificate store."""
import hashlib
from pathlib import Path
import platform
import shutil
import ssl
import subprocess
import sys

CERTIFICATE = Path(__file__).resolve().parents[2] / '.local/local-ca.crt'


def run(*args):
    return subprocess.run(args, check=True, text=True, capture_output=False)


def trust(certificate=CERTIFICATE):
    if not certificate.is_file():
        raise RuntimeError('Run make local-setup first; .local/local-ca.crt is missing.')
    pem = certificate.read_text()
    if 'PRIVATE KEY' in pem or pem.count('-----BEGIN CERTIFICATE-----') != 1:
        raise RuntimeError('Expected one public certificate without a private key.')
    try:
        der = ssl.PEM_cert_to_DER_cert(pem)
        context = ssl.SSLContext(ssl.PROTOCOL_TLS_CLIENT)
        context.load_verify_locations(cadata=pem)
    except (ValueError, ssl.SSLError) as error:
        raise RuntimeError('The local CA certificate is invalid. No trust was changed.') from error
    fingerprint = hashlib.sha256(der).hexdigest()
    print(f'Trusting SpoolIn local CA (SHA-256: {fingerprint}).', flush=True)
    system = platform.system()
    if system == 'Darwin':
        run('security', 'add-trusted-cert', '-r', 'trustRoot', '-p', 'ssl',
            '-s', 'localhost', '-k', str(Path.home() / 'Library/Keychains/login.keychain-db'),
            str(certificate))
    elif system == 'Windows':
        run('certutil.exe', '-user', '-addstore', '-f', 'Root', str(certificate))
    elif system == 'Linux' and 'microsoft' in platform.release().lower():
        windows_path = subprocess.check_output(
            ['wslpath', '-w', str(certificate)], text=True).strip()
        run('certutil.exe', '-user', '-addstore', '-f', 'Root', windows_path)
    elif system == 'Linux':
        trust_linux(certificate, fingerprint)
    else:
        raise RuntimeError(f'Automatic certificate trust is unavailable on {system}. See README.')
    print('Certificate installed. Restart your browser, then open https://localhost:8443.')


def trust_linux(certificate, fingerprint):
    if not shutil.which('update-ca-certificates'):
        raise RuntimeError('Automatic Linux trust requires update-ca-certificates (Ubuntu/Debian). '
                           'Import .local/local-ca.crt in your browser instead; see README.')
    destination = f'/usr/local/share/ca-certificates/spoolin-local-{fingerprint}.crt'
    run('sudo', 'install', '-m', '0644', str(certificate), destination)
    run('sudo', 'update-ca-certificates')
    print('Linux browsers with a separate certificate store may need the README browser import step.')


if __name__ == '__main__':
    try:
        trust()
    except (RuntimeError, OSError, subprocess.SubprocessError) as failure:
        print(f'local-trust: {failure}', file=sys.stderr)
        sys.exit(1)
