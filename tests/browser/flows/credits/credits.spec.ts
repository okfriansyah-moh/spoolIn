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
  const creditsLink = page.getByRole('link', { name: 'Cast and creators', exact: true }).first();
  await expect(creditsLink).toBeVisible();
  await creditsLink.click();
  await expect(page).toHaveURL(/\/manage\/movies\/[^?]+\?.*step=credits/);
  await expect(
    page.getByRole('heading', { level: 2, name: 'Cast and creators', exact: true }),
  ).toBeVisible();
  await expect(page.getByText('No credits yet for this movie.', { exact: true })).toBeVisible();
  await expect(page.getByText('Credits could not be loaded. Reload before making changes.', { exact: true })).toHaveCount(0);
});
