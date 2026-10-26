import { expect, test } from '@playwright/test';
import { freshSession, seedMaintainerArchives } from '../archives/fixture';

test.use({ viewport: { width: 1440, height: 900 } });

test('initial shell and client navigation retain exactly one page title', async ({ page, context }) => {
  const shell = await page.request.get('/');
  expect(shell.ok()).toBe(true);
  const html = await shell.text();
  expect(html.match(/<title(?:\s[^>]*)?>/g)).toHaveLength(1);
  expect(html).toContain('<title>SpoolIn</title>');
  await freshSession(context);
  await seedMaintainerArchives(page, 'archives-empty');
  await page.goto('/manage/archives');
  await expect(page).toHaveTitle('Archives · SpoolIn');
  await expect(page.locator('head title')).toHaveCount(1);
  await page.locator('a[href="/people"]:visible').first().click();
  await expect(page).toHaveURL(/\/people$/);
  await expect(page).toHaveTitle('People · SpoolIn');
  await expect(page.locator('head title')).toHaveCount(1);
  await page.goBack();
  await expect(page).toHaveTitle('Archives · SpoolIn');
  await expect(page.locator('head title')).toHaveCount(1);
});
