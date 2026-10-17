/**
 * T017 – Movie management browser journey tests.
 * Structural shell assertions for management list/create/edit states.
 */
import { test, expect } from '@playwright/test';
import { freshSession, goToCreateMovie, goToMovieList } from './fixture';

test('T017-movies: management list shows create action and TV read-only notice', async ({
  page,
  context,
}) => {
  await freshSession(context);
  await goToMovieList(page);
  await expect(page.getByRole('heading', { level: 1, name: 'Movies' })).toBeVisible();
  await expect(page.getByRole('link', { name: 'Create movie' })).toBeVisible();
  await expect(page.getByText(/TV entries are read-only/i)).toBeVisible();
});

test('T017-create: create form exposes required movie fields and honest optional guidance', async ({
  page,
  context,
}) => {
  await freshSession(context);
  await goToCreateMovie(page);
  await expect(page.getByRole('heading', { level: 1, name: 'Create movie' })).toBeVisible();
  await expect(page.getByLabel(/Title/i)).toBeVisible();
  await expect(page.getByLabel(/Synopsis/i)).toBeVisible();
  await expect(page.getByLabel(/Release year/i)).toBeVisible();
  await expect(page.getByText(/Genres · at least one required/i)).toBeVisible();
  await expect(page.getByLabel(/Original title/i)).toBeVisible();
  await expect(page.getByRole('button', { name: 'Create movie' })).toBeVisible();
});
