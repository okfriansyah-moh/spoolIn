<script lang="ts">
  import { onDestroy } from 'svelte';
  import { goto } from '$app/navigation';
  import { resolve } from '$app/paths';
  import ConfirmDialog from '../../components/ConfirmDialog.svelte';
  import { createTransport } from '../../transport/graphql';
  import { loadWriteContext } from '../movie/write-context';
  import { recoverCatalogOperation } from '../movie/api';
  import { runMovieLifecycle, runPersonLifecycle } from './api';

  let {
    kind,
    id,
    version,
    disabled = false,
  }: {
    kind: 'movie' | 'person';
    id: string;
    version: string;
    disabled?: boolean;
  } = $props();
  let open = $state(false);
  let pending = $state(false);
  let operationId = $state<string | null>(null);
  let message = $state('');
  const abort = new AbortController();
  onDestroy(() => abort.abort());

  async function outcome(state: string) {
    open = false;
    if (state === 'SUCCEEDED') {
      await goto(resolve('/manage/archives'));
    } else if (state === 'FAILED') {
      message =
        'The record was not archived. Reload it to review the current version.';
      operationId = null;
    } else {
      message =
        'Archive confirmation is pending. Check the original result before trying again.';
    }
  }

  async function checkArchiveResult(originalOperationId: string, csrf: string) {
    const receipt = await recoverCatalogOperation(
      createTransport(),
      originalOperationId,
      csrf,
      abort.signal,
    );
    if (receipt?.resultExpired) {
      message =
        'The retained result has expired. Reload the record to inspect its current state.';
    } else {
      await outcome(receipt?.state ?? 'UNKNOWN');
    }
  }

  async function submit(checkOriginal = false) {
    if (pending || disabled) return;
    pending = true;
    try {
      const context = await loadWriteContext(abort.signal);
      if (!context) {
        message = 'Sign in as a Maintainer to archive this record.';
        return;
      }
      const transport = createTransport();
      if (checkOriginal && operationId) {
        await checkArchiveResult(operationId, context.csrf);
      } else {
        operationId = globalThis.crypto.randomUUID();
        const input = {
          operationId,
          id,
          expectedVersion: version,
          action: 'ARCHIVE' as const,
          confirmed: true,
        };
        const run = kind === 'movie' ? runMovieLifecycle : runPersonLifecycle;
        await outcome(await run(transport, context.csrf, input));
      }
    } catch {
      open = false;
      message =
        'The archive result is uncertain. Check the original result before trying again.';
    } finally {
      pending = false;
    }
  }
</script>

{#if operationId}
  <button
    type="button"
    class="secondary"
    disabled={pending}
    onclick={() => void submit(true)}>Check original archive result</button
  >
{:else}
  <button
    type="button"
    class="secondary"
    disabled={disabled || pending}
    onclick={() => (open = true)}>Archive {kind}</button
  >
{/if}
<p role="status">{message}</p>
<ConfirmDialog
  bind:open
  title={`Archive ${kind}?`}
  action={`Archive ${kind}`}
  {pending}
  onconfirm={() => void submit()}
>
  <p>
    {kind === 'movie'
      ? 'This movie will leave the public catalog. You can restore it from Archives.'
      : 'This person and their credits will be hidden from public titles. You can restore the person from Archives.'}
  </p>
</ConfirmDialog>
