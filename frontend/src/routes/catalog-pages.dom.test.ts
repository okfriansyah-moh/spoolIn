import { cleanup, fireEvent, render, screen } from '@testing-library/svelte';
import { afterEach, beforeEach, expect, it, vi } from 'vitest';
import { specimenPolicy } from '../../../build-tools/verification-tests/frontend/src/routes/policy/fixture';
import { SessionController } from '../lib/features/session/controller.svelte';
import { sessionContext } from '../lib/features/session/context';
import type { SessionBootstrap } from '../lib/features/session/types';
import appTemplate from '../app.html?raw';
import People from './people/+page.svelte';
import Person from './people/[id]/+page.svelte';
import Movies from './manage/movies/+page.svelte';
import ManagedPeople from './manage/people/+page.svelte';
import Audit from './manage/audit/+page.svelte';
import Archives from './manage/archives/+page.svelte';

const fixture = vi.hoisted(() => ({
  page: {
    params: { id: '00000000-0000-4000-8000-000000000001' },
    url: new URL('https://localhost/'),
    state: {},
  },
}));
vi.mock('$app/state', () => ({
  ...fixture,
  goto: vi.fn(async () => {}),
  beforeNavigate: vi.fn(),
  afterNavigate: vi.fn(),
}));
vi.mock('$app/paths', () => ({
  resolve: (path: string, params: Record<string, string> = {}) =>
    Object.entries(params).reduce(
      (result, [key, value]) =>
        result.replace(`[${key}]`, encodeURIComponent(value)),
      path,
    ),
}));

const id = '00000000-0000-4000-8000-000000000001';
const secondId = '00000000-0000-4000-8000-000000000002';
const pageInfo = { hasNextPage: false, endCursor: null };
let controller: SessionController | undefined;

function bootstrap(
  role: 'MAINTAINER' | 'VIEWER' | null = 'MAINTAINER',
): SessionBootstrap {
  return {
    policy: specimenPolicy(),
    session: {
      authenticated: role !== null,
      role,
      accountId: role ? id : null,
      displayName: role ? 'Editor' : null,
      expiresAt: role ? '2099-01-01T00:00:00Z' : null,
      csrfToken: 'A'.repeat(43),
    },
  };
}

function responses(
  data: Record<string, unknown>,
  role: 'MAINTAINER' | 'VIEWER' | null = 'MAINTAINER',
) {
  const fetcher = vi.fn<typeof fetch>(async (_url, init) => {
    const body = JSON.parse(String(init?.body));
    return new Response(
      JSON.stringify({
        data: body.query.includes('AccountBootstrap') ? bootstrap(role) : data,
      }),
    );
  });
  vi.stubGlobal('fetch', fetcher);
  return fetcher;
}

beforeEach(() => {
  fixture.page.url = new URL('https://localhost/');
  fixture.page.state = {};
  fixture.page.params.id = id;
});
afterEach(() => {
  cleanup();
  controller?.dispose();
  controller = undefined;
  vi.unstubAllGlobals();
});

it('renders distinct people with the same name and links by identity', async () => {
  responses({
    people: {
      nodes: [id, secondId].map((personId) => ({
        id: personId,
        name: 'Alex',
        biography: 'A biography',
        state: 'ACTIVE',
        version: '1',
      })),
      pageInfo,
    },
  });
  render(People);
  const links = await screen.findAllByRole('link', { name: 'Alex' });
  expect(links.map((link) => link.getAttribute('href'))).toEqual([
    `/people/${id}`,
    `/people/${secondId}`,
  ]);
});

it.each([
  ['', 'No people have been added yet.'],
  ['?q=Missing', 'No people match your search.'],
])(
  'distinguishes empty catalog from empty search %s',
  async (query, message) => {
    fixture.page.url = new URL(`https://localhost/people${query}`);
    responses({ people: { nodes: [], pageInfo } });
    render(People);
    expect(await screen.findByText(message)).toBeTruthy();
  },
);

it('recovers a failed people read through the visible retry control', async () => {
  const fetcher = responses({ people: { nodes: [], pageInfo } });
  fetcher.mockResolvedValueOnce(new Response('{}', { status: 503 }));
  render(People);
  expect(await screen.findByRole('alert')).toHaveProperty(
    'textContent',
    expect.stringContaining('People could not be loaded'),
  );
  await fireEvent.click(screen.getByRole('button', { name: 'Try again' }));
  expect(
    await screen.findByText('No people have been added yet.'),
  ).toBeTruthy();
});

it('shows a person with no credits without inventing a contribution', async () => {
  responses({
    person: {
      id,
      name: 'Alex',
      biography: 'A biography',
      state: 'ACTIVE',
      version: '1',
    },
    personCredits: { nodes: [], pageInfo },
  });
  render(Person);
  expect(await screen.findByRole('heading', { name: 'Alex' })).toBeTruthy();
  expect(await screen.findByText('No credits known.')).toBeTruthy();
  expect(screen.getByText('A biography')).toBeTruthy();
});

it('does not disclose an archived or missing person', async () => {
  responses({ person: null, personCredits: { nodes: [], pageInfo } });
  render(Person);
  expect(
    await screen.findByRole('heading', { name: 'This person is unavailable' }),
  ).toBeTruthy();
  expect(screen.queryByText('A biography')).toBeNull();
});

it('loads managed movies and keeps edit and public review destinations distinct', async () => {
  responses({
    managedTitles: {
      nodes: [
        { id, kind: 'MOVIE', state: 'ACTIVE', title: 'A movie', version: '1' },
      ],
      pageInfo,
    },
  });
  render(Movies);
  expect(await screen.findAllByText('A movie')).toHaveLength(2);
  for (const link of screen.getAllByRole('link', { name: 'Edit' }))
    expect(link.getAttribute('href')).toMatch(
      new RegExp(`^/manage/movies/${id}\\?return=`),
    );
  for (const link of screen.getAllByRole('link', { name: 'Review' }))
    expect(link.getAttribute('href')).toBe(`/titles/${id}`);
});

it('shows the managed-people empty result after authenticated bootstrap', async () => {
  responses({ managedPeople: { nodes: [], pageInfo } });
  render(ManagedPeople);
  expect(
    await screen.findByText('No active catalog people are available.'),
  ).toBeTruthy();
});

it('denies private history to a viewer without requesting audit rows', async () => {
  const fetcher = responses({}, 'VIEWER');
  render(Audit);
  expect(
    await screen.findByText('Sign in as a Maintainer to review history.'),
  ).toBeTruthy();
  expect(
    fetcher.mock.calls.every(([, init]) =>
      JSON.parse(String(init?.body)).query.includes('AccountBootstrap'),
    ),
  ).toBe(true);
});

it('shows an empty audit history only after a successful audit response', async () => {
  responses({ audit: { nodes: [], pageInfo } });
  render(Audit);
  expect(await screen.findByText('No history entries yet.')).toBeTruthy();
});

it('switches archive tabs while retaining a truthful empty result', async () => {
  responses({
    managedTitles: { nodes: [], pageInfo },
    managedPeople: { nodes: [], pageInfo },
  });
  controller = new SessionController({ bootstrap: async () => bootstrap() });
  await controller.check();
  render(Archives, { context: new Map([[sessionContext, controller]]) });
  expect(
    await screen.findByText('No archived records in this view.'),
  ).toBeTruthy();
  await fireEvent.click(screen.getByRole('tab', { name: 'People' }));
  expect(
    await screen.findByText('No archived records in this view.'),
  ).toBeTruthy();
  expect(
    screen.getByRole('tab', { name: 'People' }).getAttribute('aria-selected'),
  ).toBe('true');
});

it('denies archive access before fetching private records', async () => {
  const fetcher = responses({}, 'VIEWER');
  controller = new SessionController({
    bootstrap: async () => bootstrap('VIEWER'),
  });
  await controller.check();
  render(Archives, { context: new Map([[sessionContext, controller]]) });
  expect(await screen.findByRole('alert')).toHaveProperty(
    'textContent',
    'You do not have access to private archives.',
  );
  expect(
    fetcher.mock.calls.every(([, init]) =>
      JSON.parse(String(init?.body)).query.includes('AccountBootstrap'),
    ),
  ).toBe(true);
});


it('reuses the initial shell title when moving between public and archive pages', async () => {
  const shell = new DOMParser().parseFromString(appTemplate, 'text/html');
  expect(shell.head.querySelectorAll('title')).toHaveLength(1);
  expect(shell.title).toBe('SpoolIn');
  const previousTitle = document.title;
  document.title = shell.title;
  const titleElement = document.head.querySelector('title');
  responses({
    people: { nodes: [], pageInfo },
    managedTitles: { nodes: [], pageInfo },
    managedPeople: { nodes: [], pageInfo },
  });
  try {
    const peoplePage = render(People);
    expect(await screen.findByRole('heading', { name: 'People', level: 1 })).toBeTruthy();
    expect(document.title).toBe('People · SpoolIn');
    peoplePage.unmount();
    controller = new SessionController({ bootstrap: async () => bootstrap() });
    await controller.check();
    render(Archives, { context: new Map([[sessionContext, controller]]) });
    expect(await screen.findByText('No archived records in this view.')).toBeTruthy();
    expect(document.title).toBe('Archives · SpoolIn');
    expect(document.head.querySelectorAll('title')).toHaveLength(1);
    expect(document.head.querySelector('title')).toBe(titleElement);
  } finally {
    document.title = previousTitle;
  }
});
