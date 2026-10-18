#!/usr/bin/env python3
"""Retain only fixed structured owner diagnostics from the actual disposable stack."""
import argparse
import json
import os
from pathlib import Path
import platform
import re
import subprocess

if os.environ.get('RUNNER_ENVIRONMENT') != 'github-hosted' or os.environ.get('GITHUB_ACTIONS') != 'true' or platform.system() != 'Linux':
    raise SystemExit('Requires GitHub-hosted Linux')
parser = argparse.ArgumentParser()
parser.add_argument('--phase', choices=('before-browser', 'after-browser'), required=True)
phase = parser.parse_args().phase
installation = 'ci-' + os.environ['GITHUB_RUN_ID'] + '-' + os.environ['GITHUB_RUN_ATTEMPT']
if os.environ.get('SPOOLIN_INSTALLATION') != installation:
    raise SystemExit('Exact owned primary installation required')
result = subprocess.run(['docker', 'compose', '--project-name', installation, '-f', 'ops/compose.yaml',
                         'logs', '--no-color', '--tail', '120', 'core', 'catalog', 'media'],
                        capture_output=True, text=True, timeout=45, check=False)
records = []
for line in result.stdout.splitlines():
    if len(line) > 16384 or '{' not in line:
        continue
    try:
        value = json.loads(line[line.index('{'):])
    except ValueError:
        continue
    service = (value.get('resource') or {}).get('service.name')
    if service not in ('core', 'catalog', 'media'):
        continue
    attributes = value.get('attributes') or {}
    safe = {key: item for key, item in attributes.items() if key in ('event', 'code', 'error_class', 'operation')
            and isinstance(item, str) and re.fullmatch(r'[A-Za-z][A-Za-z0-9_]{0,63}', item)}
    records.append({'service': service, 'level': value.get('level') if value.get('level') in ('debug', 'info', 'warning', 'error', 'fatal') else 'unknown', 'attributes': safe})
output = Path('artifacts/runtime/owner-diagnostics-' + phase + '.json')
output.parent.mkdir(parents=True, exist_ok=True)
output.write_text(json.dumps({'testedSha': os.environ['TESTED_SHA'], 'runId': os.environ['GITHUB_RUN_ID'],
                              'commandExit': result.returncode, 'records': records,
                              'rawMessagesRetained': False, 'headersOrBodiesRetained': False}, indent=2) + '\n')
if result.returncode:
    raise SystemExit(result.returncode)
