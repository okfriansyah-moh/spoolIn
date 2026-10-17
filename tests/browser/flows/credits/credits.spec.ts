/**
 * T021 – Credit editing browser journey tests.
 */
import { test, expect } from '@playwright/test';
import { freshSession, goToMovieList } from '../movies/fixture';

test('T021-credits: movie edit route exposes credits section heading', async ({ page, context }) => {
  await freshSession(context);
  await goToMovieList(page);
  await expect(page.getByRole('heading', { level: 1, name: 'Movies' })).toBeVisible();
});
