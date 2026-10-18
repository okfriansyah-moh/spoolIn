import { test, expect } from '@playwright/test';

test('Public catalog uses real GraphQL, renders seeded records and survives a detail reload', async ({ page }, info) => {
  const apiFailures: string[] = [];
  page.on('response', async (response) => {
    if (new URL(response.url()).pathname !== '/graphql') return;
    if (!response.ok()) apiFailures.push(`HTTP ${response.status()}`);
    const body: unknown = await response.json().catch(() => null);
    if (body && typeof body === 'object' && 'errors' in body) apiFailures.push('GraphQL errors');
  });
  await test.step('Open the catalog and wait for actual API results', async () => {
    await page.goto('/');
    await expect(page.getByRole('heading', { name: 'Every reel holds a story.' })).toBeVisible();
    await expect(page.getByRole('list', { name: 'Titles' })).toBeVisible();
    await expect(page.locator('.title-card')).not.toHaveCount(0);
    await expect(page.getByLabel('Month', { exact: true })).toBeDisabled();
    expect(apiFailures).toEqual([]);
  });
  await page.screenshot({ path: info.outputPath('catalog.png'), fullPage: true });
  await test.step('Follow a visible title link and reload its production route', async () => {
    const link = page.locator('.title-card h3 a').first();
    const title = await link.innerText();
    await link.click();
    await expect(page.getByRole('heading', { level: 1 })).toHaveText(title);
    await page.reload();
    await expect(page.getByRole('heading', { level: 1 })).toHaveText(title);
    expect(apiFailures).toEqual([]);
  });
  await page.screenshot({ path: info.outputPath('detail.png'), fullPage: true });
  const broken = await page.locator('img').evaluateAll((images) =>
    images.filter((image) => !image.complete || image.naturalWidth === 0).map((image) => image.getAttribute('src')));
  expect(broken).toEqual([]);
  expect(await page.locator('html').evaluate((root) => root.scrollWidth <= window.innerWidth)).toBe(true);
});
