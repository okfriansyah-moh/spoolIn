import type { BrowserContext, Page } from '@playwright/test';
import { specimenPolicy } from '../../../../build-tools/verification-tests/frontend/src/routes/policy/fixture';

export async function freshSession(context: BrowserContext): Promise<void> {
  await context.clearCookies();
}

export const MAINTAINER_CSRF = 'A'.repeat(43);

export const FIXTURE_ACTIVE_MOVIE = Object.freeze({
  id: '20000000-0000-4000-8000-000000000001',
  title: 'Fixture Last Light',
  version: '4',
});

export const FIXTURE_ARCHIVED_MOVIE = Object.freeze({
  id: '20000000-0000-4000-8000-000000000002',
  label: 'Archived Last Light',
  version: '2',
});

export const FIXTURE_ARCHIVED_PERSON = Object.freeze({
  id: '30000000-0000-4000-8000-000000000001',
  label: 'Alex Fixture',
  version: '3',
});

export const FIXTURE_DELETION_IMPACT = Object.freeze({
  token: 'impact-token-fixture',
  affectedCreditCount: 2,
  affectedTitles: ['Fixture Last Light', 'Letters from Tomorrow'],
});

function graphqlOk(data: Record<string, unknown>) {
  return {
    status: 200,
    contentType: 'application/graphql-response+json',
    body: JSON.stringify({ data }),
  };
}

function bootstrapPayload() {
  const policy = specimenPolicy();
  return graphqlOk({
    session: {
      authenticated: true,
      role: 'MAINTAINER',
      accountId: '10000000-0000-4000-8000-000000000010',
      displayName: 'Browser maintainer',
      expiresAt: '2099-01-01T00:00:00Z',
      csrfToken: MAINTAINER_CSRF,
    },
    policy: {
      revision: policy.revision,
      textBounds: policy.textBounds,
      artwork: policy.artwork,
      requestLimits: policy.requestLimits,
    },
  });
}

function managedTitlesActive() {
  return graphqlOk({
    managedTitles: {
      nodes: [
        {
          id: FIXTURE_ACTIVE_MOVIE.id,
          kind: 'MOVIE',
          state: 'ACTIVE',
          title: FIXTURE_ACTIVE_MOVIE.title,
          releaseContext: '2024',
          version: FIXTURE_ACTIVE_MOVIE.version,
          release: { year: 2024, month: null, day: null },
        },
      ],
      pageInfo: { endCursor: null, hasNextPage: false },
    },
  });
}

function managedTitlesArchived() {
  return graphqlOk({
    managedTitles: {
      nodes: [
        {
          id: FIXTURE_ARCHIVED_MOVIE.id,
          title: FIXTURE_ARCHIVED_MOVIE.label,
          state: 'ARCHIVED',
          version: FIXTURE_ARCHIVED_MOVIE.version,
        },
      ],
    },
  });
}

function managedPeopleArchived() {
  return graphqlOk({
    managedPeople: {
      nodes: [
        {
          id: FIXTURE_ARCHIVED_PERSON.id,
          name: FIXTURE_ARCHIVED_PERSON.label,
          state: 'ARCHIVED',
          version: FIXTURE_ARCHIVED_PERSON.version,
        },
      ],
    },
  });
}

function managedTitleEdit() {
  return graphqlOk({
    managedTitle: {
      id: FIXTURE_ACTIVE_MOVIE.id,
      kind: 'MOVIE',
      state: 'ACTIVE',
      title: FIXTURE_ACTIVE_MOVIE.title,
      originalTitle: null,
      synopsis: 'A fixture synopsis for artwork upload status.',
      genres: [{ id: '10000000-0000-4000-8000-000000000010', label: 'Drama' }],
      release: { year: 2024, month: null, day: null },
      releaseContext: '2024',
      artwork: null,
      version: FIXTURE_ACTIVE_MOVIE.version,
    },
  });
}

function genres() {
  return graphqlOk({
    genres: [{ id: '10000000-0000-4000-8000-000000000010', label: 'Drama' }],
  });
}

function deletionImpact() {
  return graphqlOk({
    deletionImpact: {
      targetId: FIXTURE_ARCHIVED_PERSON.id,
      token: FIXTURE_DELETION_IMPACT.token,
      affectedCreditCount: FIXTURE_DELETION_IMPACT.affectedCreditCount,
      affectedTitles: [...FIXTURE_DELETION_IMPACT.affectedTitles],
    },
  });
}

function emptyArchived(tab: 'movies' | 'people') {
  return tab === 'movies'
    ? graphqlOk({ managedTitles: { nodes: [] } })
    : graphqlOk({ managedPeople: { nodes: [] } });
}

export type MaintainerGraphqlMode =
  | 'movies-active'
  | 'archives-empty'
  | 'archives-ready';

/** Controlled Maintainer GraphQL for shell journeys without a live stack. */
export async function seedMaintainerSession(
  page: Page,
  mode: MaintainerGraphqlMode = 'movies-active',
): Promise<void> {
  await page.route('**/graphql', async (route) => {
    const body = route.request().postDataJSON() as { query?: string; variables?: Record<string, unknown> };
    const query = body?.query ?? '';
    if (query.includes('AccountBootstrap')) {
      await route.fulfill(bootstrapPayload());
      return;
    }
    if (query.includes('query Genres')) {
      await route.fulfill(genres());
      return;
    }
    if (query.includes('managedTitles')) {
      const archivedQuery = query.includes('managedTitles(state: ARCHIVED');
      const activeQuery =
        body.variables?.state === 'ACTIVE' || query.includes('managedTitles(state: ACTIVE');
      if (archivedQuery) {
        await route.fulfill(
          mode === 'archives-ready' ? managedTitlesArchived() : emptyArchived('movies'),
        );
        return;
      }
      if (activeQuery || mode === 'movies-active') {
        await route.fulfill(managedTitlesActive());
        return;
      }
      await route.fulfill(emptyArchived('movies'));
      return;
    }
    if (query.includes('managedPeople')) {
      const archivedQuery = query.includes('managedPeople(state: ARCHIVED');
      if (archivedQuery) {
        await route.fulfill(
          mode === 'archives-ready' ? managedPeopleArchived() : emptyArchived('people'),
        );
        return;
      }
      await route.fulfill(emptyArchived('people'));
      return;
    }
    if (query.includes('managedTitle(')) {
      await route.fulfill(managedTitleEdit());
      return;
    }
    if (query.includes('deletionImpact')) {
      await route.fulfill(deletionImpact());
      return;
    }
    if (query.includes('query DraftList')) {
      await route.fulfill(graphqlOk({ drafts: [] }));
      return;
    }
    await route.fulfill({
      status: 503,
      contentType: 'application/graphql-response+json',
      body: '{"errors":[{"message":"Unhandled fixture query","extensions":{"code":"UNAVAILABLE"}}]}',
    });
  });
}

export async function goToMovieList(page: Page): Promise<void> {
  await page.goto('/manage/movies');
  await page.waitForLoadState('networkidle');
}

export async function goToCreateMovie(page: Page): Promise<void> {
  await page.goto('/manage/movies/new');
  await page.waitForLoadState('networkidle');
}

export async function goToMovieEdit(page: Page, movieId = FIXTURE_ACTIVE_MOVIE.id): Promise<void> {
  await page.goto(`/manage/movies/${movieId}`);
  await page.waitForLoadState('networkidle');
}
