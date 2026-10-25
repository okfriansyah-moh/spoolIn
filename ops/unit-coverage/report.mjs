import { createHash } from 'node:crypto';
import { execFileSync } from 'node:child_process';
import {
  existsSync,
  readFileSync,
  readdirSync,
  statSync,
  writeFileSync,
} from 'node:fs';
import { join } from 'node:path';

export const output = '.local/unit-coverage';
export const components = [
  { id: 'frontend', path: 'frontend', kind: 'vitest' },
  { id: 'core', path: 'services/core', kind: 'kover' },
  { id: 'catalog', path: 'services/catalog', kind: 'kover' },
  { id: 'media', path: 'services/media', kind: 'kover' },
  { id: 'backend-support', path: 'libraries/backend-support', kind: 'kover' },
  { id: 'mcp', path: 'mcp', kind: 'vitest' },
];

export function fingerprint() {
  const paths = execFileSync(
    'git',
    [
      'ls-files',
      '-z',
      '--cached',
      '--others',
      '--exclude-standard',
      '--',
      'frontend',
      'mcp',
      'services',
      'libraries',
      'contracts',
      'gradle',
      'build-tools',
      'data/seed',
      'ops/unit-coverage',
      'ops/local/toolchain.Dockerfile',
      'ops/bin/coverage.sh',
      'build.gradle.kts',
      'settings.gradle.kts',
      'gradle.properties',
      'gradlew',
      'gradlew.bat',
      'package.json',
      'package-lock.json',
    ],
    { encoding: 'utf8' },
  );
  const hash = createHash('sha256');
  for (const path of [...new Set(paths.split('\0').filter(Boolean))].sort()) {
    hash.update(path).update('\0');
    hash
      .update(existsSync(path) ? readFileSync(path) : '<deleted>')
      .update('\0');
  }
  return hash.digest('hex');
}

function metric(covered, total) {
  if (
    !Number.isInteger(covered) ||
    !Number.isInteger(total) ||
    covered < 0 ||
    total < covered
  ) {
    throw new Error('Invalid coverage counters');
  }
  return { covered, total, percent: total ? (covered * 100) / total : null };
}

export function xmlAttributes(text) {
  return Object.fromEntries(
    [...text.matchAll(/([\w.-]+)="([^"]*)"/g)].map((match) => [
      match[1],
      match[2],
    ]),
  );
}

export function koverMetrics(xml) {
  if (!xml.trimEnd().endsWith('</report>'))
    throw new Error('Incomplete Kover report');
  const totals = xml.match(
    /((?:\s*<counter\b[^>]*\/>)+)\s*<\/report>\s*$/,
  )?.[1];
  if (!totals) throw new Error('Missing report-level counters');
  const counters = Object.fromEntries(
    [...totals.matchAll(/<counter\b([^>]+)\/>/g)].map((match) => {
      const value = xmlAttributes(match[1]);
      return [
        value.type,
        metric(
          Number(value.covered),
          Number(value.covered) + Number(value.missed),
        ),
      ];
    }),
  );
  if (!counters.LINE) throw new Error('Missing line counters');
  return {
    lines: counters.LINE,
    branches: counters.BRANCH,
    functions: counters.METHOD,
  };
}

export function vitestMetrics(summary) {
  return Object.fromEntries(
    ['lines', 'branches', 'functions', 'statements'].map((key) => {
      const value = summary.total?.[key];
      if (!value) throw new Error(`Missing ${key} counters`);
      return [key, metric(value.covered, value.total)];
    }),
  );
}

export function junitResults(xmlFiles) {
  const counts = { total: 0, failures: 0, errors: 0, skipped: 0 };
  for (const xml of xmlFiles) {
    for (const match of xml.matchAll(/<testsuite\b([^>]+)>/g)) {
      const attributes = xmlAttributes(match[1]);
      for (const [key, attr] of Object.entries({
        total: 'tests',
        failures: 'failures',
        errors: 'errors',
        skipped: 'skipped',
      })) {
        const value = Number(attributes[attr] ?? 0);
        if (!Number.isInteger(value) || value < 0)
          throw new Error('Invalid JUnit test counters');
        counts[key] += value;
      }
    }
  }
  counts.executed = counts.total - counts.skipped;
  return counts;
}

function testResults(component, directory) {
  if (component.kind === 'vitest') {
    const tests = JSON.parse(
      readFileSync(join(directory, 'test-results.json'), 'utf8'),
    );
    for (const key of ['numTotalTests', 'numPassedTests', 'numFailedTests']) {
      if (!Number.isInteger(tests[key]) || tests[key] < 0)
        throw new Error('Invalid Vitest test counters');
    }
    return {
      total: tests.numTotalTests,
      executed: tests.numPassedTests + tests.numFailedTests,
      failures: tests.numFailedTests,
      errors: tests.success === true ? 0 : 1,
      skipped: tests.numPendingTests ?? 0,
    };
  }
  const testDirectory = join(directory, 'tests');
  const files = readdirSync(testDirectory).filter(
    (file) => file.startsWith('TEST-') && file.endsWith('.xml'),
  );
  return junitResults(
    files.map((file) => readFileSync(join(testDirectory, file), 'utf8')),
  );
}

export function componentResult(
  component,
  command,
  directory = join(output, component.id),
) {
  const result = {
    component: component.id,
    status: 'failed',
    command,
    html: `${component.id}/index.html`,
  };
  try {
    result.metrics =
      component.kind === 'vitest'
        ? vitestMetrics(
            JSON.parse(
              readFileSync(join(directory, 'coverage-summary.json'), 'utf8'),
            ),
          )
        : koverMetrics(readFileSync(join(directory, 'report.xml'), 'utf8'));
    result.tests = testResults(component, directory);
    const failures = [];
    if (command !== 0) failures.push('Suite command failed');
    if (!(result.tests.executed > 0)) failures.push('No tests executed');
    if (result.tests.failures || result.tests.errors)
      failures.push('Tests failed');
    if (!(result.metrics.lines.percent >= 80))
      failures.push('Line coverage below 80% or empty');
    if (!statSync(join(directory, 'index.html')).isFile())
      failures.push('HTML report missing');
    if (
      component.kind === 'vitest' &&
      !readFileSync(join(directory, 'lcov.info'), 'utf8').includes(
        'end_of_record',
      )
    ) {
      failures.push('LCOV report missing or empty');
    }
    result.reasons = failures;
    if (!failures.length) result.status = 'passed';
  } catch (error) {
    result.reasons = [error.message];
  }
  return result;
}

const escape = (value) =>
  String(value).replace(
    /[&<>"']/g,
    (character) =>
      ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[
        character
      ],
  );
const percent = (value) =>
  value == null ? 'Unavailable' : `${value.toFixed(2)}%`;

export function writeSummary(summary) {
  writeFileSync(
    join(output, 'summary.json'),
    `${JSON.stringify(summary, null, 2)}\n`,
  );
  const rows = summary.components
    .map(
      (item) => `<tr><th>${escape(item.component)}</th>
    <td>${escape(item.status)}</td><td>${percent(item.metrics?.lines.percent)}</td>
    <td>${item.metrics?.lines.covered ?? '—'} / ${item.metrics?.lines.total ?? '—'}</td>
    <td>${percent(item.metrics?.branches?.percent)}</td><td>${percent(item.metrics?.functions?.percent)}</td>
    <td>${item.tests?.executed ?? '—'} executed; ${(item.tests?.failures ?? 0) + (item.tests?.errors ?? 0)} failed</td>
    <td><a href="${item.html}">HTML</a> · <a href="${escape(summary.sonarUrl)}/dashboard?id=spoolin-unit-${item.component}">Sonar</a></td>
    <td>${escape((item.reasons ?? []).join('; '))}</td></tr>`,
    )
    .join('');
  writeFileSync(
    join(output, 'index.html'),
    `<!doctype html><html lang="en"><meta charset="utf-8">
    <meta name="viewport" content="width=device-width,initial-scale=1"><title>SpoolIn unit coverage</title>
    <style>body{font:16px system-ui;margin:2rem;line-height:1.5}table{border-collapse:collapse}td,th{padding:.6rem;border:1px solid #999;text-align:left}.scroll{overflow:auto}a{color:#0645ad}</style>
    <h1>SpoolIn unit coverage — ${escape(summary.status)}</h1><p>Minimum: 80% executable lines in every component.</p>
    <p>Revision: <code>${escape(summary.revision)}</code>${summary.dirty ? ' (working changes)' : ''}<br>
    Started: ${escape(summary.startedAt)}<br>Finished: ${escape(summary.finishedAt)}</p>
    <p>${escape(summary.reason ?? '')}</p><div class="scroll"><table><thead><tr><th>Component</th><th>Status</th><th>Lines</th>
    <th>Covered / total</th><th>Branches</th><th>Functions / methods</th><th>Tests</th><th>Reports</th><th>Failure</th></tr></thead><tbody>${rows}</tbody></table></div>
    <p>Frontend HTML and the enforced threshold include TypeScript and Svelte. Sonar's frontend view covers TypeScript; its Svelte analysis is unavailable.
    Sonar links show the last published analysis, which may predate this run. See <a href="sonar-results.json">Sonar publication results</a>.</p></html>`,
  );
}
