import {
  cleanup,
  fireEvent,
  render,
  screen,
  waitFor,
  within,
} from '@testing-library/svelte';
import { afterEach, beforeEach, expect, it, vi } from 'vitest';
import { specimenPolicy } from '../../../build-tools/verification-tests/frontend/src/routes/policy/fixture';
import { SessionController } from '../lib/features/session/controller.svelte';
import { sessionContext } from '../lib/features/session/context';
import Archives from './manage/archives/+page.svelte';
import Audit from './manage/audit/+page.svelte';
const id = '00000000-0000-4000-8000-000000000001';
const other = '00000000-0000-4000-8000-000000000002';
const policy = specimenPolicy();
const dialogMethods = ['showModal', 'close'] as const;
const originalDialogMethods = dialogMethods.map((name) =>
  Object.getOwnPropertyDescriptor(HTMLDialogElement.prototype, name),
);
let session: SessionController;
let role: 'MAINTAINER' | 'VIEWER';
let listFailure: boolean;
let writeFailure: boolean;
let impactFailure: boolean;
let resultState: string;
let resultProblem: string | null;
let expired: boolean;
let more: boolean;
let empty: boolean;
let fetcher: ReturnType<typeof vi.fn<typeof fetch>>;
const bootstrap = () => ({
  policy,
  session: {
    authenticated: true,
    role,
    accountId: id,
    displayName: 'Editor',
    expiresAt: '2099-01-01T00:00:00Z',
    csrfToken: 'A'.repeat(43),
  },
});
function reply(data: unknown) {
  return new Response(JSON.stringify({ data }));
}
function mutationResponse(query: string, operationId: string) {
  if (writeFailure) return new Response('{}', { status: 503 });
  const field = query.includes('MovieLifecycle')
    ? 'movieLifecycle'
    : 'personLifecycle';
  return reply({
    [field]: {
      operation: {
        id: operationId,
        state: resultState,
        resultExpired: expired,
        problem: resultProblem ? { code: resultProblem } : null,
      },
      problem: null,
    },
  });
}
function auditRow(after: unknown) {
  return {
    id: after ? other : id,
    actorRef: null,
    action: 'UPDATE',
    subjectKind: 'MOVIE',
    subjectId: id,
    outcome: 'SUCCEEDED',
    at: '2026-10-25T12:00:00Z',
    changes: {
      originalTitle: { before: 'Old', after: 'New' },
      biography: { before: { valueOmitted: true, length: 99 }, after: null },
      note: 'Reviewed',
    },
  };
}
function listResponse(query: string, after: unknown) {
  if (listFailure) return new Response('{}', { status: 503 });
  const hasNextPage = more && !after;
  const pageInfo = { hasNextPage, endCursor: hasNextPage ? 'next-page' : null };
  if (query.includes('query Audit'))
    return reply({
      audit: { nodes: empty ? [] : [auditRow(after)], pageInfo },
    });
  const field = query.includes('ArchivedPeople')
    ? 'managedPeople'
    : 'managedTitles';
  const nodes = empty
    ? []
    : [
        {
          id: after ? other : id,
          title: after ? 'Second film' : 'Archived film',
          name: 'Archived artist',
          state: 'ARCHIVED',
          version: '3',
        },
      ];
  return reply({ [field]: { nodes, pageInfo } });
}
beforeEach(async () => {
  for (const name of dialogMethods)
    Object.defineProperty(HTMLDialogElement.prototype, name, {
      configurable: true,
      value: function (this: HTMLDialogElement) {
        this.open = name === 'showModal';
      },
    });
  role = 'MAINTAINER';
  listFailure = false;
  writeFailure = false;
  impactFailure = false;
  resultState = 'SUCCEEDED';
  resultProblem = null;
  expired = false;
  more = false;
  empty = false;
  fetcher = vi.fn<typeof fetch>(async (_url, init) => {
    const { query, variables } = JSON.parse(String(init?.body));
    if (query.includes('AccountBootstrap')) return reply(bootstrap());
    if (query.includes('mutation '))
      return mutationResponse(query, variables.operationId);
    if (query.includes('DeletionImpact'))
      return reply({
        deletionImpact: impactFailure
          ? null
          : {
              token: 'impact-proof',
              affectedCreditCount: 2,
              affectedTitles: ['Related film'],
            },
      });
    if (query.includes('CatalogOperation'))
      return reply({
        operation: {
          id: variables.id,
          state: resultState,
          resultExpired: expired,
          resultId: id,
          version: '4',
          problem: null,
        },
      });
    return listResponse(query, variables.after);
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
  dialogMethods.forEach((name, index) => {
    const original = originalDialogMethods[index];
    if (original)
      Object.defineProperty(HTMLDialogElement.prototype, name, original);
    else Reflect.deleteProperty(HTMLDialogElement.prototype, name);
  });
});
const context = () => new Map([[sessionContext, session]]);
const archive = async () => {
  render(Archives, { context: context() });
  await screen.findByText('Archived film');
};
const mutationCalls = () =>
  fetcher.mock.calls
    .map(([, init]) => JSON.parse(String(init?.body)))
    .filter(({ query }) => query.includes('mutation '));
it('restores the selected version and refreshes the archive after confirmation', async () => {
  await archive();
  await fireEvent.click(screen.getByRole('button', { name: 'Restore' }));
  await screen.findByText('The change is confirmed.');
  expect(mutationCalls()[0].variables).toMatchObject({
    id,
    expectedVersion: '3',
    action: 'RESTORE',
    confirmed: true,
  });
});
it('confirms a movie deletion separately before submitting it', async () => {
  await archive();
  await fireEvent.click(
    screen.getByRole('button', { name: 'Delete permanently' }),
  );
  const dialog = await screen.findByRole('dialog');
  expect(mutationCalls()).toHaveLength(0);
  await fireEvent.click(
    within(dialog).getByRole('button', { name: 'Permanently delete' }),
  );
  await screen.findByText('The change is confirmed.');
  expect(mutationCalls()[0].variables.action).toBe('DELETE');
});
it('loads complete person deletion impact and binds it to the confirmed request', async () => {
  await archive();
  await fireEvent.keyDown(screen.getByRole('tab', { name: 'Movies' }), {
    key: 'End',
  });
  await screen.findByText('Archived artist');
  expect(document.activeElement).toBe(
    screen.getByRole('tab', { name: 'People' }),
  );
  await fireEvent.click(
    screen.getByRole('button', { name: 'Delete permanently' }),
  );
  const dialog = await screen.findByRole('dialog');
  expect(within(dialog).getByText(/Related film/)).toBeTruthy();
  await fireEvent.click(
    within(dialog).getByRole('button', { name: 'Permanently delete' }),
  );
  await screen.findByText('The change is confirmed.');
  expect(mutationCalls()[0].variables.impactToken).toBe('impact-proof');
});
it('submits no deletion when its complete impact cannot be read', async () => {
  impactFailure = true;
  await archive();
  await fireEvent.click(screen.getByRole('tab', { name: 'People' }));
  await screen.findByText('Archived artist');
  await fireEvent.click(
    screen.getByRole('button', { name: 'Delete permanently' }),
  );
  await screen.findByText(
    'The complete deletion impact could not be loaded. No deletion was submitted.',
  );
  expect(mutationCalls()).toHaveLength(0);
});
it('retains an uncertain request for identical retry and then confirms it', async () => {
  writeFailure = true;
  await archive();
  await fireEvent.click(screen.getByRole('button', { name: 'Restore' }));
  await screen.findByText(/The result is uncertain/);
  writeFailure = false;
  await fireEvent.click(
    screen.getByRole('button', { name: 'Retry original request' }),
  );
  await screen.findByText('The change is confirmed.');
  expect(mutationCalls()[1].variables).toEqual(mutationCalls()[0].variables);
});
it('checks a pending original result before enabling another action', async () => {
  resultState = 'PENDING';
  await archive();
  await fireEvent.click(screen.getByRole('button', { name: 'Restore' }));
  await screen.findByText(/The result is pending/);
  expect(
    screen.getByRole('tab', { name: 'People' }).hasAttribute('disabled'),
  ).toBe(true);
  resultState = 'SUCCEEDED';
  await fireEvent.click(
    screen.getByRole('button', { name: 'Check original result' }),
  );
  await screen.findByText('The change is confirmed.');
  expect(mutationCalls()).toHaveLength(1);
});
it('requires inspection after an expired original result', async () => {
  resultState = 'PENDING';
  await archive();
  await fireEvent.click(screen.getByRole('button', { name: 'Restore' }));
  await screen.findByText(/The result is pending/);
  expired = true;
  await fireEvent.click(
    screen.getByRole('button', { name: 'Check original result' }),
  );
  await screen.findByText(/The original result has expired/);
});
it('discards an impact-changed intent and requires a fresh confirmation', async () => {
  resultProblem = 'IMPACT_CHANGED';
  resultState = 'FAILED';
  await archive();
  await fireEvent.click(screen.getByRole('button', { name: 'Restore' }));
  await screen.findByText(/The deletion impact changed/);
  expect(
    screen.queryByRole('button', { name: 'Retry original request' }),
  ).toBeNull();
});
it('appends another archive page without losing the first record', async () => {
  more = true;
  await archive();
  await fireEvent.click(
    screen.getByRole('button', { name: 'Load more archives' }),
  );
  await screen.findByText('Second film');
  expect(screen.getByText('Archived film')).toBeTruthy();
  expect(
    screen.queryByRole('button', { name: 'Load more archives' }),
  ).toBeNull();
});
it('retries a failed archive read', async () => {
  listFailure = true;
  render(Archives, { context: context() });
  await screen.findByText('Archives could not be loaded. Try again.');
  listFailure = false;
  await fireEvent.click(screen.getByRole('button', { name: 'Retry archives' }));
  await screen.findByText('Archived film');
});
it('shows an empty archive and denies Viewer access', async () => {
  empty = true;
  render(Archives, { context: context() });
  await screen.findByText('No archived records in this view.');
  cleanup();
  role = 'VIEWER';
  render(Archives, { context: context() });
  await screen.findByText('You do not have access to private archives.');
});
it('renders audit field changes and omitted values, then appends the next page', async () => {
  more = true;
  render(Audit);
  await screen.findByRole('region', { name: 'Catalog history' });
  expect(screen.getAllByText(/Before: Old; After: New/)).toHaveLength(2);
  expect(screen.getAllByText(/Value omitted. Retained summary/)).toHaveLength(
    2,
  );
  await fireEvent.click(
    screen.getByRole('button', { name: 'Load more history' }),
  );
  await waitFor(() => expect(screen.getAllByRole('row')).toHaveLength(3));
});
it('retries audit read failures and reports an empty history', async () => {
  listFailure = true;
  render(Audit);
  await screen.findByText('History could not be loaded. Retry the read.');
  empty = true;
  listFailure = false;
  await fireEvent.click(screen.getByRole('button', { name: 'Retry history' }));
  await screen.findByText('No history entries yet.');
});
it('does not request history for a Viewer', async () => {
  role = 'VIEWER';
  render(Audit);
  await screen.findByText('Sign in as a Maintainer to review history.');
  expect(
    fetcher.mock.calls.some(([, init]) =>
      JSON.parse(String(init?.body)).query.includes('query Audit'),
    ),
  ).toBe(false);
});
