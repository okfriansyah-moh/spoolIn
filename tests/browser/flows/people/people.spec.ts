/**
 * T019 – People management browser journey tests.
 */
import { test, expect } from '@playwright/test';
import {
  freshSession,
  goToCreatePerson,
  goToPeopleList,
  seedMaintainerSession,
} from './fixture';

test('T019-people: management list shows create action', async ({ page, context }) => {
  await freshSession(context);
  await seedMaintainerSession(page, 'movies-active');
  await goToPeopleList(page);
  await expect(page.getByRole('heading', { level: 1, name: 'People' })).toBeVisible();
  await expect(page.getByRole('link', { name: 'Create person' })).toBeVisible();
});

test('T019-create: editor exposes required professional name field', async ({ page, context }) => {
  await freshSession(context);
  await seedMaintainerSession(page, 'movies-active');
  await goToCreatePerson(page);
  await expect(page.getByRole('heading', { level: 1, name: 'Create person' })).toBeVisible();
  await expect(
    page.getByRole('textbox', { name: /Professional name/i }),
  ).toBeVisible();
  await expect(page.getByRole('button', { name: 'Save person' })).toBeVisible();
});
