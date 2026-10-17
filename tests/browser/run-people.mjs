/**
 * T019 – People management browser test runner.
 */
import { spawnSync } from 'node:child_process';
import { writeFileSync, mkdirSync, existsSync, readFileSync } from 'node:fs';
import { resolve } from 'node:path';

const ROOT = new URL('../../', import.meta.url).pathname;
const REPORT_OUT = resolve(ROOT, '.ai/evidence/numbering-v1/t019-integration.json');
const RESULTS_JSON = resolve(ROOT, 'tests/browser/results/t019-people.json');

mkdirSync(resolve(ROOT, 'tests/browser/results'), { recursive: true });
mkdirSync(resolve(ROOT, '.ai/evidence/numbering-v1'), { recursive: true });

const run = spawnSync(
  'npx',
  [
    'playwright',
    'test',
    '--config',
    'playwright.app.config.ts',
    'flows/people/',
    '--reporter=json',
    `--output=${RESULTS_JSON}`,
  ],
  {
    stdio: 'inherit',
    cwd: resolve(ROOT, 'tests/browser'),
  },
);

let selected = 0;
let executed = 0;
let failed = 0;
if (existsSync(RESULTS_JSON)) {
  try {
    const pw = JSON.parse(readFileSync(RESULTS_JSON, 'utf8'));
    for (const suite of pw.suites ?? []) {
      for (const spec of suite.specs ?? []) {
        selected += 1;
        executed += 1;
        if (spec.ok === false) failed += 1;
      }
    }
  } catch {
    failed = 1;
  }
}

writeFileSync(
  REPORT_OUT,
  JSON.stringify({ selected, executed, failed, exit_code: run.status ?? failed }, null, 2) + '\n',
);
process.exit(run.status ?? failed);
