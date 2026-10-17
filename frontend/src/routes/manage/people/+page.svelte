<script lang="ts">
  import { onMount } from 'svelte';
  import { resolve } from '$app/paths';
  import Notice from '../../../lib/components/Notice.svelte';
  import { createTransport } from '../../../lib/transport/graphql';
  import { createPersonListController } from '../../../lib/features/person/write/list-controller';
  import { loadWriteContext } from '../../../lib/features/movie/write-context';
  import type { ListState } from '../../../lib/features/person/write/types';

  let listState = $state<ListState>({ kind: 'loading' });

  onMount(() => {
    const transport = createTransport();
    const controller = createPersonListController(transport);
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

<svelte:head><title>People · SpoolIn</title></svelte:head>
<h1 tabindex="-1">People</h1>
<a class="button" href={resolve('/manage/people/new')}>Create person</a>

{#if listState.kind === 'loading'}
  <p role="status">Loading people…</p>
{:else if listState.kind === 'failure'}
  <Notice title="People could not load" tone="error">
    <p>Try again after confirming your Maintainer session.</p>
  </Notice>
{:else if !listState.people.length}
  <Notice title="No people available" tone="info">
    <p>No active catalog people match the current managed list.</p>
  </Notice>
{:else}
  <div class="table-wrap">
    <table>
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
              <a class="button secondary" href={resolve(`/manage/people/${row.id}`)}>Edit</a>
              <a class="button secondary" href={resolve(`/people/${row.id}`)}>Review</a>
            </td>
          </tr>
        {/each}
      </tbody>
    </table>
  </div>
{/if}

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
  }
</style>
