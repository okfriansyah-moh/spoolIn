import {
  test,
  expect,
  signInMaintainer,
  createMovie,
  createPerson,
  capture,
  uniqueName,
  readApi,
} from './helpers';

test('ATDD-016/017/023: movie and person writes survive public-route reload', async ({
  page,
}, info) => {
  await signInMaintainer(page);
  const person = await createPerson(page);
  await page
    .getByLabel('Biography · Optional')
    .fill('Revised biography with a retained shared identity.');
  await page.getByRole('button', { name: 'Save person', exact: true }).click();
  await expect(page.getByText('Person saved', { exact: true })).toBeVisible();
  await page.getByRole('link', { name: 'Review public record' }).click();
  await page.reload();
  await expect(page.getByRole('heading', { level: 1 })).toHaveText(person.name);
  await expect(
    page.getByText('Revised biography with a retained shared identity.'),
  ).toBeVisible();
  await capture(page, info, ['ATDD-023'], 'person-persisted');
  const movie = await createMovie(page);
  await page
    .getByLabel('Synopsis · Required')
    .fill('Revised synopsis after a real API write.');
  await page.getByRole('button', { name: 'Save movie', exact: true }).click();
  await expect(page.getByText('Movie saved', { exact: true })).toBeVisible();
  await page.getByRole('link', { name: 'Review public record' }).click();
  await page.reload();
  await expect(page.getByRole('heading', { level: 1 })).toHaveText(movie.title);
  await expect(
    page.getByText('Revised synopsis after a real API write.'),
  ).toBeVisible();
  await capture(page, info, ['ATDD-016', 'ATDD-017'], 'movie-persisted');
});

test('ATDD-026/028 partial: shared person credit persists and can be removed visibly', async ({
  page,
}, info) => {
  await signInMaintainer(page);
  const person = await createPerson(page);
  await createMovie(page);
  await chooseCreditPerson(page, person.id);
  await page.getByLabel('Role', { exact: true }).selectOption({ index: 1 });
  await page.getByRole('button', { name: 'Add credit', exact: true }).click();
  await expect(page.getByText('Credit saved.', { exact: true })).toBeVisible();
  await page.reload();
  const credit = page
    .locator('.credit-list li')
    .filter({ hasText: person.name });
  await expect(credit).toHaveCount(1);
  await capture(page, info, ['ATDD-026'], 'credit-persisted');
  await credit
    .getByRole('button', { name: 'Remove credit', exact: true })
    .click();
  await page
    .getByRole('dialog')
    .getByRole('button', { name: 'Remove credit', exact: true })
    .click();
  await expect(credit).toHaveCount(0);
  await page.reload();
  await expect(
    page.locator('.credit-list li').filter({ hasText: person.name }),
  ).toHaveCount(0);
  await capture(page, info, ['ATDD-028'], 'credit-removed');
});

test('ATDD-079 partial: competing real editor retains draft and reports stale save', async ({
  page,
  context,
}, info) => {
  await signInMaintainer(page);
  await createMovie(page);
  const competing = await context.newPage();
  await competing.goto(page.url());
  await competing
    .getByLabel('Synopsis · Required')
    .fill('Competing editor committed version.');
  await page
    .getByLabel('Synopsis · Required')
    .fill('My retained conflicting draft.');
  await competing
    .getByRole('button', { name: 'Save movie', exact: true })
    .click();
  await expect(
    competing.getByText('Movie saved', { exact: true }),
  ).toBeVisible();
  await page.getByRole('button', { name: 'Save movie', exact: true }).click();
  await expect(
    page.getByText('This movie changed while you were editing', {
      exact: true,
    }),
  ).toBeVisible();
  await expect(page.getByLabel('Synopsis · Required')).toHaveValue(
    'My retained conflicting draft.',
  );
  await page.getByRole('button', { name: 'Review current version' }).click();
  await expect(
    page
      .getByText('Competing editor committed version.', { exact: true })
      .first(),
  ).toBeVisible();
  await capture(page, info, ['ATDD-079'], 'conflict-retained');
  const continueButton = page.getByRole('button', {
    name: 'Use reviewed version for my edits',
    exact: true,
  });
  await expect(continueButton).toBeDisabled();
  await page
    .getByLabel('I reviewed the current values and my retained edits.')
    .check();
  await continueButton.click();
  await expect(
    page.getByRole('button', { name: 'Save movie', exact: true }),
  ).toBeEnabled();
  await page.getByRole('button', { name: 'Save movie', exact: true }).click();
  await expect(page.getByText('Movie saved', { exact: true })).toBeVisible();
  await page.getByRole('link', { name: 'Review public record' }).click();
  await page.reload();
  await expect(
    page.getByText('My retained conflicting draft.', { exact: true }),
  ).toBeVisible();
  await capture(page, info, ['ATDD-079'], 'conflict-rebased');
  await competing.close();
});

test('ATDD-260 partial: backed movie draft is recovered after reload', async ({
  page,
}, info) => {
  await signInMaintainer(page);
  await createMovie(page);
  const saved = page.waitForResponse(async (response) => {
    if (new URL(response.url()).pathname !== '/graphql') return false;
    return (
      response
        .request()
        .postDataJSON()
        ?.query?.includes('mutation SavePersonDraft') === true
    );
  });
  await page
    .getByLabel('Synopsis · Required')
    .fill('Recover this unsaved movie synopsis.');
  expect((await saved).ok()).toBe(true);
  await page.reload();
  await expect(page.getByLabel('Synopsis · Required')).toHaveValue(
    'Recover this unsaved movie synopsis.',
  );
  await capture(page, info, ['ATDD-260'], 'draft-recovered');
});

async function backCreateDraft(
  page: import('@playwright/test').Page,
  title: string,
) {
  const saved = page.waitForResponse((response) => {
    if (new URL(response.url()).pathname !== '/graphql') return false;
    const body = response.request().postDataJSON();
    return (
      body?.query?.includes('mutation SavePersonDraft') &&
      body?.variables?.input?.values?.title === title
    );
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
  await page
    .getByRole('dialog')
    .getByRole('button', { name: 'Discard draft', exact: true })
    .click();
  await expect(page.getByRole('dialog')).not.toBeVisible();
}

test('ATDD-260 partial: explicit retained-draft picker preserves the selected original draft ID', async ({
  page,
  context,
}, info) => {
  await signInMaintainer(page);
  await page.goto('/manage/movies/new');
  await expect(
    page.getByLabel('Title · Required', { exact: true }),
  ).toBeEnabled();
  const second = await context.newPage();
  await second.goto('/manage/movies/new');
  await expect(
    second.getByLabel('Title · Required', { exact: true }),
  ).toBeEnabled();
  const firstTitle = uniqueName('First Draft');
  const secondTitle = uniqueName('Chosen Draft');
  await backCreateDraft(page, firstTitle);
  const chosenId = await backCreateDraft(second, secondTitle);
  await second.close();
  await page.reload();
  await expect(
    page.getByRole('heading', { name: 'Choose a retained draft', exact: true }),
  ).toBeVisible();
  await expect(
    page.getByLabel('Title · Required', { exact: true }),
  ).toBeDisabled();
  await capture(page, info, ['ATDD-260'], 'draft-picker');
  await page
    .getByRole('button', { name: `Resume ${secondTitle}`, exact: true })
    .click();
  await expect(
    page.getByLabel('Title · Required', { exact: true }),
  ).toHaveValue(secondTitle);
  const updated = page.waitForResponse(
    (response) =>
      new URL(response.url()).pathname === '/graphql' &&
      response
        .request()
        .postDataJSON()
        ?.query?.includes('mutation SavePersonDraft') &&
      response.request().postDataJSON()?.variables?.id === chosenId,
  );
  await page
    .getByLabel('Synopsis · Required', { exact: true })
    .fill('Retained selection continues under its original draft identity.');
  expect((await updated).ok()).toBe(true);
  await capture(page, info, ['ATDD-260'], 'draft-selected');
  await discardVisibleDraft(page);
  await page.reload();
  await expect(
    page.getByLabel('Title · Required', { exact: true }),
  ).toHaveValue(firstTitle);
  await discardVisibleDraft(page);
});

async function chooseCreditPerson(
  page: import('@playwright/test').Page,
  personId: string,
) {
  const picker = page.getByLabel('Person', { exact: true });
  while ((await picker.locator(`option[value="${personId}"]`).count()) === 0) {
    await expect(
      page.getByRole('button', { name: 'Load more people', exact: true }),
    ).toBeVisible();
    const next = page.waitForResponse(
      (response) =>
        new URL(response.url()).pathname === '/graphql' &&
        response
          .request()
          .postDataJSON()
          ?.query?.includes('query ManagedPeople'),
    );
    await page
      .getByRole('button', { name: 'Load more people', exact: true })
      .click();
    const response = await next;
    expect(response.ok()).toBe(true);
    expect((await response.json()).errors).toBeUndefined();
    await expect(picker).toBeEnabled();
  }
  await picker.selectOption(personId);
}

async function currentCredits(
  page: import('@playwright/test').Page,
  titleId: string,
) {
  const data = await readApi(
    page,
    `query BrowserCredits($titleId: UUID!) {
    managedCredits(titleId: $titleId, first: 48) {
      nodes { id personId personName characters version role { id label category allowsCharacters } }
    }
  }`,
    { titleId },
  );
  return data.managedCredits.nodes as Array<{
    id: string;
    personId: string;
    personName: string;
    characters: string[];
    role: {
      id: string;
      label: string;
      category: string;
      allowsCharacters: boolean;
    };
  }>;
}

async function addActorCredit(
  page: import('@playwright/test').Page,
  personId: string,
  names: string[],
) {
  await chooseCreditPerson(page, personId);
  await page
    .getByLabel('Role', { exact: true })
    .selectOption({ label: 'Actor (CAST)' });
  await page
    .getByLabel('Character names · one per line')
    .fill(names.join('\n'));
  await page.getByRole('button', { name: 'Add credit', exact: true }).click();
  await expect(page.getByText('Credit saved.', { exact: true })).toBeVisible();
  await page.reload();
}

async function removeCreditAfterCancel(
  page: import('@playwright/test').Page,
  credit: import('@playwright/test').Locator,
) {
  await credit
    .getByRole('button', { name: 'Remove credit', exact: true })
    .click();
  await page
    .getByRole('dialog')
    .getByRole('button', { name: 'Cancel', exact: true })
    .click();
  await expect(credit).toHaveCount(1);
  await credit
    .getByRole('button', { name: 'Remove credit', exact: true })
    .click();
  await page
    .getByRole('dialog')
    .getByRole('button', { name: 'Remove credit', exact: true })
    .click();
  await expect(credit).toHaveCount(0);
  await page.reload();
}

test('ATDD-026/027/028 partial: update ordered complete character names without cloning a shared person', async ({
  page,
}, info) => {
  await signInMaintainer(page);
  const person = await createPerson(page);
  const movie = await createMovie(page);
  const names = ['Maya', 'Young Maya', 'Maya / Echo'];
  await addActorCredit(page, person.id, names);
  const credit = page
    .locator('.credit-list li')
    .filter({ hasText: person.name });
  await expect(credit).toHaveCount(1);
  const original = (await currentCredits(page, movie.id)).find(
    (row) => row.personId === person.id,
  );
  expect(original?.characters).toEqual(names);
  await credit
    .getByRole('button', { name: 'Edit credit', exact: true })
    .click();
  await expect(page.getByLabel('Person', { exact: true })).toBeDisabled();
  await expect(page.getByLabel('Character names · one per line')).toHaveValue(
    names.join('\n'),
  );
  const reordered = ['Maya / Echo', 'Maya', 'Young Maya'];
  await page
    .getByLabel('Character names · one per line')
    .fill(reordered.join('\n'));
  await capture(page, info, ['ATDD-026'], 'credit-edit-ordered-names');
  await page.getByRole('button', { name: 'Save credit', exact: true }).click();
  await expect(page.getByText('Credit saved.', { exact: true })).toBeVisible();
  await page.reload();
  const updated = (await currentCredits(page, movie.id)).filter(
    (row) => row.personId === person.id,
  );
  expect(updated).toHaveLength(1);
  expect(updated[0].id).toBe(original?.id);
  expect(updated[0].characters).toEqual(reordered);
  await expect(credit).toContainText('Maya / Echo, Maya, Young Maya');
  await capture(page, info, ['ATDD-026'], 'credit-updated-persisted');
  await chooseCreditPerson(page, person.id);
  await page
    .getByLabel('Role', { exact: true })
    .selectOption({ label: 'Actor (CAST)' });
  await page.getByRole('button', { name: 'Add credit', exact: true }).click();
  await expect(
    page.getByText('Review the credit — no duplicate added.', { exact: true }),
  ).toBeVisible();
  await expect(credit).toHaveCount(1);
  expect(
    (await currentCredits(page, movie.id)).filter(
      (row) => row.personId === person.id,
    ),
  ).toHaveLength(1);
  await capture(page, info, ['ATDD-027'], 'credit-duplicate-rejected');
  await discardVisibleCreditDraft(page);
  await page.reload();
  await removeCreditAfterCancel(page, credit);
  expect(
    (await currentCredits(page, movie.id)).filter(
      (row) => row.personId === person.id,
    ),
  ).toHaveLength(0);
  await page.goto(`/people/${person.id}`);
  await expect(page.getByRole('heading', { level: 1 })).toHaveText(person.name);
  await capture(page, info, ['ATDD-028'], 'credit-removed-person-preserved');
});

async function backPersonDraft(
  page: import('@playwright/test').Page,
  label: string,
  field: 'name' | 'biography',
  value: string,
  expectedId?: string,
) {
  const saved = page.waitForResponse((response) => {
    if (new URL(response.url()).pathname !== '/graphql') return false;
    const request = response.request().postDataJSON();
    return (
      request?.query?.includes('mutation SavePersonDraft') &&
      request?.variables?.input?.targetKind === 'PERSON' &&
      request?.variables?.input?.values?.[field] === value &&
      (!expectedId || request?.variables?.id === expectedId)
    );
  });
  await page.getByLabel(label, { exact: true }).fill(value);
  const response = await saved;
  expect(response.ok()).toBe(true);
  const body = await response.json();
  expect(body.errors).toBeUndefined();
  expect(body.data.saveDraft.operation.state).toBe('SUCCEEDED');
  return String(body.data.saveDraft.operation.resultId);
}

test('ATDD-179/261 partial: person backed biography reloads under the original draft and saves to the same identity', async ({
  page,
}, info) => {
  await signInMaintainer(page);
  const person = await createPerson(page);
  const biography =
    'This retained person biography survives a real full reload.';
  const draftId = await backPersonDraft(
    page,
    'Biography · Optional',
    'biography',
    biography,
  );
  await page.reload();
  await expect(page.getByLabel('Biography · Optional')).toHaveValue(biography);
  await expect(page.getByLabel('Professional name · Required')).toHaveValue(
    person.name,
  );
  await capture(page, info, ['ATDD-179', 'ATDD-261'], 'person-draft-reloaded');
  const revision = biography + ' Reviewed after recovery.';
  await backPersonDraft(
    page,
    'Biography · Optional',
    'biography',
    revision,
    draftId,
  );
  await page.getByRole('button', { name: 'Save person', exact: true }).click();
  await expect(page.getByText('Person saved', { exact: true })).toBeVisible();
  await page
    .getByRole('link', { name: 'Review public record', exact: true })
    .click();
  await page.reload();
  await expect(page).toHaveURL(new RegExp(`/people/${person.id}$`));
  await expect(page.getByText(revision, { exact: true })).toBeVisible();
  const drafts = await readApi(
    page,
    'query { drafts { id baseVersion values operationId } }',
  );
  const retained = drafts.drafts.find(
    (draft: { id: string }) => draft.id === draftId,
  );
  expect(retained.values).toMatchObject({
    name: person.name,
    biography: revision,
  });
  expect(retained.operationId).toBeNull();
  expect(retained.baseVersion).not.toBeNull();
  await capture(
    page,
    info,
    ['ATDD-179', 'ATDD-261'],
    'person-recovered-draft-committed',
  );
});

test('ATDD-025/261 partial: person conflict preserves biography until explicit current-value review', async ({
  page,
  context,
}, info) => {
  await signInMaintainer(page);
  const person = await createPerson(page);
  const competing = await context.newPage();
  await competing.goto(page.url());
  await page
    .getByLabel('Biography · Optional')
    .fill('My permitted retained biography.');
  await competing
    .getByLabel('Biography · Optional')
    .fill('Other editor committed biography.');
  await competing
    .getByRole('button', { name: 'Save person', exact: true })
    .click();
  await expect(
    competing.getByText('Person saved', { exact: true }),
  ).toBeVisible();
  await page.getByRole('button', { name: 'Save person', exact: true }).click();
  await expect(
    page.getByText('This person changed', { exact: true }),
  ).toBeVisible();
  await expect(page.getByLabel('Biography · Optional')).toHaveValue(
    'My permitted retained biography.',
  );
  await page
    .getByRole('button', { name: 'Review current record', exact: true })
    .click();
  await expect(
    page.getByText('Other editor committed biography.', { exact: true }),
  ).toBeVisible();
  const proceed = page.getByRole('button', {
    name: 'Use reviewed version for my edits',
    exact: true,
  });
  await expect(proceed).toBeDisabled();
  await capture(
    page,
    info,
    ['ATDD-025', 'ATDD-261'],
    'person-conflict-review-required',
  );
  await page
    .getByLabel('I reviewed the current values and my retained edits.')
    .check();
  await proceed.click();
  await page.getByRole('button', { name: 'Save person', exact: true }).click();
  await expect(page.getByText('Person saved', { exact: true })).toBeVisible();
  await page
    .getByRole('link', { name: 'Review public record', exact: true })
    .click();
  await page.reload();
  await expect(page).toHaveURL(new RegExp(`/people/${person.id}$`));
  await expect(
    page.getByText('My permitted retained biography.', { exact: true }),
  ).toBeVisible();
  await capture(
    page,
    info,
    ['ATDD-025', 'ATDD-261'],
    'person-conflict-reviewed-persisted',
  );
  await competing.close();
});

async function createVisibleRole(
  page: import('@playwright/test').Page,
  label: string,
  category: 'CAST' | 'CREATOR',
) {
  await page.getByLabel('Custom role name · Required').fill(label);
  await page
    .getByLabel('Custom role category · Required')
    .selectOption(category);
  const response = page.waitForResponse(
    (item) =>
      new URL(item.url()).pathname === '/graphql' &&
      item
        .request()
        .postDataJSON()
        ?.query?.includes('mutation CreateCreditRole') &&
      item.request().postDataJSON()?.variables?.label === label,
  );
  await page.getByRole('button', { name: 'Create role', exact: true }).click();
  const result = await response;
  expect(result.ok()).toBe(true);
  const body = await result.json();
  expect(body.errors).toBeUndefined();
  expect(body.data.createRole.operation.state).toBe('SUCCEEDED');
  await expect(
    page.getByText('Role created. It is selected for this credit.', {
      exact: true,
    }),
  ).toBeVisible();
  const id = String(body.data.createRole.operation.resultId);
  await expect(page.getByLabel('Role', { exact: true })).toHaveValue(id);
  await expect(page.getByLabel('Character names · one per line')).toHaveCount(
    0,
  );
  return id;
}

async function addSelectedRole(
  page: import('@playwright/test').Page,
  personId: string,
  label: string,
) {
  await chooseCreditPerson(page, personId);
  await page.getByLabel('Role', { exact: true }).selectOption({ label });
  await page.getByRole('button', { name: 'Add credit', exact: true }).click();
  await expect(page.getByText('Credit saved.', { exact: true })).toBeVisible();
}

test('ATDD-026/027 partial: actual custom Cast and Creator roles keep one person and reject a normalized duplicate', async ({
  page,
}, info) => {
  await signInMaintainer(page);
  const person = await createPerson(page);
  const movie = await createMovie(page);
  for (const label of [
    'Actor (CAST)',
    'Director (CREATOR)',
    'Writer (CREATOR)',
    'Producer (CREATOR)',
  ]) {
    await expect(
      page.getByRole('option', { name: label, exact: true }),
    ).toHaveCount(1);
  }
  await addActorCredit(page, person.id, ['Maya', 'Young Maya', 'Maya / Echo']);
  await addSelectedRole(page, person.id, 'Director (CREATOR)');
  await addSelectedRole(page, person.id, 'Writer (CREATOR)');
  const stunt = uniqueName('Stunt');
  const editor = uniqueName('Editor');
  const stuntId = await createVisibleRole(page, stunt, 'CAST');
  await addSelectedRole(page, person.id, `${stunt} (CAST)`);
  const editorId = await createVisibleRole(page, editor, 'CREATOR');
  await addSelectedRole(page, person.id, `${editor} (CREATOR)`);
  await page.reload();
  const credits = (await currentCredits(page, movie.id)).filter(
    (row) => row.personId === person.id,
  );
  expect(credits).toHaveLength(5);
  expect(new Set(credits.map((row) => row.personId))).toEqual(
    new Set([person.id]),
  );
  expect(credits.find((row) => row.role.label === 'Actor')?.characters).toEqual(
    ['Maya', 'Young Maya', 'Maya / Echo'],
  );
  for (const roleId of [stuntId, editorId]) {
    const credit = credits.find((row) => row.role.id === roleId);
    expect(credit?.characters).toEqual([]);
    expect(credit?.role.allowsCharacters).toBe(false);
  }
  await capture(
    page,
    info,
    ['ATDD-026'],
    'custom-roles-shared-person-persisted',
  );
  await page
    .getByLabel('Custom role name · Required')
    .fill(stunt.toUpperCase());
  await page.getByLabel('Custom role category · Required').selectOption('CAST');
  await page.getByRole('button', { name: 'Create role', exact: true }).click();
  await expect(
    page.getByText(
      'This role already exists. Choose the existing role or correct its name.',
      { exact: true },
    ),
  ).toBeVisible();
  const roles = await readApi(
    page,
    'query { creditRoles { id label category allowsCharacters } }',
  );
  expect(
    roles.creditRoles.filter(
      (role: { label: string }) =>
        role.label.toLowerCase() === stunt.toLowerCase(),
    ),
  ).toHaveLength(1);
  expect(
    (await currentCredits(page, movie.id)).filter(
      (row) => row.personId === person.id,
    ),
  ).toHaveLength(5);
  await capture(page, info, ['ATDD-027'], 'custom-role-duplicate-rejected');
});

async function discardPersonDraft(
  page: import('@playwright/test').Page,
  draftId: string,
) {
  await page.getByRole('button', { name: 'Leave draft', exact: true }).click();
  const dialog = page.getByRole('dialog');
  await expect(
    dialog.getByRole('button', { name: 'Cancel', exact: true }),
  ).toBeFocused();
  const discarded = page.waitForResponse(
    (response) =>
      new URL(response.url()).pathname === '/graphql' &&
      response
        .request()
        .postDataJSON()
        ?.query?.includes('mutation DiscardPersonDraft') &&
      response.request().postDataJSON()?.variables?.id === draftId,
  );
  await dialog
    .getByRole('button', { name: 'Discard draft', exact: true })
    .click();
  const body = await (await discarded).json();
  expect(body.errors).toBeUndefined();
  expect(body.data.discardDraft.operation.state).toBe('SUCCEEDED');
  expect(body.data.discardDraft.discarded).toBe(true);
  await expect(dialog).not.toBeVisible();
}

test('ATDD-179/261 partial: person create picker resumes the chosen original draft and discards only that selection', async ({
  page,
  context,
}, info) => {
  await signInMaintainer(page);
  await page.goto('/manage/people/new');
  await expect(page.getByLabel('Professional name · Required')).toBeEnabled();
  const second = await context.newPage();
  await second.goto('/manage/people/new');
  await expect(second.getByLabel('Professional name · Required')).toBeEnabled();
  const firstName = uniqueName('First Person Draft');
  const chosenName = uniqueName('Chosen Person Draft');
  const firstId = await backPersonDraft(
    page,
    'Professional name · Required',
    'name',
    firstName,
  );
  const chosenId = await backPersonDraft(
    second,
    'Professional name · Required',
    'name',
    chosenName,
  );
  await second.close();
  await page.reload();
  await expect(
    page.getByRole('heading', { name: 'Choose a retained draft', exact: true }),
  ).toBeVisible();
  await expect(page.getByLabel('Professional name · Required')).toBeDisabled();
  await capture(page, info, ['ATDD-179'], 'person-draft-picker');
  await page
    .getByRole('button', { name: `Resume ${chosenName}`, exact: true })
    .click();
  await expect(page.getByLabel('Professional name · Required')).toHaveValue(
    chosenName,
  );
  await backPersonDraft(
    page,
    'Biography · Optional',
    'biography',
    'Selected person draft keeps its original identity.',
    chosenId,
  );
  await page.getByRole('button', { name: 'Leave draft', exact: true }).click();
  await expect(
    page
      .getByRole('dialog')
      .getByRole('button', { name: 'Cancel', exact: true }),
  ).toBeFocused();
  await capture(page, info, ['ATDD-179'], 'person-draft-discard-confirmation');
  await page.keyboard.press('Escape');
  await expect(page.getByLabel('Professional name · Required')).toHaveValue(
    chosenName,
  );
  await discardPersonDraft(page, chosenId);
  const drafts = await readApi(page, 'query { drafts { id } }');
  expect(
    drafts.drafts.some((draft: { id: string }) => draft.id === chosenId),
  ).toBe(false);
  expect(
    drafts.drafts.some((draft: { id: string }) => draft.id === firstId),
  ).toBe(true);
  await page.reload();
  await expect(page.getByLabel('Professional name · Required')).toHaveValue(
    firstName,
  );
  await capture(page, info, ['ATDD-179'], 'person-other-draft-preserved');
  await discardPersonDraft(page, firstId);
  await page.reload();
  await expect(page.getByLabel('Professional name · Required')).toHaveValue('');
});

async function discardVisibleCreditDraft(
  page: import('@playwright/test').Page,
) {
  await page
    .getByRole('button', { name: 'Discard credit draft', exact: true })
    .click();
  const dialog = page.getByRole('dialog', {
    name: 'Discard this unsaved credit draft?',
  });
  await expect(
    dialog.getByRole('button', { name: 'Cancel', exact: true }),
  ).toBeFocused();
  await dialog
    .getByRole('button', { name: 'Discard credit draft', exact: true })
    .click();
  await expect(
    page.getByText('Credit draft discarded. Saved credits are unchanged.', {
      exact: true,
    }),
  ).toBeVisible();
}

async function backCreditNames(
  page: import('@playwright/test').Page,
  names: string[],
  targetId: string | null,
  expectedDraft?: string,
) {
  const response = page.waitForResponse((item) => {
    if (new URL(item.url()).pathname !== '/graphql') return false;
    const variables = item.request().postDataJSON()?.variables;
    return (
      variables?.input?.targetKind === 'CREDIT' &&
      variables.input.targetId === targetId &&
      JSON.stringify(variables.input.values?.characters) ===
        JSON.stringify(names) &&
      (!expectedDraft || variables.id === expectedDraft)
    );
  });
  await page
    .getByLabel('Character names · one per line')
    .fill(names.join('\n'));
  const actual = await response;
  expect(actual.ok()).toBe(true);
  const body = await actual.json();
  expect(body.errors).toBeUndefined();
  expect(body.data.saveDraft.operation.state).toBe('SUCCEEDED');
  return String(body.data.saveDraft.operation.resultId);
}

test('ATDD-026/179/262 partial: exact credit edit draft reloads and retains ordered names under its original ID', async ({
  page,
}, info) => {
  await signInMaintainer(page);
  const person = await createPerson(page);
  const movie = await createMovie(page);
  await addActorCredit(page, person.id, ['Maya']);
  const original = (await currentCredits(page, movie.id)).find(
    (credit) => credit.personId === person.id,
  );
  expect(original).toBeDefined();
  const row = page.locator('.credit-list li').filter({ hasText: person.name });
  await row.getByRole('button', { name: 'Edit credit', exact: true }).click();
  const names = ['Maya / Echo', 'Young Maya', 'Maya'];
  const draftId = await backCreditNames(page, names, original!.id);
  const before = await readApi(
    page,
    'query { drafts { id targetKind targetId baseVersion values operationId } }',
  );
  expect(
    before.drafts.find((draft: { id: string }) => draft.id === draftId),
  ).toMatchObject({
    targetKind: 'CREDIT',
    targetId: original!.id,
    values: { titleId: movie.id, personId: person.id, characters: names },
    operationId: null,
  });
  await page.reload();
  await expect(page.getByLabel('Person', { exact: true })).toBeDisabled();
  await expect(page.getByLabel('Character names · one per line')).toHaveValue(
    names.join('\n'),
  );
  await expect(
    page.getByRole('button', { name: 'Save credit', exact: true }),
  ).toBeEnabled();
  await capture(
    page,
    info,
    ['ATDD-026', 'ATDD-179', 'ATDD-262'],
    'credit-edit-draft-restored',
  );
  const revised = ['Young Maya', 'Maya / Echo', 'Maya'];
  await backCreditNames(page, revised, original!.id, draftId);
  await page.getByRole('button', { name: 'Save credit', exact: true }).click();
  await expect(page.getByText('Credit saved.', { exact: true })).toBeVisible();
  await page.reload();
  const current = (await currentCredits(page, movie.id)).filter(
    (credit) => credit.personId === person.id,
  );
  expect(current).toHaveLength(1);
  expect(current[0]).toMatchObject({ id: original!.id, characters: revised });
  const after = await readApi(page, 'query { drafts { id } }');
  expect(
    after.drafts.some((draft: { id: string }) => draft.id === draftId),
  ).toBe(false);
  await expect(row).toContainText(revised.join(', '));
  await capture(
    page,
    info,
    ['ATDD-026', 'ATDD-179', 'ATDD-262'],
    'credit-edit-draft-committed',
  );
});

test('ATDD-026/179 partial: new credit drafts use no edit target and an explicit original-ID picker', async ({
  page,
  context,
}, info) => {
  await signInMaintainer(page);
  const person = await createPerson(page);
  const movie = await createMovie(page);
  const second = await context.newPage();
  await second.goto(page.url());
  await expect(second.getByLabel('Role', { exact: true })).toBeEnabled();
  const firstNames = ['Maya / Echo', 'First retained draft'];
  await chooseCreditPerson(page, person.id);
  await page
    .getByLabel('Role', { exact: true })
    .selectOption({ label: 'Actor (CAST)' });
  const firstId = await backCreditNames(page, firstNames, null);
  const selectedNames = ['Young Maya', 'Selected retained draft'];
  await chooseCreditPerson(second, person.id);
  await second
    .getByLabel('Role', { exact: true })
    .selectOption({ label: 'Actor (CAST)' });
  const selectedId = await backCreditNames(second, selectedNames, null);
  expect(selectedId).not.toBe(firstId);
  await second.close();
  await page.reload();
  await expect(
    page.getByRole('heading', {
      name: 'Choose a retained credit draft',
      exact: true,
    }),
  ).toBeVisible();
  await expect(page.getByLabel('Person', { exact: true })).toBeDisabled();
  await capture(page, info, ['ATDD-179'], 'credit-draft-picker');
  await page
    .getByRole('button', { name: /^Resume Add / })
    .filter({ hasText: selectedNames.join(', ') })
    .click();
  await expect(page.getByLabel('Character names · one per line')).toHaveValue(
    selectedNames.join('\n'),
  );
  await expect(
    page.getByRole('button', { name: 'Add credit', exact: true }),
  ).toBeEnabled();
  const finalNames = ['Young Maya', 'Maya / Echo'];
  await backCreditNames(page, finalNames, null, selectedId);
  const drafts = await readApi(
    page,
    'query { drafts { id targetId baseVersion values } }',
  );
  expect(
    drafts.drafts.find((draft: { id: string }) => draft.id === selectedId),
  ).toMatchObject({
    targetId: null,
    values: { titleId: movie.id, personId: person.id, characters: finalNames },
  });
  await page.getByRole('button', { name: 'Add credit', exact: true }).click();
  await expect(page.getByText('Credit saved.', { exact: true })).toBeVisible();
  expect(
    (await currentCredits(page, movie.id)).filter(
      (credit) => credit.personId === person.id,
    ),
  ).toMatchObject([{ characters: finalNames }]);
  await page.reload();
  await expect(page.getByLabel('Character names · one per line')).toHaveValue(
    firstNames.join('\n'),
  );
  await discardVisibleCreditDraft(page);
  const remaining = await readApi(page, 'query { drafts { id } }');
  expect(
    remaining.drafts.some((draft: { id: string }) =>
      [firstId, selectedId].includes(draft.id),
    ),
  ).toBe(false);
  expect(
    (await currentCredits(page, movie.id)).filter(
      (credit) => credit.personId === person.id,
    ),
  ).toHaveLength(1);
  await capture(
    page,
    info,
    ['ATDD-026', 'ATDD-179'],
    'credit-selected-draft-persisted',
  );
});

test('ATDD-017 partial: movie metadata survives a real update and editor reload', async ({
  page,
}, info) => {
  await signInMaintainer(page);
  const movie = await createMovie(page);
  await page.getByLabel('Release year · Required').fill('2024');
  await page.getByLabel('Release month', { exact: true }).fill('2');
  await page.getByLabel('Release day', { exact: true }).fill('29');
  await page.getByLabel('Runtime in minutes', { exact: true }).fill('123');
  await page.getByLabel('Original language', { exact: true }).fill('id');
  await page.getByLabel('Synopsis language', { exact: true }).fill('en-GB');
  await page.getByLabel('Countries', { exact: true }).fill('ID, GB');
  await page.getByRole('button', { name: 'Save movie', exact: true }).click();
  await expect(page.getByText('Movie saved', { exact: true })).toBeVisible();
  await page.reload();
  await expect(page.getByLabel('Release month', { exact: true })).toHaveValue(
    '2',
  );
  await expect(page.getByLabel('Release day', { exact: true })).toHaveValue(
    '29',
  );
  await expect(
    page.getByLabel('Runtime in minutes', { exact: true }),
  ).toHaveValue('123');
  await expect(
    page.getByLabel('Original language', { exact: true }),
  ).toHaveValue('id');
  await expect(
    page.getByLabel('Synopsis language', { exact: true }),
  ).toHaveValue('en-GB');
  await expect(page.getByLabel('Countries', { exact: true })).toHaveValue(
    'ID, GB',
  );
  const loaded = await readApi(
    page,
    `query Metadata($id: UUID!) {
    managedTitle(id: $id) { release { year month day } runtimeMinutes originalLanguage synopsisLanguage countries }
  }`,
    { id: movie.id },
  );
  expect(loaded.managedTitle).toMatchObject({
    release: { year: 2024, month: 2, day: 29 },
    runtimeMinutes: 123,
    originalLanguage: 'id',
    synopsisLanguage: 'en-GB',
  });
  expect([...loaded.managedTitle.countries].sort()).toEqual(['GB', 'ID']);
  await capture(page, info, ['ATDD-017'], 'movie-complete-metadata-persisted');
});

test('ATDD-028/262 partial: archived-person credit is private, removable and never recreated by later restore', async ({
  page,
}, info) => {
  await signInMaintainer(page);
  const person = await createPerson(page);
  const movie = await createMovie(page);
  await addActorCredit(page, person.id, ['Maya / Echo']);
  await page.goto(`/manage/people/${person.id}`);
  await page
    .getByRole('button', { name: 'Archive person', exact: true })
    .click();
  await page
    .getByRole('dialog')
    .getByRole('button', { name: 'Archive person', exact: true })
    .click();
  await expect(page).toHaveURL(/\/manage\/archives$/);
  await page.goto(`/manage/movies/${movie.id}`);
  const credit = page
    .locator('.credit-list li')
    .filter({ hasText: person.name });
  await expect(credit).toContainText(
    'Private credit · hidden publicly while this person is archived.',
  );
  await expect(
    credit.getByRole('button', { name: 'Edit credit', exact: true }),
  ).toBeDisabled();
  await expect(
    credit.getByRole('button', { name: 'Remove credit', exact: true }),
  ).toBeEnabled();
  await capture(
    page,
    info,
    ['ATDD-028', 'ATDD-262'],
    'archived-person-credit-private',
  );
  await page.goto(`/titles/${movie.id}`);
  await expect(page.getByRole('heading', { level: 1 })).toHaveText(movie.title);
  await expect(
    page.getByRole('link', { name: person.name, exact: true }),
  ).toHaveCount(0);
  await page.goto(`/manage/movies/${movie.id}`);
  await credit
    .getByRole('button', { name: 'Remove credit', exact: true })
    .click();
  const dialog = page.getByRole('dialog');
  await expect(dialog).toContainText(
    'Restoring this person later cannot recreate this removed credit.',
  );
  await dialog
    .getByRole('button', { name: 'Remove credit', exact: true })
    .click();
  await expect(credit).toHaveCount(0);
  await page.reload();
  expect(
    (await currentCredits(page, movie.id)).some(
      (entry) => entry.personId === person.id,
    ),
  ).toBe(false);
  const retained = await readApi(
    page,
    'query RetainedPerson($id: UUID!) { managedPerson(id: $id) { id state } }',
    { id: person.id },
  );
  expect(retained.managedPerson).toMatchObject({
    id: person.id,
    state: 'ARCHIVED',
  });
  await page.goto('/manage/archives');
  await page.getByRole('tab', { name: 'People', exact: true }).click();
  await expect(page.getByRole('tabpanel')).not.toContainText(
    'Loading archives…',
  );
  const archived = page
    .getByRole('tabpanel')
    .locator('li')
    .filter({ hasText: person.name });
  while ((await archived.count()) === 0) {
    await expect(
      page.getByRole('button', { name: 'Load more archives', exact: true }),
    ).toBeVisible();
    await page
      .getByRole('button', { name: 'Load more archives', exact: true })
      .click();
    const more = page.getByRole('button', {
      name: 'Load more archives',
      exact: true,
    });
    await expect
      .poll(async () => (await more.count()) === 0 || (await more.isEnabled()))
      .toBe(true);
  }
  await archived.getByRole('button', { name: 'Restore', exact: true }).click();
  await expect(archived).toHaveCount(0);
  await page.goto(`/people/${person.id}`);
  await expect(page.getByRole('heading', { level: 1 })).toHaveText(person.name);
  await page.goto(`/manage/movies/${movie.id}`);
  await expect(credit).toHaveCount(0);
  expect(
    (await currentCredits(page, movie.id)).some(
      (entry) => entry.personId === person.id,
    ),
  ).toBe(false);
  await capture(
    page,
    info,
    ['ATDD-028', 'ATDD-262'],
    'removed-credit-not-recreated-after-restore',
  );
});
