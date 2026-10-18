#!/usr/bin/env python3
"""Retain owner observations only when their embedded provenance matches this actual run."""
import hashlib
import json
import os
from pathlib import Path
import shutil


def main():
    if os.environ.get('GITHUB_ACTIONS') != 'true' or os.environ.get('RUNNER_ENVIRONMENT') != 'github-hosted':
        raise SystemExit('Owner evidence collection requires a verified hosted runner')
    revision, run = os.environ['TESTED_SHA'], os.environ['GITHUB_RUN_ID']
    destination = Path('artifacts/source-checks/owner-observations')
    destination.mkdir(parents=True, exist_ok=True)
    rows = []
    for path in sorted(Path('.ai/evidence/numbering-v1').glob('*.json')):
        try:
            value = json.loads(path.read_text())
        except (OSError, ValueError):
            continue
        if not isinstance(value, list) or not value:
            continue
        if not all(isinstance(row, dict) and row.get('assessedSha') == revision and row.get('runId') == run for row in value):
            continue
        target = destination / path.name
        shutil.copyfile(path, target)
        rows.append({'file': target.name, 'sha256': hashlib.sha256(target.read_bytes()).hexdigest(), 'observations': len(value)})
    (destination / 'manifest.json').write_text(json.dumps({'testedSha': revision, 'runId': run, 'files': rows,
        'acceptance': 'UNVERIFIED; these observations supplement actual suite assertions, not task acceptance'}, indent=2) + '\n')


if __name__ == '__main__':
    main()
