#!/usr/bin/env python3
"""Verify every service over its private mutual-TLS readiness endpoint."""
import json
import ssl
import urllib.request


def main():
    context = ssl.create_default_context(cafile='/run/secrets/service-ca')
    context.load_cert_chain('/run/secrets/core-cert', '/run/secrets/core-key')
    for owner in ('core', 'catalog', 'media'):
        request = urllib.request.Request(f'https://{owner}:8081/actuator/health/readiness')
        with urllib.request.urlopen(request, context=context, timeout=3) as response:
            value = json.loads(response.read(16384))
            if value.get('status') != 'UP':
                raise RuntimeError(f'{owner} is not ready')
        print(f'{owner}: ready (private mutual TLS)')


if __name__ == '__main__':
    main()
