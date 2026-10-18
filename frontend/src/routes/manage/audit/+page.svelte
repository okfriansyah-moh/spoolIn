<script lang="ts">
  import { onMount } from 'svelte';
  import { createTransport } from '../../../lib/transport/graphql';
  import {
    fetchAuditPage,
    type AuditRow,
  } from '../../../lib/features/audit/api';
  import { loadWriteContext } from '../../../lib/features/movie/write-context';

  let rows = $state<readonly AuditRow[]>([]);
  let message = $state('Loading history…');
  let loading = $state(false);
  let endCursor = $state<string | null>(null);
  let hasNextPage = $state(false);
  let permitted = $state(false);
  let loadPage: () => Promise<void> = async () => {};

  onMount(() => {
    const transport = createTransport();
    const abort = new AbortController();
    loadPage = async () => {
      if (loading) return;
      loading = true;
      try {
        const context = await loadWriteContext(abort.signal);
        if (!context) {
          message = 'Sign in as a Maintainer to review history.';
          return;
        }
        permitted = true;
        const page = await fetchAuditPage(
          transport,
          context.csrf,
          abort.signal,
          endCursor,
        );
        if (abort.signal.aborted) return;
        rows = [...rows, ...page.nodes];
        endCursor = page.endCursor;
        hasNextPage = page.hasNextPage;
        message = rows.length ? '' : 'No history entries yet.';
      } catch {
        if (!abort.signal.aborted)
          message = 'History could not be loaded. Retry the read.';
      } finally {
        loading = false;
      }
    };
    void loadPage();
    return () => abort.abort();
  });

  function displayChange(value: unknown): string {
    return typeof value === 'string' ? value : (JSON.stringify(value) ?? '—');
  }
</script>

<svelte:head><title>Audit history · SpoolIn</title></svelte:head>
<h1 tabindex="-1">Audit history</h1>
<p class="muted">Read-only catalog and approval outcomes.</p>
<p role="status" aria-live="polite">{loading ? 'Loading history…' : message}</p>

{#if rows.length}
  <!-- svelte-ignore a11y_no_noninteractive_tabindex (Keyboard users must be able to scroll the history table.) -->
  <div
    class="table-wrap"
    tabindex="0"
    role="region"
    aria-label="Catalog history"
    aria-busy={loading}
  >
    <table>
      <thead>
        <tr>
          <th scope="col">Time</th>
          <th scope="col">Actor</th>
          <th scope="col">Action</th>
          <th scope="col">Affected record</th>
          <th scope="col">Outcome</th>
          <th scope="col">Changes</th>
        </tr>
      </thead>
      <tbody>
        {#each rows as row (row.id)}
          <tr>
            <td><time datetime={row.at}>{row.at}</time></td>
            <td>{row.actorRef ?? '—'}</td>
            <td>{row.action}</td>
            <td>{row.subjectKind} {row.subjectId ?? '—'}</td>
            <td>{row.outcome}</td>
            <td>
              <details>
                <summary>Inspect changes</summary>
                {#each Object.entries(row.changes) as [field, change]}
                  <p><strong>{field}</strong>: {displayChange(change)}</p>
                {:else}
                  <p>No field changes recorded.</p>
                {/each}
              </details>
            </td>
          </tr>
        {/each}
      </tbody>
    </table>
  </div>
{/if}
{#if (permitted && hasNextPage) || message.includes('Retry')}
  <button
    type="button"
    class="secondary"
    disabled={loading}
    onclick={() => void loadPage()}
  >
    {message.includes('Retry') ? 'Retry history' : 'Load more history'}
  </button>
{/if}

<style>
  .table-wrap {
    overflow-x: auto;
  }
  table {
    width: 100%;
    border-collapse: collapse;
  }
  th,
  td {
    text-align: left;
    vertical-align: top;
    padding: var(--space-2, 0.5rem);
    border-bottom: 1px solid var(--border, currentColor);
    overflow-wrap: anywhere;
  }
  .muted {
    margin-bottom: var(--space-4, 1rem);
  }
</style>
