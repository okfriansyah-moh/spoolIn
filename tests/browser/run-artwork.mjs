/**
 * T026 – Artwork browser test runner.
 */
import { resolve } from 'node:path';
import { runPlaywrightTask } from './playwright-task-report.mjs';

const ROOT = resolve(new URL('../../', import.meta.url).pathname);
const REPORT_OUT = resolve(ROOT, '.ai/evidence/numbering-v1/t026-integration.json');
const ARTIFACTS = resolve(ROOT, 'tests/browser/results/t026-artwork');

runPlaywrightTask(
  [
    'playwright',
    'test',
    '--config',
    'playwright.app.config.ts',
    'flows/artwork/',
    '--reporter=json',
    `--output=${ARTIFACTS}`,
  ],
  REPORT_OUT,
  ARTIFACTS,
);
