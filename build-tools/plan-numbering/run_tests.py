"""Run nonempty migration regressions and retain actual JUnit outcomes."""
from pathlib import Path
import sys
import unittest

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / 'build-tools/verification-tests'))
from unittest_reports import ObservedResult, test_list, write_junit


def main():
    suite = unittest.defaultTestLoader.discover(str(Path(__file__).parent), pattern='test_*.py')
    tests = test_list(suite)
    if not tests:
        raise RuntimeError('Migration regression selection is empty')
    result = unittest.TextTestRunner(verbosity=2, resultclass=ObservedResult).run(suite)
    counts = write_junit(ROOT / '.ai/evidence/numbering-v1/migration-regressions.xml', tests, result,
                         suite_name='Plan numbering migration')
    return int(counts['failed'] > 0 or counts['executed'] != counts['selected'])


if __name__ == '__main__':
    sys.exit(main())
