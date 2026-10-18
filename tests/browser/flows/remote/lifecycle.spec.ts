import { test, expect, signInMaintainer, createMovie, createPerson, capture, readApi } from './helpers';

for (const kind of ['movie', 'person'] as const) {
  test(`ATDD lifecycle ${kind} partial: archive, restore, cancel and permanent deletion persist`, async ({ page }, info) => {
    await signInMaintainer(page);
    const record = kind === 'movie' ? await createMovie(page) : await createPerson(page);
    const label = 'title' in record ? record.title : record.name;
    const editor = page.url();
    const archive = async () => {
      await page.getByRole('button', { name: `Archive ${kind}`, exact: true }).click();
      const confirm = page.getByRole('dialog');
      await expect(confirm).toBeVisible();
      await confirm.getByRole('button', { name: `Archive ${kind}`, exact: true }).click();
      await page.getByRole('link', { name: 'Archives', exact: true }).click();
      if (kind === 'person') await page.getByRole('tab', { name: 'People', exact: true }).click();
    };
    await archive();
    const row = page.locator('li').filter({ hasText: label });
    await expect(row).toHaveCount(1);
    await capture(page, info, [kind === 'movie' ? 'ATDD-082' : 'ATDD-105'], `${kind}-archived`);
    const restoreResponse = lifecycleResponse(page, 'RESTORE');
    await row.getByRole('button', { name: 'Restore', exact: true }).click();
    await assertOriginalReceipt(page, await restoreResponse, 'SUCCEEDED', info);
    await expect(row).toHaveCount(0);
    await page.goto(editor);
    await page.reload();
    await expect(page.getByRole('heading', { level: 1 })).toHaveText(label);
    await capture(page, info, [kind === 'movie' ? 'ATDD-083' : 'ATDD-106'], `${kind}-restored`);
    await archive();
    await row.getByRole('button', { name: 'Delete permanently', exact: true }).click();
    await expect(page.getByRole('heading', { name: 'Confirm permanent deletion' })).toBeVisible();
    await capture(page, info, [kind === 'movie' ? 'ATDD-084' : 'ATDD-107'], `${kind}-delete-dialog`);
    await page.keyboard.press('Escape');
    await expect(row.getByRole('button', { name: 'Delete permanently', exact: true })).toBeFocused();
    await expect(row).toHaveCount(1);
    await row.getByRole('button', { name: 'Delete permanently', exact: true }).click();
    const deleted = lifecycleResponse(page, 'DELETE');
    await page.getByRole('button', { name: 'Permanently delete', exact: true }).click();
    await assertOriginalReceipt(page, await deleted, 'SUCCEEDED', info);
    await expect(row).toHaveCount(0);
    await page.reload();
    if (kind === 'person') await page.getByRole('tab', { name: 'People', exact: true }).click();
    await expect(row).toHaveCount(0);
    await capture(page, info, [kind === 'movie' ? 'ATDD-019' : 'ATDD-024'], `${kind}-deleted`);
  });
}


function lifecycleResponse(page: import('@playwright/test').Page, action: string) {
  return page.waitForResponse((response) => {
    if (new URL(response.url()).pathname !== '/graphql') return false;
    const request = response.request().postDataJSON();
    return /mutation (Movie|Person)Lifecycle/.test(request?.query ?? '') && request?.variables?.action === action;
  });
}

async function assertOriginalReceipt(page: import('@playwright/test').Page,
  response: import('@playwright/test').Response, state: string, info: import('@playwright/test').TestInfo) {
  expect(response.ok()).toBe(true);
  const request = response.request().postDataJSON();
  const operationId = request.variables.operationId;
  const data = await readApi(page, 'query OriginalLifecycle($id: UUID!) { operation(id: $id, owner: CATALOG) { id state resultExpired problem { code } } }', { id: operationId });
  expect(data.operation.id).toBe(operationId);
  expect(data.operation.state).toBe(state);
  expect(data.operation.resultExpired).toBe(false);
  await info.attach(`lifecycle-receipt-${operationId}`, { body: JSON.stringify({ operationId,
    action: request.variables.action, subjectId: request.variables.id, state: data.operation.state,
    problemCode: data.operation.problem?.code ?? null, sha: process.env.TESTED_REF,
    limitation: 'Direct original receipt read after an actual UI mutation; this does not establish interrupted-response UI recovery.' }), contentType: 'application/json' });
  return data.operation;
}

async function addVisibleCredit(page: import('@playwright/test').Page, personId: string) {
  await page.getByLabel('Person', { exact: true }).selectOption(personId);
  await page.getByLabel('Role', { exact: true }).selectOption({ index: 1 });
  await page.getByRole('button', { name: 'Add credit', exact: true }).click();
  await expect(page.getByText('Credit saved.', { exact: true })).toBeVisible();
}

async function archivedPersonWithCredit(page: import('@playwright/test').Page) {
  const person = await createPerson(page);
  const movie = await createMovie(page);
  await addVisibleCredit(page, person.id);
  await page.goto(`/manage/people/${person.id}`);
  await page.getByRole('button', { name: 'Archive person', exact: true }).click();
  await page.getByRole('dialog').getByRole('button', { name: 'Archive person', exact: true }).click();
  await expect(page.getByRole('heading', { name: 'Archives', exact: true })).toBeVisible();
  await page.getByRole('tab', { name: 'People', exact: true }).click();
  const row = page.locator('li').filter({ hasText: person.name });
  await expect(row).toHaveCount(1);
  return { person, movie, row };
}

test('ATDD-103 partial: actual concurrent credit removal invalidates old impact and requires fresh confirmation', async ({ page, context }, info) => {
  await signInMaintainer(page);
  const { person, movie, row } = await archivedPersonWithCredit(page);
  await row.getByRole('button', { name: 'Delete permanently', exact: true }).click();
  const dialog = page.getByRole('dialog', { name: 'Confirm permanent deletion', exact: true });
  await expect(dialog).toContainText('Affects 1 credit(s)');
  const competing = await context.newPage();
  await competing.goto(`/manage/movies/${movie.id}`);
  const credit = competing.locator('.credit-list li').filter({ hasText: person.name });
  await credit.getByRole('button', { name: 'Remove credit', exact: true }).click();
  await competing.getByRole('dialog').getByRole('button', { name: 'Remove credit', exact: true }).click();
  await expect(credit).toHaveCount(0);
  const staleDelete = lifecycleResponse(page, 'DELETE');
  await dialog.getByRole('button', { name: 'Permanently delete', exact: true }).click();
  const stale = await assertOriginalReceipt(page, await staleDelete, 'FAILED', info);
  expect(stale.problem.code).toBe('IMPACT_CHANGED');
  await expect(page.getByText('The deletion impact changed. Load and confirm the current impact before deletion.', { exact: true })).toBeVisible();
  await expect(row).toHaveCount(1);
  await capture(page, info, ['ATDD-103'], 'impact-changed-real-credit');
  await row.getByRole('button', { name: 'Delete permanently', exact: true }).click();
  await expect(dialog).toContainText('No active credits reference this person.');
  const freshDelete = lifecycleResponse(page, 'DELETE');
  await dialog.getByRole('button', { name: 'Permanently delete', exact: true }).click();
  await assertOriginalReceipt(page, await freshDelete, 'SUCCEEDED', info);
  await expect(row).toHaveCount(0);
  await page.reload();
  await page.getByRole('tab', { name: 'People', exact: true }).click();
  await expect(row).toHaveCount(0);
  await capture(page, info, ['ATDD-103'], 'impact-refreshed-confirmed');
  await competing.close();
});
