<script lang="ts">
  import { tick, onDestroy } from 'svelte';
  import Button from '../../components/Button.svelte';
  import Notice from '../../components/Notice.svelte';
  import { boundFor, checkText } from '../../policy/rules';
  import type { Policy } from '../../policy/types';
  import type { WriteContext } from '../movie/types';
  import { createTransport } from '../../transport/graphql';
  import { createCreditRole, recoverCreditRole, type RoleIntent, type RoleOutcome } from './role-api';

  let { policy, context, disabled = false, onCreated }: {
    policy: Policy; context: WriteContext; disabled?: boolean;
    onCreated: (id: string) => Promise<void>;
  } = $props();
  let label = $state('');
  let category = $state<'' | 'CAST' | 'CREATOR'>('');
  let pending = $state(false);
  let original = $state<RoleIntent | null>(null);
  let message = $state('');
  let error = $state(false);
  let feedback: HTMLDivElement;
  const id = $props.id();
  let active: AbortController | null = null;
  onDestroy(() => active?.abort());

  async function report(text: string, failed = true) {
    message = text; error = failed;
    await tick();
    if (failed) feedback?.focus();
  }

  function duplicateRole(code: string | undefined): boolean {
    return code === 'ALREADY_EXISTS' || code === 'ROLE_EXISTS';
  }

  async function accept(result: RoleOutcome | null) {
    if (!result || result.state === 'PENDING') {
      await report('Role result not confirmed. Check the original result before repeating it.');
    } else if (result.state === 'REJECTED') {
      original = null;
      await report(duplicateRole(result.problem.code)
        ? 'This role already exists. Choose the existing role or correct its name.'
        : 'Role was not created. Review the name, category and current policy.');
    } else if (result.resultExpired || result.state === 'EXPIRED') {
      await report('Original role result expired. Review current roles before further changes.');
    } else if (result.state === 'FAILED') {
      original = null;
      await report(duplicateRole(result.problem?.code)
        ? 'This role already exists. Choose the existing role or correct its name.'
        : 'Role was not created. Review the name, category and current policy.');
    } else {
      await onCreated(requireRole(result));
      original = null; label = ''; category = '';
      await report('Role created. It is selected for this credit.', false);
    }
  }

  function requireRole(result: Exclude<RoleOutcome, { state: 'REJECTED' }>): string {
    if (!result.resultId) throw new Error('Confirmed role identity unavailable.');
    return result.resultId;
  }

  async function execute(recover = false) {
    if (!original || pending || disabled) return;
    pending = true;
    try {
      const request = createTransport();
      active = new AbortController();
      const signal = active.signal;
      const result = recover ? await recoverCreditRole(request, original.operationId, context.csrf, signal)
        : await createCreditRole(request, original, context.csrf, signal);
      if (!signal.aborted) await accept(result);
    } catch {
      await report('Role result not confirmed. Check the original result before repeating it.');
    } finally { pending = false; active = null; }
  }

  async function submit(event: SubmitEvent) {
    event.preventDefault();
    if (pending || disabled || original) return;
    const checked = checkText(label, boundFor(policy, 'creditRole'));
    if (!category || checked.error) {
      await report(checked.error || 'Choose Cast or Creator for this role.');
      return;
    }
    original = { operationId: crypto.randomUUID(), category, label: checked.value, policyRevision: context.policyRevision };
    await execute();
  }
</script>

<section class="role-creator" aria-labelledby={`${id}-heading`}>
  <h3 id={`${id}-heading`}>Create a custom role</h3>
  <p>Choose Cast or Creator. Custom roles do not accept character names.</p>
  <form onsubmit={(event) => void submit(event)}>
    <label for={`${id}-label`}>Custom role name · Required</label>
    <input id={`${id}-label`} bind:value={label} disabled={pending || !!original || disabled}
      aria-describedby={message ? `${id}-feedback` : undefined} />
    <label for={`${id}-category`}>Custom role category · Required</label>
    <select id={`${id}-category`} bind:value={category} disabled={pending || !!original || disabled}>
      <option value="">Choose a category</option><option value="CAST">Cast</option><option value="CREATOR">Creator</option>
    </select>
    <Button type="submit" pending={pending} disabledReason={disabled || !!original ? 'Check the original result first.' : ''}>
      Create role
    </Button>
  </form>
  {#if message}<div id={`${id}-feedback`} bind:this={feedback} tabindex="-1">
    <Notice title={error ? 'Review the role' : 'Role created'} tone={error ? 'warning' : 'success'} urgent={error}>
      <p>{message}</p>
    </Notice>
  </div>{/if}
  {#if original}
    <Button type="button" variant="secondary" disabledReason={pending || disabled ? 'Wait for the current request.' : ''}
      onclick={() => void execute(true)}>Check original role result</Button>
    <Button type="button" variant="secondary" disabledReason={pending || disabled ? 'Wait for the current request.' : ''}
      onclick={() => void execute()}>Retry original role creation</Button>
  {/if}
</section>

<style>
  .role-creator { display: grid; gap: var(--primitive-space-3); margin-block: var(--primitive-space-4); }
  form { display: grid; gap: var(--primitive-space-2); }
</style>
