import { test, expect, credentials } from './fixture';
import type { Page } from '@playwright/test';
async function enterRegistration(page: Page, name: string, password: string) {
  const email = `row-${crypto.randomUUID()}@example.test`;
  await page.goto('/register');
  await page.getByLabel(/^Name\b/).fill(name);
  await page.getByLabel(/^Email\b/).fill(email);
  await page.getByLabel(/^Password\b/).fill(password);
  await page
    .getByRole('button', { name: 'Create Viewer account', exact: true })
    .click();
  return email;
}
async function verifyExactSignIn(page: Page, email: string, password: string) {
  await page.goto('/sign-in');
  await page.getByLabel(/^Email\b/).fill(email);
  await page.getByLabel(/^Password\b/).fill(password);
  await page.getByRole('button', { name: 'Sign in', exact: true }).click();
  await expect(page.locator('.session-menu [data-private]')).toContainText(
    'Viewer',
  );
}
test.beforeEach(async ({ context }) => {
  await context.clearCookies();
});
for (const length of [1, 2, 100, 101]) {
  test(`registration name boundary ${length}`, async ({ page }) => {
    const password = credentials().password;
    const email = await enterRegistration(page, 'x'.repeat(length), password);
    if (length === 2 || length === 100) {
      await expect(
        page.getByRole('heading', { name: 'Your Viewer account is ready' }),
      ).toBeVisible();
      await verifyExactSignIn(page, email, password);
    } else {
      await expect(page.getByRole('alert')).toContainText(
        'Name: Use 2 to 100 Unicode code points.',
      );
      expect((await page.getByLabel(/^Password\b/).inputValue()) === '').toBe(
        true,
      );
      await expect(page.getByLabel(/^Name\b/)).toHaveValue('x'.repeat(length));
    }
  });
}
const passwordRows = [
  { label: 'below minimum', value: 'x'.repeat(14), valid: false },
  { label: 'minimum', value: 'x'.repeat(15), valid: true },
  { label: 'maximum', value: 'x'.repeat(128), valid: true },
  { label: 'above maximum', value: 'x'.repeat(129), valid: false },
  { label: 'exact whitespace', value: ' '.repeat(15), valid: true },
  { label: 'composed text', value: 'é'.repeat(15), valid: true },
  { label: 'decomposed text', value: 'e\u0301'.repeat(8), valid: true },
  { label: 'maximum UTF8 bytes', value: '🦉'.repeat(128), valid: true },
];
for (const row of passwordRows) {
  test(`new password ${row.label}`, async ({ page }) => {
    const email = await enterRegistration(page, 'Li', row.value);
    if (row.valid) {
      await expect(
        page.getByRole('heading', { name: 'Your Viewer account is ready' }),
      ).toBeVisible();
      await verifyExactSignIn(page, email, row.value);
    } else {
      await expect(page.getByRole('alert')).toContainText(
        'Password: Use 15 to 128 Unicode code points.',
      );
      expect((await page.getByLabel(/^Password\b/).inputValue()) === '').toBe(
        true,
      );
    }
  });
}
for (const kind of [
  'wrong password',
  'unknown email',
  'invalid syntax',
] as const) {
  test(`sign-in safely rejects ${kind}`, async ({ page }) => {
    const input = credentials();
    await page.goto('/sign-in');
    const email =
      kind === 'unknown email'
        ? `missing-${crypto.randomUUID()}@example.test`
        : kind === 'invalid syntax'
          ? 'invalid'
          : input.viewerEmail;
    await page.getByLabel(/^Email\b/).fill(email);
    await page.getByLabel(/^Password\b/).fill(input.password + '!');
    await page.getByRole('button', { name: 'Sign in', exact: true }).click();
    await expect(page.getByRole('alert')).toBeVisible();
    await expect(page.locator('.session-menu [data-private]')).toHaveCount(0);
    expect((await page.getByLabel(/^Password\b/).inputValue()) === '').toBe(
      true,
    );
    if (kind !== 'invalid syntax')
      await expect(page.getByRole('alert')).toContainText(
        'Sign-in did not complete. Check your email and password.',
      );
  });
}
test('duplicate normalized email retains only permitted correction values', async ({
  page,
}) => {
  const input = credentials();
  await page.goto('/register');
  await page.getByLabel(/^Name\b/).fill('Retained name');
  await page.getByLabel(/^Email\b/).fill(input.viewerEmail.toUpperCase());
  await page.getByLabel(/^Password\b/).fill(input.password);
  await page
    .getByRole('button', { name: 'Create Viewer account', exact: true })
    .click();
  await expect(page.getByRole('alert')).toContainText(
    'The account could not be created. Check your details or sign in.',
  );
  await expect(page.getByLabel(/^Name\b/)).toHaveValue('Retained name');
  expect((await page.getByLabel(/^Password\b/).inputValue()) === '').toBe(true);
  await expect(
    page.getByRole('heading', { name: 'Your Viewer account is ready' }),
  ).toHaveCount(0);
});
