<script lang="ts">
  import { onMount } from 'svelte';
  import { appendDistinct } from '../../state/pagination';
  import Notice from '../../components/Notice.svelte';
  import Button from '../../components/Button.svelte';
  import RoleCreator from './RoleCreator.svelte';
  import ConfirmDialog from '../../components/ConfirmDialog.svelte';
  import { fetchManagedMovie } from '../movie/api';
  import { createTransport } from '../../transport/graphql';
  import type { Policy } from '../../policy/types';
  import type { WriteContext } from '../movie/types';
  import type { DraftSource, DraftRecord } from '../draft/types';
  import { fetchManagedPerson } from '../person/write/api';
  import { CreditDraftSync, type CreditDraftValues } from './draft-sync';
  import {
    type CreditCommandOutcome,
    addCredit,
    updateCredit,
    removeCredit,
    recoverCredit,
    fetchActivePeoplePage,
    fetchCreditRoles,
    fetchTitleCreditsPage,
  } from './api';
  import type { CreditRoleOption, ManagedCredit, PersonOption } from './types';
  import { validateCharacterNames } from './validation';

  let {
    titleId,
    titleVersion,
    policy,
    context,
    draftSource,
    onTitleVersionChange,
  }: {
    titleId: string;
    titleVersion: string;
    policy: Policy;
    context: WriteContext;
    draftSource: DraftSource;
    onTitleVersionChange: (version: string) => void;
  } = $props();

  let credits = $state<readonly ManagedCredit[]>([]);
  let roles = $state<readonly CreditRoleOption[]>([]);
  let people = $state<readonly PersonOption[]>([]);
  let loading = $state(true);
  let loadingPeople = $state(false);
  let loadingCredits = $state(false);
  let nextCredits = $state<string | null>(null);
  let creditPageError = $state('');
  let creditGeneration = 0;
  let creditAbort: AbortController | null = null;
  let awaitingDraft: DraftRecord | null = null;
  let nextPeople = $state<string | null>(null);
  let message = $state('');
  let personId = $state('');
  let roleId = $state('');
  let characterText = $state('');
  let submitting = $state(false);
  let editing = $state<ManagedCredit | null>(null);
  let removing = $state<ManagedCredit | null>(null);
  let pendingId = $state<string | null>(null);
  let pendingKind: 'save' | 'remove' = 'save';
  let retainedSubmissionDraft: DraftRecord | null = null;
  let retryOriginal = $state<(() => Promise<CreditCommandOutcome>) | null>(
    null,
  );

  const selectedRole = $derived(
    roles.find((role) => role.id === roleId) ?? null,
  );

  let draftSync = $state<CreditDraftSync | null>(null);
  let creditDraftChoices = $state<readonly DraftRecord[]>([]);
  let creditReviewRequired = $state(false);
  let reviewedCredit = $state(false);
  let creditDirty = $state(false);
  let discardingDraft = $state(false);
  let draftUnavailable = $state(false);
  let backup = $state({
    backed: false,
    unbacked: false,
    pendingOperationId: null as string | null,
  });

  function creditFormLocked(): boolean {
    return (
      submitting ||
      !!pendingId ||
      !!creditDraftChoices.length ||
      creditReviewRequired ||
      draftUnavailable
    );
  }

  function creditBaseVersion(): string {
    return editing?.version ?? titleVersion;
  }

  function autosaveCreditValues(): CreditDraftValues {
    return {
      titleId,
      personId,
      roleId,
      characters: selectedRole?.allowsCharacters
        ? characterText.split('\n')
        : [],
    };
  }

  $effect(() => {
    if (
      !creditDirty ||
      !draftSync ||
      loading ||
      creditFormLocked() ||
      !personId ||
      !roleId
    )
      return;
    draftSync.schedule(autosaveCreditValues(), context, creditBaseVersion());
    creditDirty = false;
  });

  onMount(() => {
    const transport = createTransport();
    const abort = new AbortController();
    const sync = new CreditDraftSync(draftSource, titleId, (next) => {
      backup = next;
    });
    draftSync = sync;
    void (async () => {
      try {
        const [loadedCredits, loadedRoles, loadedPeople] = await Promise.all([
          fetchTitleCreditsPage(
            transport,
            titleId,
            context.csrf,
            null,
            abort.signal,
          ),
          fetchCreditRoles(transport, abort.signal),
          fetchActivePeoplePage(transport, context.csrf, null, abort.signal),
        ]);
        if (abort.signal.aborted) return;
        credits = loadedCredits.nodes;
        nextCredits = loadedCredits.pageInfo.hasNextPage
          ? loadedCredits.pageInfo.endCursor
          : null;
        roles = loadedRoles;
        people = loadedPeople.nodes;
        nextPeople = loadedPeople.next;
        const drafts = await sync.load(context, abort.signal);
        if (abort.signal.aborted) return;
        if (drafts.length > 1) creditDraftChoices = drafts;
        else if (drafts[0]) await resumeCreditDraft(drafts[0]);
      } catch {
        if (!abort.signal.aborted) {
          draftUnavailable = true;
          message =
            'Credits could not be loaded. Reload before making changes.';
        }
      } finally {
        if (!abort.signal.aborted) loading = false;
      }
    })();
    return () => {
      abort.abort();
      creditAbort?.abort();
      creditGeneration += 1;
      sync.dispose();
    };
  });

  function draftValues(draft: DraftRecord): CreditDraftValues {
    const value = draft.values as Partial<CreditDraftValues>;
    if (
      value.titleId !== titleId ||
      typeof value.personId !== 'string' ||
      typeof value.roleId !== 'string' ||
      !Array.isArray(value.characters) ||
      !value.characters.every((name) => typeof name === 'string')
    ) {
      throw new Error('Saved credit draft shape unavailable.');
    }
    return {
      titleId,
      personId: value.personId,
      roleId: value.roleId,
      characters: value.characters,
    };
  }

  async function currentDraftReferences(values: CreditDraftValues) {
    const transport = createTransport();
    const [person, movie] = await Promise.all([
      fetchManagedPerson(transport, values.personId, context.csrf),
      fetchManagedMovie(transport, titleId, context.csrf),
    ]);
    return person?.state === 'ACTIVE' &&
      movie?.state === 'ACTIVE' &&
      roles.some((role) => role.id === values.roleId)
      ? { person, movie }
      : null;
  }

  async function resumeCreditDraft(draft: DraftRecord) {
    try {
      await restoreCreditDraft(draft);
    } catch {
      draftUnavailable = true;
      message =
        'Saved credit draft could not be checked. Reload before continuing.';
    }
  }

  async function reconcileStoredSubmission(
    draft: DraftRecord,
  ): Promise<boolean> {
    if (!draft.operationId) return true;
    pendingId = draft.operationId;
    retainedSubmissionDraft = draft;
    retryOriginal = null;
    const receipt = await recoverCredit(
      createTransport(),
      pendingId,
      context.csrf,
      new AbortController().signal,
    );
    if (receipt?.state === 'FAILED' && !receipt.resultExpired) {
      pendingId = null;
      retainedSubmissionDraft = null;
      return true;
    }
    await applyReceipt(receipt);
    return false;
  }

  function requireDraftTarget(
    draft: DraftRecord,
    values: CreditDraftValues,
  ): ManagedCredit | null | undefined {
    if (!draft.targetId) return null;
    const target = credits.find((credit) => credit.id === draft.targetId);
    if (!target && nextCredits) {
      awaitingDraft = draft;
      draftUnavailable = true;
      message =
        'Load more credits to check the exact saved credit before resuming this draft.';
      return undefined;
    }
    if (
      !target ||
      target.personId !== values.personId ||
      target.personArchived !== false
    )
      throw new Error('Saved credit reference unavailable.');
    return target;
  }

  function installDraftFields(
    draft: DraftRecord,
    values: CreditDraftValues,
    target: ManagedCredit | null,
    references: NonNullable<Awaited<ReturnType<typeof currentDraftReferences>>>,
  ) {
    const { person, movie } = references;
    editing = target;
    if (!people.some((entry) => entry.id === person.id))
      people = [...people, { id: person.id, name: person.name }];
    personId = values.personId;
    roleId = values.roleId;
    characterText = values.characters.join('\n');
    creditReviewRequired =
      draft.baseVersion !== (target?.version ?? movie.version);
    onTitleVersionChange(movie.version);
    message = creditReviewRequired
      ? 'The saved relationship changed. Review its current values and retained edits before continuing.'
      : 'Saved credit draft recovered.';
  }

  async function restoreCreditDraft(draft: DraftRecord) {
    if (!draftSync) return;
    creditDraftChoices = [];
    draftSync.adopt(draft);
    if (!(await reconcileStoredSubmission(draft))) return;
    const values = draftValues(draft);
    const references = await currentDraftReferences(values);
    if (!references) {
      draftUnavailable = true;
      message =
        'Saved credit references changed. Discard this draft or restore its references before adding a credit.';
      return;
    }
    const target = requireDraftTarget(draft, values);
    if (target === undefined) return;
    awaitingDraft = null;
    draftUnavailable = false;
    installDraftFields(draft, values, target, references);
    if (
      draft.operationId &&
      !(await clearRejectedCreditDraft(draft.baseVersion))
    )
      message =
        'Original failure confirmed. Retained edits are not backed up yet. Retry their backup before submitting.';
  }

  async function discardCreditDraft() {
    if (!draftSync || pendingId || submitting) return;
    submitting = true;
    try {
      if (!(await draftSync.discard(context))) {
        message =
          'Credit draft discard is not confirmed. Retry its original result.';
        return;
      }
      awaitingDraft = null;
      resetFields();
      creditReviewRequired = false;
      reviewedCredit = false;
      draftUnavailable = false;
      discardingDraft = false;
      backup = draftSync.snapshot;
      message = 'Credit draft discarded. Saved credits are unchanged.';
    } finally {
      submitting = false;
    }
  }

  async function backOriginalCredit(
    operationId: string,
    values: CreditDraftValues,
    version: string,
  ) {
    if (
      !draftSync ||
      !(await draftSync.prepareSubmission(
        operationId,
        values,
        context,
        version,
      ))
    ) {
      throw new Error('Credit draft is not backed up.');
    }
  }

  async function loadMorePeople() {
    if (!nextPeople || loadingPeople) return;
    loadingPeople = true;
    try {
      const page = await fetchActivePeoplePage(
        createTransport(),
        context.csrf,
        nextPeople,
      );
      const known = new Set(people.map((person) => person.id));
      people = [
        ...people,
        ...page.nodes.filter((person) => !known.has(person.id)),
      ];
      nextPeople = page.next;
    } catch {
      message =
        'More people could not be loaded. Retry before choosing a missing person.';
    } finally {
      loadingPeople = false;
    }
  }

  async function refreshCredits() {
    creditAbort?.abort();
    creditGeneration += 1;
    creditAbort = new AbortController();
    const signal = creditAbort.signal;
    const page = await fetchTitleCreditsPage(
      createTransport(),
      titleId,
      context.csrf,
      null,
      signal,
    );
    if (signal.aborted) return;
    credits = page.nodes;
    nextCredits = page.pageInfo.hasNextPage ? page.pageInfo.endCursor : null;
    loadingCredits = false;
    creditPageError = '';
  }

  async function checkCreditReferences() {
    loadingCredits = true;
    try {
      await refreshCredits();
    } catch {
      creditPageError =
        'Current credit references could not be loaded. Retry the read.';
    } finally {
      loadingCredits = false;
    }
  }

  function creditPageAvailable(): boolean {
    return !!nextCredits && !loadingCredits && !submitting && !pendingId;
  }

  async function loadMoreCredits() {
    if (!creditPageAvailable()) return;
    const generation = creditGeneration;
    creditAbort = new AbortController();
    const signal = creditAbort.signal;
    loadingCredits = true;
    creditPageError = '';
    try {
      const page = await fetchTitleCreditsPage(
        createTransport(),
        titleId,
        context.csrf,
        nextCredits,
        signal,
      );
      if (signal.aborted || generation !== creditGeneration) return;
      credits = appendDistinct(credits, page.nodes);
      nextCredits = page.pageInfo.hasNextPage ? page.pageInfo.endCursor : null;
      if (awaitingDraft) await resumeCreditDraft(awaitingDraft);
    } catch {
      if (!signal.aborted)
        creditPageError = 'More credits could not be loaded. Retry this page.';
    } finally {
      if (!signal.aborted) loadingCredits = false;
    }
  }

  async function refreshCreatedRole(id: string) {
    const current = await fetchCreditRoles(createTransport());
    if (!current.some((role) => role.id === id))
      throw new Error('Created role is not available yet.');
    roles = current;
    roleId = id;
    creditDirty = true;
  }

  function parseCreditCharacters(
    role: CreditRoleOption | undefined,
    text: string,
  ): string[] {
    if (!role?.allowsCharacters || !text.trim()) return [];
    return text
      .split('\n')
      .map((line) => line.trim())
      .filter(Boolean);
  }

  function resetFields() {
    creditDirty = false;
    editing = null;
    personId = '';
    roleId = '';
    characterText = '';
  }

  function editCredit(credit: ManagedCredit) {
    if (credit.personArchived !== false) {
      message =
        'Restore the person and check current references before editing this credit.';
      return;
    }
    if (
      !draftSync ||
      backup.backed ||
      backup.unbacked ||
      creditDirty ||
      personId ||
      roleId ||
      pendingId
    )
      return;
    draftSync.reset({ targetId: credit.id });
    creditDirty = false;
    editing = credit;
    personId = credit.personId;
    roleId = credit.role.id;
    characterText = credit.characters.join('\n');
  }

  function cancelCreditEdit() {
    discardingDraft = true;
  }

  function draftLabel(draft: DraftRecord): string {
    const values = draft.values as Partial<CreditDraftValues>;
    const person =
      people.find((entry) => entry.id === values.personId)?.name ??
      'Saved person';
    const role =
      roles.find((entry) => entry.id === values.roleId)?.label ?? 'saved role';
    const names = Array.isArray(values.characters)
      ? values.characters.join(', ')
      : '';
    return `${draft.targetId ? 'Edit' : 'Add'} ${person} · ${role}${names ? ` · ${names}` : ''} · expires ${draft.expiresAt}`;
  }

  function creditDraftBacked(): boolean {
    return !!draftSync?.snapshot.backed && !draftSync.snapshot.unbacked;
  }

  async function clearRejectedCreditDraft(
    baseVersion: string | null = editing?.version ?? titleVersion,
  ): Promise<boolean> {
    if (pendingKind === 'remove') return true;
    if (draftSync && personId && roleId)
      await draftSync.clearSubmission(
        {
          titleId,
          personId,
          roleId,
          characters: parseCreditCharacters(
            selectedRole ?? undefined,
            characterText,
          ),
        },
        context,
        baseVersion,
      );
    return creditDraftBacked();
  }

  function creditFailureMessage(code: string | undefined): string {
    return code === 'ALREADY_EXISTS' || code === 'CREDIT_DUPLICATE'
      ? 'Review the credit — no duplicate added.'
      : 'Changes were not saved. Review the current credit.';
  }

  async function applyCreditFailure(receipt: CreditCommandOutcome) {
    if (retainedSubmissionDraft) {
      await restoreCreditDraft(retainedSubmissionDraft);
      return;
    }
    if (!(await clearRejectedCreditDraft())) {
      message =
        'The original failure is confirmed, but draft recovery is not backed up. Check the original result again.';
      return;
    }
    pendingId = null;
    retryOriginal = null;
    message = creditFailureMessage(receipt.problem?.code);
  }

  function unresolvedCredit(receipt: CreditCommandOutcome | null): boolean {
    if (!receipt || receipt.state === 'PENDING') {
      message =
        'Credit result not confirmed. Check the original result before repeating it.';
      return true;
    }
    if (
      receipt.state !== 'REJECTED' &&
      (receipt.state === 'EXPIRED' || receipt.resultExpired)
    ) {
      message =
        'Original credit result expired. Review the current record before further changes.';
      return true;
    }
    return false;
  }

  async function applyConfirmedCredit() {
    const transport = createTransport();
    const movie = await fetchManagedMovie(transport, titleId, context.csrf);
    if (!movie || movie.state !== 'ACTIVE')
      throw new Error('Current movie unavailable.');
    await refreshCredits();
    onTitleVersionChange(movie.version);
    if (draftSync && !(await draftSync.discard(context))) {
      message =
        'Credit result confirmed, but its draft cleanup is not confirmed. Check the original result again.';
      return;
    }
    if (draftSync) backup = draftSync.snapshot;
    creditReviewRequired = false;
    reviewedCredit = false;
    draftUnavailable = false;
    pendingId = null;
    retainedSubmissionDraft = null;
    retryOriginal = null;
    resetFields();
    message = pendingKind === 'remove' ? 'Credit removed.' : 'Credit saved.';
  }

  async function applyReceipt(receipt: CreditCommandOutcome | null) {
    if (unresolvedCredit(receipt) || !receipt) return;
    if (receipt.state === 'FAILED' || receipt.state === 'REJECTED') {
      await applyCreditFailure(receipt);
      return;
    }
    await applyConfirmedCredit();
  }

  async function executeOriginal() {
    if (!retryOriginal || submitting) return;
    submitting = true;
    try {
      await applyReceipt(await retryOriginal());
    } catch {
      message =
        'Credit result not confirmed. Check the original result before repeating it.';
    } finally {
      submitting = false;
    }
  }

  async function checkOriginal() {
    if (!pendingId || submitting) return;
    submitting = true;
    try {
      const receipt = await recoverCredit(
        createTransport(),
        pendingId,
        context.csrf,
        new AbortController().signal,
      );
      await applyReceipt(receipt);
    } catch {
      message =
        'Credit result not confirmed. Check the original result before repeating it.';
    } finally {
      submitting = false;
    }
  }

  function originalCreditSubmission(
    operationId: string,
    characters: string[],
  ): () => Promise<CreditCommandOutcome> {
    const transport = createTransport();
    const signal = new AbortController().signal;
    const input = { personId, roleId, characters };
    const selected = editing;
    const originalTitleVersion = titleVersion;
    const originalTitleId = titleId;
    const { policyRevision, csrf } = context;
    const send = selected
      ? () =>
          updateCredit(
            transport,
            operationId,
            selected.id,
            selected.version,
            policyRevision,
            { roleId: input.roleId, characters: input.characters },
            csrf,
            signal,
          )
      : () =>
          addCredit(
            transport,
            operationId,
            originalTitleId,
            originalTitleVersion,
            policyRevision,
            input,
            csrf,
            signal,
          );
    return async () => {
      await backOriginalCredit(
        operationId,
        {
          titleId: originalTitleId,
          personId: input.personId,
          roleId: input.roleId,
          characters: input.characters,
        },
        selected?.version ?? originalTitleVersion,
      );
      return send();
    };
  }

  async function submitCredit(): Promise<void> {
    if (!personId || !roleId || creditFormLocked()) return;
    const characters = parseCreditCharacters(
      selectedRole ?? undefined,
      characterText,
    );
    const validation = validateCharacterNames(
      characters,
      policy,
      selectedRole?.allowsCharacters ?? false,
    );
    if (validation) {
      message = validation;
      return;
    }
    const operationId = globalThis.crypto.randomUUID();
    retryOriginal = originalCreditSubmission(operationId, characters);
    pendingKind = 'save';
    pendingId = operationId;
    await executeOriginal();
  }

  async function confirmRemove() {
    const selected = removing;
    removing = null;
    if (!selected || pendingId || submitting) return;
    const operationId = globalThis.crypto.randomUUID();
    const transport = createTransport();
    const signal = new AbortController().signal;
    const originalCsrf = context.csrf;
    pendingKind = 'remove';
    pendingId = operationId;
    retryOriginal = () =>
      removeCredit(
        transport,
        operationId,
        selected.id,
        selected.version,
        originalCsrf,
        signal,
      );
    await executeOriginal();
  }
</script>

<section class="credit-panel" aria-labelledby="credits-heading">
  <h2 id="credits-heading">Cast and creators</h2>
  <p class="muted">
    Shared person picker, Cast/Creator roles, and optional complete character
    names. A slash inside a name is data, not a splitter.
  </p>

  {#if loading}
    <p role="status">Loading credits…</p>
  {:else}
    {#if creditDraftChoices.length}
      <section aria-label="Retained credit drafts">
        <h3>Choose a retained credit draft</h3>
        {#each creditDraftChoices as draft (draft.id)}
          <Button
            type="button"
            variant="secondary"
            onclick={() => void resumeCreditDraft(draft)}
          >
            Resume {draftLabel(draft)}
          </Button>
        {/each}
      </section>
    {/if}
    {#if creditReviewRequired}
      <Notice title="Review the current credit" tone="warning">
        <p>{message}</p>
        {#if editing}<p>
            Current saved role: {editing.role.label}. Current saved character
            names: {editing.characters.join(', ') || 'None'}.
          </p>{/if}
        <label
          ><input type="checkbox" bind:checked={reviewedCredit} />I reviewed the
          current person, role and retained character names.</label
        >
        <Button
          type="button"
          disabledReason={reviewedCredit
            ? ''
            : 'Review the current relationship first.'}
          onclick={() => {
            creditReviewRequired = false;
            message =
              'Current references reviewed. Save the retained credit deliberately.';
          }}
        >
          Use reviewed version for this credit
        </Button>
      </Notice>
    {/if}
    {#if backup.backed || backup.unbacked || draftUnavailable}
      <Button
        type="button"
        variant="secondary"
        disabledReason={!!pendingId || submitting
          ? 'Check the original result first.'
          : ''}
        onclick={() => (discardingDraft = true)}>Discard credit draft</Button
      >
    {/if}
    {#if backup.unbacked}
      <Notice title="Draft not backed up" tone="info">
        <p>Recent credit edits could not be saved to a draft yet.</p>
      </Notice>
    {/if}
    {#if message}
      <Notice
        title={message.includes('duplicate') ? 'Review the credit' : 'Credits'}
        tone="info"
      >
        <p>{message}</p>
      </Notice>
    {/if}
    <ul class="credit-list">
      {#each credits as credit (credit.id)}
        <li>
          <strong>{credit.personName}</strong> · {credit.role.label} ({credit
            .role.category})
          {#if credit.characters.length}
            — {credit.characters.join(', ')}
          {/if}
          {#if credit.personArchived === true}<p>
              Private credit · hidden publicly while this person is archived.
              Removing it keeps the shared person; later restoration cannot
              recreate the removed credit.
            </p>
          {:else if credit.personArchived === undefined}<p role="status">
              Current person reference unavailable. Check credit references
              before changing this relationship.
            </p>{/if}
          <button
            type="button"
            disabled={credit.personArchived !== false ||
              submitting ||
              !!pendingId ||
              !!creditDraftChoices.length ||
              draftUnavailable ||
              !!personId ||
              !!roleId ||
              backup.backed ||
              backup.unbacked}
            onclick={() => editCredit(credit)}>Edit credit</button
          >
          <button
            type="button"
            disabled={credit.personArchived === undefined ||
              submitting ||
              !!pendingId ||
              !!creditDraftChoices.length ||
              !!personId ||
              !!roleId ||
              backup.backed ||
              backup.unbacked}
            onclick={() => (removing = credit)}>Remove credit</button
          >
        </li>
      {:else}
        <li>No credits yet for this movie.</li>
      {/each}
    </ul>
    <button
      type="button"
      disabled={submitting || !!pendingId || loadingCredits}
      onclick={() => void checkCreditReferences()}
      >Check credit references</button
    >
    {#if creditPageError}<p role="alert">{creditPageError}</p>{/if}
    {#if nextCredits}<button
        type="button"
        disabled={loadingCredits || submitting || !!pendingId}
        aria-busy={loadingCredits}
        onclick={() => void loadMoreCredits()}
      >
        {creditPageError ? 'Retry more credits' : 'Load more credits'}
      </button>{/if}

    <div class="field">
      <label for="credit-person">Person</label>
      <select
        id="credit-person"
        bind:value={personId}
        onchange={() => {
          creditDirty = true;
          reviewedCredit = false;
        }}
        disabled={submitting ||
          !!pendingId ||
          !!editing ||
          loadingPeople ||
          !!creditDraftChoices.length ||
          draftUnavailable}
      >
        <option value="">Select an active person</option>
        {#each people as person (person.id)}
          <option value={person.id}>{person.name}</option>
        {/each}
      </select>
      {#if nextPeople}<button
          type="button"
          disabled={loadingPeople || submitting || !!pendingId || !!editing}
          aria-busy={loadingPeople}
          onclick={() => void loadMorePeople()}>Load more people</button
        >{/if}
    </div>
    <div class="field">
      <label for="credit-role">Role</label>
      <select
        id="credit-role"
        bind:value={roleId}
        onchange={() => {
          creditDirty = true;
          reviewedCredit = false;
        }}
        disabled={submitting ||
          !!pendingId ||
          !!creditDraftChoices.length ||
          draftUnavailable}
      >
        <option value="">Select Cast or Creator role</option>
        {#each roles as role (role.id)}
          <option value={role.id}>{role.label} ({role.category})</option>
        {/each}
      </select>
    </div>
    <RoleCreator
      {policy}
      {context}
      disabled={submitting ||
        !!pendingId ||
        !!creditDraftChoices.length ||
        draftUnavailable}
      onCreated={refreshCreatedRole}
    />
    {#if selectedRole?.allowsCharacters}
      <div class="field">
        <label for="credit-characters">Character names · one per line</label>
        <textarea
          id="credit-characters"
          rows="3"
          bind:value={characterText}
          oninput={() => {
            creditDirty = true;
            reviewedCredit = false;
          }}
          disabled={submitting ||
            !!pendingId ||
            !!creditDraftChoices.length ||
            draftUnavailable}></textarea>
      </div>
    {/if}
    <Button
      type="button"
      pending={submitting}
      disabledReason={!personId ||
      !roleId ||
      !!pendingId ||
      !!creditDraftChoices.length ||
      creditReviewRequired ||
      draftUnavailable
        ? 'Choose a person and role first.'
        : ''}
      onclick={() => void submitCredit()}
      >{editing ? 'Save credit' : 'Add credit'}</Button
    >
    {#if editing && !pendingId}<button type="button" onclick={cancelCreditEdit}
        >Cancel credit edit</button
      >{/if}
    {#if pendingId}
      <button
        type="button"
        disabled={submitting}
        onclick={() => void checkOriginal()}>Check original result</button
      >
      <button
        type="button"
        disabled={submitting || !retryOriginal}
        onclick={() => void executeOriginal()}>Retry original credit</button
      >
      {#if !retryOriginal}<p>
          Retry inputs are unavailable after reload. Check the original result
          before further changes.
        </p>{/if}
    {/if}
  {/if}
</section>
<ConfirmDialog
  open={!!removing}
  title="Remove this credit?"
  action="Remove credit"
  oncancel={() => (removing = null)}
  onconfirm={() => void confirmRemove()}
>
  <p>
    Remove this movie credit. The shared person and their other credits remain.
    {#if removing?.personArchived}Restoring this person later cannot recreate
      this removed credit.{/if}
  </p>
</ConfirmDialog>

<ConfirmDialog
  open={discardingDraft}
  title="Discard this unsaved credit draft?"
  action="Discard credit draft"
  pending={submitting}
  onconfirm={() => void discardCreditDraft()}
  oncancel={() => (discardingDraft = false)}
>
  <p>
    Clear this unsaved credit draft. Saved credits and the shared person stay
    unchanged.
  </p>
  {#if message}<p role="status">{message}</p>{/if}
</ConfirmDialog>

<style>
  .credit-panel {
    display: grid;
    gap: var(--primitive-space-4);
    padding-block: var(--primitive-space-8);
    margin-top: var(--primitive-space-6);
    border-top: 1px solid var(--line);
    max-width: 48rem;
  }
  .credit-panel > :global(button),
  .credit-panel > :global(.button-group) {
    justify-self: start;
  }
  .credit-panel > h2,
  .credit-panel > p {
    margin: 0;
  }
  .credit-list {
    margin: var(--primitive-space-2) 0;
    padding: 0;
    list-style: none;
  }
  .credit-list li {
    padding-block: var(--primitive-space-4);
    border-bottom: 1px solid var(--line);
    line-height: 1.8;
  }
  .credit-list button {
    margin: var(--primitive-space-2) var(--primitive-space-2) 0 0;
    background: transparent;
    color: var(--text);
    border-color: var(--line);
    font-size: var(--primitive-type-small);
  }
  .credit-list strong {
    font-weight: 650;
  }
  .field label {
    display: block;
    margin-bottom: var(--primitive-space-2);
    font-size: 14px;
  }
  .muted {
    color: var(--muted);
    font-size: 14px;
  }
  @media (max-width: 699px) {
    .credit-panel {
      padding-block: var(--primitive-space-6);
    }
    .credit-list button {
      width: 100%;
      margin-inline: 0;
    }
  }
</style>
