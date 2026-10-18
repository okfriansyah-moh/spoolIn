<script lang="ts">
  import { tick, onDestroy } from 'svelte';
  import Button from '../../components/Button.svelte';
  import Notice from '../../components/Notice.svelte';
  import { boundFor, checkText } from '../../policy/rules';
  import type { Policy } from '../../policy/types';
  import type { WriteContext } from '../movie/types';
  import { createTransport, TransportFailure } from '../../transport/graphql';
  import {
    createCreditRole,
    recoverCreditRole,
    type RoleIntent,
    type RoleOutcome,
  } from './role-api';

  let {
    policy,
    context,
    disabled = false,
    onCreated,
    inheritedCategory = null,
    resetKey = 0,
    onStateChange,
  }: {
    policy: Policy;
    context: WriteContext;
    disabled?: boolean;
    onCreated: (id: string) => Promise<void>;
    inheritedCategory?: 'CAST' | 'CREATOR' | null;
    resetKey?: number;
    onStateChange?: (state: { dirty: boolean; blocked: boolean }) => void;
  } = $props();
  let label = $state('');
  let category = $state<'' | 'CAST' | 'CREATOR'>('');
  let expanded = $state(false);
  let confirmedId = $state<string | null>(null);
  let appliedReset = resetKey;
  let pending = $state(false);
  const effectiveCategory = $derived(inheritedCategory ?? category);
  $effect(() => {
    onStateChange?.({
      dirty: !!label.trim() || !!category || !!original || !!confirmedId,
      blocked: pending || !!original,
    });
  });
  $effect(() => {
    if (resetKey !== appliedReset && !pending && !original) {
      appliedReset = resetKey;
      label = '';
      category = '';
      confirmedId = null;
      message = '';
      expanded = false;
    }
  });
  let original = $state<RoleIntent | null>(null);
  let message = $state('');
  let error = $state(false);
  let feedback = $state<HTMLDivElement>();
  const id = $props.id();
  let active: AbortController | null = null;
  onDestroy(() => active?.abort());

  async function report(text: string, failed = true) {
    message = text;
    error = failed;
    await tick();
    if (failed) feedback?.focus();
  }

  function cancelRole() {
    if (pending || original || confirmedId) return;
    label = '';
    category = '';
    message = '';
    error = false;
    expanded = false;
  }

  function duplicateRole(code: string | undefined): boolean {
    return code === 'ALREADY_EXISTS' || code === 'ROLE_EXISTS';
  }

  async function accept(result: RoleOutcome | null) {
    if (!result || result.state === 'PENDING') {
      await report(
        'Creation is not confirmed yet. Check creation status, or safely retry the same request. Do not create another role.',
      );
    } else if (result.state === 'REJECTED') {
      original = null;
      await report(
        duplicateRole(result.problem.code)
          ? 'This role already exists. Choose the existing role or correct its name.'
          : 'Role was not created. Review the name, category and current policy.',
      );
    } else if (result.resultExpired || result.state === 'EXPIRED') {
      await report(
        'Original role result expired. Review current roles before further changes.',
      );
    } else if (result.state === 'FAILED') {
      original = null;
      await report(
        duplicateRole(result.problem?.code)
          ? 'This role already exists. Choose the existing role or correct its name.'
          : 'Role was not created. Review the name, category and current policy.',
      );
    } else {
      confirmedId = requireRole(result);
      original = null;
      await selectConfirmedRole();
    }
  }

  async function selectConfirmedRole() {
    if (!confirmedId) return;
    try {
      await onCreated(confirmedId);
      confirmedId = null;
      label = '';
      category = '';
      await report(
        'Role ready. Choose a person, then select Add credit.',
        false,
      );
    } catch {
      await report(
        'The role was created, but the role list could not refresh. Select Refresh role list to select it. Do not create it again.',
      );
    }
  }

  async function refreshConfirmedRole() {
    if (pending || disabled) return;
    pending = true;
    try {
      await selectConfirmedRole();
    } finally {
      pending = false;
    }
  }

  function requireRole(
    result: Exclude<RoleOutcome, { state: 'REJECTED' }>,
  ): string {
    if (!result.resultId)
      throw new Error('Confirmed role identity unavailable.');
    return result.resultId;
  }

  async function execute(recover = false, fresh = false) {
    if (!original || pending || disabled) return;
    pending = true;
    try {
      const request = createTransport();
      active = new AbortController();
      const signal = active.signal;
      const result = recover
        ? await recoverCreditRole(
            request,
            original.operationId,
            context.csrf,
            signal,
          )
        : await createCreditRole(request, original, context.csrf, signal);
      if (!signal.aborted) await accept(result);
    } catch (failure) {
      if (
        fresh &&
        failure instanceof TransportFailure &&
        failure.kind === 'rejected' &&
        [
          'FORBIDDEN',
          'UNAUTHENTICATED',
          'FIELD_INVALID',
          'VALIDATION_FAILED',
          'RATE_LIMITED',
        ].includes(failure.code)
      ) {
        original = null;
        await report(
          failure.code === 'RATE_LIMITED'
            ? 'Role creation was rate limited. Wait before trying again.'
            : 'Role creation was rejected. Review the name and sign-in before trying again.',
        );
        return;
      }
      await report(
        'Creation is not confirmed yet. Check creation status, or safely retry the same request. Do not create another role.',
      );
    } finally {
      pending = false;
      active = null;
    }
  }

  async function submit(event: SubmitEvent) {
    event.preventDefault();
    if (pending || disabled || original || confirmedId) return;
    const checked = checkText(label, boundFor(policy, 'creditRole'));
    if (!effectiveCategory || checked.error) {
      await report(checked.error || 'Choose Cast or Creator for this role.');
      return;
    }
    original = {
      operationId: crypto.randomUUID(),
      category: effectiveCategory,
      label: checked.value,
      policyRevision: context.policyRevision,
    };
    await execute(false, true);
  }
</script>

<details class="role-creator" bind:open={expanded}>
  <summary>Need another role?</summary>
  <h3>Create a custom role</h3>
  <p>
    Create a reusable contribution label, such as Stunt performer or Historical
    adviser. Creating a role does not add a person to this movie. Select the
    role and person, then select Add credit to save their contribution.
  </p>
  <p>
    Custom roles do not support character names. Choose Actor when you need to
    enter character names.
  </p>
  {#if inheritedCategory}<p>
      This role will be added to {inheritedCategory === 'CAST'
        ? 'Cast'
        : 'Creators'}.
    </p>{/if}
  <form onsubmit={(event) => void submit(event)}>
    <label for={`${id}-label`}>Custom role name · Required</label>
    <input
      id={`${id}-label`}
      bind:value={label}
      disabled={pending || !!original || !!confirmedId || disabled}
      aria-describedby={message ? `${id}-feedback` : undefined}
    />
    {#if !inheritedCategory}<label for={`${id}-category`}
        >Custom role category · Required</label
      >
      <select
        id={`${id}-category`}
        bind:value={category}
        disabled={pending || !!original || !!confirmedId || disabled}
      >
        <option value="">Choose a category</option><option value="CAST"
          >Cast</option
        ><option value="CREATOR">Creator</option>
      </select>{/if}
    <Button
      type="submit"
      {pending}
      disabledReason={disabled || !!original || !!confirmedId
        ? confirmedId
          ? 'Refresh the role list to select the created role.'
          : original
            ? 'Resolve the existing creation request first.'
            : 'Credit changes are unavailable.'
        : ''}
    >
      Create role
    </Button>
    {#if !pending && !original && !confirmedId}
      <Button type="button" variant="secondary" onclick={cancelRole}>
        Cancel custom role
      </Button>
    {/if}
  </form>
  {#if message}<div id={`${id}-feedback`} bind:this={feedback} tabindex="-1">
      <Notice
        title={error ? 'Review the role' : 'Role ready'}
        tone={error ? 'warning' : 'success'}
        urgent={error}
      >
        <p>{message}</p>
      </Notice>
    </div>{/if}
  {#if confirmedId}<Button
      type="button"
      variant="secondary"
      disabledReason={pending || disabled
        ? 'Wait for the current request.'
        : ''}
      onclick={() => void refreshConfirmedRole()}>Refresh role list</Button
    >{/if}
  {#if original}
    <Button
      type="button"
      variant="secondary"
      disabledReason={pending || disabled
        ? 'Wait for the current request.'
        : ''}
      onclick={() => void execute(true)}>Check creation status</Button
    >
    <Button
      type="button"
      variant="secondary"
      disabledReason={pending || disabled
        ? 'Wait for the current request.'
        : ''}
      onclick={() => void execute()}>Safely retry creation</Button
    >
  {/if}
</details>

<style>
  .role-creator {
    margin-block: var(--primitive-space-4);
  }
  form {
    display: grid;
    gap: var(--primitive-space-2);
  }
</style>
