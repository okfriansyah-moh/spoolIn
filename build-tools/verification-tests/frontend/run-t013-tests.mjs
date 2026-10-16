/**
 * T013 – Frontend unit/component test runner.
 * Runs the catalog and person/read feature tests in isolation and writes
 * a machine-readable report for the verification selector.
 */
import { spawnSync } from 'node:child_process';
import { readFileSync, writeFileSync, mkdirSync, rmSync, existsSync } from 'node:fs';

const REPORT_OUT = '.ai/evidence/numbering-v1/t013-unit.json';
const COVERAGE_JSON = 'frontend/coverage/test-results.json';

rmSync(COVERAGE_JSON, { force: true });

const run = spawnSync(
  'npm',
  [
    'run',
    'test:coverage',
    '--workspace',
    'frontend',
    '--',
    '--reporter=default',
    '--reporter=json',
    '--outputFile.json=coverage/test-results.json',
    // Run only T013 test files
    'src/lib/features/catalog/filter.test.ts',
    'src/lib/features/catalog/controller.test.ts',
    'src/lib/features/person/read/read.test.ts',
  ],
  { stdio: 'inherit', cwd: process.cwd() },
);

if (!existsSync(COVERAGE_JSON)) {
  // Fall back to full suite report if targeted run produced no file
  // (some vitest configs write the file only on success)
  writeFileSync(
    COVERAGE_JSON,
    JSON.stringify({ numTotalTests: 0, numPassedTests: 0, numFailedTests: 1 }),
  );
}

const report = JSON.parse(readFileSync(COVERAGE_JSON, 'utf8'));
mkdirSync('.ai/evidence/numbering-v1', { recursive: true });
writeFileSync(
  REPORT_OUT,
  JSON.stringify(
    {
      selected: report.numTotalTests,
      executed: report.numPassedTests + report.numFailedTests,
      failed: report.numFailedTests,
      source: COVERAGE_JSON,
    },
    null,
    2,
  ) + '\n',
);
process.exit(run.status ?? 2);
