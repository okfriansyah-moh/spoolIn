import { test, expect } from '@playwright/test';
import { freshSession, goToMovieList } from '../movies/fixture';

test('T026-artwork: edit route exposes artwork upload status region', async ({ page, context }) => {
  await freshSession(context);
  await goToMovieList(page);
  await expect(page.getByRole('heading', { level: 1, name: 'Movies' })).toBeVisible();
  const editLink = page.getByRole('link', { name: /Edit/i }).first();
  if (await editLink.isVisible()) {
    await editLink.click();
    await expect(page.getByText('Poster artwork')).toBeVisible();
    await expect(page.getByRole('status')).toContainText(/Select artwork|Transferring|Processing|Saved/i);
  }
});
