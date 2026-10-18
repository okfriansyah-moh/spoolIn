<script lang="ts">
  import { onMount } from 'svelte';
  import Notice from '../../components/Notice.svelte';
  import Button from '../../components/Button.svelte';
  import ConfirmDialog from '../../components/ConfirmDialog.svelte';
  import type { IdentityOutcome } from '../session/commands';
  import { fetchManagedMovie } from '../movie/api';
  import { createTransport } from '../../transport/graphql';
  import type { Policy } from '../../policy/types';
  import type { WriteContext } from '../movie/types';
  import type { DraftSource } from '../draft/types';
  import { CreditDraftSync } from './draft-sync';
  import {
    addCredit,
    updateCredit,
    removeCredit,
    recoverCredit,
    fetchActivePeople,
    fetchCreditRoles,
    fetchTitleCredits,
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
  let message = $state('');
  let personId = $state('');
  let roleId = $state('');
  let characterText = $state('');
  let submitting = $state(false);
  let editing = $state<ManagedCredit | null>(null);
  let removing = $state<ManagedCredit | null>(null);
  let pendingId = $state<string | null>(null);
  let pendingKind: 'save' | 'remove' = 'save';
  let retryOriginal: (() => Promise<IdentityOutcome>) | null = null;

  const selectedRole = $derived(
    roles.find((role) => role.id === roleId) ?? null,
  );

  let draftSync = $state<CreditDraftSync | null>(null);

  $effect(() => {
    const sync = new CreditDraftSync(draftSource, titleId);
    draftSync = sync;
    return () => sync.dispose();
  });

  $effect(() => {
    if (!draftSync || !personId || !roleId || pendingId || editing) return;
    draftSync.schedule(
      {
        titleId,
        personId,
        roleId,
        characters: parseCreditCharacters(selectedRole ?? undefined, characterText),
      },
      context,
      titleVersion,
    );
  });

  onMount(() => {
    const transport = createTransport();
    const abort = new AbortController();
    void (async () => {
      try {
        const [loadedCredits, loadedRoles, loadedPeople] = await Promise.all([
          fetchTitleCredits(transport, titleId, context.csrf, abort.signal),
          fetchCreditRoles(transport, abort.signal),
          fetchActivePeople(transport, context.csrf, abort.signal),
        ]);
        if (abort.signal.aborted) return;
        credits = loadedCredits;
        roles = loadedRoles;
        people = loadedPeople;
      } catch {
        if (!abort.signal.aborted) message = 'Credits could not be loaded. Reload before making changes.';
      } finally {
        if (!abort.signal.aborted) loading = false;
      }
    })();
    return () => abort.abort();
  });

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
    editing = null;
    personId = '';
    roleId = '';
    characterText = '';
  }

  function editCredit(credit: ManagedCredit) {
    editing = credit;
    personId = credit.personId;
    roleId = credit.role.id;
    characterText = credit.characters.join('\n');
  }

  async function applyReceipt(receipt: IdentityOutcome | null) {
    if (!receipt || receipt.state === 'PENDING') {
      message = 'Credit result not confirmed. Check the original result before repeating it.';
      return;
    }
    if (receipt.state === 'EXPIRED' || receipt.resultExpired) {
      message = 'Original credit result expired. Review the current record before further changes.';
      return;
    }
    if (receipt.state === 'FAILED') {
      pendingId = null;
      retryOriginal = null;
      message = receipt.problem?.code === 'ALREADY_EXISTS' || receipt.problem?.code === 'CREDIT_DUPLICATE'
        ? 'Review the credit — no duplicate added.' : 'Changes were not saved. Review the current credit.';
      return;
    }
    const transport = createTransport();
    const movie = await fetchManagedMovie(transport, titleId, context.csrf);
    if (!movie || movie.state !== 'ACTIVE') throw new Error('Current movie unavailable.');
    credits = await fetchTitleCredits(transport, titleId, context.csrf);
    onTitleVersionChange(movie.version);
    pendingId = null;
    retryOriginal = null;
    resetFields();
    message = pendingKind === 'remove' ? 'Credit removed.' : 'Credit saved.';
  }

  async function executeOriginal() {
    if (!retryOriginal || submitting) return;
    submitting = true;
    try {
      await applyReceipt(await retryOriginal());
    } catch {
      message = 'Credit result not confirmed. Check the original result before repeating it.';
    } finally {
      submitting = false;
    }
  }

  async function checkOriginal() {
    if (!pendingId || submitting) return;
    submitting = true;
    try {
      const receipt = await recoverCredit(createTransport(), pendingId, context.csrf, new AbortController().signal);
      await applyReceipt(receipt);
    } catch {
      message = 'Credit result not confirmed. Check the original result before repeating it.';
    } finally {
      submitting = false;
    }
  }

  async function submitCredit(): Promise<void> {
    if (!personId || !roleId || submitting || pendingId) return;
    const characters = parseCreditCharacters(selectedRole ?? undefined, characterText);
    const validation = validateCharacterNames(characters, policy, selectedRole?.allowsCharacters ?? false);
    if (validation) { message = validation; return; }
    const operationId = globalThis.crypto.randomUUID();
    const transport = createTransport();
    const signal = new AbortController().signal;
    const input = { personId, roleId, characters };
    const selected = editing;
    const originalTitleVersion = titleVersion;
    const originalTitleId = titleId;
    const { policyRevision, csrf } = context;
    pendingKind = 'save';
    pendingId = operationId;
    retryOriginal = selected
      ? () => updateCredit(transport, operationId, selected.id, selected.version, policyRevision,
        { roleId: input.roleId, characters: input.characters }, csrf, signal)
      : () => addCredit(transport, operationId, originalTitleId, originalTitleVersion, policyRevision, input, csrf, signal);
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
    retryOriginal = () => removeCredit(transport, operationId, selected.id, selected.version, originalCsrf, signal);
    await executeOriginal();
  }

</script>

<section aria-labelledby="credits-heading">
  <h2 id="credits-heading">Cast and creators</h2>
  <p class="muted">
    Shared person picker, Cast/Creator roles, and optional complete character
    names. A slash inside a name is data, not a splitter.
  </p>

  {#if loading}
    <p role="status">Loading credits…</p>
  {:else}
    {#if draftSync?.snapshot.unbacked}
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
          <button type="button" disabled={submitting || !!pendingId} onclick={() => editCredit(credit)}>Edit credit</button>
          <button type="button" disabled={submitting || !!pendingId} onclick={() => (removing = credit)}>Remove credit</button>
        </li>
      {:else}
        <li>No credits yet for this movie.</li>
      {/each}
    </ul>

    <div class="field">
      <label for="credit-person">Person</label>
      <select id="credit-person" bind:value={personId} disabled={submitting || !!pendingId || !!editing}>
        <option value="">Select an active person</option>
        {#each people as person (person.id)}
          <option value={person.id}>{person.name}</option>
        {/each}
      </select>
    </div>
    <div class="field">
      <label for="credit-role">Role</label>
      <select id="credit-role" bind:value={roleId} disabled={submitting || !!pendingId}>
        <option value="">Select Cast or Creator role</option>
        {#each roles as role (role.id)}
          <option value={role.id}>{role.label} ({role.category})</option>
        {/each}
      </select>
    </div>
    {#if selectedRole?.allowsCharacters}
      <div class="field">
        <label for="credit-characters">Character names · one per line</label>
        <textarea
          id="credit-characters"
          rows="3"
          bind:value={characterText}
          disabled={submitting || !!pendingId}></textarea>
      </div>
    {/if}
    <Button
      type="button"
      pending={submitting}
      disabledReason={!personId || !roleId || !!pendingId
        ? 'Choose a person and role first.'
        : ''}
      onclick={() => void submitCredit()}>{editing ? 'Save credit' : 'Add credit'}</Button
    >
    {#if editing && !pendingId}<button type="button" onclick={resetFields}>Cancel credit edit</button>{/if}
    {#if pendingId}
      <button type="button" disabled={submitting} onclick={() => void checkOriginal()}>Check original result</button>
      <button type="button" disabled={submitting} onclick={() => void executeOriginal()}>Retry original credit</button>
    {/if}
  {/if}
</section>
<ConfirmDialog open={!!removing} title="Remove this credit?" action="Remove credit"
  oncancel={() => (removing = null)} onconfirm={() => void confirmRemove()}>
  <p>Remove this movie credit. The shared person and their other credits remain.</p>
</ConfirmDialog>

<style>
  .credit-list {
    margin: 1rem 0;
    padding-left: 1.25rem;
  }
  .field {
    margin-bottom: 0.75rem;
  }
  .muted {
    opacity: 0.85;
  }
</style>
