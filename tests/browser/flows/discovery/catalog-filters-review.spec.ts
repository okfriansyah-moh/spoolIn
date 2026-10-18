import { test, expect, type Page } from '@playwright/test';
import { seedCatalogGraphql } from './fixture';

test.use({ viewport: { width: 1440, height: 900 } });
const drama = '10000000-0000-4000-8000-000000000010';
const comedy = '10000000-0000-4000-8000-000000000011';

async function seedFilters(page: Page) {
  await seedCatalogGraphql(page);
  await page.route('**/graphql', async (route) => {
    const body = route.request().postDataJSON();
    if (body.query.includes('query CatalogFilterOptions')) {
      await route.fulfill({
        json: {
          data: {
            catalogFilterOptions: {
              genres: [
                { id: drama, label: 'Drama' },
                { id: comedy, label: 'Comedy' },
              ],
              releaseYears: [
                { year: 2006, months: [1, 3] },
                { year: 1999, months: [] },
              ],
            },
          },
        },
      });
      return;
    }
    if (!body.query.includes('query Titles')) return route.fallback();
    const filter = body.variables.filter;
    const empty =
      filter.year === 1800 ||
      filter.kind === 'TV' ||
      (filter.year === 1999 && filter.genreIds?.includes(drama));
    await new Promise((resolve) => setTimeout(resolve, 150));
    await route.fulfill({
      json: {
        data: {
          titles: {
            nodes: empty
              ? []
              : Array.from({ length: 24 }, (_, i) => ({
                  id: `00000000-0000-4000-8000-${String(i + 1).padStart(12, '0')}`,
                  kind: 'MOVIE',
                  title: `Filter result ${i + 1}`,
                  release: null,
                  artwork: null,
                  genres: [],
                })),
            pageInfo: { endCursor: null, hasNextPage: false },
          },
        },
      },
    });
  });
  await page.goto('/');
  await expect(page.locator('.title-grid > li')).toHaveCount(24);
  await expect(
    page.getByLabel('Release year', { exact: true }).locator('option'),
  ).toHaveCount(3);
}

async function staysAtControls(page: Page, action: () => Promise<unknown>) {
  await page.evaluate(() => {
    const controls = document.querySelector('.catalog-filters')!;
    window.scrollTo(
      0,
      controls.getBoundingClientRect().top + window.scrollY - 40,
    );
  });
  const before = await page.evaluate(() => window.scrollY);
  expect(before).toBeGreaterThan(200);
  const response = page.waitForResponse(
    (reply) =>
      reply.url().endsWith('/graphql') &&
      reply.request().postDataJSON()?.query.includes('query Titles'),
  );
  await action();
  await response;
  await expect(
    page.getByText('Finding titles for the current criteria…'),
  ).toBeHidden();
  await expect
    .poll(() => page.evaluate(() => window.scrollY))
    .toBeCloseTo(before, 0);
}

test('review filters: type, genre, year and month controls keep the scroll position through loading and empty results', async ({
  page,
}) => {
  await seedFilters(page);
  for (const label of ['Movies', 'TV series', 'All titles']) {
    await staysAtControls(page, () =>
      page.getByLabel(label, { exact: true }).check(),
    );
  }
  await page.getByText('Choose genres', { exact: false }).click();
  await staysAtControls(page, () =>
    page.getByLabel('Drama', { exact: true }).check(),
  );
  await staysAtControls(page, () =>
    page.getByRole('button', { name: 'Remove Drama filter' }).click(),
  );
  const year = page.getByLabel('Release year', { exact: true });
  const month = page.getByLabel('Month', { exact: true });
  await staysAtControls(page, () => year.selectOption('2006'));
  await staysAtControls(page, () => month.selectOption('3'));
  await expect(month).toHaveValue('3');
  await expect(page).toHaveURL(/month=3/);
  await staysAtControls(page, () => month.selectOption(''));
  await staysAtControls(page, () => year.selectOption(''));
});

test('review filters: only available genres, release years and months are offered; empty combinations remain supported', async ({
  page,
}) => {
  await seedFilters(page);
  const year = page.getByLabel('Release year', { exact: true });
  const month = page.getByLabel('Month', { exact: true });
  await expect(year.locator('option')).toHaveText([
    'All years',
    '2006',
    '1999',
  ]);
  await expect(month).toBeDisabled();
  await page.getByText('Choose genres', { exact: false }).click();
  await expect(page.locator('.genre-options label')).toHaveText([
    'Drama',
    'Comedy',
  ]);
  await year.selectOption('2006');
  await expect(month.locator('option')).toHaveText([
    'All months',
    'January',
    'March',
  ]);
  await month.selectOption('3');
  await year.selectOption('1999');
  await expect(month).toHaveValue('');
  await expect(month.locator('option')).toHaveText(['All months']);
  await page.getByLabel('Drama', { exact: true }).check();
  await expect(
    page.getByText('No matching titles — try changing your filters.'),
  ).toBeVisible();
  await page.getByRole('button', { name: 'Remove Drama filter' }).click();
  await expect(page.locator('.title-grid > li')).toHaveCount(24);
});

test('review filters: unavailable dates from a saved URL show no matches and can be cleared', async ({
  page,
}) => {
  await seedFilters(page);
  await page.goto('/?year=1800&month=12');
  const year = page.getByLabel('Release year', { exact: true });
  const month = page.getByLabel('Month', { exact: true });
  await expect(
    page.getByText('No matching titles — try changing your filters.'),
  ).toBeVisible();
  await expect(year).toHaveValue('1800');
  await expect(year.locator('option:checked')).toBeDisabled();
  await expect(month).toHaveValue('12');
  await expect(month.locator('option:checked')).toBeDisabled();
  await year.selectOption('');
  await expect(month).toHaveValue('');
  await expect(month).toBeDisabled();
});
