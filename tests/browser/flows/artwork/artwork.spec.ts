import { test, expect } from '@playwright/test';
import {
  FIXTURE_ACTIVE_MOVIE,
  freshSession,
  goToMovieEdit,
  goToMovieList,
  seedMaintainerSession,
} from '../movies/fixture';

test('T026-artwork: maintainer edit route exposes idle upload status stage', async ({
  page,
  context,
}) => {
  await freshSession(context);
  await seedMaintainerSession(page, 'movies-active');
  await goToMovieList(page);
  await expect(
    page.getByRole('heading', { level: 1, name: 'Movies' }),
  ).toBeVisible();
  const editLink = page.getByRole('link', { name: 'Edit', exact: true });
  await expect(editLink).toBeVisible();
  const editUrl = new URL(
    (await editLink.getAttribute('href')) ?? '',
    page.url(),
  );
  expect(editUrl.pathname).toBe(`/manage/movies/${FIXTURE_ACTIVE_MOVIE.id}`);
  expect(editUrl.searchParams.get('return')).toMatch(/^[0-9a-f-]{36}$/);
  await editLink.click();
  await expect(page.getByText('Poster artwork')).toBeVisible();
  const status = page.locator('.artwork-upload').getByRole('status');
  await expect(status).toBeVisible();
  await expect(status).toHaveText('Select artwork');
  await expect(
    page.locator('progress[aria-label="Transfer progress"]'),
  ).toHaveCount(0);
});

test('T026-artwork: seeded movie edit route keeps upload status region on direct navigation', async ({
  page,
  context,
}) => {
  await freshSession(context);
  await seedMaintainerSession(page, 'movies-active');
  await goToMovieEdit(page);
  await expect(page.getByRole('heading', { level: 1 })).toContainText(
    FIXTURE_ACTIVE_MOVIE.title,
  );
  await expect(page.locator('.artwork-upload').getByRole('status')).toHaveText(
    'Select artwork',
  );
});
