<script lang="ts">
  import Notice from '../../../components/Notice.svelte';
  import Button from '../../../components/Button.svelte';
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
  <Notice title="Person saved" tone="success">
    <p>
      {state.saved?.name ?? 'This person'} keeps the same shared identity. Visible
      credits update through that identity without merging same-named people.
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
{:else if state.phase === 'stale'}
  <Notice title="This person changed" tone="warning" urgent>
    <p>{state.message}</p>
    <Button type="button" variant="secondary" onclick={onReviewStale}
      >{state.staleReviewOpen
        ? 'Hide current record'
        : 'Review current record'}</Button
    >
  </Notice>
{:else if state.phase === 'target-changed'}
  <Notice title="Editing unavailable" tone="warning" urgent>
    <p>{state.message}</p>
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

{#if state.staleReviewOpen && state.current}
  <div class="comparison" id="comparison">
    <div>
      <h3>Current saved name</h3>
      <p>{state.current.name}</p>
    </div>
    <div>
      <h3>Your retained draft</h3>
      <p>{state.draft.name ?? ''}</p>
    </div>
  </div>
{/if}

<style>
  .comparison {
    display: grid;
    gap: 1rem;
    margin: 1rem 0;
  }
  @media (min-width: 40rem) {
    .comparison {
      grid-template-columns: 1fr 1fr;
    }
  }
</style>
