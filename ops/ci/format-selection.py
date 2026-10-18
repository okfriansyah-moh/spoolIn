#!/usr/bin/env python3
"""Prepare a bounded source-format patch on a verified hosted runner."""
import os
from pathlib import Path
import subprocess

if os.environ.get('GITHUB_ACTIONS') != 'true' or os.environ.get('RUNNER_ENVIRONMENT') != 'github-hosted':
    raise SystemExit('Source formatting requires GitHub-hosted execution')
base = '2764ca8bcdb5ee182852e2bdb26c95247c81c5ed'
changed = subprocess.check_output(['git', 'diff', '--name-only', '--diff-filter=ACMR', base, 'HEAD'], text=True).splitlines()
selected = [name for name in changed if Path(name).is_file()]
subprocess.run(['./gradlew', '--no-daemon', '--console', 'plain', 'ktlintFormat'], check=True)
web = [name for name in selected if name.startswith(('frontend/', 'mcp/', 'tests/browser/'))
       and Path(name).suffix in ('.ts', '.js', '.mjs', '.json', '.svelte')]
if web:
    subprocess.run(['npm', 'exec', '--', 'prettier', '--config', 'build-tools/verification-tests/prettier.config.mjs', '--write', *web], check=True)
output = Path('artifacts/source-formatting.patch')
output.parent.mkdir(parents=True, exist_ok=True)
with output.open('wb') as patch:
    subprocess.run(['git', 'diff', '--binary', '--', *selected], check=True, stdout=patch)
print(f'Prepared formatting patch for {len(selected)} selected source files; no acceptance result')
