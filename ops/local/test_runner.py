#!/usr/bin/env python3
"""Run local setup regressions and emit measured test and line-coverage reports."""
import json
from pathlib import Path
import sys
import time
import trace
import unittest
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[2]
OUTPUT = ROOT / '.ai/evidence'
MODULES = ('run.py', 'provision.py', 'readiness.py')


class RecordedResult(unittest.TextTestResult):
    def startTest(self, test):
        super().startTest(test)
        self.started = time.monotonic()
        self.case = ET.Element('testcase', name=test.id().rsplit('.', 1)[-1],
                               classname=test.id().rsplit('.', 1)[0])

    def stopTest(self, test):
        self.case.set('time', f'{time.monotonic() - self.started:.6f}')
        self.cases.append(self.case)
        super().stopTest(test)

    def addFailure(self, test, error):
        super().addFailure(test, error)
        ET.SubElement(self.case, 'failure').text = self._exc_info_to_string(error, test)

    def addError(self, test, error):
        super().addError(test, error)
        ET.SubElement(self.case, 'error').text = self._exc_info_to_string(error, test)

    def addSkip(self, test, reason):
        super().addSkip(test, reason)
        ET.SubElement(self.case, 'skipped', message=reason)

    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self.cases = []


def coverage_report(counts):
    modules = {}
    for name in MODULES:
        path = ROOT / 'ops/local' / name
        executable = {line for line in trace._find_executable_linenos(str(path))
                      if isinstance(line, int) and line > 0}
        covered = {line for (filename, line), hits in counts.items()
                   if filename == str(path) and hits > 0} & executable
        percentage = len(covered) * 100 / len(executable) if executable else 0
        modules[name] = {'executable_lines': len(executable), 'covered_lines': len(covered),
                         'line_percent': round(percentage, 2), 'missing_lines': sorted(executable - covered)}
    return {'minimum_line_percent': 80, 'modules': modules,
            'passed': all(value['line_percent'] >= 80 for value in modules.values())}


def write_reports(result, coverage, output=OUTPUT):
    output.mkdir(parents=True, exist_ok=True)
    suite = ET.Element('testsuite', name='local-setup', tests=str(result.testsRun),
                       failures=str(len(result.failures)), errors=str(len(result.errors)),
                       skipped=str(len(result.skipped)))
    suite.extend(result.cases)
    ET.ElementTree(suite).write(output / 'local-setup-tests.xml', encoding='utf-8', xml_declaration=True)
    (output / 'local-setup-coverage.json').write_text(json.dumps(coverage, indent=2) + '\n')


def main():
    counter = trace.Trace(count=True, trace=False)
    runner = unittest.TextTestRunner(verbosity=2, resultclass=RecordedResult)

    def execute():
        suite = unittest.defaultTestLoader.discover(str(ROOT / 'ops/local'), pattern='test_*.py')
        return runner.run(suite)

    result = counter.runfunc(execute)
    coverage = coverage_report(counter.results().counts)
    write_reports(result, coverage)
    for name, value in coverage['modules'].items():
        print(f'{name}: {value["line_percent"]}% measured executable-line coverage')
    return 0 if result.testsRun > 0 and result.wasSuccessful() and coverage['passed'] else 1


if __name__ == '__main__':
    sys.exit(main())
