import { spawnSync } from 'node:child_process';
import {
  closeSync,
  mkdirSync,
  openSync,
  readFileSync,
  readdirSync,
  rmSync,
  writeFileSync,
} from 'node:fs';
import { dirname, resolve } from 'node:path';
const root = resolve(import.meta.dirname, '../..');
const report = resolve(root, 'test-results/account/junit.xml');
function execute(argv, env = process.env) {
  const result = spawnSync(argv[0], argv.slice(1), {
    cwd: root,
    env,
    stdio: 'inherit',
    timeout: 600000,
  });
  if (result.error) throw result.error;
  return result.status ?? 1;
}
function readerEnvironment() {
  const environment = {
    ...process.env,
    DISPLAY: process.env.DISPLAY || ':99',
    NODE_EXTRA_CA_CERTS: process.env.ACCOUNT_CA,
  };
  const server = readdirSync('/proc')
    .filter((name) => /^\d+$/.test(name))
    .find((name) => {
      try {
        return readFileSync(`/proc/${name}/cmdline`, 'utf8')
          .split('\0')
          .includes('/opt/orca-run/browser-server.js');
      } catch {
        return false;
      }
    });
  if (server) {
    const bus = readFileSync(`/proc/${server}/environ`, 'utf8')
      .split('\0')
      .find((entry) => entry.startsWith('DBUS_SESSION_BUS_ADDRESS='));
    if (bus)
      environment.DBUS_SESSION_BUS_ADDRESS = bus.slice(bus.indexOf('=') + 1);
  }
  return environment;
}
function releaseFixture(handoff) {
  const target = handoff.releaseFile;
  if (target) {
    closeSync(openSync(target, 'a'));
  }
}

function run() {
  if (process.env.ACCOUNT_BROWSER_GATE === '1') {
    const script = resolve(root, 'ops/bin/account-browser-gate.sh');
    return execute(['bash', script]);
  }
  const cli = [
    process.execPath,
    'node_modules/@playwright/test/cli.js',
    'test',
    '--config',
    'tests/browser/playwright.account.config.ts',
  ];
  if (process.argv.includes('--inside'))
    return execute(cli, readerEnvironment());
  mkdirSync(dirname(report), { recursive: true });
  rmSync(report, { force: true });
  const handoff = JSON.parse(
    readFileSync(
      resolve(root, '.ai/state/account-browser-public.json'),
      'utf8',
    ),
  );
  const container = process.env.ACCOUNT_BROWSER_CONTAINER;
  const env = {
    ...process.env,
    ACCOUNT_ORIGIN: handoff.origin,
    ACCOUNT_CA: handoff.ca,
    NODE_EXTRA_CA_CERTS: handoff.ca,
    ACCOUNT_CREDENTIALS_FILE: handoff.credentialsFile,
    ACCOUNT_JUNIT: report,
    ACCOUNT_BROWSER_ENGINE: process.env.ACCOUNT_BROWSER_ENGINE || 'chromium',
    ACCOUNT_BROWSER_HEADED: process.env.ACCOUNT_BROWSER_HEADED || '0',
  };
  const code = container ? runContainer(container, handoff) : execute(cli, env);
  if (code === 0) releaseFixture(handoff);
  return code;
}
function runContainer(container, handoff) {
  const insideReport = '/tmp/account-results/gate/junit.xml';
  const argv = [
    'docker',
    'exec',
    '-e',
    `ACCOUNT_ORIGIN=${handoff.origin}`,
    '-e',
    'ACCOUNT_CA=/work/public/ca.crt',
    '-e',
    'ACCOUNT_CREDENTIALS_FILE=/tmp/account-input.json',
    '-e',
    'ACCOUNT_RESULTS=/tmp/account-results/gate/browser',
    '-e',
    `ACCOUNT_JUNIT=${insideReport}`,
    '-w',
    '/work/project',
    container,
    'node',
    'tests/browser/run-account.mjs',
    '--inside',
  ];
  const removed = execute([
    'docker',
    'exec',
    container,
    'rm',
    '-f',
    insideReport,
  ]);
  if (removed) return removed;
  const code = execute(argv);
  const copied = spawnSync('docker', ['exec', container, 'cat', insideReport], {
    encoding: null,
    timeout: 30000,
  });
  if (copied.status === 0) writeFileSync(report, copied.stdout);
  const finalCode = code || (copied.status === 0 ? 0 : 2);
  if (finalCode === 0) releaseFixture(handoff);
  return finalCode;
}
try {
  process.exitCode = run();
} catch {
  console.error(
    'Actual account browser environment is unavailable; inspect the configured fixture and runtime.',
  );
  process.exitCode = 2;
}
