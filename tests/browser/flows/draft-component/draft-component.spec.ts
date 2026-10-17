import { test, expect } from '@playwright/test';

const catalogSubmission = '30000000-0000-4000-8000-000000000003';

test('autosave waits 750 ms and surfaces unbacked changes when save fails', async ({
  page,
}) => {
  await page.goto('/draft?mode=save-fail');
  await expect(page.getByRole('form', { name: 'Person draft' })).toBeVisible();
  const field = page.getByLabel(/^Name\b/);
  await field.fill('O’Connor');
  await page.waitForTimeout(749);
  await expect(page.getByText('Saving draft')).toHaveCount(0);
  await expect(
    page.getByRole('alert').filter({ hasText: 'not backed up' }),
  ).toBeVisible({ timeout: 5000 });
  await expect(field).toHaveValue('O’Connor');
  const storage = await page.evaluate(() =>
    JSON.stringify({
      local: { ...localStorage },
      session: { ...sessionStorage },
    }),
  );
  expect(storage).not.toContain('O’Connor');
});

test('conceals draft values after account switch and keeps catalog identity separate', async ({
  page,
}) => {
  await page.goto('/draft?mode=account-switch&account=A');
  const field = page.getByLabel(/^Name\b/);
  await field.fill('Account A only');
  await page.waitForTimeout(750);
  await page.waitForFunction((catalogId) => {
    const log = (window as { __draftRequests?: string[] }).__draftRequests ?? [];
    return log.some((entry) => entry.includes(catalogId));
  }, catalogSubmission);
  await page.getByRole('button', { name: 'Account B' }).click();
  await expect(page.locator('#draft-concealed')).toContainText('another account');
  await expect(page.getByLabel(/^Name\b/)).toHaveCount(0);
});

test('recovery resolves Core operation before listing drafts', async ({
  page,
}) => {
  await page.goto('/draft?mode=pending-recover&account=A');
  await page.getByLabel(/^Name\b/).fill('Recover me');
  await page.waitForTimeout(750);
  await page.waitForFunction(() => {
    const log = (window as { __draftRequests?: string[] }).__draftRequests ?? [];
    return log.some((entry) => entry.startsWith('save:'));
  });
  await page.evaluate(async () => {
    const controller = (window as { __draftController?: { resumeAfterEligibility: (value: { permitted: boolean; message: string }) => Promise<void> } }).__draftController;
    await controller?.resumeAfterEligibility({ permitted: true, message: '' });
    (window as { __draftReleaseSave?: () => void }).__draftReleaseSave?.();
  });
  const requests = await page.evaluate(() => {
    return (window as { __draftRequests?: string[] }).__draftRequests ?? [];
  });
  const recoverIndex = requests.findIndex((entry) =>
    entry.startsWith('recover:'),
  );
  const draftsIndex = requests.indexOf('drafts');
  expect(recoverIndex).toBeGreaterThanOrEqual(0);
  expect(draftsIndex).toBeGreaterThan(recoverIndex);
});
