import { test, expect } from '@playwright/test';
import { freshSession, goToMovieList } from '../movies/fixture';

test('T026-artwork: movie management shell leaves room for measured upload feedback', async ({
  page,
  context,
}) => {
  await freshSession(context);
  await goToMovieList(page);
  await expect(page.getByRole('heading', { level: 1, name: 'Movies' })).toBeVisible();
  await expect(page.getByText(/TV entries are read-only/i)).toBeVisible();
});
