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
import ProposalPage from './manage/proposals/[id]/+page.svelte';

const browser = vi.hoisted(() => ({
  page: {
    params: { id: '00000000-0000-4000-8000-000000000001' },
    url: new URL('https://localhost/?revision=1'),
    state: {},
  },
  goto: vi.fn(async () => {}),
  beforeNavigate: vi.fn(),
  afterNavigate: vi.fn(),
}));
vi.mock('$app/state', () => browser);
vi.mock('$app/paths', () => ({
  resolve: (path: string, params: Record<string, string> = {}) =>
    Object.entries(params).reduce(
      (result, [key, value]) => result.replace(`[${key}]`, value),
      path,
    ),
}));
const id = '00000000-0000-4000-8000-000000000001';
const csrf = 'A'.repeat(43);
const policy = specimenPolicy();
const bootstrap = () => ({
  policy,
  session: {
    authenticated: true,
    role: 'MAINTAINER' as const,
    accountId: id,
    displayName: 'Reviewer',
    expiresAt: '2099-01-01T00:00:00Z',
    csrfToken: csrf,
  },
});
const proposal = () => ({
  proposalId: id,
  revision: 1,
  contentHash: 'a'.repeat(64),
  challenge: 'review-challenge',
  expiresAt: '2099-01-01T00:00:00Z',
  state: 'REVIEW',
  operationId: id,
  values: { title: 'Proposed film', release: { year: 2024, month: 3, day: 2 } },
  credits: [
    {
      personId: id,
      personLabel: 'Artist',
      roleLabel: 'Actor',
      characterNames: ['Hero'],
      billingOrder: 1,
    },
  ],
  sources: [
    {
      field: 'title',
      url: 'https://example.test/title',
      note: 'Reviewed',
      reuseBasis: 'Licensed',
    },
  ],
  poster: null,
});
let session: SessionController;
let loaded: ReturnType<typeof proposal>;
let readStatus: number;
let writeStatus: number;
let decision: { state: string; problem: { code: string } | null };
let posterResponses: unknown[];
let fetcher: ReturnType<typeof vi.fn<typeof fetch>>;
beforeEach(async () => {
  loaded = proposal();
  readStatus = 200;
  writeStatus = 200;
  decision = { state: 'SUCCEEDED', problem: null };
  posterResponses = [];
  browser.page.params.id = id;
  browser.page.url = new URL('https://localhost/?revision=1');
  browser.goto.mockReset();
  browser.goto.mockResolvedValue(undefined);
  fetcher = vi.fn<typeof fetch>(async (input, init) => {
    if (String(input).includes('graphql'))
      return new Response(JSON.stringify({ data: bootstrap() }));
    const write = init?.method === 'POST';
    const body = String(input).endsWith('/poster')
      ? posterResponses.shift()
      : write
        ? decision
        : loaded;
    return new Response(JSON.stringify(body), {
      status: write ? writeStatus : readStatus,
    });
  });
  vi.stubGlobal('fetch', fetcher);
  session = new SessionController({ bootstrap: async () => bootstrap() });
  await session.check();
});
afterEach(() => {
  cleanup();
  session.dispose();
  vi.restoreAllMocks();
  vi.unstubAllGlobals();
});
const open = () =>
  render(ProposalPage, { context: new Map([[sessionContext, session]]) });
const ready = () =>
  screen.findByRole('button', { name: 'Approve exact revision' });

it('shows the exact revision, sources and credit details before approving with its challenge', async () => {
  open();
  await ready();
  expect(screen.getByText('Proposed film')).toBeTruthy();
  expect(screen.getByText('2024-03-02')).toBeTruthy();
  expect(
    screen
      .getByRole('link', { name: 'https://example.test/title' })
      .getAttribute('href'),
  ).toBe('https://example.test/title');
  expect(screen.getByText(/Artist — Actor/)).toBeTruthy();
  await fireEvent.click(await ready());
  await screen.findByText(/Movie created and its poster attached/);
  const call = fetcher.mock.calls.find(([url]) =>
    String(url).endsWith('/approve'),
  )!;
  expect(JSON.parse(String(call[1]?.body))).toEqual({
    revision: 1,
    contentHash: loaded.contentHash,
    challenge: loaded.challenge,
  });
  expect(call[1]?.headers).toMatchObject({ 'X-CSRF-Token': csrf });
  expect((await ready()).hasAttribute('disabled')).toBe(true);
});
it('rejects the reviewed revision and prevents another decision', async () => {
  open();
  await ready();
  await fireEvent.click(screen.getByRole('button', { name: 'Reject' }));
  await screen.findByText('Proposal rejected. Nothing was published.');
  expect(
    screen.getByRole('button', { name: 'Reject' }).hasAttribute('disabled'),
  ).toBe(true);
});
it.each([
  ['PENDING', null, 'The operation is not yet complete.'],
  ['FAILED', { code: 'CONFLICT' }, 'The operation returned CONFLICT.'],
])(
  'keeps %s decisions locked until persisted state is reloaded',
  async (state, problem, message) => {
    decision = { state, problem };
    open();
    await fireEvent.click(await ready());
    await screen.findByText(new RegExp(message));
    expect((await ready()).hasAttribute('disabled')).toBe(true);
  },
);
it.each([
  [401, 'Your session expired.'],
  [403, 'This action requires a current Maintainer session.'],
  [409, 'This revision has changed.'],
  [503, 'The result is not confirmed.'],
])(
  'locks decisions after HTTP %s with recovery guidance',
  async (status, message) => {
    writeStatus = status;
    open();
    await fireEvent.click(await ready());
    await screen.findByText(new RegExp(message));
    expect((await ready()).hasAttribute('disabled')).toBe(true);
  },
);
it('refuses a response for a different revision', async () => {
  loaded.revision = 2;
  open();
  await screen.findByText(/This revision has changed/);
  expect(
    screen.queryByRole('button', { name: 'Approve exact revision' }),
  ).toBeNull();
});
it.each(['bad-id', '00000000-0000-4000-8000-000000000001'])(
  'rejects malformed proposal links before requesting a proposal: %s',
  async (linkId) => {
    browser.page.params.id = linkId;
    if (linkId === id)
      browser.page.url = new URL('https://localhost/?revision=0');
    open();
    await screen.findByText(/The proposal could not be loaded or changed/);
    expect(
      fetcher.mock.calls.some(([url]) =>
        String(url).includes('/api/movie-proposals/'),
      ),
    ).toBe(false);
  },
);
it('shows terminal proposals without upload or decision controls enabled', async () => {
  loaded.state = 'APPROVED';
  loaded.sources = [];
  loaded.credits = [];
  open();
  await ready();
  await screen.findByText(
    'This proposal is approved. Approval and replacement are unavailable.',
  );
  expect((await ready()).hasAttribute('disabled')).toBe(true);
  expect(screen.queryByLabelText('Upload a poster')).toBeNull();
  expect(screen.getByText('No source evidence was provided.')).toBeTruthy();
});
it('resumes a processing poster with the same payload and opens the new revision', async () => {
  posterResponses = [{ state: 'PROCESSING' }, { ...proposal(), revision: 2 }];
  open();
  await ready();
  await fireEvent.change(screen.getByLabelText('Upload a poster'), {
    target: {
      files: [new File(['image'], 'poster.png', { type: 'image/png' })],
    },
  });
  await fireEvent.input(screen.getByLabelText('Image source URL'), {
    target: { value: ' https://example.test/poster ' },
  });
  await fireEvent.input(screen.getByLabelText('Permission or reuse basis'), {
    target: { value: ' Licensed ' },
  });
  await fireEvent.click(
    screen.getByRole('button', { name: 'Upload and revise proposal' }),
  );
  const resume = await screen.findByRole('button', {
    name: 'Check poster processing',
  });
  expect((await ready()).hasAttribute('disabled')).toBe(true);
  await fireEvent.click(resume);
  await waitFor(() =>
    expect(browser.goto).toHaveBeenCalledWith(
      `/manage/proposals/${id}?revision=2`,
    ),
  );
  const calls = fetcher.mock.calls.filter(([url]) =>
    String(url).endsWith('/poster'),
  );
  expect(calls).toHaveLength(2);
  expect(calls[1]?.[1]?.body).toBe(calls[0]?.[1]?.body);
  expect(JSON.parse(String(calls[0]?.[1]?.body))).toMatchObject({
    revision: 1,
    sourceUrl: 'https://example.test/poster',
    reuseBasis: 'Licensed',
    content: 'aW1hZ2U=',
    mediaType: 'image/png',
  });
});
it('does not accept an unchanged revision after poster upload', async () => {
  posterResponses = [proposal()];
  open();
  await ready();
  await fireEvent.change(screen.getByLabelText('Upload a poster'), {
    target: {
      files: [new File(['image'], 'poster.png', { type: 'image/png' })],
    },
  });
  await fireEvent.click(
    screen.getByRole('button', { name: 'Upload and revise proposal' }),
  );
  await screen.findByText(/The result is not confirmed/);
  expect(browser.goto).not.toHaveBeenCalled();
});
