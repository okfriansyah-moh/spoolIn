import { test, expect } from '@playwright/test';
import { freshSession, goToArchives } from './fixture';

test('T025-archives: maintainer archive page exposes tabs and safe empty state', async ({
  page,
  context,
}) => {
  await freshSession(context);
  await goToArchives(page);
  await expect(page.getByRole('heading', { level: 1, name: 'Archives' })).toBeVisible();
  await expect(page.getByRole('tab', { name: 'Movies' })).toBeVisible();
  await expect(page.getByRole('tab', { name: 'People' })).toBeVisible();
  await page.getByRole('tab', { name: 'People' }).click();
  await expect(page.getByRole('tab', { name: 'People' })).toHaveAttribute('aria-selected', 'true');
  await expect(
    page.getByText(/No archived records|Loading archives|do not have access/i),
  ).toBeVisible();
});
