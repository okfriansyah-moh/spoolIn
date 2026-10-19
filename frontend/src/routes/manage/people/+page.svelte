<script lang="ts">
  import { onMount } from 'svelte';
  import { page } from '$app/state';
  import {
    readPersonListReturn,
    rememberPersonList,
    type PersonListReturn,
  } from '../../../lib/features/person/write/list-return';
  import CreatedRecordNotice from '../../../lib/features/management/CreatedRecordNotice.svelte';
  import { resolve } from '$app/paths';
  import Notice from '../../../lib/components/Notice.svelte';
  import { createTransport } from '../../../lib/transport/graphql';
  import { createPersonListController } from '../../../lib/features/person/write/list-controller.svelte';
  import { loadWriteContext } from '../../../lib/features/movie/write-context';
  import type { ListState } from '../../../lib/features/person/write/types';

  let controller = $state.raw<ReturnType<
    typeof createPersonListController
  > | null>(null);
  let listOverride = $state.raw<ListState | null>(null);
  const listState = $derived<ListState>(
    listOverride ?? controller?.snapshot ?? { kind: 'loading' },
  );
  let reload = async () => {};
  let loadMore = async () => {};

  let searchText = $state('');
  let appliedSearch = $state('');
  let search = () => {};
  let returnToken = $state('');
  let restoring = $state(false);

  function rememberList() {
    rememberPersonList(returnToken, {
      searchText,
      appliedSearch,
      scrollY: window.scrollY,
      rowCount: listState.kind === 'ready' ? listState.people.length : 0,
    });
  }

  async function restoreDepth(
    controller: ReturnType<typeof createPersonListController>,
    saved: PersonListReturn,
    csrf: string,
    signal: AbortSignal,
  ) {
    while (!signal.aborted && controller.snapshot.kind === 'ready') {
      const before = controller.snapshot;
      if (
        before.people.length >= saved.rowCount ||
        !before.pageInfo?.hasNextPage ||
        before.pageError
      )
        break;
      await controller.loadMore(csrf, signal);
      const after = controller.snapshot;
      if (listStopped(after, before.people.length)) break;
    }
    if (!signal.aborted) restoring = false;
  }

  function listStopped(after: ListState, count: number): boolean {
    return (
      after.kind !== 'ready' ||
      !!after.pageError ||
      after.people.length <= count
    );
  }

  function submitSearch(event: SubmitEvent) {
    event.preventDefault();
    appliedSearch = searchText.trim();
    search();
  }

  function clearSearch() {
    searchText = '';
    appliedSearch = '';
    search();
  }

  let reloadGeneration = 0;
  let cachedContext: Awaited<ReturnType<typeof loadWriteContext>> = null;

  function currentLoad(signal: AbortSignal, generation: number): boolean {
    return !signal.aborted && generation === reloadGeneration;
  }

  async function reloadPeople(
    activeController: ReturnType<typeof createPersonListController>,
    saved: PersonListReturn | null,
    signal: AbortSignal,
  ) {
    const generation = ++reloadGeneration;
    listOverride = { kind: 'loading' };
    try {
      const context = cachedContext ?? (await loadWriteContext(signal));
      if (!currentLoad(signal, generation)) return;
      if (!context) {
        restoring = false;
        listOverride = { kind: 'failure' };
        return;
      }
      cachedContext = context;
      loadMore = () => activeController.loadMore(context.csrf, signal);
      listOverride = null;
      await activeController.load(context.csrf, signal, appliedSearch);
      if (
        !signal.aborted &&
        generation === reloadGeneration &&
        saved &&
        restoring
      )
        await restoreDepth(activeController, saved, context.csrf, signal);
    } catch {
      if (currentLoad(signal, generation)) {
        restoring = false;
        listOverride = { kind: 'failure' };
      }
    }
  }

  onMount(() => {
    returnToken = crypto.randomUUID();
    const saved = readPersonListReturn(
      page.url.searchParams.get('restore') ?? '',
    );
    if (saved) {
      searchText = saved.searchText;
      appliedSearch = saved.appliedSearch;
      restoring = true;
    }
    const transport = createTransport();
    controller = createPersonListController(transport);
    const activeController = controller;
    const abort = new AbortController();
    reload = () => reloadPeople(activeController, saved, abort.signal);

    search = () => {
      void reload();
    };
    void reload();
    return () => {
      abort.abort();
      activeController.dispose();
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

<CreatedRecordNotice
  kind="person"
  readyToFocus={!restoring}
  {returnToken}
  onOpenManagement={rememberList}
/>

<form class="list-search" role="search" onsubmit={submitSearch}>
  <label for="managed-people-search">Search people</label>
  <div class="search-controls">
    <input
      id="managed-people-search"
      type="search"
      bind:value={searchText}
      disabled={restoring}
      placeholder="Person name"
    />
    <button type="submit" disabled={restoring}>Search</button>
    <button
      class="secondary"
      type="button"
      disabled={restoring}
      onclick={clearSearch}>Clear search</button
    >
  </div>
</form>

{#if listState.kind === 'loading'}
  <p role="status">Loading people…</p>
{:else if listState.kind === 'failure'}
  <Notice title="People could not load" tone="error">
    <p>Try again after confirming your Maintainer session.</p>
    <button type="button" onclick={() => void reload()}>Retry people</button>
  </Notice>
{:else if !listState.people.length}
  <Notice title="No people available" tone="info">
    <p>
      {appliedSearch
        ? 'No active people match your search. Try another name or clear the search.'
        : 'No active catalog people are available.'}
    </p>
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
                href={resolve(`/manage/people/[id]?return=${returnToken}`, { id: row.id })}
                onclick={rememberList}>Edit</a
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
            href={resolve(`/manage/people/[id]?return=${returnToken}`, { id: row.id })}
            onclick={rememberList}>Edit</a
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
      disabled={listState.loadingMore || restoring}
      onclick={() => void loadMore()}
    >
      {listState.pageError ? 'Retry more people' : 'Load more people'}
    </button>{/if}
{/if}

<style>
  .list-search {
    margin-bottom: var(--primitive-space-6);
  }
  .search-controls {
    display: flex;
    flex-wrap: wrap;
    gap: var(--primitive-space-2);
  }
  .search-controls input {
    flex: 1 1 16rem;
    min-width: 0;
  }

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
