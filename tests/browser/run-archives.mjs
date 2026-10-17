/**
 * T025 – Archives browser test runner.
 */
import { resolve } from 'node:path';
import { runPlaywrightTask } from './playwright-task-report.mjs';

const ROOT = resolve(new URL('../../', import.meta.url).pathname);
const REPORT_OUT = resolve(ROOT, '.ai/evidence/numbering-v1/t025-integration.json');
const ARTIFACTS = resolve(ROOT, 'tests/browser/results/t025-archives');

runPlaywrightTask(
  [
    'playwright',
    'test',
    '--config',
    'playwright.app.config.ts',
    'flows/archives/',
    '--reporter=json',
    `--output=${ARTIFACTS}`,
  ],
  REPORT_OUT,
  ARTIFACTS,
);
