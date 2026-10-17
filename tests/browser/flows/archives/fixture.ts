import { expect, type BrowserContext, type Page } from '@playwright/test';
import { formatDeletionImpactSummary } from '../../../../frontend/src/lib/features/archive/rules';
import {
  FIXTURE_ARCHIVED_MOVIE,
  FIXTURE_ARCHIVED_PERSON,
  FIXTURE_DELETION_IMPACT,
  seedMaintainerSession,
  type MaintainerGraphqlMode,
} from '../movies/fixture';

export async function freshSession(context: BrowserContext): Promise<void> {
  await context.clearCookies();
}

export const IRREVERSIBLE_CONFIRM_HEADING = 'Confirm permanent deletion';

export function irreversibleConfirmCopy(label: string): string {
  return `This action is irreversible for ${label}.`;
}

export const personImpactSummary = formatDeletionImpactSummary;

export { FIXTURE_ARCHIVED_MOVIE, FIXTURE_ARCHIVED_PERSON, FIXTURE_DELETION_IMPACT };

export async function seedMaintainerArchives(
  page: Page,
  mode: MaintainerGraphqlMode = 'archives-ready',
): Promise<void> {
  await seedMaintainerSession(page, mode);
}

export async function goToArchives(page: Page): Promise<void> {
  await page.goto('/manage/archives');
  await page.waitForLoadState('networkidle');
}

export async function expectArchiveTabSelected(
  page: Page,
  name: 'Movies' | 'People',
): Promise<void> {
  await expect(page.getByRole('tab', { name })).toHaveAttribute('aria-selected', 'true');
}
