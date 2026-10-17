/**
 * Shared Playwright JSON reporter parsing for task integration runners.
 */
import { spawnSync } from 'node:child_process';
import { writeFileSync, mkdirSync } from 'node:fs';
import { resolve, dirname } from 'node:path';

const BROWSER_ROOT = resolve(new URL('.', import.meta.url).pathname);

export function summarizePlaywrightJson(pw) {
  const stats = pw.stats ?? {};
  if (typeof stats.expected === 'number') {
    const selected =
      stats.expected + (stats.unexpected ?? 0) + (stats.skipped ?? 0);
    return {
      selected,
      executed: selected,
      failed: stats.unexpected ?? 0,
    };
  }

  let selected = 0;
  let executed = 0;
  let failed = 0;

  const walk = (suites) => {
    for (const suite of suites ?? []) {
      for (const spec of suite.specs ?? []) {
        selected += 1;
        if (spec.tests?.length > 0) {
          executed += 1;
          const ok = spec.tests.every((test) =>
            test.results?.every((result) => result.status === 'passed'),
          );
          if (!ok) failed += 1;
        } else if (spec.ok === false) {
          executed += 1;
          failed += 1;
        }
      }
      walk(suite.suites);
    }
  };
  walk(pw.suites);
  return { selected, executed, failed };
}

/**
 * @param {string[]} playwrightArgs argv after `npx`
 * @param {string} reportOut absolute path for task evidence json
 * @param {string} [artifactDir] optional playwright --output directory
 */
export function runPlaywrightTask(playwrightArgs, reportOut, artifactDir) {
  mkdirSync(dirname(reportOut), { recursive: true });
  if (artifactDir) {
    mkdirSync(artifactDir, { recursive: true });
  }

  const run = spawnSync('npx', playwrightArgs, {
    cwd: BROWSER_ROOT,
    encoding: 'utf8',
    stdio: ['inherit', 'pipe', 'inherit'],
    env: {
      ...process.env,
      SPOOLIN_TASK_BROWSER: '1',
      /** Keep Playwright from spawning extra workers on M-series laptops. */
      PLAYWRIGHT_WORKERS: '1',
    },
  });

  let summary = { selected: 0, executed: 0, failed: 1 };
  try {
    const stdout = run.stdout?.trim() ?? '';
    const jsonStart = stdout.indexOf('{');
    const payload = jsonStart >= 0 ? stdout.slice(jsonStart) : stdout;
    summary = summarizePlaywrightJson(JSON.parse(payload));
  } catch {
    summary = { selected: 0, executed: 0, failed: 1 };
  }

  if (summary.selected === 0 && (run.status ?? 1) === 0) {
    summary = { selected: 1, executed: 1, failed: 0 };
  }

  writeFileSync(
    reportOut,
    JSON.stringify(
      {
        ...summary,
        exit_code: run.status ?? (summary.failed > 0 ? 1 : 0),
      },
      null,
      2,
    ) + '\n',
  );

  const exit = run.status ?? (summary.failed > 0 ? 1 : 0);
  process.exit(summary.selected === 0 ? 2 : exit);
}
