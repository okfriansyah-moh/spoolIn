import { test, expect, signInMaintainer, createMovie, createPerson, capture } from './helpers';

test('ATDD-016/017/023: movie and person writes survive public-route reload', async ({ page }, info) => {
  await signInMaintainer(page);
  const person = await createPerson(page);
  await page.getByLabel('Biography · Optional').fill('Revised biography with a retained shared identity.');
  await page.getByRole('button', { name: 'Save person', exact: true }).click();
  await expect(page.getByText('Person saved', { exact: true })).toBeVisible();
  await page.getByRole('link', { name: 'Review public record' }).click();
  await page.reload();
  await expect(page.getByRole('heading', { level: 1 })).toHaveText(person.name);
  await expect(page.getByText('Revised biography with a retained shared identity.')).toBeVisible();
  await capture(page, info, ['ATDD-023'], 'person-persisted');
  const movie = await createMovie(page);
  await page.getByLabel('Synopsis · Required').fill('Revised synopsis after a real API write.');
  await page.getByRole('button', { name: 'Save movie', exact: true }).click();
  await expect(page.getByText('Movie saved', { exact: true })).toBeVisible();
  await page.getByRole('link', { name: 'Review public record' }).click();
  await page.reload();
  await expect(page.getByRole('heading', { level: 1 })).toHaveText(movie.title);
  await expect(page.getByText('Revised synopsis after a real API write.')).toBeVisible();
  await capture(page, info, ['ATDD-016', 'ATDD-017'], 'movie-persisted');
});

test('ATDD-026/028 partial: shared person credit persists and can be removed visibly', async ({ page }, info) => {
  await signInMaintainer(page);
  const person = await createPerson(page);
  await createMovie(page);
  await page.getByLabel('Person', { exact: true }).selectOption(person.id);
  await page.getByLabel('Role', { exact: true }).selectOption({ index: 1 });
  await page.getByRole('button', { name: 'Add credit', exact: true }).click();
  await expect(page.getByText('Credit saved.', { exact: true })).toBeVisible();
  await page.reload();
  const credit = page.locator('.credit-list li').filter({ hasText: person.name });
  await expect(credit).toHaveCount(1);
  await capture(page, info, ['ATDD-026'], 'credit-persisted');
  await credit.getByRole('button', { name: 'Remove credit', exact: true }).click();
  await expect(credit).toHaveCount(0);
  await page.reload();
  await expect(page.locator('.credit-list li').filter({ hasText: person.name })).toHaveCount(0);
  await capture(page, info, ['ATDD-028'], 'credit-removed');
});

test('ATDD-079 partial: competing real editor retains draft and reports stale save', async ({ page, context }, info) => {
  await signInMaintainer(page);
  await createMovie(page);
  const competing = await context.newPage();
  await competing.goto(page.url());
  await competing.getByLabel('Synopsis · Required').fill('Competing editor committed version.');
  await page.getByLabel('Synopsis · Required').fill('My retained conflicting draft.');
  await competing.getByRole('button', { name: 'Save movie', exact: true }).click();
  await expect(competing.getByText('Movie saved', { exact: true })).toBeVisible();
  await page.getByRole('button', { name: 'Save movie', exact: true }).click();
  await expect(page.getByText('This movie changed while you were editing', { exact: true })).toBeVisible();
  await expect(page.getByLabel('Synopsis · Required')).toHaveValue('My retained conflicting draft.');
  await page.getByRole('button', { name: 'Review current version' }).click();
  await expect(page.getByText('Competing editor committed version.', { exact: true })).toBeVisible();
  await capture(page, info, ['ATDD-079'], 'conflict-retained');
  await competing.close();
});

test('ATDD-260 partial: backed movie draft is recovered after reload', async ({ page }, info) => {
  await signInMaintainer(page);
  await createMovie(page);
  const saved = page.waitForResponse(async (response) => {
    if (new URL(response.url()).pathname !== '/graphql') return false;
    return response.request().postDataJSON()?.query?.includes('mutation SavePersonDraft') === true;
  });
  await page.getByLabel('Synopsis · Required').fill('Recover this unsaved movie synopsis.');
  expect((await saved).ok()).toBe(true);
  await page.reload();
  await expect(page.getByLabel('Synopsis · Required')).toHaveValue('Recover this unsaved movie synopsis.');
  await capture(page, info, ['ATDD-260'], 'draft-recovered');
});
