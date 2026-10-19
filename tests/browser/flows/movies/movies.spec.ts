/**
 * T017 – Movie management browser journey tests.
 * Structural shell assertions for management list/create/edit states.
 */
import { test, expect } from '@playwright/test';
import {
  freshSession,
  goToCreateMovie,
  goToMovieList,
  seedMaintainerSession,
} from './fixture';

test('T017-movies: management list shows create action and TV read-only notice', async ({
  page,
  context,
}) => {
  await freshSession(context);
  await seedMaintainerSession(page, 'movies-active');
  await goToMovieList(page);
  await expect(
    page.getByRole('heading', { level: 1, name: 'Movies' }),
  ).toBeVisible();
  await expect(page.getByRole('link', { name: 'Create movie' })).toBeVisible();
  await expect(page.getByText(/TV entries are read-only/i)).toBeVisible();
});

test('T017-create: movie details stay focused and optional fields are disclosed', async ({
  page,
  context,
}) => {
  await freshSession(context);
  await seedMaintainerSession(page, 'movies-active');
  await goToCreateMovie(page);
  await expect(
    page.getByRole('heading', { level: 1, name: 'Create movie' }),
  ).toBeVisible();
  await expect(
    page.getByRole('textbox', { name: 'Title · Required' }),
  ).toBeVisible();
  await expect(
    page.getByRole('textbox', { name: 'Synopsis · Required' }),
  ).toBeVisible();
  await expect(
    page.getByRole('textbox', { name: 'Release year · Required' }),
  ).toBeVisible();
  await expect(page.getByRole('group', { name: /Genres/ })).toBeVisible();
  const genreDisclosure = page.locator('.genre-disclosure');
  await expect(genreDisclosure).not.toHaveAttribute('open', '');
  await expect(page.getByLabel('Original title Optional')).toBeHidden();
  await expect(
    page.getByRole('button', { name: 'Create movie' }),
  ).toBeVisible();
  await page.locator('#movie-genres summary').click();
  await expect(
    page.getByRole('searchbox', { name: 'Find a genre' }),
  ).toBeVisible();
  await expect(page.getByRole('checkbox', { name: 'Drama' })).toBeVisible();
  await page.getByRole('checkbox', { name: 'Drama' }).check();
  await expect(page.getByText('Drama', { exact: true }).last()).toBeVisible();
  await expect(
    page.getByRole('button', { name: 'Remove genre Drama' }),
  ).toBeVisible();
  await page.getByRole('button', { name: 'Remove genre Drama' }).click();
  await expect(
    page.getByRole('button', { name: 'Remove genre Drama' }),
  ).toHaveCount(0);
  await page.locator('.optional-details > summary').click();
  await expect(
    page.getByRole('textbox', { name: 'Original title Optional' }),
  ).toBeVisible();
});
