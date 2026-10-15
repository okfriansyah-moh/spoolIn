import { test, expect } from '@playwright/test';
import {
  assertTextSinks,
  assertDeniedUrls,
  setup,
  capture,
  assertNoOverflow,
  assertGeometry,
  recordEnvironment,
  reference,
  stressReading,
  inspectFocus,
  touchTheme,
  checkNavigation,
  inspectArtworkAndMotion,
  externalReferences,
  assertValidation,
} from './helpers';

test('T004-A1/A3 adopted themes, full identity, fields, variants and responsive geometry', async ({
  page,
}, info) => {
  await setup(page);
  await expect(page.locator('html')).toHaveAttribute(
    'data-theme',
    info.project.use.colorScheme ?? 'dark',
  );
  await assertNoOverflow(page);
  await assertGeometry(page);
  await inspectArtworkAndMotion(page, info);
  await capture(page, info, 'specimens');
  await recordEnvironment(page, info);
  await touchTheme(page, info);
  await inspectFocus(page);
  await stressReading(page, info);
  await expect(page.locator('#artwork')).toContainText('Artwork unavailable');
  await expect(page.locator('#artwork h3')).toHaveText(
    'The Space Between Every Place We Have Ever Called Home · 李',
  );
  await expect(page.locator('#metadata')).toContainText('Unknown');
  await expect(page.locator('#session')).not.toContainText(
    'Private content must be concealed',
  );
  await assertValidation(page, info);
  await page.locator('body').evaluate((element) => (element.style.zoom = '2'));
  await assertNoOverflow(page);
  await capture(page, info, 'zoom-200');
  await page.locator('body').evaluate((element) => (element.style.zoom = '1'));
  await checkNavigation(page);
  await reference(page, info);
  await externalReferences(page, info);
});

test('T004-A1/A2 theme preserves input, filter state, route and keyboard focus', async ({
  page,
}, info) => {
  await setup(page);
  await page.getByLabel('Search titles', { exact: true }).fill('李 · Railway');
  await page.getByLabel('Release year', { exact: true }).selectOption('2025');
  await page.getByLabel('Month', { exact: true }).selectOption('10');
  await page.locator('#specimen-title').fill('Retained title');
  const before = await page.locator('output').textContent();
  const url = page.url();
  const themeButton = page.getByRole('button', { name: /Switch to .* theme/ });
  await themeButton.focus();
  await page.keyboard.press('Enter');
  await expect(themeButton).toBeFocused();
  await expect(page.locator('output')).toHaveText(before ?? '');
  expect(page.url()).toBe(url);
  await expect(page.locator('#specimen-title')).toHaveValue('Retained title');
  expect(await page.evaluate(() => Object.keys(localStorage))).toEqual([
    'spoolin.theme',
  ]);
  await page.getByRole('button', { name: 'Clear search text' }).click();
  await expect(page.getByLabel('Release year', { exact: true })).toHaveValue(
    '2025',
  );
  await page.getByLabel('Release year', { exact: true }).selectOption('');
  await expect(page.getByLabel('Month', { exact: true })).toBeDisabled();
  await expect(page.getByLabel('Month', { exact: true })).toHaveValue('');
  await capture(page, info, 'theme-retained');
});

test('T004-A2 dialog Cancel first, keyboard trap, Escape and opener return', async ({
  page,
}, info) => {
  await setup(page);
  const opener = page.getByRole('button', {
    name: 'Open irreversible confirmation',
  });
  await opener.focus();
  await page.keyboard.press('Enter');
  const dialog = page.getByRole('dialog');
  await expect(dialog).toBeVisible();
  const cancel = dialog.getByRole('button', { name: 'Cancel' });
  await expect(cancel).toBeFocused();
  await page.keyboard.press('Shift+Tab');
  await expect(
    dialog.getByRole('button', { name: 'Permanently delete movie' }),
  ).toBeFocused();
  await page.keyboard.press('Tab');
  await expect(cancel).toBeFocused();
  await capture(page, info, 'dialog');
  await page.keyboard.press('Escape');
  await expect(dialog).not.toBeVisible();
  await expect(opener).toBeFocused();
  await expect(page.getByText('Confirmed actions: 0')).toBeVisible();
  await opener.click();
  await cancel.click();
  await expect(opener).toBeFocused();
  await expect(page.getByText('Confirmed actions: 0')).toBeVisible();
  await opener.click();
  await dialog
    .getByRole('button', { name: 'Permanently delete movie' })
    .click();
  await expect(page.getByText('Confirmed actions: 1')).toBeVisible();
});

test('ATDD-321 exact text nodes, URL attributes, event absence and controlled navigation', async ({
  page,
}, info) => {
  await setup(page);
  await assertTextSinks(page);
  await assertDeniedUrls(page);
  expect(
    await page.evaluate(() => Reflect.get(window, '__injected')),
  ).toBeUndefined();
  for (const url of [
    'http://example.test/controlled',
    'https://example.test/controlled',
  ]) {
    await page.route(url, (route) =>
      route.fulfill({ status: 200, body: 'Controlled destination' }),
    );
    await page.locator('#source-url').fill(url);
    await expect(page.getByTestId('safe-link').locator('a')).toHaveAttribute(
      'href',
      url,
    );
    await page.getByTestId('safe-link').locator('a').click();
    await expect(page).toHaveURL(url);
    await expect(page.locator('body')).toHaveText('Controlled destination');
    await setup(page);
  }
  await capture(page, info, 'safe-sinks');
});
