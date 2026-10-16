import { test, expect, credentials } from './fixture';
import type { Page, APIResponse } from '@playwright/test';
async function viewer(page: Page) {
  const input = credentials();
  await page.goto('/sign-in');
  await page.getByLabel(/^Email\b/).fill(input.viewerEmail);
  await page.getByLabel(/^Password\b/).fill(input.password);
  await page.getByRole('button', { name: 'Sign in', exact: true }).click();
  await expect(page.getByText('Browser viewer · Viewer')).toBeVisible();
}
async function confirmSignOut(page: Page) {
  await page.getByRole('button', { name: 'Sign out', exact: true }).click();
  await page
    .getByRole('dialog', { name: 'Sign out?' })
    .getByRole('button', { name: 'Sign out', exact: true })
    .click();
}
async function assertCommittedSignOut(response: APIResponse) {
  expect(response.status()).toBe(200);
  expect(
    (await response.json())?.data?.signOut?.operation?.state === 'SUCCEEDED',
  ).toBe(true);
}
async function assertNoSessionCookie(page: Page) {
  expect(
    (await page.context().cookies()).some(
      (cookie) => cookie.name === '__Host-spoolin_session',
    ),
  ).toBe(false);
}
test.beforeEach(async ({ context }) => {
  await context.clearCookies();
});
for (const lostBodyOnly of [false, true]) {
  test(`controlled sign-out response loss with deletion applied=${lostBodyOnly}`, async ({
    page,
  }) => {
    await viewer(page);
    let calls = 0;
    const originalCookies = await page.context().cookies();
    await page.route('**/graphql', async (route) => {
      const body = route.request().postDataJSON();
      if (
        typeof body?.query === 'string' &&
        body.query.includes('mutation AccountSignOut')
      ) {
        calls++;
        const response = await route.fetch();
        await assertCommittedSignOut(response);
        if (lostBodyOnly) await route.fulfill({ response, body: '{' });
        else {
          // Driver fetch applies cookies; restore the pre-response jar to model missing headers.
          await page.context().addCookies(originalCookies);
          await route.abort('failed');
        }
      } else await route.continue();
    });
    await confirmSignOut(page);
    await expect(
      page.getByRole('button', { name: 'Check sign-out', exact: true }),
    ).toBeVisible();
    await expect(
      page
        .getByRole('status')
        .filter({ hasText: 'Sign-out is not confirmed.' }),
    ).toBeVisible();
    await expect(page.locator('[data-private]')).toHaveCount(0);
    await page
      .getByRole('button', { name: 'Check sign-out', exact: true })
      .click();
    if (lostBodyOnly) {
      await expect(
        page.getByRole('button', { name: 'Check sign-out', exact: true }),
      ).toBeVisible();
      await expect(
        page.getByRole('status').filter({ hasText: /^Signed out\.$/ }),
      ).toHaveCount(0);
    } else
      await expect(
        page.getByRole('status').filter({ hasText: /^Signed out\.$/ }),
      ).toBeVisible();
    expect(calls).toBe(1);
    await assertNoSessionCookie(page);
  });
}
test('controlled identity outage conceals an existing account without claiming an anonymous session', async ({
  page,
}) => {
  await viewer(page);
  await page.route('**/graphql', (route) =>
    route.fulfill({
      status: 503,
      contentType: 'application/graphql-response+json',
      body: '{"errors":[{"message":"Unavailable","extensions":{"code":"UNAVAILABLE"}}]}',
    }),
  );
  await page.goto('/sign-in');
  await expect(
    page.getByRole('heading', {
      name: 'Sign-in is temporarily unavailable',
      exact: true,
    }),
  ).toBeVisible();
  await expect(page.locator('[data-private]')).toHaveCount(0);
  await expect(
    page.getByRole('form', { name: 'Sign in', exact: true }),
  ).toHaveCount(0);
  await expect(
    page.getByRole('link', { name: 'Browse catalog', exact: true }),
  ).toBeVisible();
});
test('controlled pagehide conceals identity before a later current check restores it', async ({
  page,
}) => {
  await viewer(page);
  await page.evaluate(() =>
    window.dispatchEvent(
      new PageTransitionEvent('pagehide', { persisted: true }),
    ),
  );
  await expect(page.locator('[data-private]')).toHaveCount(0);
  await page.evaluate(() =>
    window.dispatchEvent(
      new PageTransitionEvent('pageshow', { persisted: true }),
    ),
  );
  await expect(page.getByText('Browser viewer · Viewer')).toBeVisible();
});
