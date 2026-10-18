<script lang="ts">
  import { onMount } from 'svelte';
  import CreatedRecordNotice from '../../../lib/features/management/CreatedRecordNotice.svelte';
  import { resolve } from '$app/paths';
  import Button from '../../../lib/components/Button.svelte';
  import Notice from '../../../lib/components/Notice.svelte';
  import { createTransport } from '../../../lib/transport/graphql';
  import { createMovieListController } from '../../../lib/features/movie/list-controller';
  import { loadWriteContext } from '../../../lib/features/movie/write-context';
  import type { ListState } from '../../../lib/features/movie/types';

  let controller: ReturnType<typeof createMovieListController> | null = null;
  let csrf = '';
  let signal: AbortSignal | null = null;
  let listState = $state<ListState>({ kind: 'loading' });

  let searchText = $state('');
  let appliedSearch = $state('');
  let search = () => {};

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

  onMount(() => {
    const transport = createTransport();
    controller = createMovieListController(transport);
    const unsubscribe = controller.subscribe((next) => {
      listState = next;
    });
    const abort = new AbortController();
    signal = abort.signal;
    void loadWriteContext(abort.signal).then((context) => {
      if (!context || abort.signal.aborted) return;
      csrf = context.csrf;
      search = () => {
        void controller?.load(csrf, abort.signal, appliedSearch);
      };
      search();
    });
    return () => {
      abort.abort();
      unsubscribe();
      controller?.dispose();
    };
  });
</script>

<svelte:head><title>Movies · SpoolIn</title></svelte:head>
<header class="list-page-heading">
  <div>
    <h1 tabindex="-1">Movies</h1>
    <p>Manage movie records and review the titles in your catalog.</p>
  </div>
  <a class="button" href={resolve('/manage/movies/new')}>Create movie</a>
</header>

<CreatedRecordNotice kind="movie" />

<form class="list-search" role="search" onsubmit={submitSearch}>
  <label for="managed-movies-search">Search movies</label>
  <div class="search-controls">
    <input
      id="managed-movies-search"
      type="search"
      bind:value={searchText}
      placeholder="Movie title"
    />
    <button type="submit">Search</button>
    <button class="secondary" type="button" onclick={clearSearch}
      >Clear search</button
    >
  </div>
</form>

{#if listState.kind === 'loading'}
  <p role="status">Loading managed titles…</p>
{:else if listState.kind === 'failure'}
  <Notice title="Movies could not be loaded" tone="error">
    <p>Try again after confirming your Maintainer session.</p>
    <Button
      variant="secondary"
      onclick={() => signal && controller?.load(csrf, signal)}
      >Retry movies</Button
    >
  </Notice>
{:else if !listState.titles.length}
  <Notice title="No movies found" tone="info">
    <p>
      {appliedSearch
        ? 'No active movie titles match your search. Try another title or clear the search.'
        : 'No active movies are available.'}
    </p>
  </Notice>
{:else}
  <div class="table-wrap">
    <table class="desktop-records">
      <thead>
        <tr>
          <th scope="col">Title</th>
          <th scope="col">Release / state</th>
          <th scope="col">Actions</th>
        </tr>
      </thead>
      <tbody>
        {#each listState.titles as row (row.id)}
          <tr>
            <td data-label="Title">{row.title}</td>
            <td data-label="Release / state"
              >{row.releaseContext} · {row.state === 'ACTIVE'
                ? 'Active'
                : 'Archived'}</td
            >
            <td data-label="Actions">
              {#if row.kind === 'MOVIE' && row.state === 'ACTIVE'}
                <a
                  class="button secondary"
                  href={resolve('/manage/movies/[id]', { id: row.id })}>Edit</a
                >
                <a
                  class="button secondary"
                  href={resolve('/titles/[id]', { id: row.id })}>Review</a
                >
                <a
                  class="button secondary"
                  href={resolve('/manage/movies/[id]', { id: row.id }) +
                    '?step=credits'}>Cast and creators</a
                >
              {:else}
                <span class="muted">Read-only TV</span>
              {/if}
            </td>
          </tr>
        {/each}
      </tbody>
    </table>
  </div>
  <ul class="mobile-records" aria-label="Managed movies">
    {#each listState.titles as row (row.id)}
      <li>
        <div class="record-heading">
          <strong>{row.title}</strong>
          <span
            >{row.releaseContext} · {row.state === 'ACTIVE'
              ? 'Active'
              : 'Archived'}</span
          >
        </div>
        <div class="record-actions">
          {#if row.kind === 'MOVIE' && row.state === 'ACTIVE'}
            <a
              class="button secondary"
              href={resolve('/manage/movies/[id]', { id: row.id })}>Edit</a
            >
            <a
              class="button secondary"
              href={resolve('/titles/[id]', { id: row.id })}>Review</a
            >
            <a
              class="button secondary"
              href={resolve('/manage/movies/[id]', { id: row.id }) +
                '?step=credits'}>Cast and creators</a
            >
          {:else}
            <span class="muted">Read-only TV</span>
          {/if}
        </div>
      </li>
    {/each}
  </ul>
  {#if listState.pagingError}<p role="alert">
      More movies could not be loaded. Retry the same page.
    </p>{/if}
  {#if listState.next}
    <Button
      variant="secondary"
      pending={listState.loadingMore}
      onclick={() => signal && controller?.loadMore(csrf, signal)}
      >Load more movies</Button
    >
  {/if}
{/if}

<Notice title="TV entries are read-only" tone="info">
  <p>
    Shared-person corrections can change displayed TV credits; no direct TV
    edit, artwork, archive, or episode controls are provided here.
  </p>
</Notice>

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
    vertical-align: top;
  }
  .muted {
    opacity: 0.8;
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
      padding: var(--primitive-space-4);
      border: 1px solid var(--line);
      border-radius: var(--radius);
      background: var(--surface);
    }
    .record-heading {
      display: grid;
      gap: var(--primitive-space-1);
      overflow-wrap: anywhere;
    }
    .record-heading span {
      color: var(--muted);
      font-size: var(--primitive-type-small);
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
