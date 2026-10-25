import {
  cleanup,
  fireEvent,
  render,
  screen,
  waitFor,
} from '@testing-library/svelte';
import { afterEach, beforeEach, expect, it, vi } from 'vitest';
import { specimenPolicy } from '../../../build-tools/verification-tests/frontend/src/routes/policy/fixture';
import { SessionController } from '../lib/features/session/controller.svelte';
import { sessionContext } from '../lib/features/session/context';
import NewMovie from './manage/movies/new/+page.svelte';
import EditMovie from './manage/movies/[id]/+page.svelte';
import NewPerson from './manage/people/new/+page.svelte';
import EditPerson from './manage/people/[id]/+page.svelte';
import CreatedRecord from '../lib/features/management/CreatedRecordNotice.svelte';

const browser = vi.hoisted(() => ({
  page: {
    params: { id: '00000000-0000-4000-8000-000000000001' },
    url: new URL('https://localhost/'),
    state: {},
  },
  goto: vi.fn(async () => {}),
  beforeNavigate: vi.fn(),
  afterNavigate: vi.fn(),
}));
vi.mock('$app/state', () => browser);
vi.mock('$app/paths', () => ({
  resolve: (path: string, values: Record<string, string> = {}) =>
    Object.entries(values).reduce(
      (result, [key, value]) => result.replace(`[${key}]`, value),
      path,
    ),
}));
const id = '00000000-0000-4000-8000-000000000001';
const genreId = '00000000-0000-4000-8000-000000000010';
const restoreId = '00000000-0000-4000-8000-000000000020';
const policy = specimenPolicy();
const pageInfo = { hasNextPage: false, endCursor: null };
const movie = {
  id,
  kind: 'MOVIE',
  state: 'ACTIVE',
  title: 'Saved film',
  originalTitle: null,
  synopsis: 'A saved story.',
  release: { year: 2000, month: null, day: null },
  runtimeMinutes: null,
  originalLanguage: null,
  synopsisLanguage: 'und',
  countries: [],
  genres: [{ id: genreId, label: 'Drama' }],
  artwork: null,
  version: '3',
};
const person = {
  id,
  name: 'Saved person',
  biography: 'A biography.',
  state: 'ACTIVE',
  version: '3',
};
let session: SessionController;
let role: 'MAINTAINER' | 'VIEWER';
let bootstrapFailure: boolean;
let reads: Record<string, unknown>;
let fetcher: ReturnType<typeof vi.fn<typeof fetch>>;
const originalScroll = Object.getOwnPropertyDescriptor(
  HTMLElement.prototype,
  'scrollIntoView',
);

function bootstrap() {
  return {
    policy,
    session: {
      authenticated: true,
      role,
      accountId: id,
      displayName: 'Editor',
      expiresAt: '2099-01-01T00:00:00Z',
      csrfToken: 'A'.repeat(43),
    },
  };
}
beforeEach(async () => {
  role = 'MAINTAINER';
  bootstrapFailure = false;
  browser.page.url = new URL('https://localhost/');
  browser.page.params.id = id;
  browser.page.state = {};
  browser.goto.mockReset();
  browser.goto.mockResolvedValue(undefined);
  reads = {
    genres: [{ id: genreId, label: 'Drama' }],
    drafts: [],
    managedTitle: movie,
    managedPerson: person,
    managedCredits: { nodes: [], pageInfo },
    creditRoles: [
      {
        id: genreId,
        label: 'Director',
        category: 'CREATOR',
        allowsCharacters: false,
      },
    ],
    managedPeople: { nodes: [person], pageInfo },
  };
  fetcher = vi.fn<typeof fetch>(async (_url, init) => {
    const body = JSON.parse(String(init?.body));
    if (body.query.includes('AccountBootstrap'))
      return new Response(JSON.stringify({ data: bootstrap() }), {
        status: bootstrapFailure ? 503 : 200,
      });
    const match = body.query.match(/mutation\s+(\w+)/);
    if (match) {
      if (match[1] === 'CreateMovie' || match[1] === 'UpdateMovie')
        reads.managedTitle = { ...movie, version: '4' };
      const field = body.query.includes('saveDraft(')
        ? 'saveDraft'
        : body.query.includes('discardDraft(')
          ? 'discardDraft'
          : match[1].charAt(0).toLowerCase() + match[1].slice(1);
      const operation = {
        id: body.variables.operationId,
        state: 'SUCCEEDED',
        resultId:
          field === 'saveDraft' || field === 'discardDraft'
            ? body.variables.id
            : id,
        version: '4',
        resultExpired: false,
        problem: null,
      };
      return new Response(
        JSON.stringify({
          data: { [field]: { operation, problem: null, discarded: true } },
        }),
      );
    }
    return new Response(JSON.stringify({ data: reads }));
  });
  vi.stubGlobal('fetch', fetcher);
  session = new SessionController({ bootstrap: async () => bootstrap() });
  await session.check();
  Object.defineProperty(HTMLElement.prototype, 'scrollIntoView', {
    configurable: true,
    writable: true,
    value: vi.fn(),
  });
});
afterEach(() => {
  cleanup();
  session.dispose();
  vi.restoreAllMocks();
  vi.unstubAllGlobals();
  if (originalScroll)
    Object.defineProperty(
      HTMLElement.prototype,
      'scrollIntoView',
      originalScroll,
    );
  else Reflect.deleteProperty(HTMLElement.prototype, 'scrollIntoView');
});
const context = () => new Map([[sessionContext, session]]);

it('creates a movie through the real editor and moves to its credits step', async () => {
  render(NewMovie, { context: context() });
  await screen.findByRole('form', { name: 'Create movie' });
  await fireEvent.input(screen.getByLabelText('Title · Required'), {
    target: { value: 'A new film' },
  });
  await fireEvent.input(screen.getByLabelText('Synopsis · Required'), {
    target: { value: 'A story about a new film.' },
  });
  await fireEvent.input(screen.getByLabelText('Release year · Required'), {
    target: { value: '2024' },
  });
  await fireEvent.click(screen.getByText('Choose genres'));
  await fireEvent.click(screen.getByRole('checkbox', { name: 'Drama' }));
  await fireEvent.submit(screen.getByRole('form', { name: 'Create movie' }));
  await waitFor(() =>
    expect(browser.goto).toHaveBeenCalledWith(
      `/manage/movies/${id}?step=credits&created=1`,
      { replaceState: true },
    ),
  );
  const mutation = fetcher.mock.calls
    .map(([, init]) => JSON.parse(String(init?.body)))
    .find((body) => body.query.includes('mutation CreateMovie'));
  expect(mutation.variables.input.title).toBe('A new film');
  expect(mutation.variables.input.genreIds).toEqual([genreId]);
});

it('does not submit an empty movie and shows actionable validation', async () => {
  render(NewMovie, { context: context() });
  await screen.findByRole('form', { name: 'Create movie' });
  await fireEvent.submit(screen.getByRole('form', { name: 'Create movie' }));
  await screen.findByText('Review these fields');
  expect(
    fetcher.mock.calls
      .map(([, init]) => JSON.parse(String(init?.body)))
      .some((body) => body.query.includes('mutation CreateMovie')),
  ).toBe(false);
});

it('creates a person and returns to the list with the new shared identity', async () => {
  render(NewPerson, { context: context() });
  await screen.findByRole('form', { name: 'Create person' });
  await fireEvent.input(screen.getByLabelText('Professional name · Required'), {
    target: { value: 'Cher' },
  });
  await fireEvent.input(screen.getByLabelText('Biography · Optional'), {
    target: { value: 'An artist.' },
  });
  await fireEvent.submit(screen.getByRole('form', { name: 'Create person' }));
  await waitFor(() =>
    expect(browser.goto).toHaveBeenCalledWith(`/manage/people?created=${id}`, {
      replaceState: true,
    }),
  );
});

it('requires a current Maintainer read context and supports retrying person bootstrap', async () => {
  role = 'VIEWER';
  render(NewPerson, { context: context() });
  expect(await screen.findByRole('alert')).toHaveProperty(
    'textContent',
    'Check your Maintainer session before loading this editor.',
  );
  role = 'MAINTAINER';
  await fireEvent.click(
    screen.getByRole('button', { name: 'Retry person editor' }),
  );
  expect(
    await screen.findByRole('form', { name: 'Create person' }),
  ).toBeTruthy();
});

it('recovers a person editor bootstrap read failure without submitting a mutation', async () => {
  bootstrapFailure = true;
  render(NewPerson, { context: context() });
  expect(await screen.findByRole('alert')).toHaveProperty(
    'textContent',
    'Person editor could not load. Retry the read.',
  );
  bootstrapFailure = false;
  await fireEvent.click(
    screen.getByRole('button', { name: 'Retry person editor' }),
  );
  expect(
    await screen.findByRole('form', { name: 'Create person' }),
  ).toBeTruthy();
});

it('finishes a saved movie and returns to the list with its retained restore context', async () => {
  browser.page.url = new URL(
    `https://localhost/manage/movies/${id}?return=${restoreId}`,
  );
  render(EditMovie, { context: context() });
  await screen.findByRole('form', { name: 'Edit movie' });
  expect(screen.getByLabelText('Title · Required')).toHaveProperty(
    'value',
    'Saved film',
  );
  await fireEvent.click(screen.getByRole('button', { name: 'Save movie' }));
  await waitFor(() =>
    expect(browser.goto).toHaveBeenCalledWith(
      `/manage/movies?saved=${id}&restore=${restoreId}`,
    ),
  );
});

it('finishes a saved person and keeps the previous list restore token', async () => {
  browser.page.url = new URL(
    `https://localhost/manage/people/${id}?return=${restoreId}`,
  );
  render(EditPerson, { context: context() });
  await screen.findByRole('form', { name: 'Edit person' });
  await fireEvent.click(screen.getByRole('button', { name: 'Save person' }));
  await waitFor(() =>
    expect(browser.goto).toHaveBeenCalledWith(
      `/manage/people?saved=${id}&restore=${restoreId}`,
    ),
  );
});

it('movie completion navigation failure retains the saved record and offers retry', async () => {
  browser.goto.mockRejectedValueOnce(new Error('navigation unavailable'));
  render(EditMovie, { context: context() });
  await screen.findByRole('form', { name: 'Edit movie' });
  await fireEvent.click(screen.getByRole('button', { name: 'Save movie' }));
  expect(await screen.findByText(/Completion could not finish/)).toBeTruthy();
  expect(screen.getByLabelText('Title · Required')).toHaveProperty(
    'value',
    'Saved film',
  );
  await fireEvent.click(screen.getByRole('button', { name: 'Save movie' }));
  await waitFor(() => expect(browser.goto).toHaveBeenCalledTimes(2));
});

it.each([
  [EditMovie, 'This movie link is not valid.'],
  [EditPerson, 'This person link is not valid.'],
] as const)(
  'rejects an invalid management identity before reading it',
  async (component, message) => {
    browser.page.params.id = 'invalid';
    render(component, { context: context() });
    expect(await screen.findByRole('alert')).toHaveProperty(
      'textContent',
      message,
    );
    expect(fetcher).not.toHaveBeenCalled();
  },
);

it('loads a saved movie notice and links back to the same identity and list context', async () => {
  browser.page.url = new URL(`https://localhost/manage/movies?saved=${id}`);
  render(CreatedRecord, {
    props: { kind: 'movie', returnToken: restoreId },
    context: context(),
  });
  await screen.findByText('Saved film is saved.');
  expect(
    screen.getByRole('link', { name: 'Edit movie' }).getAttribute('href'),
  ).toBe(`/manage/movies/${id}?return=${restoreId}`);
  await waitFor(() =>
    expect(document.activeElement).toBe(
      screen.getByRole('region', { name: 'Movie saved' }),
    ),
  );
});

it('retries an unavailable created-person read without hiding the management list', async () => {
  browser.page.url = new URL(`https://localhost/manage/people?created=${id}`);
  reads.managedPerson = null;
  render(CreatedRecord, { props: { kind: 'person' }, context: context() });
  await screen.findByText('Saved record could not be loaded');
  reads.managedPerson = person;
  await fireEvent.click(
    screen.getByRole('button', { name: 'Retry saved record' }),
  );
  expect(await screen.findByText('Saved person is saved.')).toBeTruthy();
});
