import { test, expect } from '@playwright/test';
import { seedCatalogGraphql } from './fixture';

test.beforeEach(async ({ page }) => {
  await seedCatalogGraphql(page);
  await page.route('**/graphql', async (route) => {
    const body = route.request().postDataJSON();
    if (!body.query.includes('query Titles')) return route.fallback();
    const next = !!body.variables.after;
    await route.fulfill({
      contentType: 'application/graphql-response+json',
      body: JSON.stringify({
        data: {
          titles: {
            nodes: Array.from({ length: 24 }, (_, i) => ({
              id: `00000000-0000-4000-8000-${String(i + (next ? 25 : 1)).padStart(12, '0')}`,
              kind: 'MOVIE',
              title: `${next ? 'Second' : 'First'} page title ${i + 1}`,
              release: null,
              artwork: null,
              genres: [],
            })),
            pageInfo: {
              endCursor: next ? 'second-page' : 'first-page',
              hasNextPage: true,
            },
          },
        },
      }),
    });
  });
  await page.goto('/');
  await expect(page.locator('.title-grid > li')).toHaveCount(24);
});

test('catalog review: next titles page keeps the reader down at the results', async ({
  page,
}) => {
  const next = page.getByRole('button', {
    name: 'Next titles page',
    exact: true,
  });
  await next.scrollIntoViewIfNeeded();
  const scrollBefore = await page.evaluate(() => window.scrollY);
  expect(scrollBefore).toBeGreaterThan(500);
  const historyBefore = await page.evaluate(() => history.length);
  await next.click();
  await expect(page.locator('.title-grid')).toContainText(
    'Second page title 24',
  );
  await expect
    .poll(() => page.evaluate(() => history.length))
    .toBe(historyBefore + 1);
  await expect
    .poll(() => page.evaluate(() => window.scrollY))
    .toBeGreaterThanOrEqual(scrollBefore - 10);
});

test('catalog review: search keeps keyboard focus across words and Enter searches the full title', async ({
  page,
}) => {
  const search = page.getByLabel('Search titles', { exact: true });
  await search.click();
  await page.keyboard.type('Breaking', { delay: 80 });
  await expect(search).toBeFocused();
  await page.keyboard.type(' Bad', { delay: 100 });
  await expect(search).toHaveValue('Breaking Bad');
  const request = page.waitForRequest((req) => {
    if (!req.url().endsWith('/graphql') || req.method() !== 'POST')
      return false;
    const body = req.postDataJSON();
    return (
      body.query.includes('query Titles') &&
      JSON.stringify(body.variables).includes('Breaking Bad')
    );
  });
  await page.keyboard.press('Enter');
  await request;
  await expect(search).toBeFocused();
  await expect(page).toHaveURL(/q=Breaking\+Bad/);
});
