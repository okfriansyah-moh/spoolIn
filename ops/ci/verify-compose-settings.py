#!/usr/bin/env python3
"""Assert actual Compose forwarding without starting containers or emitting configuration."""
import json
import os
from pathlib import Path
import subprocess
import tempfile


def main():
    if os.environ.get('GITHUB_ACTIONS') != 'true' or os.environ.get('RUNNER_ENVIRONMENT') != 'github-hosted':
        raise SystemExit('Requires verified GitHub-hosted execution')
    key = 'TITLE_MAX_CODE_POINTS'
    selected = ('core', 'catalog', 'media', 'catalog-seed')
    results = []
    for label, file_value, host_value, expected in (
        ('absent', None, None, None), ('file', '300', None, '300'),
        ('host-wins', '300', '400', '400'), ('empty', '300', '', ''),
        ('zero', None, '0', '0'),
    ):
        environment = dict(os.environ)
        environment.pop(key, None)
        if host_value is not None:
            environment[key] = host_value
        with tempfile.TemporaryDirectory(dir=os.environ['RUNNER_TEMP'], prefix='compose-policy-') as temporary:
            source = Path(temporary) / 'settings.env'
            source.write_text('' if file_value is None else key + '=' + file_value + '\n')
            command = ['docker', 'compose', '--env-file', str(source), '--project-name',
                       os.environ['SPOOLIN_INSTALLATION'], '-f', 'ops/compose.yaml', 'config', '--format', 'json']
            process = subprocess.run(command, env=environment, capture_output=True, timeout=30)
            if process.returncode:
                raise RuntimeError('Compose policy configuration failed: ' + label)
            model = json.loads(process.stdout)
            for owner in selected:
                actual = model['services'][owner]['environment'].get(key)
                if actual != expected:
                    raise RuntimeError('Compose policy presence/precedence mismatch: ' + label + '/' + owner)
            results.append({'case': label, 'owners': list(selected), 'result': 'PASS'})
    target = Path('artifacts/source-checks/compose-settings.json')
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(json.dumps({'tested_sha': os.environ['TESTED_SHA'], 'run_id': os.environ['GITHUB_RUN_ID'],
                                 'boundary': 'actual Compose configuration; owner validation remains separate',
                                 'cases': results}, indent=2) + '\n')


if __name__ == '__main__':
    main()
