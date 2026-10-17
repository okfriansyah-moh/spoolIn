/**
 * T013 – Discovery browser test fixture helpers.
 * Shared setup and assertion utilities for discovery, detail and people flows.
 */
import type { Page, BrowserContext } from '@playwright/test';
import { specimenPolicy } from '../../../../build-tools/verification-tests/frontend/src/routes/policy/fixture';

const ANONYMOUS_CSRF = 'B'.repeat(43);

function graphqlOk(data: Record<string, unknown>) {
  return {
    status: 200,
    contentType: 'application/graphql-response+json',
    body: JSON.stringify({ data }),
  };
}

function anonymousBootstrap() {
  const policy = specimenPolicy();
  return graphqlOk({
    session: {
      authenticated: false,
      role: null,
      accountId: null,
      displayName: null,
      expiresAt: null,
      csrfToken: ANONYMOUS_CSRF,
    },
    policy: {
      revision: policy.revision,
      textBounds: policy.textBounds,
      artwork: policy.artwork,
      requestLimits: policy.requestLimits,
    },
  });
}

function emptyTitles() {
  return graphqlOk({
    titles: {
      nodes: [],
      pageInfo: { endCursor: null, hasNextPage: false },
    },
  });
}

function emptyPeople() {
  return graphqlOk({
    people: {
      nodes: [],
      pageInfo: { endCursor: null, hasNextPage: false },
    },
  });
}

function sampleGenres() {
  return graphqlOk({
    genres: [{ id: '10000000-0000-4000-8000-000000000010', label: 'Drama' }],
  });
}

/** Controlled public catalog GraphQL for shell journeys without a live stack. */
export async function seedCatalogGraphql(page: Page): Promise<void> {
  await page.route('**/graphql', async (route) => {
    const body = route.request().postDataJSON() as {
      query?: string;
      variables?: Record<string, unknown>;
    };
    const query = body?.query ?? '';
    if (query.includes('AccountBootstrap')) {
      await route.fulfill(anonymousBootstrap());
      return;
    }
    if (query.includes('query Genres')) {
      await route.fulfill(sampleGenres());
      return;
    }
    if (query.includes('query Titles')) {
      await route.fulfill(emptyTitles());
      return;
    }
    if (query.includes('query Title')) {
      await route.fulfill(graphqlOk({ title: null }));
      return;
    }
    if (query.includes('query People')) {
      await route.fulfill(emptyPeople());
      return;
    }
    if (query.includes('query Person')) {
      await route.fulfill(graphqlOk({ person: null }));
      return;
    }
    if (query.includes('query PersonCredits')) {
      await route.fulfill(
        graphqlOk({
          personCredits: {
            nodes: [],
            pageInfo: { endCursor: null, hasNextPage: false },
          },
        }),
      );
      return;
    }
    await route.fulfill({
      status: 503,
      contentType: 'application/graphql-response+json',
      body: '{"errors":[{"message":"Unhandled fixture query","extensions":{"code":"UNAVAILABLE"}}]}',
    });
  });
}

/** Clear cookies for a fresh anonymous session. */
export async function freshSession(context: BrowserContext): Promise<void> {
  await context.clearCookies();
}

/** Navigate to the discovery page and wait for the shell to load. */
export async function goToDiscovery(page: Page): Promise<void> {
  await page.goto('/');
  await page.waitForLoadState('networkidle');
}

/** Navigate to the people list. */
export async function goToPeople(page: Page): Promise<void> {
  await page.goto('/people');
  await page.waitForLoadState('networkidle');
}

/** Navigate to a title detail page by ID. */
export async function goToTitle(page: Page, id: string): Promise<void> {
  await page.goto(`/titles/${encodeURIComponent(id)}`);
  await page.waitForLoadState('networkidle');
}

/** Navigate to a person detail page by ID. */
export async function goToPerson(page: Page, id: string): Promise<void> {
  await page.goto(`/people/${encodeURIComponent(id)}`);
  await page.waitForLoadState('networkidle');
}

/** Expect the discovery page headline is visible. */
export async function expectDiscoveryHeadline(page: Page): Promise<void> {
  const h1 = page.getByRole('heading', { level: 1 });
  await h1.waitFor({ state: 'visible' });
}

/** Assert the page title contains the expected string. */
export async function assertPageTitle(page: Page, expected: string): Promise<void> {
  await page.waitForFunction(
    (t: string) => document.title.includes(t),
    expected,
    { timeout: 5000 },
  );
}

/** Assert a status region is visible (loading / live feedback). */
export async function assertStatusVisible(page: Page, text: RegExp | string): Promise<void> {
  const status = page.locator('[role="status"], [role="alert"]').filter({ hasText: text });
  await status.first().waitFor({ state: 'visible' });
}

/** Assert navigation breadcrumb contains catalog link. */
export async function assertCatalogLink(page: Page): Promise<void> {
  const link = page.getByRole('link', { name: /catalog/i }).first();
  await link.waitFor({ state: 'visible' });
}

/** Check search input exists and is functional. */
export async function assertSearchInput(page: Page): Promise<void> {
  const input = page.getByLabel(/search titles/i);
  await input.waitFor({ state: 'visible' });
}
