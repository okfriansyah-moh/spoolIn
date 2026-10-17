/**
 * T021 – Credit browser test runner.
 */
import { resolve } from 'node:path';
import { runPlaywrightTask } from './playwright-task-report.mjs';

const ROOT = resolve(new URL('../../', import.meta.url).pathname);
const REPORT_OUT = resolve(ROOT, '.ai/evidence/numbering-v1/t021-integration.json');
const ARTIFACTS = resolve(ROOT, 'tests/browser/results/t021-credits');

runPlaywrightTask(
  [
    'playwright',
    'test',
    '--config',
    'playwright.app.config.ts',
    'flows/credits/',
    '--reporter=json',
    `--output=${ARTIFACTS}`,
  ],
  REPORT_OUT,
  ARTIFACTS,
);
