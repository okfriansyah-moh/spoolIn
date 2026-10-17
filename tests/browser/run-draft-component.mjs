import { spawnSync } from 'node:child_process';
import { mkdirSync, rmSync } from 'node:fs';
import { dirname, resolve } from 'node:path';

const root = resolve(import.meta.dirname, '../..');
const report = resolve(root, 'test-results/draft-component/junit.xml');

mkdirSync(dirname(report), { recursive: true });
rmSync(report, { force: true });

const result = spawnSync(
  process.execPath,
  [
    'node_modules/@playwright/test/cli.js',
    'test',
    '--config',
    'tests/browser/playwright.draft-component.config.ts',
  ],
  { cwd: root, stdio: 'inherit', timeout: 600000 },
);

process.exit(result.status ?? 2);
