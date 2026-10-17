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
      try {
        const certutil = certutilPath();
        execFileSync(
          certutil,
          ['-N', '--empty-password', '-d', `sql:${profile}`],
          { stdio: 'pipe' },
        );
        execFileSync(
          certutil,
          [
            '-A',
            '-d',
            `sql:${profile}`,
            '-n',
            'Account fixture CA',
            '-t',
            'CT,C,C',
            '-i',
            ca,
          ],
          { stdio: 'pipe' },
        );
        const context = await playwright.firefox.launchPersistentContext(
          profile,
          {
            headless: false,
            ignoreHTTPSErrors: false,
            baseURL: origin,
            viewport: { width: 390, height: 900 },
            reducedMotion: 'reduce',
          },
        );
        await observeStorage(context);
        try {
          await use(context);
        } finally {
          await context.close();
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
