import { test, expect, signInMaintainer, createMovie, createPerson, capture, uniqueName } from './helpers';

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
  await page.getByRole('dialog').getByRole('button', { name: 'Remove credit', exact: true }).click();
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
  await expect(page.getByText('Competing editor committed version.', { exact: true }).first()).toBeVisible();
  await capture(page, info, ['ATDD-079'], 'conflict-retained');
  const continueButton = page.getByRole('button', { name: 'Use reviewed version for my edits', exact: true });
  await expect(continueButton).toBeDisabled();
  await page.getByLabel('I reviewed the current values and my retained edits.').check();
  await continueButton.click();
  await expect(page.getByRole('button', { name: 'Save movie', exact: true })).toBeEnabled();
  await page.getByRole('button', { name: 'Save movie', exact: true }).click();
  await expect(page.getByText('Movie saved', { exact: true })).toBeVisible();
  await page.getByRole('link', { name: 'Review public record' }).click();
  await page.reload();
  await expect(page.getByText('My retained conflicting draft.', { exact: true })).toBeVisible();
  await capture(page, info, ['ATDD-079'], 'conflict-rebased');
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


async function backCreateDraft(page: import('@playwright/test').Page, title: string) {
  const saved = page.waitForResponse((response) => {
    if (new URL(response.url()).pathname !== '/graphql') return false;
    const body = response.request().postDataJSON();
    return body?.query?.includes('mutation SavePersonDraft') && body?.variables?.input?.values?.title === title;
  });
  await page.getByLabel('Title · Required', { exact: true }).fill(title);
  const response = await saved;
  expect(response.ok()).toBe(true);
  const body = await response.json();
  expect(body.errors).toBeUndefined();
  expect(body.data.saveDraft.operation.state).toBe('SUCCEEDED');
  return String(body.data.saveDraft.operation.resultId);
}

async function discardVisibleDraft(page: import('@playwright/test').Page) {
  await page.getByRole('button', { name: 'Leave draft', exact: true }).click();
  await page.getByRole('dialog').getByRole('button', { name: 'Discard draft', exact: true }).click();
  await expect(page.getByRole('dialog')).not.toBeVisible();
}

test('ATDD-260 partial: explicit retained-draft picker preserves the selected original draft ID', async ({ page, context }, info) => {
  await signInMaintainer(page);
  await page.goto('/manage/movies/new');
  await expect(page.getByLabel('Title · Required', { exact: true })).toBeEnabled();
  const second = await context.newPage();
  await second.goto('/manage/movies/new');
  await expect(second.getByLabel('Title · Required', { exact: true })).toBeEnabled();
  const firstTitle = uniqueName('First Draft');
  const secondTitle = uniqueName('Chosen Draft');
  await backCreateDraft(page, firstTitle);
  const chosenId = await backCreateDraft(second, secondTitle);
  await second.close();
  await page.reload();
  await expect(page.getByRole('heading', { name: 'Choose a retained draft', exact: true })).toBeVisible();
  await expect(page.getByLabel('Title · Required', { exact: true })).toBeDisabled();
  await capture(page, info, ['ATDD-260'], 'draft-picker');
  await page.getByRole('button', { name: `Resume ${secondTitle}`, exact: true }).click();
  await expect(page.getByLabel('Title · Required', { exact: true })).toHaveValue(secondTitle);
  const updated = page.waitForResponse((response) => new URL(response.url()).pathname === '/graphql'
    && response.request().postDataJSON()?.query?.includes('mutation SavePersonDraft')
    && response.request().postDataJSON()?.variables?.id === chosenId);
  await page.getByLabel('Synopsis · Required', { exact: true }).fill('Retained selection continues under its original draft identity.');
  expect((await updated).ok()).toBe(true);
  await capture(page, info, ['ATDD-260'], 'draft-selected');
  await discardVisibleDraft(page);
  await page.reload();
  await expect(page.getByLabel('Title · Required', { exact: true })).toHaveValue(firstTitle);
  await discardVisibleDraft(page);
});
