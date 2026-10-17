import { test, expect } from '@playwright/test';
import { freshSession, goToArchives } from './fixture';

test('T025-archives: maintainer archive page exposes tabs and cancel-safe confirmation', async ({
  page,
  context,
}) => {
  await freshSession(context);
  await goToArchives(page);
  await expect(page.getByRole('heading', { level: 1, name: 'Archives' })).toBeVisible();
  await expect(page.getByRole('tab', { name: 'Movies' })).toBeVisible();
  await expect(page.getByRole('tab', { name: 'People' })).toBeVisible();
  await page.getByRole('button', { name: 'Review permanent deletion' }).click();
  await expect(page.getByRole('heading', { level: 2, name: 'Confirm permanent deletion' })).toBeVisible();
  await page.getByRole('button', { name: 'Cancel' }).click();
  await expect(page.getByRole('heading', { level: 2, name: 'Confirm permanent deletion' })).toBeHidden();
});
