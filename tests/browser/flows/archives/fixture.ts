import type { BrowserContext, Page } from '@playwright/test';

export async function freshSession(context: BrowserContext): Promise<void> {
  await context.clearCookies();
}

export async function goToArchives(page: Page): Promise<void> {
  await page.goto('/manage/archives');
  await page.waitForLoadState('networkidle');
}
