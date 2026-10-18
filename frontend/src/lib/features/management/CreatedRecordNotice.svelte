<script lang="ts">
  import { tick } from 'svelte';
  import { page } from '$app/state';
  import { resolve } from '$app/paths';
  import { isRecordId } from '../../components/safe-url';
  import Notice from '../../components/Notice.svelte';
  import { createTransport } from '../../transport/graphql';
  import { loadWriteContext } from '../movie/write-context';
  import { fetchManagedMovie } from '../movie/api';
  import { fetchManagedPerson } from '../person/write/api';

  let {
    kind,
    readyToFocus = true,
    returnToken = '',
    onOpenManagement,
  }: {
    kind: 'movie' | 'person';
    readyToFocus?: boolean;
    returnToken?: string;
    onOpenManagement?: () => void;
  } = $props();
  const savedRecord = $derived(
    isRecordId(page.url.searchParams.get('saved') ?? ''),
  );
  const id = $derived(
    (savedRecord
      ? page.url.searchParams.get('saved')
      : page.url.searchParams.get('created')) ?? '',
  );
  let panel: HTMLElement;
  const returnSuffix = $derived(
    isRecordId(returnToken) ? '?return=' + encodeURIComponent(returnToken) : '',
  );
  $effect(() => {
    if (!savedRecord || !name || !readyToFocus) return;
    let cancelled = false;
    void tick().then(() => {
      if (cancelled || !panel) return;
      panel.focus({ preventScroll: true });
      panel.scrollIntoView({ block: 'start', behavior: 'instant' });
    });
    return () => {
      cancelled = true;
    };
  });
  let name = $state('');
  let loading = $state(false);
  let failed = $state(false);
  let attempt = $state(0);

  $effect(() => {
    const recordId = id;
    attempt;
    name = '';
    failed = false;
    if (!isRecordId(recordId)) return;
    loading = true;
    const abort = new AbortController();
    void (async () => {
      try {
        const context = await loadWriteContext(abort.signal);
        if (abort.signal.aborted) return;
        if (!context) throw new Error('Maintainer session unavailable.');
        const request = createTransport();
        const record =
          kind === 'movie'
            ? await fetchManagedMovie(
                request,
                recordId,
                context.csrf,
                abort.signal,
              )
            : await fetchManagedPerson(
                request,
                recordId,
                context.csrf,
                abort.signal,
              );
        if (abort.signal.aborted) return;
        if (!record || record.state !== 'ACTIVE')
          throw new Error('Created record unavailable.');
        name = 'title' in record ? record.title : record.name;
      } catch {
        if (!abort.signal.aborted) failed = true;
      } finally {
        if (!abort.signal.aborted) loading = false;
      }
    })();
    return () => abort.abort();
  });
</script>

{#if isRecordId(id)}
  {#if loading}
    <p role="status">Loading the saved {kind}…</p>
  {:else if failed}
    <Notice title="Saved record could not be loaded" tone="info">
      <p>Your list is still available. Retry loading the saved record.</p>
      <button type="button" onclick={() => (attempt += 1)}
        >Retry saved record</button
      >
    </Notice>
  {:else if name}
    <section
      bind:this={panel}
      tabindex="-1"
      class:saved-record={savedRecord}
      aria-label={savedRecord
        ? kind === 'movie'
          ? 'Movie saved'
          : 'Person saved'
        : 'Created record'}
    >
      <Notice
        title={savedRecord
          ? kind === 'movie'
            ? 'Movie saved'
            : 'Person saved'
          : kind === 'movie'
            ? 'Movie created'
            : 'Person created'}
        tone="success"
      >
        <p>{name} is saved.</p>
        <div class="created-actions">
          <a
            class="button secondary"
            href={kind === 'movie'
              ? resolve('/manage/movies/[id]', { id }) + returnSuffix
              : resolve('/manage/people/[id]', { id }) + returnSuffix}
            onclick={onOpenManagement}>Edit {kind}</a
          >
          <a
            class="button secondary"
            href={kind === 'movie'
              ? resolve('/titles/[id]', { id })
              : resolve('/people/[id]', { id })}>Review public record</a
          >
          {#if savedRecord && kind === 'movie'}
            <a
              class="button secondary"
              href={resolve('/manage/movies/[id]', { id }) +
                '?step=credits' +
                (isRecordId(returnToken)
                  ? '&return=' + encodeURIComponent(returnToken)
                  : '')}
              onclick={onOpenManagement}>Cast and creators</a
            >
          {/if}
        </div>
      </Notice>
    </section>
  {/if}
{/if}

<style>
  .saved-record {
    border: 2px solid var(--accent);
    border-radius: var(--radius);
    scroll-margin-top: var(--primitive-space-4);
    margin-bottom: var(--primitive-space-6);
  }
  .created-actions {
    display: flex;
    flex-wrap: wrap;
    gap: var(--primitive-space-2);
  }
</style>
