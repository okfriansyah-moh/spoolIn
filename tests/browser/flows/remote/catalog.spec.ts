import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { test, expect, capture, readApi } from './helpers';
import type { Page } from '@playwright/test';

test.use({ storageState: { cookies: [], origins: [] } });

const tokens = JSON.parse(
  readFileSync(resolve('../../docs/design-system/tokens.json'), 'utf8'),
).primitive;

test('Public catalog uses real GraphQL, seeded artwork and durable detail navigation', async ({
  page,
}, info) => {
  const apiFailures: string[] = [];
  page.on('response', async (response) => {
    if (new URL(response.url()).pathname !== '/graphql') return;
    if (!response.ok()) apiFailures.push(`HTTP ${response.status()}`);
    const body: unknown = await response.json().catch(() => null);
    if (body && typeof body === 'object' && 'errors' in body)
      apiFailures.push('GraphQL errors');
  });
  await test.step('Open the catalog and assert actual API results', async () => {
    await page.goto('/');
    await expect(
      page.getByRole('heading', { name: 'Every reel holds a story.' }),
    ).toBeVisible();
    await expect(page.getByRole('list', { name: 'Titles' })).toBeVisible();
    await expect(page.locator('.title-card')).not.toHaveCount(0);
    await expect(page.getByLabel('Month', { exact: true })).toBeDisabled();
    expect(apiFailures).toEqual([]);
  });
  await test.step('Keep every genre in a bounded region with native keyboard access', async () => {
    await assertGenreRegion(page);
  });
  const theme = info.project.use.colorScheme ?? 'dark';
  await expect(page.locator('html')).toHaveAttribute('data-theme', theme);
  const required = ['bg', 'text', 'accent', 'surface', 'line'];
  const values = await page
    .locator('html')
    .evaluate(
      (root, names) =>
        Object.fromEntries(
          names.map((name) => [
            name,
            getComputedStyle(root).getPropertyValue(`--${name}`).trim(),
          ]),
        ),
      required,
    );
  for (const name of required)
    expect(values[name].toLowerCase()).toBe(tokens[`${theme}-${name}`]);
  await page.keyboard.press('Tab');
  const focused = page.locator(':focus-visible');
  await expect(focused).toHaveCount(1);
  expect(
    await focused.evaluate((element) => getComputedStyle(element).outlineStyle),
  ).not.toBe('none');
  expect(
    await page
      .locator('html')
      .evaluate((root) => root.scrollWidth <= window.innerWidth),
  ).toBe(true);
  await capture(
    page,
    info,
    ['ATDD-001', 'ATDD-268', 'ATDD-270', 'ATDD-273'],
    'catalog',
  );
  await test.step('Follow a visible title and reload the production detail route', async () => {
    const link = page.locator('.title-card h3 a').first();
    const title = await link.innerText();
    await link.click();
    await expect(page.getByRole('heading', { level: 1 })).toHaveText(title);
    await page.reload();
    await expect(page.getByRole('heading', { level: 1 })).toHaveText(title);
    expect(apiFailures).toEqual([]);
  });
  await page.locator('img').evaluateAll((images) =>
    Promise.all(
      images.map((image) => {
        if (!(image instanceof HTMLImageElement))
          throw new Error('Artwork element is not an image.');
        return image.decode();
      }),
    ),
  );
  const broken = await page.locator('img').evaluateAll((images) =>
    images
      .filter((image) => {
        if (!(image instanceof HTMLImageElement))
          throw new Error('Artwork element is not an image.');
        return !image.complete || image.naturalWidth === 0;
      })
      .map((image) => image.getAttribute('src')),
  );
  expect(broken).toEqual([]);
  await capture(page, info, ['ATDD-001', 'ATDD-257'], 'detail');
});

test('ATDD-006/007/014 partial: visible search and filter changes produce actual results', async ({
  page,
}, info) => {
  await page.goto('/');
  const title = await page.locator('.title-card h3 a').first().innerText();
  await page.getByLabel('Search titles', { exact: true }).fill(title);
  await expect(
    page.locator('.title-card h3 a').filter({ hasText: title }),
  ).toBeVisible();
  await page
    .getByLabel('Search titles', { exact: true })
    .fill('zzzz-nonexistent-remote-title-271828');
  await expect(
    page.getByText('No matching titles — try changing your filters.', {
      exact: true,
    }),
  ).toBeVisible();
  await capture(page, info, ['ATDD-007'], 'no-match');
  await page
    .getByRole('button', { name: 'Clear search text', exact: true })
    .click();
  await expect(page.locator('.title-card')).not.toHaveCount(0);
  await page
    .getByLabel('Release year', { exact: true })
    .selectOption({ index: 1 });
  await expect(page.getByLabel('Month', { exact: true })).toBeEnabled();
  await page.getByLabel('Month', { exact: true }).selectOption('10');
  await page.getByLabel('Release year', { exact: true }).selectOption('');
  await expect(page.getByLabel('Month', { exact: true })).toBeDisabled();
  await expect(page.getByLabel('Month', { exact: true })).toHaveValue('');
  await capture(page, info, ['ATDD-006', 'ATDD-014'], 'filters-cleared');
});

test('ATDD-001 partial: actual catalog cursor pages survive reload and browser Back', async ({
  page,
}, info) => {
  await page.goto('/');
  await expect(page.getByRole('list', { name: 'Titles' })).toBeVisible();
  const first = await page
    .locator('.title-card h3 a')
    .evaluateAll((links) => links.map((link) => link.getAttribute('href')));
  const response = page.waitForResponse((item) => {
    if (new URL(item.url()).pathname !== '/graphql') return false;
    const input = item.request().postDataJSON();
    return (
      input?.query?.includes('query Titles(') &&
      typeof input?.variables?.after === 'string'
    );
  });
  await page
    .getByRole('button', { name: 'Next titles page', exact: true })
    .click();
  const actual = await response;
  expect(actual.ok()).toBe(true);
  const body = await actual.json();
  expect(body.errors).toBeUndefined();
  expect(body.data.titles.nodes.length).toBeGreaterThan(0);
  const nextId = String(body.data.titles.nodes[0].id);
  await expect(
    page.locator(`.title-card h3 a[href$="/${nextId}"]`),
  ).toBeVisible();
  const second = await page
    .locator('.title-card h3 a')
    .evaluateAll((links) => links.map((link) => link.getAttribute('href')));
  expect(second.every((href) => !first.includes(href))).toBe(true);
  await capture(page, info, ['ATDD-001'], 'catalog-next-page');
  await page.reload();
  await expect(
    page.locator(`.title-card h3 a[href$="/${nextId}"]`),
  ).toBeVisible();
  await page.goBack();
  await expect(page.locator('.title-card h3 a').first()).toHaveAttribute(
    'href',
    first[0]!,
  );
  expect(
    await page
      .locator('.title-card h3 a')
      .evaluateAll((links) => links.map((link) => link.getAttribute('href'))),
  ).toEqual(first);
  await capture(page, info, ['ATDD-001'], 'catalog-cursor-history-restored');
});

test('ATDD-011/012 partial: an older known release year is selectable and invalid correction does not silently change it', async ({
  page,
}, info) => {
  await page.goto('/');
  await expect(page.getByRole('list', { name: 'Titles' })).toBeVisible();
  await page.getByLabel('Release year', { exact: true }).selectOption('other');
  await page.getByLabel('Other release year', { exact: true }).fill('1999');
  await expect(page).toHaveURL(/[?&]year=1999(?:&|$)/);
  await expect(page.getByLabel('Month', { exact: true })).toBeEnabled();
  await page.getByLabel('Other release year', { exact: true }).fill('invalid');
  await expect(
    page.getByLabel('Other release year', { exact: true }),
  ).toHaveAttribute('aria-invalid', 'true');
  await expect(
    page.getByText(
      'Enter a release year from 1 to 9999. The current year filter stays unchanged.',
      { exact: true },
    ),
  ).toBeVisible();
  await expect(page).toHaveURL(/[?&]year=1999(?:&|$)/);
  await capture(page, info, ['ATDD-011', 'ATDD-012'], 'older-year-correction');
  await page.getByLabel('Release year', { exact: true }).selectOption('');
  await expect(page.getByLabel('Month', { exact: true })).toBeDisabled();
  await expect(
    page.getByLabel('Other release year', { exact: true }),
  ).toHaveCount(0);
});

async function assertGenreRegion(page: Page) {
  const group = page.getByRole('group', {
    name: 'Genres · match any selected',
    exact: true,
  });
  const region = group.getByRole('region', {
    name: 'Genre choices',
    exact: true,
  });
  const disclosure = group.locator('details');
  await expect(disclosure).not.toHaveAttribute('open', '');
  await group.locator('summary').click();
  await expect(disclosure).toHaveAttribute('open', '');
  const choices = region.locator('input[type="checkbox"]');
  const data = await readApi(page, 'query { genres { id } }');
  expect(Array.isArray(data.genres)).toBe(true);
  expect(data.genres.length).toBeGreaterThan(0);
  await expect(choices).toHaveCount(data.genres.length);
  expect(
    await choices.evaluateAll((inputs) =>
      inputs.map((input) => (input as HTMLInputElement).value).sort(),
    ),
  ).toEqual(data.genres.map((genre: { id: string }) => genre.id).sort());
  await expect(region).toHaveCSS('overflow-y', 'auto');
  const geometry = await region.evaluate((element) => ({
    height: element.clientHeight,
    total: element.scrollHeight,
    width: element.getBoundingClientRect().width,
    viewport: window.innerWidth,
    viewportHeight: window.innerHeight,
  }));
  expect(geometry.height).toBeLessThanOrEqual(geometry.viewportHeight / 2);
  expect(geometry.width).toBeLessThanOrEqual(geometry.viewport);
  await region.focus();
  await expect(region).toBeFocused();
  if (geometry.total > geometry.height) {
    await page.keyboard.press('End');
    await expect
      .poll(() => region.evaluate((element) => element.scrollTop))
      .toBeGreaterThan(0);
    await page.keyboard.press('Home');
    await expect
      .poll(() => region.evaluate((element) => element.scrollTop))
      .toBe(0);
  }
  await page.keyboard.press('Tab');
  await expect(choices.first()).toBeFocused();
  await expect(choices.first()).not.toBeChecked();
  const search = group.getByRole('searchbox', { name: 'Find a genre' });
  await search.fill('not-a-real-genre');
  await expect(choices).toHaveCount(0);
  await expect(
    group.getByText('No genres match that search. Try another name.'),
  ).toBeVisible();
  await search.clear();
  await expect(choices).toHaveCount(data.genres.length);
  await group.locator('summary').click();
  await expect(disclosure).not.toHaveAttribute('open', '');
}
