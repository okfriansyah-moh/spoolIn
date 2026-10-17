import { test, expect } from '@playwright/test';
import {
  FIXTURE_ARCHIVED_MOVIE,
  FIXTURE_ARCHIVED_PERSON,
  FIXTURE_DELETION_IMPACT,
  IRREVERSIBLE_CONFIRM_HEADING,
  freshSession,
  goToArchives,
  irreversibleConfirmCopy,
  personImpactSummary,
  seedMaintainerArchives,
  expectArchiveTabSelected,
} from './fixture';

test('T025-archives: maintainer tabs keep aria-selected state when switching', async ({
  page,
  context,
}) => {
  await freshSession(context);
  await seedMaintainerArchives(page, 'archives-empty');
  await goToArchives(page);
  await expect(page.getByRole('heading', { level: 1, name: 'Archives' })).toBeVisible();
  await expectArchiveTabSelected(page, 'Movies');
  await page.getByRole('tab', { name: 'People' }).click();
  await expectArchiveTabSelected(page, 'People');
  await expect(page.getByText('No archived records in this view.')).toBeVisible();
  await page.getByRole('tab', { name: 'Movies' }).click();
  await expectArchiveTabSelected(page, 'Movies');
  await expect(page.getByText('No archived records in this view.')).toBeVisible();
});

test('T025-archives: permanent delete dialog shows irreversible copy from fixture oracle', async ({
  page,
  context,
}) => {
  await freshSession(context);
  await seedMaintainerArchives(page, 'archives-ready');
  await goToArchives(page);
  await expect(page.getByText(FIXTURE_ARCHIVED_MOVIE.label)).toBeVisible();
  await page
    .getByRole('listitem')
    .filter({ hasText: FIXTURE_ARCHIVED_MOVIE.label })
    .getByRole('button', { name: 'Delete permanently' })
    .click();
  await expect(
    page.getByRole('heading', { name: IRREVERSIBLE_CONFIRM_HEADING }),
  ).toBeVisible();
  await expect(
    page.getByText(irreversibleConfirmCopy(FIXTURE_ARCHIVED_MOVIE.label)),
  ).toBeVisible();

  await page.getByRole('tab', { name: 'People' }).click();
  await expectArchiveTabSelected(page, 'People');
  await page
    .getByRole('listitem')
    .filter({ hasText: FIXTURE_ARCHIVED_PERSON.label })
    .getByRole('button', { name: 'Delete permanently' })
    .click();
  await expect(
    page.getByText(
      personImpactSummary(
        FIXTURE_DELETION_IMPACT.affectedCreditCount,
        FIXTURE_DELETION_IMPACT.affectedTitles,
      ),
    ),
  ).toBeVisible();
  await expect(
    page.getByRole('button', { name: 'Permanently delete' }),
  ).toBeEnabled();
});
