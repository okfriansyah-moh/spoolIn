<script lang="ts">
  import { onMount } from 'svelte';
  import { createTransport } from '../../../lib/transport/graphql';
  import { sessionController } from '../../../lib/features/session/context';
  import { loadWriteContext } from '../../../lib/features/movie/write-context';
  import {
    fetchArchived,
    fetchDeletionImpact,
    runMovieLifecycle,
    runPersonLifecycle,
    type ArchiveRow,
  } from '../../../lib/features/archive/api';
  import type { ArchiveListState, ArchiveTab } from '../../../lib/features/archive/types';

  const sessionCtl = sessionController();
  let tab = $state<ArchiveTab>('movies');
  let listState = $state<ArchiveListState>({ kind: 'loading' });
  let confirmOpen = $state(false);
  let selected = $state<ArchiveRow | null>(null);
  let impactSummary = $state('');
  let impactToken = $state<string | null>(null);
  let busy = $state(false);
  let csrf = $state('');

  async function reload(signal?: AbortSignal) {
    if (!csrf) return;
    listState = { kind: 'loading' };
    const role = sessionCtl.snapshot.identity?.role;
    if (role !== 'MAINTAINER') {
      listState = { kind: 'denied' };
      return;
    }
    try {
      const items = await fetchArchived(createTransport(), tab, csrf, signal);
      listState =
        items.length === 0
          ? { kind: 'empty' }
          : { kind: 'ready', tab, items: [...items] };
    } catch {
      listState = { kind: 'error', message: 'Archives could not be loaded. Try again.' };
    }
  }

  onMount(() => {
    const abort = new AbortController();
    void loadWriteContext(abort.signal).then((context) => {
      if (!context || abort.signal.aborted) return;
      csrf = context.csrf;
      void reload(abort.signal);
    });
    return () => abort.abort();
  });

  async function switchTab(next: ArchiveTab) {
    tab = next;
    await reload();
  }

  async function restoreRow(row: ArchiveRow) {
    if (!csrf || busy) return;
    busy = true;
    try {
      const operationId = crypto.randomUUID();
      if (tab === 'movies') {
        await runMovieLifecycle(createTransport(), csrf, {
          operationId,
          id: row.id,
          expectedVersion: row.version,
          action: 'RESTORE',
          confirmed: true,
        });
      } else {
        await runPersonLifecycle(createTransport(), csrf, {
          operationId,
          id: row.id,
          expectedVersion: row.version,
          action: 'RESTORE',
          confirmed: true,
        });
      }
      await reload();
    } finally {
      busy = false;
    }
  }

  async function openConfirm(row: ArchiveRow) {
    selected = row;
    impactSummary = '';
    impactToken = null;
    if (tab === 'people' && csrf) {
      const impact = await fetchDeletionImpact(createTransport(), row.id, csrf);
      impactToken = impact.token;
      impactSummary =
        impact.affectedCreditCount === 0
          ? 'No active credits reference this person.'
          : `Affects ${impact.affectedCreditCount} credit(s): ${impact.affectedTitles.join(', ')}`;
    }
    confirmOpen = true;
  }

  async function confirmDelete() {
    if (!selected || !csrf || busy) return;
    busy = true;
    try {
      const operationId = crypto.randomUUID();
      if (tab === 'movies') {
        await runMovieLifecycle(createTransport(), csrf, {
          operationId,
          id: selected.id,
          expectedVersion: selected.version,
          action: 'DELETE',
          confirmed: true,
        });
      } else {
        await runPersonLifecycle(createTransport(), csrf, {
          operationId,
          id: selected.id,
          expectedVersion: selected.version,
          action: 'DELETE',
          impactToken: impactToken ?? undefined,
          confirmed: true,
        });
      }
      confirmOpen = false;
      selected = null;
      await reload();
    } finally {
      busy = false;
    }
  }
</script>

<main class="stack">
  <h1>Archives</h1>
  <p>Maintainer-only retained movies and people. Restore or confirm permanent deletion separately.</p>

  <div role="tablist" aria-label="Archive kind">
    <button type="button" role="tab" aria-selected={tab === 'movies'} onclick={() => switchTab('movies')}>
      Movies
    </button>
    <button type="button" role="tab" aria-selected={tab === 'people'} onclick={() => switchTab('people')}>
      People
    </button>
  </div>

  {#if listState.kind === 'loading'}
    <p role="status">Loading archives…</p>
  {:else if listState.kind === 'denied'}
    <p role="alert">You do not have access to private archives.</p>
  {:else if listState.kind === 'error'}
    <p role="alert">{listState.message}</p>
  {:else if listState.kind === 'empty'}
    <p>No archived records in this view.</p>
  {:else}
    <ul>
      {#each listState.items as item (item.id)}
        <li>
          <span>{item.label} · {item.state}</span>
          <button type="button" disabled={busy} onclick={() => restoreRow(item)}>Restore</button>
          <button type="button" disabled={busy} onclick={() => openConfirm(item)}>Delete permanently</button>
        </li>
      {/each}
    </ul>
  {/if}

  {#if confirmOpen && selected}
    <section aria-labelledby="confirm-title" class="panel">
      <h2 id="confirm-title">Confirm permanent deletion</h2>
      <p>This action is irreversible for <strong>{selected.label}</strong>.</p>
      {#if impactSummary}
        <p>{impactSummary}</p>
      {/if}
      <div class="actions">
        <button type="button" onclick={() => (confirmOpen = false)}>Cancel</button>
        <button type="button" disabled={busy || (tab === 'people' && !impactToken)} onclick={confirmDelete}>
          Permanently delete
        </button>
      </div>
    </section>
  {/if}
</main>

<style>
  .stack {
    display: flex;
    flex-direction: column;
    gap: 1rem;
  }
  .panel {
    border: 1px solid var(--border-subtle, #333);
    padding: 1rem;
    border-radius: 0.5rem;
  }
  .actions {
    display: flex;
    gap: 0.75rem;
  }
  ul {
    list-style: none;
    padding: 0;
    display: flex;
    flex-direction: column;
    gap: 0.5rem;
  }
  li {
    display: flex;
    gap: 0.75rem;
    align-items: center;
    flex-wrap: wrap;
  }
</style>
