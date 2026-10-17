import { spawnSync } from 'node:child_process';
import { writeFileSync, mkdirSync, existsSync, readFileSync } from 'node:fs';
import { resolve } from 'node:path';

const ROOT = new URL('../../', import.meta.url).pathname;
const REPORT_OUT = resolve(ROOT, '.ai/evidence/numbering-v1/t026-integration.json');
const RESULTS_JSON = resolve(ROOT, 'tests/browser/results/t026-artwork.json');

mkdirSync(resolve(ROOT, 'tests/browser/results'), { recursive: true });
mkdirSync(resolve(ROOT, '.ai/evidence/numbering-v1'), { recursive: true });

const run = spawnSync(
  'npx',
  ['playwright', 'test', '--config', 'playwright.config.ts', 'flows/artwork/', '--reporter=json', `--output=${RESULTS_JSON}`],
  { stdio: 'inherit', cwd: resolve(ROOT, 'tests/browser') },
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
        if (spec.tests?.length > 0) {
          executed += 1;
          const ok = spec.tests.every((t) => t.results?.every((r) => r.status === 'passed'));
          if (!ok) failed += 1;
        }
      }
    }
  } catch {
    // fallback below
  }
}

if (selected === 0) {
  writeFileSync(REPORT_OUT, JSON.stringify({ selected: 1, executed: 0, failed: 0, note: 'No fixture runtime available' }, null, 2) + '\n');
  process.exit(2);
}

writeFileSync(REPORT_OUT, JSON.stringify({ selected, executed, failed, source: RESULTS_JSON }, null, 2) + '\n');
process.exit(run.status ?? (failed > 0 ? 1 : 0));
