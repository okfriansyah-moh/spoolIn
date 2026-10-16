import { spawnSync } from 'node:child_process';
import { readFileSync, writeFileSync, mkdirSync, rmSync } from 'node:fs';
rmSync('frontend/coverage/test-results.json', { force: true });
const run = spawnSync(
  'npm',
  ['run', 'test:coverage', '--workspace', 'frontend'],
  { stdio: 'inherit' },
);
const report = JSON.parse(
  readFileSync('frontend/coverage/test-results.json', 'utf8'),
);
mkdirSync('.ai/evidence/numbering-v1', { recursive: true });
writeFileSync(
  '.ai/evidence/numbering-v1/t004-unit.json',
  JSON.stringify(
    {
      selected: report.numTotalTests,
      executed: report.numPassedTests + report.numFailedTests,
      failed: report.numFailedTests,
      source: 'frontend/coverage/test-results.json',
    },
    null,
    2,
  ) + '\n',
);
process.exit(run.status ?? 2);
