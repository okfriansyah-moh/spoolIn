<script lang="ts">
  import Notice from '../../components/Notice.svelte';
  import Button from '../../components/Button.svelte';
  import type { EditorState } from './types';
  let {
    state,
    onCheckOriginal,
    onReviewStale,
  }: {
    state: EditorState;
    onCheckOriginal: () => void;
    onReviewStale: () => void;
  } = $props();
</script>

{#if state.phase === 'saved'}
  <Notice title="Movie saved" tone="success">
    <p>
      {state.saved?.title ?? 'This movie'} has been saved under the same identity.
      Unchanged credits and metadata remain.
    </p>
  </Notice>
{:else if state.phase === 'failed'}
  <Notice title="Changes were not saved" tone="error" urgent>
    <p>{state.message}</p>
  </Notice>
{:else if state.phase === 'uncertain'}
  <Notice title="Save result not confirmed" tone="warning" urgent>
    <p>{state.message}</p>
    <Button type="button" variant="secondary" onclick={onCheckOriginal}
      >Check original result</Button
    >
  </Notice>
{:else if state.phase === 'resolved'}
  <Notice title="Original save confirmed" tone="success">
    <p>{state.message}</p>
  </Notice>
{:else if state.phase === 'stale'}
  <Notice
    title="This movie changed while you were editing"
    tone="warning"
    urgent
  >
    <p>{state.message}</p>
    <Button type="button" variant="secondary" onclick={onReviewStale}
      >{state.staleReviewOpen
        ? 'Hide current version'
        : 'Review current version'}</Button
    >
  </Notice>
{:else if state.phase === 'target-changed'}
  <Notice title="This movie is now archived" tone="warning" urgent>
    <p>{state.message}</p>
  </Notice>
{:else if state.mediaOutage}
  <Notice title="Artwork is temporarily unavailable" tone="warning">
    <p>
      Movie metadata remains readable and editable. Existing artwork is
      retained; placeholder rendering does not mean deletion.
    </p>
  </Notice>
{/if}

{#if state.fieldErrors.length}
  <div class="notice error" role="alert" tabindex="-1">
    <strong>Review these fields</strong>
    {#each state.fieldErrors as error (error.id)}
      <p><a href={`#${error.id}`}>{error.message}</a></p>
    {/each}
  </div>
{/if}

{#if state.staleReviewOpen && state.current && state.saved}
  <div class="comparison" id="comparison">
    <div>
      <h3>Current saved synopsis</h3>
      <p>{state.current.synopsis}</p>
    </div>
    <div>
      <h3>Your retained draft</h3>
      <p>{state.draft.synopsis ?? ''}</p>
    </div>
  </div>
{/if}

<style>
  .comparison {
    display: grid;
    gap: 1rem;
    grid-template-columns: repeat(auto-fit, minmax(16rem, 1fr));
    margin-top: 1rem;
  }
</style>
