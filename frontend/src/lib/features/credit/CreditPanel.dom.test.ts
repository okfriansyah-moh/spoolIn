import {
  cleanup,
  fireEvent,
  render,
  screen,
  waitFor,
} from '@testing-library/svelte';
import { afterEach, expect, it, vi } from 'vitest';
import CreditPanel from './CreditPanel.svelte';
import { addCredit, fetchActivePeoplePage } from './api';
import type { DraftSource } from '../draft/types';
import { specimenPolicy } from '../../../../../build-tools/verification-tests/frontend/src/routes/policy/fixture';
const guard = vi.hoisted(() =>
  vi.fn<
    (event: { cancel: () => void; willUnload: boolean; to: null }) => void
  >(),
);
vi.mock('$app/navigation', () => ({
  beforeNavigate: (callback: typeof guard) =>
    guard.mockImplementation(callback),
  goto: vi.fn(),
}));
vi.mock('../movie/api', () => ({
  fetchManagedMovie: vi.fn(async () => ({
    id: 'title',
    state: 'ACTIVE',
    version: '2',
  })),
}));
vi.mock('../person/write/api', () => ({
  fetchManagedPerson: vi.fn(async () => ({
    id: 'person',
    name: 'Alex',
    state: 'ACTIVE',
  })),
}));
vi.mock('./api', () => ({
  fetchTitleCreditsPage: vi.fn(async () => ({
    nodes: [],
    pageInfo: { hasNextPage: false, endCursor: null },
  })),
  fetchCreditRoles: vi.fn(async () => [
    { id: 'actor', label: 'Actor', category: 'CAST', allowsCharacters: true },
  ]),
  fetchActivePeoplePage: vi.fn(async () => ({
    nodes: [{ id: 'person', name: 'Alex' }],
    next: null,
  })),
  addCredit: vi.fn(async () => ({
    state: 'SUCCEEDED',
    resultExpired: false,
    problem: null,
  })),
  updateCredit: vi.fn(),
  removeCredit: vi.fn(),
  recoverCredit: vi.fn(),
}));
afterEach(() => {
  cleanup();
  vi.clearAllMocks();
});
it('loads references, searches people, protects unfinished changes and confirms a saved credit', async () => {
  const policy = specimenPolicy();
  const source: DraftSource = {
    listDrafts: async () => [],
    recoverOperation: async () => null,
    saveDraft: async (operationId, draftId) => ({
      receipt: {
        operationId,
        state: 'SUCCEEDED',
        resultId: draftId,
        version: '1',
        resultExpired: false,
      },
      problemCode: null,
    }),
    discardDraft: async (operationId, draftId) => ({
      receipt: {
        operationId,
        state: 'SUCCEEDED',
        resultId: draftId,
        version: '2',
        resultExpired: false,
      },
      discarded: true,
      problemCode: null,
    }),
  };
  const onTitleVersionChange = vi.fn();
  render(CreditPanel, {
    props: {
      titleId: 'title',
      titleVersion: '1',
      policy,
      context: {
        accountId: 'account',
        csrf: 'A'.repeat(43),
        policyRevision: policy.revision,
      },
      draftSource: source,
      onTitleVersionChange,
    },
  });
  const add = await screen.findByRole('button', { name: '+ Add cast' });
  const cancel = vi.fn();
  const navigate = guard;
  navigate({ cancel, willUnload: false, to: null });
  expect(cancel).not.toHaveBeenCalled();
  await fireEvent.click(add);
  const person = screen.getByRole('combobox', { name: 'Person' });
  await fireEvent.focus(person);
  await waitFor(() => expect(fetchActivePeoplePage).toHaveBeenCalledTimes(2));
  await fireEvent.click(await screen.findByRole('button', { name: 'Alex' }));
  await fireEvent.change(screen.getByLabelText('Role'), {
    target: { value: 'actor' },
  });
  expect((screen.getByLabelText('Role') as HTMLSelectElement).value).toBe(
    'actor',
  );
  expect((person as HTMLInputElement).value).toBe('Alex');
  navigate({ cancel, willUnload: true, to: null });
  expect(cancel).toHaveBeenCalledOnce();
  await fireEvent.click(
    screen.getByRole('button', { name: /^Add credit$/ }),
  );
  await screen.findAllByText('Credit saved: Alex · Actor.');
  expect(addCredit).toHaveBeenCalledOnce();
  expect(onTitleVersionChange).toHaveBeenCalledWith('2');
});
