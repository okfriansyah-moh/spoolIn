import { test, expect, credentials } from './fixture';
import type { Page } from '@playwright/test';
async function signIn(page: Page, email: string, password: string) {
  await page.goto('/sign-in');
  await expect(
    page.getByRole('form', { name: 'Sign in', exact: true }),
  ).toBeVisible();
  await page.getByLabel(/^Email\b/).fill(email);
  await page.getByLabel(/^Password\b/).fill(password);
  await page.getByRole('button', { name: 'Sign in', exact: true }).click();
}
async function signOut(page: Page) {
  await page.getByRole('button', { name: 'Sign out', exact: true }).click();
  const dialog = page.getByRole('dialog', { name: 'Sign out?' });
  await expect(dialog.getByRole('button', { name: 'Cancel' })).toBeFocused();
  await dialog.getByRole('button', { name: 'Sign out', exact: true }).click();
  await expect(
    page.getByRole('status').filter({ hasText: /^Signed out\.$/ }),
  ).toBeVisible();
}
async function assertPrivateStorage(page: Page, password: string) {
  const absent = await page.evaluate(
    (secret) =>
      !JSON.stringify({
        local: { ...localStorage },
        session: { ...sessionStorage },
        url: location.href,
      }).includes(secret),
    password,
  );
  expect(absent).toBe(true);
  const storage = await page.evaluate(() => {
    const descriptor = Object.getOwnPropertyDescriptor(
      window,
      '__accountStorageProof',
    );
    return (
      Array.isArray(descriptor?.value) &&
      descriptor.value.length > 0 &&
      descriptor.value.every((allowed: unknown) => allowed === true)
    );
  });
  expect(storage).toBe(true);
}
test.beforeEach(async ({ context }) => {
  await context.clearCookies();
});
test('actual registration assigns Viewer and keeps the password out of recovery storage', async ({
  page,
}) => {
  const input = credentials();
  await page.goto('/register');
  await expect(
    page.getByRole('form', { name: 'Create Viewer account' }),
  ).toBeVisible();
  await expect(page.getByLabel(/^Password\b/)).toHaveAttribute(
    'autocomplete',
    'new-password',
  );
  await page.getByLabel(/^Name\b/).fill('Browser registered');
  await page.getByLabel(/^Email\b/).fill(input.registrationEmail);
  await page.getByLabel(/^Password\b/).fill(input.password);
  await page
    .getByRole('button', { name: 'Create Viewer account', exact: true })
    .click();
  await expect(page.locator('#account-success')).toContainText(
    'account is ready',
  );
  await expect(
    page.getByText('Viewer is assigned to this account.', { exact: false }),
  ).toBeVisible();
  await assertPrivateStorage(page, input.password);
  await page.getByRole('link', { name: 'Sign in', exact: true }).last().click();
  await page.getByLabel(/^Email\b/).fill(input.registrationEmail);
  await page.getByLabel(/^Password\b/).fill(input.password);
  await page.getByRole('button', { name: 'Sign in', exact: true }).click();
  await expect(page.getByText('Browser registered · Viewer')).toBeVisible();
  await signOut(page);
});
test('real cookies rotate at sign-in and Viewer cannot reveal management', async ({
  page,
  context,
}) => {
  const input = credentials();
  await signIn(page, input.viewerEmail, input.password);
  await expect(page.getByText('Browser viewer · Viewer')).toBeVisible();
  const cookies = await context.cookies();
  const session = cookies.find(
    (cookie) => cookie.name === '__Host-spoolin_session',
  );
  expect(
    Boolean(
      session &&
      session.secure &&
      session.httpOnly &&
      session.sameSite === 'Lax' &&
      session.path === '/' &&
      session.expires === -1,
    ),
  ).toBe(true);
  expect(
    await page.evaluate(() =>
      document.cookie.includes('__Host-spoolin_session'),
    ),
  ).toBe(false);
  await page.goto('/manage');
  await expect(
    page.getByText('You do not have access', { exact: true }),
  ).toBeVisible();
  await expect(page.locator('[data-private] section')).toHaveCount(0);
  await signOut(page);
  expect(
    (await context.cookies()).some(
      (cookie) => cookie.name === '__Host-spoolin_session',
    ),
  ).toBe(false);
});
test('verified Maintainer enters only the local management shell', async ({
  page,
}) => {
  const input = credentials();
  await signIn(page, input.maintainerEmail, input.password);
  await expect(page.getByText('Browser maintainer · Maintainer')).toBeVisible();
  await page.goto('/manage');
  await expect(
    page.getByRole('heading', { name: 'Management', exact: true }),
  ).toBeVisible();
  await signOut(page);
  await expect(page.locator('[data-private]')).toHaveCount(0);
});
