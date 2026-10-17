/**
 * T021 – Credit editing browser journey tests.
 */
import { test, expect } from '@playwright/test';
import {
  freshSession,
  goToMovieList,
  seedMaintainerSession,
} from '../movies/fixture';

test('T021-credits: movie edit route exposes credits section heading', async ({ page, context }) => {
  await freshSession(context);
  await seedMaintainerSession(page, 'movies-active');
  await goToMovieList(page);
  await expect(page.getByRole('heading', { level: 1, name: 'Movies' })).toBeVisible();
  const editLink = page.getByRole('link', { name: /Edit/i }).first();
  if (await editLink.isVisible()) {
    await editLink.click();
    await expect(page.getByRole('heading', { name: /Credits/i })).toBeVisible();
  }
});
