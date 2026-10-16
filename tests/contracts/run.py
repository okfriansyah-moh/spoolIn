"""Run only the owned contract suites and retain actual JSON/JUnit outcomes."""
from pathlib import Path
import json
import os
import shutil
import subprocess
import sys
import unittest

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / 'build-tools/verification-tests'))
from unittest_reports import ObservedResult, test_list, write_junit


def prerequisites():
    for command, expected in [('node', 'v24.21.0'), ('npm', '11.19.0')]:
        if shutil.which(command) is None:
            return 'Missing required executable: ' + command
        try:
            result = subprocess.run([command, '--version'], capture_output=True, timeout=15)
        except (OSError, subprocess.TimeoutExpired):
            return 'Unavailable required executable: ' + command
        if result.returncode or result.stdout.decode().strip() != expected:
            return 'Select the exact accepted tool version: ' + command
    java = Path(os.environ.get('JAVA_HOME', '')) / 'bin/java'
    if not java.is_file():
        return 'Select accepted Temurin21 through JAVA_HOME'
    try:
        result = subprocess.run([str(java), '-version'], capture_output=True, timeout=15)
    except (OSError, subprocess.TimeoutExpired):
        return 'Unavailable required Java executable'
    if result.returncode or b'Temurin-21.0.12.1+1' not in result.stdout + result.stderr:
        return 'Select accepted Temurin21.0.12.1+1'
    if not ROOT.joinpath('node_modules/graphql/package.json').is_file():
        return 'Install the accepted npm lock graph before contract verification'
    return None


if __name__ == '__main__':
    suite = unittest.defaultTestLoader.discover(str(Path(__file__).parent), pattern='test_*.py')
    tests = test_list(suite)
    setup = prerequisites()
    if setup:
        print('Setup unavailable: ' + setup, file=sys.stderr)
        result = ObservedResult(sys.stderr, True, 2)
    else:
        result = unittest.TextTestRunner(verbosity=2, resultclass=ObservedResult).run(suite)
    counts = write_junit(ROOT / '.ai/evidence/numbering-v1/t002-contract-tests.xml', tests, result, suite_name='T002 shared contracts')
    path = ROOT / '.ai/evidence/numbering-v1/t002-contract-tests.json'
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps({**counts, 'cases': ['T002-A1', 'T002-A2', 'T002-A3', 'ATDD-302']}, indent=2) + '\n')
    sys.exit(2 if counts['selected'] < 1 or counts['selected'] != counts['executed'] else 1 if counts['failed'] else 0)
