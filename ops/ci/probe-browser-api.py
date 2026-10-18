#!/usr/bin/env python3
"""Actual public HTTPS policy/catalog probes with fixed safe error fields only."""
import json
import os
from pathlib import Path
import platform
import ssl
import urllib.error
import urllib.request

if os.environ.get('RUNNER_ENVIRONMENT') != 'github-hosted' or os.environ.get('GITHUB_ACTIONS') != 'true' or platform.system() != 'Linux':
    raise SystemExit('Requires GitHub-hosted Linux')
origin = 'https://localhost:' + os.environ.get('CORE_HTTPS_PORT', '8443')
context = ssl.create_default_context(cafile=os.environ['SERVICE_CA_FILE'])
results = []
for name, query in (('policy', 'query PublicPolicyProbe {policy {revision}}'),
                    ('titles', 'query PublicCatalogProbe {titles(filter:{},first:1){nodes{id}}}')):
    request = urllib.request.Request(origin + '/graphql', data=json.dumps({'query': query}).encode(),
                                     headers={'Content-Type': 'application/json', 'Origin': origin, 'X-Spoolin-Client': 'web'})
    try:
        response = urllib.request.urlopen(request, context=context, timeout=15)
    except urllib.error.HTTPError as failure:
        response = failure
    with response:
        body = json.loads(response.read(65537))
        allowed = {'FIELD_INVALID', 'UNAVAILABLE', 'UNAUTHENTICATED', 'FORBIDDEN', 'RATE_LIMITED',
                   'DEADLINE_EXCEEDED', 'RESOURCE_EXHAUSTED', 'CONFLICT', 'VALIDATION_FAILED'}
        errors = [item.get('extensions', {}).get('code') if item.get('extensions', {}).get('code') in allowed
                  else 'UNCLASSIFIED' for item in body.get('errors', [])]
        passed = response.status == 200 and not errors and bool((body.get('data') or {}).get(name))
        results.append({'root': name, 'httpStatus': response.status, 'errorCodes': errors,
                        'result': 'PASS' if passed else 'FAIL'})
output = Path('artifacts/runtime/public-api-probes.json')
output.parent.mkdir(parents=True, exist_ok=True)
output.write_text(json.dumps({'testedSha': os.environ['TESTED_SHA'], 'runId': os.environ['GITHUB_RUN_ID'],
                              'results': results, 'rawBodiesRetained': False}, indent=2) + '\n')
raise SystemExit(0 if all(row['result'] == 'PASS' for row in results) else 1)
