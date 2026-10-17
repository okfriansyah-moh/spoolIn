<script lang="ts">
  import { onMount } from 'svelte';
  import { resolve } from '$app/paths';
  import Notice from '../../../lib/components/Notice.svelte';
  import { createTransport } from '../../../lib/transport/graphql';
  import { createMovieListController } from '../../../lib/features/movie/list-controller';
  import { loadWriteContext } from '../../../lib/features/movie/write-context';
  import type { ListState } from '../../../lib/features/movie/types';

  let listState = $state<ListState>({ kind: 'loading' });

  onMount(() => {
    const transport = createTransport();
    const controller = createMovieListController(transport);
    const unsubscribe = controller.subscribe((next) => {
      listState = next;
    });
    const abort = new AbortController();
    void loadWriteContext(abort.signal).then((context) => {
      if (!context || abort.signal.aborted) return;
      void controller.load(context.csrf, abort.signal);
    });
    return () => {
      abort.abort();
      unsubscribe();
      controller.dispose();
    };
  });
</script>

<svelte:head><title>Movies · SpoolIn</title></svelte:head>
<h1 tabindex="-1">Movies</h1>
<a class="button" href={resolve('/manage/movies/new')}>Create movie</a>

{#if listState.kind === 'loading'}
  <p role="status">Loading managed titles…</p>
{:else if listState.kind === 'failure'}
  <Notice title="Movies could not be loaded" tone="error">
    <p>Try again after confirming your Maintainer session.</p>
  </Notice>
{:else}
  <div class="table-wrap">
    <table>
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
              {:else}
                <span class="muted">Read-only TV</span>
              {/if}
            </td>
          </tr>
        {/each}
      </tbody>
    </table>
  </div>
{/if}

<Notice title="TV entries are read-only" tone="info">
  <p>
    Shared-person corrections can change displayed TV credits; no direct TV
    edit, artwork, archive, or episode controls are provided here.
  </p>
</Notice>

<style>
  .table-wrap {
    overflow-x: auto;
    margin: 1rem 0;
  }
  table {
    width: 100%;
    border-collapse: collapse;
  }
  th,
  td {
    text-align: left;
    padding: 0.5rem 0.75rem;
    vertical-align: top;
  }
  .muted {
    opacity: 0.8;
  }
</style>
