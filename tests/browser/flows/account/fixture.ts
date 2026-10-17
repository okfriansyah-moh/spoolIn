import { test as base, type BrowserContext } from '@playwright/test';
import { execFileSync, execSync } from 'node:child_process';
import { mkdtempSync, readFileSync, rmSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
export type Credentials = {
  password: string;
  viewerEmail: string;
  maintainerEmail: string;
  registrationEmail: string;
};
function certutilPath(): string {
  const explicit = process.env.ACCOUNT_CERTUTIL;
  if (explicit) return explicit;
  try {
    return execSync('which certutil', { encoding: 'utf8' }).trim();
  } catch {
    return '/opt/certutil';
  }
}

function trustFixtureCa(profile: string, ca: string) {
  const certutil = certutilPath();
  const nss = `sql:${profile}`;
  const importCa = () =>
    execFileSync(
      certutil,
      ['-A', '-d', nss, '-n', 'Account fixture CA', '-t', 'CT,C,C', '-i', ca],
      { stdio: 'pipe' },
    );
  try {
    importCa();
  } catch {
    execFileSync(certutil, ['-N', '--empty-password', '-d', nss], {
      stdio: 'pipe',
    });
    importCa();
  }
}

function accountEngine(): 'chromium' | 'firefox' {
  const raw = process.env.ACCOUNT_BROWSER_ENGINE?.toLowerCase();
  if (raw === 'firefox') return 'firefox';
  if (raw === 'chromium' || raw === 'chrome') return 'chromium';
  if (process.env.ACCOUNT_BROWSER_HEADED === '1') return 'firefox';
  return 'chromium';
}

export function credentials(): Credentials {
  const file = process.env.ACCOUNT_CREDENTIALS_FILE;
  if (!file) throw new Error('Protected browser input is required');
  return JSON.parse(readFileSync(file, 'utf8'));
}
async function observeStorage(context: BrowserContext) {
  await context.addInitScript(() => {
    const observations: boolean[] = [];
    Object.defineProperty(window, '__accountStorageProof', {
      value: observations,
    });
    const set = Storage.prototype.setItem;
    Storage.prototype.setItem = function (key: string, value: string) {
      observations.push(
        this === localStorage &&
          key === 'spoolin.theme' &&
          (value === 'dark' || value === 'light'),
      );
      return set.call(this, key, value);
    };
    const get = Storage.prototype.getItem;
    Storage.prototype.getItem = function (key: string) {
      observations.push(this === localStorage && key === 'spoolin.theme');
      return get.call(this, key);
    };
    const open = indexedDB.open.bind(indexedDB);
    indexedDB.open = (...arguments_: Parameters<typeof open>) => {
      observations.push(false);
      return open(...arguments_);
    };
  });
}
export const test = base.extend<object, { accountContext: BrowserContext }>({
  accountContext: [
    async ({ playwright }, use) => {
      const ca = process.env.ACCOUNT_CA;
      const origin = process.env.ACCOUNT_ORIGIN;
      if (!ca || !origin)
        throw new Error('Actual HTTPS origin and public CA are required');
      const profile = mkdtempSync(join(tmpdir(), 'spoolin-account-profile-'));
      const engine = accountEngine();
      const headed = process.env.ACCOUNT_BROWSER_HEADED === '1';
      const shared = {
        ignoreHTTPSErrors: false,
        baseURL: origin,
        viewport: { width: 390, height: 900 },
        reducedMotion: 'reduce' as const,
      };
      try {
        if (engine === 'chromium') {
          const bootstrap = await playwright.chromium.launchPersistentContext(
            profile,
            { ...shared, channel: 'chrome', headless: true },
          );
          await bootstrap.close();
          trustFixtureCa(profile, ca);
          const context = await playwright.chromium.launchPersistentContext(
            profile,
            { ...shared, channel: 'chrome', headless: true },
          );
          await observeStorage(context);
          try {
            await use(context);
          } finally {
            await context.close();
          }
        } else {
          const bootstrap = await playwright.firefox.launchPersistentContext(
            profile,
            { ...shared, headless: false },
          );
          await bootstrap.close();
          trustFixtureCa(profile, ca);
          const context = await playwright.firefox.launchPersistentContext(
            profile,
            { ...shared, headless: !headed },
          );
          await observeStorage(context);
          try {
            await use(context);
          } finally {
            await context.close();
          }
        }
      } finally {
        rmSync(profile, { recursive: true, force: true });
      }
    },
    { scope: 'worker' },
  ],
  context: async ({ accountContext }, use) => {
    await use(accountContext);
  },
});
export { expect } from '@playwright/test';
