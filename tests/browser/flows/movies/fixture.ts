import type { BrowserContext, Page } from '@playwright/test';

export async function freshSession(context: BrowserContext): Promise<void> {
  await context.clearCookies();
}

export async function goToMovieList(page: Page): Promise<void> {
  await page.goto('/manage/movies');
  await page.waitForLoadState('networkidle');
}

export async function goToCreateMovie(page: Page): Promise<void> {
  await page.goto('/manage/movies/new');
  await page.waitForLoadState('networkidle');
}
