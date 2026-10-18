import { test, expect, signInMaintainer, createMovie, createPerson, capture } from './helpers';

for (const kind of ['movie', 'person'] as const) {
  test(`ATDD lifecycle ${kind}: archive, restore, cancel and permanent deletion persist`, async ({ page }, info) => {
    await signInMaintainer(page);
    const record = kind === 'movie' ? await createMovie(page) : await createPerson(page);
    const label = 'title' in record ? record.title : record.name;
    const editor = page.url();
    const archive = async () => {
      await page.getByRole('button', { name: `Archive ${kind}`, exact: true }).click();
      const confirm = page.getByRole('dialog');
      if (await confirm.count()) await confirm.getByRole('button', { name: `Archive ${kind}`, exact: true }).click();
      await page.getByRole('link', { name: 'Archives', exact: true }).click();
      if (kind === 'person') await page.getByRole('tab', { name: 'People', exact: true }).click();
    };
    await archive();
    const row = page.locator('li').filter({ hasText: label });
    await expect(row).toHaveCount(1);
    await capture(page, info, [kind === 'movie' ? 'ATDD-082' : 'ATDD-105'], `${kind}-archived`);
    await row.getByRole('button', { name: 'Restore', exact: true }).click();
    await expect(row).toHaveCount(0);
    await page.goto(editor);
    await page.reload();
    await expect(page.getByRole('heading', { level: 1 })).toHaveText(label);
    await capture(page, info, [kind === 'movie' ? 'ATDD-083' : 'ATDD-106'], `${kind}-restored`);
    await archive();
    await row.getByRole('button', { name: 'Delete permanently', exact: true }).click();
    await expect(page.getByRole('heading', { name: 'Confirm permanent deletion' })).toBeVisible();
    await page.getByRole('button', { name: 'Cancel', exact: true }).click();
    await expect(row).toHaveCount(1);
    await row.getByRole('button', { name: 'Delete permanently', exact: true }).click();
    await page.getByRole('button', { name: 'Permanently delete', exact: true }).click();
    await expect(row).toHaveCount(0);
    await page.reload();
    if (kind === 'person') await page.getByRole('tab', { name: 'People', exact: true }).click();
    await expect(row).toHaveCount(0);
    await capture(page, info, [kind === 'movie' ? 'ATDD-019' : 'ATDD-024'], `${kind}-deleted`);
  });
}
