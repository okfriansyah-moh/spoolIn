import type { BrowserContext, Page } from '@playwright/test';

export async function freshSession(context: BrowserContext): Promise<void> {
  await context.clearCookies();
}

export async function goToPeopleList(page: Page): Promise<void> {
  await page.goto('/manage/people');
  await page.waitForLoadState('networkidle');
}

export async function goToCreatePerson(page: Page): Promise<void> {
  await page.goto('/manage/people/new');
  await page.waitForLoadState('networkidle');
}
