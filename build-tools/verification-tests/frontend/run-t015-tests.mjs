import { spawnSync } from 'node:child_process';
import {
  existsSync,
  mkdirSync,
  readFileSync,
  rmSync,
  writeFileSync,
} from 'node:fs';

const REPORT_OUT = '.ai/evidence/numbering-v1/t015-unit-vitest.json';
const COVERAGE_JSON = 'frontend/coverage/test-results.json';

rmSync(COVERAGE_JSON, { force: true });

const run = spawnSync(
  'npm',
  [
    'exec',
    '--workspace',
    'frontend',
    '--',
    'vitest',
    'run',
    '--config',
    'vitest.config.ts',
    'src/lib/features/draft/draft-controller.test.ts',
    '--reporter=json',
    `--outputFile=../${REPORT_OUT}`,
  ],
  { stdio: 'inherit', cwd: process.cwd() },
);

if (!existsSync(REPORT_OUT)) {
  writeFileSync(
    REPORT_OUT,
    JSON.stringify({ testResults: [], numFailedTests: 1 }),
  );
}

const report = JSON.parse(readFileSync(REPORT_OUT, 'utf8'));
const cases = report.testResults ?? [];
const selected = cases.reduce(
  (total, suite) => total + suite.assertionResults.length,
  0,
);
const failed = cases.reduce(
  (total, suite) =>
    total +
    suite.assertionResults.filter((item) => item.status !== 'passed').length,
  0,
);
mkdirSync('.ai/evidence/numbering-v1', { recursive: true });
writeFileSync(
  '.ai/evidence/numbering-v1/t015-unit-results.json',
  JSON.stringify(
    {
      selected,
      executed: selected,
      failed,
      source: REPORT_OUT,
    },
    null,
    2,
  ) + '\n',
);
process.exit(run.status ?? 2);
