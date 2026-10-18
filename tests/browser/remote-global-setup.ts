import { chromium, expect } from '@playwright/test';
import { rmSync } from 'node:fs';
import {
  authenticatedStatePath,
  authenticateMaintainer,
  observeAdmission,
  observeAuthentication,
  protectedBrowserDirectory,
  saveProtectedState,
} from './flows/remote/authentication';

export default async function setup() {
  protectedBrowserDirectory();
  const browser = await chromium.launch();
  const context = await browser.newContext({
    baseURL: 'https://localhost:8443',
    ignoreHTTPSErrors: false,
    viewport: { width: 1440, height: 900 },
    colorScheme: 'dark',
    reducedMotion: 'reduce',
  });
  observeAdmission(context);
  let diagnostics: ReturnType<typeof observeAuthentication> | undefined;
  try {
    const page = await context.newPage();
    diagnostics = observeAuthentication(page);
    try {
      await authenticateMaintainer(page, diagnostics);
      await expect(page.locator('input[type="password"]')).toHaveCount(0);
      saveProtectedState(
        authenticatedStatePath(),
        await context.storageState(),
      );
    } catch (error) {
      await diagnostics.capture('failure').catch(() => {
        console.warn('Authentication failure diagnostic capture unavailable.');
      });
      throw error;
    }
  } finally {
    diagnostics?.dispose();
    await context.close();
    await browser.close();
  }
  return async () =>
    rmSync(protectedBrowserDirectory(), { recursive: true, force: true });
}
