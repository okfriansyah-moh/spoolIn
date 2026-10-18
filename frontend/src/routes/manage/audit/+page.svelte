<script lang="ts">
  import { onMount } from 'svelte';
  import { createTransport } from '../../../lib/transport/graphql';
  import { fetchAuditPage, type AuditRow } from '../../../lib/features/audit/api';
  import { loadWriteContext } from '../../../lib/features/movie/write-context';

  let rows = $state<readonly AuditRow[]>([]);
  let message = $state('Loading history…');
  onMount(() => {
    const transport = createTransport();
    const abort = new AbortController();
    void (async () => {
      const context = await loadWriteContext(abort.signal);
      if (!context) {
        message = 'Sign in as a Maintainer to review history.';
        return;
      }
      try {
        rows = await fetchAuditPage(transport, context.csrf, abort.signal);
        message = rows.length ? '' : 'No history entries yet.';
      } catch {
        message = 'History could not be loaded.';
      }
    })();
    return () => abort.abort();
  });
</script>

<svelte:head><title>Audit history · SpoolIn</title></svelte:head>
<h1 tabindex="-1">Audit history</h1>
<p class="muted">Read-only catalog and approval outcomes.</p>

{#if message}
  <p role="status">{message}</p>
{:else}
  <table>
    <thead>
      <tr>
        <th scope="col">When</th>
        <th scope="col">Actor</th>
        <th scope="col">Action</th>
        <th scope="col">Outcome</th>
      </tr>
    </thead>
    <tbody>
      {#each rows as row}
        <tr>
          <td>{row.createdAt}</td>
          <td>{row.actorRef ?? '—'}</td>
          <td>{row.action}</td>
          <td>{row.outcome}</td>
        </tr>
      {/each}
    </tbody>
  </table>
{/if}

<style>
  table {
    width: 100%;
    border-collapse: collapse;
  }
  th,
  td {
    text-align: left;
    padding: 0.5rem;
    border-bottom: 1px solid color-mix(in srgb, currentColor 20%, transparent);
  }
  .muted {
    margin-bottom: 1rem;
  }
</style>
