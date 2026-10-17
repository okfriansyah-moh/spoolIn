/**
 * T017 – Frontend unit/component test runner.
 */
import { spawnSync } from 'node:child_process';
import {
  readFileSync,
  writeFileSync,
  mkdirSync,
  rmSync,
  existsSync,
} from 'node:fs';

const REPORT_OUT = '.ai/evidence/numbering-v1/t017-unit.json';
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
    'src/lib/features/movie/patch.test.ts',
    'src/lib/features/movie/validation.test.ts',
    'src/lib/features/movie/editor-controller.test.ts',
  ],
  { stdio: 'inherit', cwd: process.cwd() },
);

if (!existsSync(COVERAGE_JSON)) {
  writeFileSync(
    COVERAGE_JSON,
    JSON.stringify({ numTotalTests: 0, numPassedTests: 0, numFailedTests: 1 }),
  );
}

const summary = JSON.parse(readFileSync(COVERAGE_JSON, 'utf8'));
mkdirSync('.ai/evidence/numbering-v1', { recursive: true });
writeFileSync(
  REPORT_OUT,
  JSON.stringify(
    {
      selected: summary.numTotalTests ?? 0,
      executed: summary.numTotalTests ?? 0,
      failed: summary.numFailedTests ?? 0,
    },
    null,
    2,
  ) + '\n',
);
process.exit(run.status ?? (summary.numFailedTests ? 1 : 0));
