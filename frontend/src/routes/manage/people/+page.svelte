<script lang="ts">
  import { onMount } from 'svelte';
  import { resolve } from '$app/paths';
  import Notice from '../../../lib/components/Notice.svelte';
  import { createTransport } from '../../../lib/transport/graphql';
  import { createPersonListController } from '../../../lib/features/person/write/list-controller';
  import { loadWriteContext } from '../../../lib/features/movie/write-context';
  import type { ListState } from '../../../lib/features/person/write/types';

  let listState = $state<ListState>({ kind: 'loading' });
  let reload = async () => {};
  let loadMore = async () => {};

  onMount(() => {
    const transport = createTransport();
    const controller = createPersonListController(transport);
    const unsubscribe = controller.subscribe((next) => {
      listState = next;
    });
    const abort = new AbortController();
    reload = async () => {
      try {
        const context = await loadWriteContext(abort.signal);
        if (abort.signal.aborted) return;
        if (!context) {
          listState = { kind: 'failure' };
          return;
        }
        loadMore = () => controller.loadMore(context.csrf, abort.signal);
        await controller.load(context.csrf, abort.signal);
      } catch {
        if (!abort.signal.aborted) listState = { kind: 'failure' };
      }
    };
    void reload();
    return () => {
      abort.abort();
      unsubscribe();
      controller.dispose();
    };
  });
</script>

<svelte:head><title>People · SpoolIn</title></svelte:head>
<header class="list-page-heading">
  <div>
    <h1 tabindex="-1">People</h1>
    <p>Keep public names and biographies accurate across the catalog.</p>
  </div>
  <a class="button" href={resolve('/manage/people/new')}>Create person</a>
</header>

{#if listState.kind === 'loading'}
  <p role="status">Loading people…</p>
{:else if listState.kind === 'failure'}
  <Notice title="People could not load" tone="error">
    <p>Try again after confirming your Maintainer session.</p>
    <button type="button" onclick={() => void reload()}>Retry people</button>
  </Notice>
{:else if !listState.people.length}
  <Notice title="No people available" tone="info">
    <p>No active catalog people match the current managed list.</p>
  </Notice>
{:else}
  <div class="table-wrap">
    <table class="desktop-records">
      <thead>
        <tr>
          <th scope="col">Name</th>
          <th scope="col">Actions</th>
        </tr>
      </thead>
      <tbody>
        {#each listState.people as row (row.id)}
          <tr>
            <td data-label="Name">{row.name}</td>
            <td data-label="Actions">
              <a
                class="button secondary"
                href={resolve('/manage/people/[id]', { id: row.id })}>Edit</a
              >
              <a
                class="button secondary"
                href={resolve('/people/[id]', { id: row.id })}>Review</a
              >
            </td>
          </tr>
        {/each}
      </tbody>
    </table>
  </div>
  <ul class="mobile-records" aria-label="Managed people">
    {#each listState.people as row (row.id)}
      <li>
        <strong>{row.name}</strong>
        <div class="record-actions">
          <a
            class="button secondary"
            href={resolve('/manage/people/[id]', { id: row.id })}>Edit</a
          >
          <a
            class="button secondary"
            href={resolve('/people/[id]', { id: row.id })}>Review</a
          >
        </div>
      </li>
    {/each}
  </ul>
  {#if listState.pageError}<p role="alert">{listState.pageError}</p>{/if}
  {#if listState.pageInfo?.hasNextPage}<button
      type="button"
      disabled={listState.loadingMore}
      onclick={() => void loadMore()}
    >
      {listState.pageError ? 'Retry more people' : 'Load more people'}
    </button>{/if}
{/if}

<style>
  .list-page-heading {
    display: flex;
    align-items: end;
    justify-content: space-between;
    flex-wrap: wrap;
    gap: var(--primitive-space-4);
    margin-bottom: var(--primitive-space-6);
  }
  .list-page-heading h1 {
    margin-bottom: var(--primitive-space-2);
  }
  .list-page-heading p {
    margin: 0;
    color: var(--muted);
  }
  .mobile-records {
    display: none;
  }
  .table-wrap {
    overflow-x: auto;
    margin: 1rem 0;
  }
  table {
    width: 100%;
    border-collapse: collapse;
    font-size: 14px;
  }
  th,
  td {
    text-align: left;
    padding: var(--primitive-space-4) var(--primitive-space-3);
    border-bottom: 1px solid var(--line);
    overflow-wrap: anywhere;
  }

  th {
    color: var(--muted);
    font-size: 12px;
    font-weight: 600;
  }
  td:first-child {
    font-weight: 600;
  }
  td :global(.button) {
    margin: 0 var(--primitive-space-2) var(--primitive-space-2) 0;
    font-size: var(--primitive-type-small);
    padding: var(--primitive-space-2) var(--primitive-space-3);
    text-decoration: none;
  }
  tbody tr:hover {
    background: var(--surface);
  }
  @media (max-width: 699px) {
    .list-page-heading {
      align-items: stretch;
    }
    .list-page-heading > a {
      width: 100%;
    }
    .table-wrap {
      display: none;
    }
    .mobile-records {
      display: grid;
      gap: var(--primitive-space-3);
      padding: 0;
      margin: 0 0 var(--primitive-space-6);
      list-style: none;
    }
    .mobile-records li {
      display: grid;
      gap: var(--primitive-space-3);
      min-width: 0;
      padding: var(--primitive-space-4);
      border: 1px solid var(--line);
      border-radius: var(--radius);
      background: var(--surface);
      overflow-wrap: anywhere;
    }
    .record-actions {
      display: flex;
      flex-wrap: wrap;
      gap: var(--primitive-space-2);
    }
    .record-actions .button {
      flex: 1 1 7rem;
      text-align: center;
    }
  }
</style>
