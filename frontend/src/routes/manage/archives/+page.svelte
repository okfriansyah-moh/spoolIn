<script lang="ts">
  import ConfirmDialog from '../../../lib/components/ConfirmDialog.svelte';
  import { recoverCatalogOperation } from '../../../lib/features/movie/api';
  import { LifecycleFailure } from '../../../lib/features/archive/api';
  import { appendDistinct, type PageInfo } from '../../../lib/state/pagination';
  import { onMount, tick } from 'svelte';
  import { createTransport } from '../../../lib/transport/graphql';
  import { sessionController } from '../../../lib/features/session/context';
  import { loadWriteContext } from '../../../lib/features/movie/write-context';
  import {
    fetchArchivedPage,
    fetchDeletionImpact,
    runMovieLifecycle,
    runPersonLifecycle,
    type ArchiveRow,
  } from '../../../lib/features/archive/api';
  import {
    formatDeletionImpactSummary,
    permanentDeleteReady,
  } from '../../../lib/features/archive/rules';
  import type {
    ArchiveListState,
    ArchiveTab,
  } from '../../../lib/features/archive/types';

  const sessionCtl = sessionController();
  let tab = $state<ArchiveTab>('movies');
  let listState = $state<ArchiveListState>({ kind: 'loading' });
  let confirmOpen = $state(false);
  let selected = $state<ArchiveRow | null>(null);
  let impactSummary = $state('');
  let impactToken = $state<string | null>(null);
  let busy = $state(false);
  let csrf = $state('');
  let message = $state('');
  type Intent = {
    operationId: string;
    row: ArchiveRow;
    tab: ArchiveTab;
    action: 'RESTORE' | 'DELETE';
    impactToken?: string;
  };
  let intent = $state<Intent | null>(null);
  let listGeneration = 0;
  let activeList: AbortController | null = null;

  function listCurrent(generation: number, signal?: AbortSignal): boolean {
    return generation === listGeneration && !signal?.aborted;
  }

  async function reload(signal?: AbortSignal) {
    if (!csrf) return;
    const generation = ++listGeneration;
    const selectedTab = tab;
    activeList?.abort();
    activeList = new AbortController();
    listState = { kind: 'loading' };
    const role = sessionCtl.snapshot.identity?.role;
    if (role !== 'MAINTAINER') {
      listState = { kind: 'denied' };
      return;
    }
    try {
      const page = await fetchArchivedPage(
        createTransport(),
        selectedTab,
        csrf,
        null,
        activeList.signal,
      );
      if (!listCurrent(generation, signal)) return;
      const items = page.nodes;
      listState =
        items.length === 0
          ? { kind: 'empty' }
          : {
              kind: 'ready',
              tab: selectedTab,
              items: [...items],
              pageInfo: page.pageInfo,
            };
    } catch {
      if (!listCurrent(generation, signal)) return;
      listState = {
        kind: 'error',
        message: 'Archives could not be loaded. Try again.',
      };
    }
  }

  onMount(() => {
    const abort = new AbortController();
    void loadWriteContext(abort.signal)
      .then((context) => {
        if (abort.signal.aborted) return;
        if (!context) {
          listState = { kind: 'denied' };
          return;
        }
        csrf = context.csrf;
        void reload(abort.signal);
      })
      .catch(() => {
        if (!abort.signal.aborted)
          listState = {
            kind: 'error',
            message:
              'Archives could not be loaded. Reload this page to check your current session.',
          };
      });
    return () => {
      abort.abort();
      activeList?.abort();
      listGeneration += 1;
    };
  });

  function archivePageReady(state: ArchiveListState): state is Extract<
    ArchiveListState,
    { kind: 'ready' }
  > & {
    pageInfo: PageInfo;
  } {
    return (
      state.kind === 'ready' &&
      !state.loadingMore &&
      !!state.pageInfo?.hasNextPage
    );
  }

  async function moreArchived() {
    const current = listState;
    if (!archivePageReady(current) || !activeList) return;
    const generation = listGeneration;
    const signal = activeList.signal;
    listState = { ...current, loadingMore: true, pageError: null };
    try {
      const page = await fetchArchivedPage(
        createTransport(),
        current.tab,
        csrf,
        current.pageInfo.endCursor,
        signal,
      );
      if (generation !== listGeneration || signal.aborted) return;
      listState = {
        ...current,
        items: appendDistinct(current.items, page.nodes),
        pageInfo: page.pageInfo,
        loadingMore: false,
      };
    } catch {
      if (generation === listGeneration && !signal.aborted)
        listState = {
          ...current,
          loadingMore: false,
          pageError: 'More archives could not be loaded. Retry this page.',
        };
    }
  }

  async function switchTab(next: ArchiveTab) {
    tab = next;
    await reload();
  }

  async function archiveTabKey(event: KeyboardEvent) {
    if (busy || intent) return;
    const moves: Readonly<Record<string, ArchiveTab>> = {
      ArrowLeft: tab === 'movies' ? 'people' : 'movies',
      ArrowRight: tab === 'movies' ? 'people' : 'movies',
      Home: 'movies',
      End: 'people',
    };
    const next = moves[event.key];
    if (!next) return;
    event.preventDefault();
    const loading = switchTab(next);
    await tick();
    document.getElementById(`archives-tab-${next}`)?.focus();
    await loading;
  }

  async function restoreRow(row: ArchiveRow) {
    if (!csrf || busy || intent) return;
    intent = { operationId: crypto.randomUUID(), row, tab, action: 'RESTORE' };
    await submitOriginal();
  }

  async function openConfirm(row: ArchiveRow) {
    if (busy || intent) return;
    selected = row;
    impactSummary = '';
    impactToken = null;
    busy = true;
    try {
      if (tab === 'people') {
        const impact = await fetchDeletionImpact(
          createTransport(),
          row.id,
          csrf,
        );
        impactToken = impact.token;
        impactSummary = formatDeletionImpactSummary(
          impact.affectedCreditCount,
          impact.affectedTitles,
        );
      }
      confirmOpen = true;
    } catch {
      message =
        'The complete deletion impact could not be loaded. No deletion was submitted.';
    } finally {
      busy = false;
    }
  }

  async function confirmDelete() {
    if (
      !selected ||
      !csrf ||
      busy ||
      intent ||
      !permanentDeleteReady(tab, impactToken)
    )
      return;
    intent = {
      operationId: crypto.randomUUID(),
      row: selected,
      tab,
      action: 'DELETE',
      ...(impactToken ? { impactToken } : {}),
    };
    confirmOpen = false;
    await submitOriginal();
  }

  async function finish(state: string, expired = false) {
    if (expired || state === 'EXPIRED') {
      message =
        'The original result has expired. Reload and inspect the current record before another action.';
    } else if (state === 'SUCCEEDED') {
      message = 'The change is confirmed.';
      intent = null;
      selected = null;
      await reload();
    } else if (state === 'FAILED') {
      message =
        'The change failed. Review the current record before trying again.';
      intent = null;
      await reload();
    } else {
      message =
        'The result is pending. Check the original result before another action.';
    }
  }

  async function submitOriginal() {
    if (!intent || !csrf || busy) return;
    busy = true;
    try {
      const original = intent;
      const run =
        original.tab === 'movies' ? runMovieLifecycle : runPersonLifecycle;
      await finish(
        await run(createTransport(), csrf, {
          operationId: original.operationId,
          id: original.row.id,
          expectedVersion: original.row.version,
          action: original.action,
          confirmed: true,
          ...(original.impactToken
            ? { impactToken: original.impactToken }
            : {}),
        }),
      );
    } catch (error) {
      if (
        error instanceof LifecycleFailure &&
        error.code !== 'RESULT_EXPIRED'
      ) {
        intent = null;
        message =
          error.code === 'IMPACT_CHANGED'
            ? 'The deletion impact changed. Load and confirm the current impact before deletion.'
            : 'The change failed. Reload the current record before trying again.';
        await reload();
      } else {
        message =
          'The result is uncertain. Check the original result or retry the same request.';
      }
    } finally {
      busy = false;
    }
  }

  async function checkOriginal() {
    if (!intent || !csrf || busy) return;
    busy = true;
    const abort = new AbortController();
    try {
      const receipt = await recoverCatalogOperation(
        createTransport(),
        intent.operationId,
        csrf,
        abort.signal,
      );
      await finish(
        receipt?.state ?? 'UNKNOWN',
        receipt?.resultExpired ?? false,
      );
    } catch {
      message =
        'The original result could not be checked. Keep the original request for retry.';
    } finally {
      busy = false;
    }
  }
</script>

<section class="stack" aria-label="Archived records">
  <h1>Archives</h1>
  <p>
    Maintainer-only retained movies and people. Restore or confirm permanent
    deletion separately.
  </p>

  <div class="archive-tabs" role="tablist" aria-label="Archive kind">
    <button
      type="button"
      role="tab"
      id="archives-tab-movies"
      aria-controls="archives-panel"
      tabindex={tab === 'movies' ? 0 : -1}
      aria-selected={tab === 'movies'}
      disabled={busy || intent !== null}
      onclick={() => switchTab('movies')}
      onkeydown={(event) => void archiveTabKey(event)}
    >
      Movies
    </button>
    <button
      type="button"
      role="tab"
      id="archives-tab-people"
      aria-controls="archives-panel"
      tabindex={tab === 'people' ? 0 : -1}
      aria-selected={tab === 'people'}
      disabled={busy || intent !== null}
      onclick={() => switchTab('people')}
      onkeydown={(event) => void archiveTabKey(event)}
    >
      People
    </button>
  </div>

  <div
    id="archives-panel"
    role="tabpanel"
    aria-labelledby={`archives-tab-${tab}`}
    tabindex="0"
  >
    {#if listState.kind === 'loading'}
      <p role="status">Loading archives…</p>
    {:else if listState.kind === 'denied'}
      <p role="alert">You do not have access to private archives.</p>
    {:else if listState.kind === 'error'}
      <p role="alert">{listState.message}</p>
      {#if csrf}<button type="button" onclick={() => void reload()}
          >Retry archives</button
        >{/if}
    {:else if listState.kind === 'empty'}
      <p>No archived records in this view.</p>
    {:else}
      <ul>
        {#each listState.items as item (item.id)}
          <li class="archive-record">
            <div class="archive-record-heading">
              <strong>{item.label}</strong>
              <span>{item.state}</span>
            </div>
            <div class="archive-actions">
              <button
                type="button"
                class="secondary"
                disabled={busy || intent !== null}
                onclick={() => restoreRow(item)}>Restore</button
              >
              <button
                type="button"
                class="danger"
                disabled={busy || intent !== null}
                onclick={() => openConfirm(item)}>Delete permanently</button
              >
            </div>
          </li>
        {/each}
      </ul>
      {#if listState.pageError}<p role="alert">{listState.pageError}</p>{/if}
      {#if listState.pageInfo?.hasNextPage}<button
          type="button"
          disabled={listState.loadingMore || busy || !!intent}
          onclick={() => void moreArchived()}
        >
          {listState.pageError ? 'Retry more archives' : 'Load more archives'}
        </button>{/if}
    {/if}
  </div>
  <p role="status">{message}</p>
  {#if intent}
    <div class="actions">
      <button type="button" disabled={busy} onclick={() => void checkOriginal()}
        >Check original result</button
      >
      <button
        type="button"
        disabled={busy}
        onclick={() => void submitOriginal()}>Retry original request</button
      >
    </div>
  {/if}
  <ConfirmDialog
    bind:open={confirmOpen}
    title="Confirm permanent deletion"
    action="Permanently delete"
    pending={busy || !permanentDeleteReady(tab, impactToken)}
    onconfirm={() => void confirmDelete()}
  >
    <p>This action is irreversible for <strong>{selected?.label}</strong>.</p>
    {#if impactSummary}<p>{impactSummary}</p>{/if}
  </ConfirmDialog>
</section>

<style>
  .stack {
    display: flex;
    flex-direction: column;
    gap: 1rem;
  }
  .actions {
    display: flex;
    flex-wrap: wrap;
    gap: 0.75rem;
  }
  .archive-tabs {
    display: flex;
    flex-wrap: wrap;
    gap: var(--primitive-space-1);
    width: fit-content;
    max-width: 100%;
    padding: var(--primitive-space-1);
    border: 1px solid var(--line);
    border-radius: calc(var(--radius) + var(--primitive-space-1));
    background: var(--surface);
  }
  .archive-tabs [role='tab'] {
    border-color: transparent;
    background: transparent;
    color: var(--muted);
  }
  .archive-tabs [role='tab'][aria-selected='true'] {
    border-color: var(--line);
    background: var(--selected);
    color: var(--text);
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
    gap: var(--primitive-space-3);
    align-items: center;
    flex-wrap: wrap;
  }
  .archive-record {
    justify-content: space-between;
    padding: var(--primitive-space-4);
    border: 1px solid var(--line);
    border-radius: var(--radius);
    background: var(--surface);
  }
  .archive-record-heading {
    display: grid;
    min-width: 0;
    gap: var(--primitive-space-1);
    overflow-wrap: anywhere;
  }
  .archive-record-heading span {
    color: var(--muted);
    font-size: var(--primitive-type-small);
  }
  .archive-actions {
    display: flex;
    flex-wrap: wrap;
    gap: var(--primitive-space-2);
  }
  @media (max-width: 699px) {
    .archive-tabs {
      display: grid;
      grid-template-columns: repeat(2, minmax(0, 1fr));
      width: 100%;
    }
    .archive-record {
      align-items: stretch;
    }
    .archive-record-heading,
    .archive-actions {
      flex-basis: 100%;
    }
    .archive-actions button {
      flex: 1 1 100%;
    }
  }
</style>
