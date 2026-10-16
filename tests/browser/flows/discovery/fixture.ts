/**
 * T013 – Discovery browser test fixture helpers.
 * Shared setup and assertion utilities for discovery, detail and people flows.
 */
import type { Page, BrowserContext } from '@playwright/test';

/** Clear cookies for a fresh anonymous session. */
export async function freshSession(context: BrowserContext): Promise<void> {
  await context.clearCookies();
}

/** Navigate to the discovery page and wait for the shell to load. */
export async function goToDiscovery(page: Page): Promise<void> {
  await page.goto('/');
  await page.waitForLoadState('networkidle');
}

/** Navigate to the people list. */
export async function goToPeople(page: Page): Promise<void> {
  await page.goto('/people');
  await page.waitForLoadState('networkidle');
}

/** Navigate to a title detail page by ID. */
export async function goToTitle(page: Page, id: string): Promise<void> {
  await page.goto(`/titles/${encodeURIComponent(id)}`);
  await page.waitForLoadState('networkidle');
}

/** Navigate to a person detail page by ID. */
export async function goToPerson(page: Page, id: string): Promise<void> {
  await page.goto(`/people/${encodeURIComponent(id)}`);
  await page.waitForLoadState('networkidle');
}

/** Expect the discovery page headline is visible. */
export async function expectDiscoveryHeadline(page: Page): Promise<void> {
  const h1 = page.getByRole('heading', { level: 1 });
  await h1.waitFor({ state: 'visible' });
}

/** Assert the page title contains the expected string. */
export async function assertPageTitle(page: Page, expected: string): Promise<void> {
  await page.waitForFunction(
    (t: string) => document.title.includes(t),
    expected,
    { timeout: 5000 },
  );
}

/** Assert a status region is visible (loading / live feedback). */
export async function assertStatusVisible(page: Page, text: RegExp | string): Promise<void> {
  const status = page.locator('[role="status"], [role="alert"]').filter({ hasText: text });
  await status.first().waitFor({ state: 'visible' });
}

/** Assert navigation breadcrumb contains catalog link. */
export async function assertCatalogLink(page: Page): Promise<void> {
  const link = page.getByRole('link', { name: /catalog/i }).first();
  await link.waitFor({ state: 'visible' });
}

/** Check search input exists and is functional. */
export async function assertSearchInput(page: Page): Promise<void> {
  const input = page.getByLabel(/search titles/i);
  await input.waitFor({ state: 'visible' });
}
