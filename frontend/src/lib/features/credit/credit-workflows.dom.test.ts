import { cleanup, fireEvent, render, screen, waitFor, within } from '@testing-library/svelte';
import { afterEach, beforeEach, expect, it, vi } from 'vitest';
import { specimenPolicy } from '../../../../../build-tools/verification-tests/frontend/src/routes/policy/fixture';
import CreditPanel from './CreditPanel.svelte';
import RoleCreator from './RoleCreator.svelte';
import { createDraftSource } from '../draft/api';
import type { DraftRecord } from '../draft/types';

const navigation = vi.hoisted(() => ({
  guard: vi.fn<(event: { cancel: () => void; willUnload: boolean; to: { url: URL } | null }) => void>(),
  goto: vi.fn(async () => {}),
}));
vi.mock('$app/navigation', () => ({
  beforeNavigate: (guard: typeof navigation.guard) => navigation.guard.mockImplementation(guard),
  afterNavigate: vi.fn(),
  goto: navigation.goto,
}));

const ids = {
  title: '10000000-0000-4000-8000-000000000001',
  person: '10000000-0000-4000-8000-000000000002',
  role: '10000000-0000-4000-8000-000000000003',
  credit: '10000000-0000-4000-8000-000000000004',
  custom: '10000000-0000-4000-8000-000000000005',
  draft: '10000000-0000-4000-8000-000000000006',
  secondPerson: '10000000-0000-4000-8000-000000000007',
  secondCredit: '10000000-0000-4000-8000-000000000008',
};
const actor = { id: ids.role, label: 'Actor', category: 'CAST', allowsCharacters: true };
const person = { id: ids.person, name: 'Alex', state: 'ACTIVE', biography: 'A performer', version: '1' };
const pageInfo = { hasNextPage: false, endCursor: null };
type Variables = Record<string, unknown>;
type Reply = (variables: Variables, ordinal: number) => unknown | Promise<unknown>;
type WireCredit = { id: string; personId: string; personName: string; version: string; characters: string[]; role: typeof actor };
function credit(id = ids.credit, personId = ids.person): WireCredit {
  return { id, personId, personName: personId === ids.person ? 'Alex' : 'Sam', version: '1', characters: ['Captain'], role: actor };
}
function receipt(id: unknown, state = 'SUCCEEDED', resultId: unknown = ids.credit, problem: unknown = null) {
  return { id, state, resultId, version: '2', resultExpired: false, problem };
}
function draft(overrides: Partial<DraftRecord> = {}): DraftRecord {
  return { id: ids.draft, targetKind: 'CREDIT', targetId: null, baseVersion: '2',
    values: { titleId: ids.title, personId: ids.person, roleId: ids.role, characters: ['Retained captain'] },
    operationId: null, version: '1', expiresAt: '2099-01-01T00:00:00Z', ...overrides };
}

/** GraphQL wire responses replace HTTP only; component APIs and draft controllers stay real. */
function boundary(overrides: Record<string, Reply> = {}) {
  const state = { credits: [] as WireCredit[], roles: [actor], people: [person], drafts: [] as DraftRecord[] };
  const calls: { operation: string; variables: Variables }[] = [];
  const counts = new Map<string, number>();
  vi.stubGlobal('fetch', vi.fn<typeof fetch>(async (_url, init) => {
    const request = JSON.parse(String(init?.body)) as { query: string; variables: Variables };
    const operation = /(?:query|mutation)\s+(\w+)/.exec(request.query)?.[1] ?? '';
    const variables = request.variables;
    calls.push({ operation, variables });
    const count = (counts.get(operation) ?? 0) + 1;
    counts.set(operation, count);
    let data: unknown;
    if (overrides[operation]) data = await overrides[operation](variables, count);
    else switch (operation) {
      case 'ManagedCredits': data = { managedCredits: { nodes: state.credits, pageInfo } }; break;
      case 'CreditRoles': data = { creditRoles: state.roles }; break;
      case 'ManagedPeople': data = { managedPeople: { nodes: state.people, pageInfo } }; break;
      case 'ManagedCreditPerson':
      case 'ManagedPerson': data = { managedPerson: { ...person, id: variables.id } }; break;
      case 'ManagedTitle': data = { managedTitle: { id: ids.title, kind: 'MOVIE', state: 'ACTIVE', title: 'The voyage', version: '2' } }; break;
      case 'DraftList': data = { drafts: state.drafts }; break;
      case 'SavePersonDraft': data = { saveDraft: { operation: receipt(variables.operationId, 'SUCCEEDED', variables.id), problem: null } }; break;
      case 'DiscardPersonDraft': data = { discardDraft: { discarded: true, operation: receipt(variables.operationId, 'SUCCEEDED', variables.id), problem: null } }; break;
      case 'RecoverDraftOperation': data = { operation: receipt(variables.id, 'SUCCEEDED', ids.draft) }; break;
      case 'AddCredit':
        state.credits = [credit()];
        data = { addCredit: { operation: receipt(variables.operationId), problem: null } }; break;
      case 'UpdateCredit': data = { updateCredit: { operation: receipt(variables.operationId), problem: null } }; break;
      case 'RemoveCredit':
        state.credits = [];
        data = { removeCredit: { operation: receipt(variables.operationId), problem: null } }; break;
      case 'CreditOperation': data = { operation: receipt(variables.id) }; break;
      case 'CreateCreditRole':
        state.roles = [...state.roles, { id: ids.custom, label: String(variables.label), category: String(variables.category), allowsCharacters: false }];
        data = { createRole: { operation: receipt(variables.operationId, 'SUCCEEDED', ids.custom), problem: null } }; break;
      case 'CreditRoleOperation': data = { operation: receipt(variables.id, 'SUCCEEDED', ids.custom) }; break;
      default: throw new Error(`Unconfigured HTTP operation ${operation}`);
    }
    return data instanceof Response ? data : new Response(JSON.stringify({ data }));
  }));
  return { state, calls };
}
function panel(options: { showSkip?: boolean } = {}) {
  const policy = specimenPolicy();
  const onFinish = vi.fn();
  const onTitleVersionChange = vi.fn();
  const result = render(CreditPanel, { props: {
    titleId: ids.title, titleVersion: '1', policy,
    context: { accountId: ids.person, csrf: 'A'.repeat(43), policyRevision: policy.revision },
    draftSource: createDraftSource(), onTitleVersionChange, onFinish, ...options,
  } });
  return { ...result, onFinish, onTitleVersionChange };
}
async function choosePersonAndRole() {
  await fireEvent.click(await screen.findByRole('button', { name: '+ Add cast' }));
  await fireEvent.focus(screen.getByRole('combobox', { name: 'Person' }));
  await fireEvent.click(await screen.findByRole('button', { name: 'Alex' }));
  await fireEvent.change(screen.getByLabelText('Role'), { target: { value: ids.role } });
}
async function submitRole() {
  const name = screen.getByLabelText('Custom role name · Required');
  await fireEvent.input(name, { target: { value: 'Stunt performer' } });
  await fireEvent.submit(name.closest('form')!);
}
function roleCreator(onCreated: (id: string) => Promise<void> = vi.fn(async (_id: string) => {})) {
  const policy = specimenPolicy();
  const result = render(RoleCreator, { props: { policy, context: { accountId: ids.person, csrf: 'A'.repeat(43), policyRevision: policy.revision }, inheritedCategory: 'CAST', onCreated } });
  result.container.querySelector('details')!.open = true;
  return { ...result, onCreated };
}
beforeEach(() => {
  navigation.guard.mockReset(); navigation.goto.mockClear();
  vi.spyOn(window, 'scrollTo').mockImplementation(() => {});
  if (!HTMLDialogElement.prototype.showModal) Object.defineProperty(HTMLDialogElement.prototype, 'showModal', { configurable: true, value(this: HTMLDialogElement) { this.open = true; } });
  if (!HTMLDialogElement.prototype.close) Object.defineProperty(HTMLDialogElement.prototype, 'close', { configurable: true, value(this: HTMLDialogElement) { this.open = false; } });
});
afterEach(() => { cleanup(); vi.restoreAllMocks(); vi.unstubAllGlobals(); });

it('backs up an added credit before saving, refreshes current values and finishes with a clean form', async () => {
  const wire = boundary(); const view = panel({ showSkip: true });
  await choosePersonAndRole();
  await fireEvent.input(screen.getByLabelText('Character names · one per line'), { target: { value: ' Captain\n Navigator ' } });
  await fireEvent.click(screen.getByRole('button', { name: /^Add credit$/ }));
  await screen.findByText('Credit saved: Alex · Actor.');
  const backup = wire.calls.find((call) => call.operation === 'SavePersonDraft')!;
  const command = wire.calls.find((call) => call.operation === 'AddCredit')!;
  expect(backup.variables.input).toMatchObject({ operationId: command.variables.operationId, values: { titleId: ids.title, characters: ['Captain', 'Navigator'] } });
  expect(command.variables).toMatchObject({ titleId: ids.title, expectedVersion: '1', input: { personId: ids.person, roleId: ids.role, characters: ['Captain', 'Navigator'] } });
  expect(wire.calls.some((call) => call.operation === 'DiscardPersonDraft')).toBe(true);
  expect(view.onTitleVersionChange).toHaveBeenCalledWith('2');
  await fireEvent.click(screen.getByRole('button', { name: 'Done' }));
  expect(view.onFinish).toHaveBeenCalledOnce();
});

it('keeps an uncertain original credit request stable across retry and receipt recovery', async () => {
  const wire = boundary({ AddCredit: (variables) => ({ addCredit: { operation: receipt(variables.operationId, 'PENDING'), problem: null } }) });
  panel(); await choosePersonAndRole();
  await fireEvent.click(screen.getByRole('button', { name: /^Add credit$/ }));
  await screen.findByText('Credit result not confirmed. Check the original result before repeating it.');
  await fireEvent.click(screen.getByRole('button', { name: 'Retry original credit' }));
  await waitFor(() => expect(wire.calls.filter((call) => call.operation === 'AddCredit')).toHaveLength(2));
  const sends = wire.calls.filter((call) => call.operation === 'AddCredit');
  expect(sends[0]?.variables).toEqual(sends[1]?.variables);
  await waitFor(() => expect((screen.getByRole('button', { name: 'Check original result' }) as HTMLButtonElement).disabled).toBe(false));
  await fireEvent.click(screen.getByRole('button', { name: 'Check original result' }));
  await screen.findByText('Credit saved: Alex · Actor.');
  expect(wire.calls.find((call) => call.operation === 'CreditOperation')?.variables.id).toBe(sends[0]?.variables.operationId);
});

it('edits an existing relationship with its own version and discards cancelled edits safely', async () => {
  const wire = boundary(); wire.state.credits = [credit()]; panel();
  await fireEvent.click(await screen.findByRole('button', { name: 'Edit credit' }));
  expect((screen.getByRole('combobox', { name: 'Person' }) as HTMLInputElement).disabled).toBe(true);
  await fireEvent.input(screen.getByLabelText('Character names · one per line'), { target: { value: 'Admiral' } });
  await fireEvent.click(screen.getByRole('button', { name: 'Save credit' }));
  await screen.findByText('Credit saved: Alex · Actor.');
  expect(wire.calls.find((call) => call.operation === 'UpdateCredit')?.variables).toMatchObject({ id: ids.credit, expectedVersion: '1', patch: { roleId: ids.role, characters: ['Admiral'] } });
  await fireEvent.click(screen.getByRole('button', { name: 'Edit credit' }));
  await fireEvent.click(screen.getByRole('button', { name: 'Cancel credit edit' }));
  const dialog = await screen.findByRole('dialog', { name: 'Discard this unsaved credit draft?' });
  await fireEvent.click(within(dialog).getByRole('button', { name: 'Discard credit draft' }));
  await screen.findByText('Credit draft discarded. Saved credits are unchanged.');
});

it('removes only the selected credit after confirmation and refreshes the remaining list', async () => {
  const wire = boundary(); wire.state.credits = [credit()]; panel();
  await fireEvent.click(await screen.findByRole('button', { name: 'Remove credit' }));
  const dialog = await screen.findByRole('dialog', { name: 'Remove this credit?' });
  await fireEvent.click(within(dialog).getByRole('button', { name: 'Remove credit' }));
  await screen.findByText('Credit removed.');
  expect(wire.calls.find((call) => call.operation === 'RemoveCredit')?.variables).toMatchObject({ id: ids.credit, expectedVersion: '1' });
  expect(screen.getByText('No credits yet for this movie.')).toBeTruthy();
});

it('requires a deliberate review before saving a stale retained credit draft', async () => {
  const wire = boundary(); wire.state.drafts = [draft({ baseVersion: '1' })]; const view = panel();
  await screen.findByText('Review the current credit');
  expect((screen.getByLabelText('Character names · one per line') as HTMLTextAreaElement).value).toBe('Retained captain');
  await fireEvent.click(screen.getByRole('checkbox', { name: 'I reviewed the current person, role and retained character names.' }));
  await fireEvent.click(screen.getByRole('button', { name: 'Use reviewed version for this credit' }));
  await screen.findByText('Current references reviewed. Save the retained credit deliberately.');
  expect(view.onTitleVersionChange).toHaveBeenCalledWith('2');
  await fireEvent.click(screen.getByRole('button', { name: /^Add credit$/ }));
  await screen.findByText('Credit saved: Alex · Actor.');
});

it('requires choosing between retained drafts and blocks leave until the chosen draft is discarded', async () => {
  const wire = boundary(); wire.state.drafts = [draft(), draft({ id: ids.custom })]; const view = panel();
  const choices = await screen.findByRole('region', { name: 'Retained credit drafts' });
  await fireEvent.click(within(choices).getAllByRole('button')[0]!);
  await screen.findByText('Saved credit draft recovered.');
  const cancel = vi.fn(); navigation.guard({ cancel, willUnload: false, to: { url: new URL('https://localhost/manage') } });
  expect(cancel).toHaveBeenCalledOnce();
  const dialog = await screen.findByRole('dialog', { name: 'Finish this credit before leaving?' });
  await fireEvent.click(within(dialog).getByRole('button', { name: 'Continue editing' }));
  await fireEvent.click(screen.getByRole('button', { name: 'Done' }));
  const finish = await screen.findByRole('dialog', { name: 'Finish this credit before leaving?' });
  await fireEvent.click(within(finish).getByRole('button', { name: 'Discard and leave' }));
  await waitFor(() => expect(view.onFinish).toHaveBeenCalledOnce());
  expect(navigation.goto).not.toHaveBeenCalled();
});

it('shows a failed reference load as unavailable and keeps credit changes locked', async () => {
  boundary({ CreditRoles: () => { throw new TypeError('network unavailable'); } }); panel();
  await screen.findByText('Credits could not be loaded. Reload before making changes.');
  expect((screen.getByRole('button', { name: '+ Add cast' }) as HTMLButtonElement).disabled).toBe(true);
});

it('requires checking archived person references before editing but permits deliberate credit removal', async () => {
  const wire = boundary({ ManagedCreditPerson: () => ({ managedPerson: { ...person, state: 'ARCHIVED' } }) });
  wire.state.credits = [credit()]; panel();
  await screen.findByText(/Private credit · hidden publicly/);
  expect((screen.getByRole('button', { name: 'Edit credit' }) as HTMLButtonElement).disabled).toBe(true);
  expect((screen.getByRole('button', { name: 'Remove credit' }) as HTMLButtonElement).disabled).toBe(false);
});

it('loads additional credit pages without duplicating relationships and retries refresh failures', async () => {
  let failRefresh = false;
  boundary({ ManagedCredits: (variables) => {
    if (failRefresh) throw new Error('network unavailable');
    return { managedCredits: { nodes: variables.after ? [credit(), credit(ids.secondCredit, ids.secondPerson)] : [credit()], pageInfo: { hasNextPage: !variables.after, endCursor: variables.after ? null : 'next-credit-page' } } };
  } }); panel();
  await fireEvent.click(await screen.findByRole('button', { name: 'Load more credits' }));
  await screen.findByText('Sam');
  expect(screen.getAllByRole('button', { name: 'Edit credit' })).toHaveLength(2);
  failRefresh = true;
  await fireEvent.click(screen.getByRole('button', { name: 'Refresh saved credits' }));
  await screen.findByText('Saved credits could not be refreshed. Your input is unchanged. Select Refresh saved credits to retry.');
  failRefresh = false;
  await fireEvent.click(screen.getByRole('button', { name: 'Refresh saved credits' }));
  await screen.findByText('Saved credits refreshed. No changes found.');
});

it('creates a custom role through the real API and then selects it without creating a credit', async () => {
  const wire = boundary(); const view = roleCreator(); await submitRole();
  await screen.findByText('Role ready. Choose a person, then select Add credit.');
  expect(view.onCreated).toHaveBeenCalledWith(ids.custom);
  expect(wire.calls.filter((call) => call.operation === 'CreateCreditRole')).toHaveLength(1);
  expect(wire.calls.some((call) => call.operation === 'AddCredit')).toBe(false);
});

it('retains a pending role operation for safe retry and explicit status recovery', async () => {
  const wire = boundary({ CreateCreditRole: (variables) => ({ createRole: { operation: receipt(variables.operationId, 'PENDING'), problem: null } }) });
  const view = roleCreator(); await submitRole();
  await screen.findByText(/Creation is not confirmed yet/);
  await fireEvent.click(screen.getByRole('button', { name: 'Safely retry creation' }));
  await waitFor(() => expect(wire.calls.filter((call) => call.operation === 'CreateCreditRole')).toHaveLength(2));
  const sends = wire.calls.filter((call) => call.operation === 'CreateCreditRole');
  expect(sends[0]?.variables).toEqual(sends[1]?.variables);
  await waitFor(() => expect((screen.getByRole('button', { name: 'Check creation status' }) as HTMLButtonElement).disabled).toBe(false));
  await fireEvent.click(screen.getByRole('button', { name: 'Check creation status' }));
  await screen.findByText('Role ready. Choose a person, then select Add credit.');
  expect(view.onCreated).toHaveBeenCalledWith(ids.custom);
});

it.each(['ROLE_EXISTS', 'FIELD_INVALID'])('allows role corrections after a confirmed %s rejection', async (code) => {
  boundary({ CreateCreditRole: () => ({ createRole: { operation: null, problem: { code } } }) }); roleCreator(); await submitRole();
  await screen.findByText(code === 'ROLE_EXISTS' ? 'This role already exists. Choose the existing role or correct its name.' : 'Role was not created. Review the name, category and current policy.');
  expect((screen.getByLabelText('Custom role name · Required') as HTMLInputElement).disabled).toBe(false);
});

it('refreshes a confirmed custom role without submitting a second creation request', async () => {
  const wire = boundary(); let attempts = 0;
  roleCreator(async () => { attempts++; if (attempts === 1) throw new Error('role list unavailable'); }); await submitRole();
  await screen.findByText(/The role was created, but the role list could not refresh/);
  await fireEvent.click(screen.getByRole('button', { name: 'Refresh role list' }));
  await screen.findByText('Role ready. Choose a person, then select Add credit.');
  expect(wire.calls.filter((call) => call.operation === 'CreateCreditRole')).toHaveLength(1);
  expect(attempts).toBe(2);
});
