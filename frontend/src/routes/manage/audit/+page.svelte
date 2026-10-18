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

  function displayValue(value: unknown): string {
    if (
      value &&
      typeof value === 'object' &&
      'valueOmitted' in value &&
      value.valueOmitted === true
    ) {
      return `Value omitted. Retained summary: ${JSON.stringify(value)}`;
    }
    return typeof value === 'string' ? value : (JSON.stringify(value) ?? '—');
  }

  function displayChange(value: unknown): string {
    if (
      value &&
      typeof value === 'object' &&
      ('before' in value || 'after' in value)
    ) {
      const change = value as { before?: unknown; after?: unknown };
      return `Before: ${displayValue(change.before)}; After: ${displayValue(change.after)}`;
    }
    return displayValue(value);
  }

  function fieldLabel(field: string): string {
    const words = field
      .replace(/([a-z0-9])([A-Z])/g, '$1 $2')
      .replaceAll('_', ' ')
      .replaceAll('-', ' ');
    return words.charAt(0).toUpperCase() + words.slice(1);
  }
</script>

<svelte:head><title>Audit history · SpoolIn</title></svelte:head>
<h1 tabindex="-1">Audit history</h1>
<p class="muted">A clear record of catalog changes and approval outcomes.</p>
<p role="status" aria-live="polite">{loading ? 'Loading history…' : message}</p>

{#if rows.length}
  <!-- svelte-ignore a11y_no_noninteractive_tabindex (Keyboard users must be able to scroll the history table.) -->
  <div
    class="table-wrap desktop-history"
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
                  <p>
                    <strong>{fieldLabel(field)}</strong>: {displayChange(
                      change,
                    )}
                  </p>
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
  <ul class="mobile-history" aria-label="Catalog history">
    {#each rows as row (row.id)}
      <li class="history-entry">
        <article>
          <header class="history-entry-heading">
            <time datetime={row.at}>{row.at}</time>
            <span class="history-outcome">{row.outcome}</span>
          </header>
          <dl class="history-fields">
            <div>
              <dt>Actor</dt>
              <dd>{row.actorRef ?? '—'}</dd>
            </div>
            <div>
              <dt>Action</dt>
              <dd>{row.action}</dd>
            </div>
            <div>
              <dt>Record</dt>
              <dd>{row.subjectKind} {row.subjectId ?? '—'}</dd>
            </div>
          </dl>
          <details>
            <summary>Review changes</summary>
            <div class="history-changes">
              {#each Object.entries(row.changes) as [field, change]}
                <p>
                  <strong>{fieldLabel(field)}</strong>: {displayChange(change)}
                </p>
              {:else}
                <p>No field changes recorded.</p>
              {/each}
            </div>
          </details>
        </article>
      </li>
    {/each}
  </ul>
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
  .mobile-history {
    display: none;
    padding: 0;
    margin: 0;
    list-style: none;
  }
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
  .history-entry {
    padding: var(--primitive-space-4);
    border: 1px solid var(--line);
    border-radius: var(--radius);
    background: var(--surface);
  }
  .history-entry-heading {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    justify-content: space-between;
    gap: var(--primitive-space-2);
    padding-bottom: var(--primitive-space-3);
    border-bottom: 1px solid var(--line);
  }
  .history-entry-heading time {
    overflow-wrap: anywhere;
    color: var(--muted);
    font-size: var(--primitive-type-small);
  }
  .history-outcome {
    padding: var(--primitive-space-1) var(--primitive-space-2);
    border-radius: 999px;
    background: var(--selected);
  }
  .history-fields {
    display: grid;
    gap: var(--primitive-space-3);
    margin: var(--primitive-space-4) 0;
  }
  .history-fields div {
    min-width: 0;
  }
  .history-fields dt {
    font-size: var(--primitive-type-small);
  }
  .history-changes {
    max-width: 70ch;
    padding-top: var(--primitive-space-2);
  }
  .history-changes p {
    overflow-wrap: anywhere;
  }
  @media (max-width: 699px) {
    .desktop-history {
      display: none;
    }
    .mobile-history {
      display: grid;
      gap: var(--primitive-space-3);
    }
    .mobile-history li {
      min-width: 0;
    }
  }
</style>
