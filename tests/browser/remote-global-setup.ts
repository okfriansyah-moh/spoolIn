import { chromium, expect } from '@playwright/test';
import { rmSync } from 'node:fs';
import {
  authenticatedStatePath,
  authenticateMaintainer,
  observeAdmission,
  protectedBrowserDirectory,
  saveProtectedState,
} from './flows/remote/authentication';

export default async function setup() {
  protectedBrowserDirectory();
  const browser = await chromium.launch();
  const context = await browser.newContext({
    baseURL: 'https://localhost:8443',
    ignoreHTTPSErrors: false,
  });
  observeAdmission(context);
  try {
    const page = await context.newPage();
    await authenticateMaintainer(page);
    await expect(page.locator('input[type="password"]')).toHaveCount(0);
    saveProtectedState(authenticatedStatePath(), await context.storageState());
  } finally {
    await context.close();
    await browser.close();
  }
  return async () =>
    rmSync(protectedBrowserDirectory(), { recursive: true, force: true });
}
