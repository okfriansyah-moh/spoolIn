import { test as base, type BrowserContext } from '@playwright/test';
import { execFileSync } from 'node:child_process';
import { mkdtempSync, readFileSync, rmSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
export type Credentials = {
  password: string;
  viewerEmail: string;
  maintainerEmail: string;
  registrationEmail: string;
};
export function credentials(): Credentials {
  const file = process.env.ACCOUNT_CREDENTIALS_FILE;
  if (!file) throw new Error('Protected browser input is required');
  return JSON.parse(readFileSync(file, 'utf8'));
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
        execFileSync(
          '/opt/certutil',
          ['-N', '--empty-password', '-d', `sql:${profile}`],
          { stdio: 'pipe' },
        );
        execFileSync(
          '/opt/certutil',
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
