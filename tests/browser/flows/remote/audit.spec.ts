import {
  test,
  expect,
  signInMaintainer,
  createMovie,
  capture,
} from './helpers';

test('ATDD-053/088/101 partial: actual movie mutation is readable in immutable audit projection', async ({
  page,
}, info) => {
  await signInMaintainer(page);
  const movie = await createMovie(page);
  await page.getByRole('link', { name: 'History', exact: true }).click();
  await expect(
    page.getByRole('heading', { name: 'Audit history', exact: true }),
  ).toBeVisible();
  await expect(
    page.getByRole('columnheader', { name: 'Actor', exact: true }),
  ).toBeVisible();
  await expect(
    page.getByRole('columnheader', { name: 'Outcome', exact: true }),
  ).toBeVisible();
  const row = page.getByRole('row').filter({ hasText: movie.id });
  for (let attempt = 0; attempt < 20 && (await row.count()) === 0; attempt++) {
    const more = page.getByRole('button', {
      name: 'Load more history',
      exact: true,
    });
    if ((await more.count()) === 0) break;
    await more.click();
    await expect(more).toBeEnabled();
  }
  await expect(row.first()).toBeVisible();
  await expect(row.first()).toContainText('SUCCEEDED');
  await row.first().getByText('Inspect changes', { exact: true }).click();
  await expect(row.first()).toContainText('After:');
  await expect(row.first()).toContainText(movie.title);
  await expect(row.first()).not.toContainText('No field changes recorded.');
  await expect(row.first()).not.toContainText('maintainer@example.invalid');
  await expect(
    page.getByRole('button', { name: /edit|delete|approve|reject/i }),
  ).toHaveCount(0);
  await capture(
    page,
    info,
    ['ATDD-053', 'ATDD-088', 'ATDD-101'],
    'audit-actual-mutation',
  );
  await page.reload();
  await expect(page.getByRole('table')).toBeVisible();
});
