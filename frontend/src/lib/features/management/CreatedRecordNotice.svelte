<script lang="ts">
  import { page } from '$app/state';
  import { resolve } from '$app/paths';
  import { isRecordId } from '../../components/safe-url';
  import Notice from '../../components/Notice.svelte';
  import { createTransport } from '../../transport/graphql';
  import { loadWriteContext } from '../movie/write-context';
  import { fetchManagedMovie } from '../movie/api';
  import { fetchManagedPerson } from '../person/write/api';

  let { kind }: { kind: 'movie' | 'person' } = $props();
  const id = $derived(page.url.searchParams.get('created') ?? '');
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
    <p role="status">Loading the created {kind}…</p>
  {:else if failed}
    <Notice title="Created record could not be loaded" tone="info">
      <p>Your list is still available. Retry loading the created record.</p>
      <button type="button" onclick={() => (attempt += 1)}
        >Retry created record</button
      >
    </Notice>
  {:else if name}
    <Notice
      title={kind === 'movie' ? 'Movie created' : 'Person created'}
      tone="success"
    >
      <p>{name} is saved.</p>
      <div class="created-actions">
        <a
          class="button secondary"
          href={kind === 'movie'
            ? resolve('/manage/movies/[id]', { id })
            : resolve('/manage/people/[id]', { id })}>Edit {kind}</a
        >
        <a
          class="button secondary"
          href={kind === 'movie'
            ? resolve('/titles/[id]', { id })
            : resolve('/people/[id]', { id })}>Review public record</a
        >
      </div>
    </Notice>
  {/if}
{/if}

<style>
  .created-actions {
    display: flex;
    flex-wrap: wrap;
    gap: var(--primitive-space-2);
  }
</style>
