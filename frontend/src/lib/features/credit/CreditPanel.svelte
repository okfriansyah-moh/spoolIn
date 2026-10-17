<script lang="ts">
  import { onMount } from 'svelte';
  import Notice from '../../components/Notice.svelte';
  import Button from '../../components/Button.svelte';
  import { createTransport } from '../../transport/graphql';
  import type { Policy } from '../../policy/types';
  import type { WriteContext } from '../movie/types';
  import {
    addCredit,
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
    onTitleVersionChange,
  }: {
    titleId: string;
    titleVersion: string;
    policy: Policy;
    context: WriteContext;
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

  const selectedRole = $derived(
    roles.find((role) => role.id === roleId) ?? null,
  );

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
      } finally {
        if (!abort.signal.aborted) loading = false;
      }
    })();
    return () => abort.abort();
  });

  async function submitCredit(): Promise<void> {
    if (!personId || !roleId || submitting) return;
    const characters =
      selectedRole?.allowsCharacters && characterText.trim()
        ? characterText
            .split('\n')
            .map((line) => line.trim())
            .filter(Boolean)
        : [];
    const validation = validateCharacterNames(
      characters,
      policy,
      selectedRole?.allowsCharacters ?? false,
    );
    if (validation) {
      message = validation;
      return;
    }
    submitting = true;
    message = '';
    const transport = createTransport();
    const operationId = globalThis.crypto.randomUUID();
    const error = await addCredit(
      transport,
      operationId,
      titleId,
      titleVersion,
      context.policyRevision,
      { personId, roleId, characters },
      context.csrf,
      new AbortController().signal,
    );
    submitting = false;
    if (error) {
      message = error;
      return;
    }
    const nextVersion = String(Number(titleVersion) + 1);
    onTitleVersionChange(nextVersion);
    credits = await fetchTitleCredits(transport, titleId, context.csrf);
    personId = '';
    roleId = '';
    characterText = '';
    message = 'Credit saved.';
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
        </li>
      {:else}
        <li>No credits yet for this movie.</li>
      {/each}
    </ul>

    <div class="field">
      <label for="credit-person">Person</label>
      <select id="credit-person" bind:value={personId} disabled={submitting}>
        <option value="">Select an active person</option>
        {#each people as person (person.id)}
          <option value={person.id}>{person.name}</option>
        {/each}
      </select>
    </div>
    <div class="field">
      <label for="credit-role">Role</label>
      <select id="credit-role" bind:value={roleId} disabled={submitting}>
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
          disabled={submitting}></textarea>
      </div>
    {/if}
    <Button
      type="button"
      disabled={submitting || !personId || !roleId}
      onclick={() => void submitCredit()}>Add credit</Button
    >
  {/if}
</section>

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
