#!/usr/bin/env python3
"""Exercise the existing live connector/stdio checks without operational fault injection."""
import importlib.util
import json
import os
from pathlib import Path
import tempfile
import uuid
from operator_runtime import require_owned_runner

require_owned_runner()
os.umask(0o077)
source = Path(__file__).with_name('verify-operator-commands.py')
spec = importlib.util.spec_from_file_location('live_connector_checks', source)
checks = importlib.util.module_from_spec(spec)
spec.loader.exec_module(checks)
report = {'testedSha': os.environ['TESTED_SHA'], 'runId': os.environ['GITHUB_RUN_ID'],
          'result': 'FAIL', 'hostCompatibility': 'PENDING', 'commands': checks.COMMAND_RESULTS}
try:
    with tempfile.TemporaryDirectory(prefix='connector-proof-', dir=os.environ['RUNNER_TEMP']) as temporary:
        report.update(checks.connector(str(uuid.UUID(os.environ['SPOOLIN_OPERATOR_ACCOUNT_ID'])), Path(temporary)))
except Exception as failure:
    report['failureType'] = type(failure).__name__
finally:
    target = Path('artifacts/runtime/connector-stdio.json')
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(json.dumps(report, indent=2) + '\n')
raise SystemExit(0 if report['result'] == 'PASS' else 1)
