<script lang="ts">
  import type { ArchiveListState, ArchiveTab } from '../../../lib/features/archive/types';

  let tab = $state<ArchiveTab>('movies');
  let listState = $state<ArchiveListState>({ kind: 'empty' });
  let confirmOpen = $state(false);

  function openConfirm() {
    confirmOpen = true;
  }

  function cancelConfirm() {
    confirmOpen = false;
  }
</script>

<main class="stack">
  <h1>Archives</h1>
  <p>Maintainer-only retained movies and people. Restore or confirm permanent deletion separately.</p>

  <div role="tablist" aria-label="Archive kind">
    <button type="button" role="tab" aria-selected={tab === 'movies'} onclick={() => (tab = 'movies')}>
      Movies
    </button>
    <button type="button" role="tab" aria-selected={tab === 'people'} onclick={() => (tab = 'people')}>
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
        <li>{item.label} · {item.state}</li>
      {/each}
    </ul>
  {/if}

  <button type="button" onclick={openConfirm}>Review permanent deletion</button>

  {#if confirmOpen}
    <section aria-labelledby="confirm-title" class="panel">
      <h2 id="confirm-title">Confirm permanent deletion</h2>
      <p>This action is irreversible. Cancel to keep all archive data.</p>
      <div class="actions">
        <button type="button" onclick={cancelConfirm}>Cancel</button>
        <button type="button" disabled>Permanently delete</button>
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
</style>
