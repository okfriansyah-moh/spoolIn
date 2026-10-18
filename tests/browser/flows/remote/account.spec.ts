import { randomBytes } from 'node:crypto';
import { test, expect, capture } from './helpers';

test('Registration assigns Viewer and real sign-in persists after reload', async ({ page }, info) => {
  const identity = `remote-${process.env.GITHUB_RUN_ID}-${randomBytes(4).toString('hex')}@example.test`;
  const password = randomBytes(24).toString('base64url');
  await page.goto('/register');
  await expect(page.getByRole('heading', { name: 'Create a Viewer account' })).toBeVisible();
  await page.getByLabel('Name', { exact: true }).fill('Remote Viewer');
  await page.getByLabel('Email', { exact: true }).fill(identity);
  await page.getByLabel('Password', { exact: true }).fill(password);
  await page.getByRole('button', { name: 'Create Viewer account' }).click();
  await expect(page.getByRole('heading', { name: 'Your Viewer account is ready' })).toBeVisible();
  await page.getByRole('link', { name: 'Sign in', exact: true }).last().click();
  await page.getByLabel('Email', { exact: true }).fill(identity);
  await page.getByLabel('Password', { exact: true }).fill(password);
  await page.getByRole('button', { name: 'Sign in', exact: true }).click();
  await expect(page.getByRole('list', { name: 'Titles' })).toBeVisible();
  await page.reload();
  const response = await page.request.post('/graphql', {
    headers: { 'X-Spoolin-Client': 'web' },
    data: { query: 'query { session { authenticated role } }' },
  });
  expect(response.ok()).toBe(true);
  expect(await response.json()).toEqual({ data: { session: { authenticated: true, role: 'VIEWER' } } });
  await capture(page, info, ['ATDD-047', 'ATDD-085'], 'signed-in-catalog');
  await page.getByRole('button', { name: 'Sign out', exact: true }).click();
  await page.getByRole('dialog').getByRole('button', { name: 'Sign out', exact: true }).click();
  await expect(page.getByRole('link', { name: 'Sign in', exact: true })).toBeVisible();
  await page.reload();
  await expect(page.getByRole('link', { name: 'History', exact: true })).toHaveCount(0);
  await capture(page, info, ['ATDD-048'], 'signed-out');
});
