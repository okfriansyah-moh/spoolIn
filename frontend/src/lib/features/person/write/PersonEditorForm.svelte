<script lang="ts">
  import { resolve } from '$app/paths';
  import Button from '../../../components/Button.svelte';
  import type { Policy } from '../../../policy/types';
  import type { EditorState } from './types';
  import PersonSaveFeedback from './PersonSaveFeedback.svelte';
  import { boundFor } from '../../../policy/rules';

  let {
    state,
    policy,
    disabled = false,
    onSubmit,
    onCheckOriginal,
    onReviewStale,
    onRebase,
    onSelectDraft,
    onStartNewDraft,
    onDraftChange,
  }: {
    state: EditorState;
    policy: Policy;
    disabled?: boolean;
    onSubmit: () => void;
    onCheckOriginal: () => void;
    onReviewStale: () => void;
    onRebase: (reviewed: boolean) => void;
    onSelectDraft: (id: string) => void;
    onStartNewDraft: () => void;
    onDraftChange: (field: 'name' | 'biography', value: string) => void;
  } = $props();

  const nameBound = $derived(boundFor(policy, 'personName'));
  const errorFor = (id: string) =>
    state.fieldErrors.find((error) => error.id === id)?.message ?? '';
  const submitLabel = $derived(
    state.mode === 'create' ? 'Save person' : 'Save person',
  );
  const submitDisabled = $derived(
    disabled ||
      state.phase === 'submitting' ||
      state.phase === 'uncertain' ||
      state.phase === 'stale' ||
      state.phase === 'target-changed',
  );
  const reviewId = $props.id();
  let reviewedCurrent = $state(false);
  $effect(() => {
    const version = state.current?.version;
    const open = state.staleReviewOpen;
    void version;
    void open;
    reviewedCurrent = false;
  });
  const inputDisabled = $derived(disabled || state.draftChoices.length > 0 || state.phase === 'submitting'
    || state.phase === 'uncertain' || state.phase === 'target-changed' || (state.mode === 'create' && state.saved !== null));
</script>

<PersonSaveFeedback {state} {onCheckOriginal} {onReviewStale} />

{#if state.phase === 'idle' && state.message && !state.draftChoices.length}<p role="status">{state.message}</p>{/if}
{#if state.draftChoices.length}
  <section class="recovery-panel" aria-labelledby="{reviewId}-drafts">
    <h2 id="{reviewId}-drafts">Choose a retained draft</h2>
    <p role="status">{state.message}</p>
    <ul>
      {#each state.draftChoices as choice (choice.id)}
        <li><Button type="button" variant="secondary" onclick={() => onSelectDraft(choice.id)}>Resume {choice.label}</Button>
          <small>Available until <time datetime={choice.expiresAt}>{choice.expiresAt}</time></small></li>
      {/each}
    </ul>
    <Button type="button" variant="secondary" onclick={onStartNewDraft}>Start a new draft from current values</Button>
  </section>
{/if}
{#if state.phase === 'stale' && state.staleReviewOpen && state.current}
  <section class="recovery-panel" aria-labelledby="{reviewId}-current">
    <h2 id="{reviewId}-current">Review current saved person values</h2>
    <dl>
      <dt>Professional name</dt><dd>{state.current.name}</dd>
      <dt>Biography</dt><dd>{state.current.biography ?? 'Unknown'}</dd>
    </dl>
    {#if state.draftBaseKnown}
      <p>Compare these values with your retained form edits. Continuing keeps your changed fields and adopts current values for fields you did not change.</p>
    {:else}
      <p>The older base values are unavailable after reload. Continuing applies every field shown in your retained form. Review each field carefully.</p>
    {/if}
    <label class="review-check" for="{reviewId}-reviewed"><input id="{reviewId}-reviewed" type="checkbox" bind:checked={reviewedCurrent} />I reviewed the current values and my retained edits.</label>
    <Button type="button" variant="secondary" disabledReason={reviewedCurrent ? '' : 'Review the current values first.'}
      onclick={() => onRebase(reviewedCurrent)}>Use reviewed version for my edits</Button>
  </section>
{/if}
{#if state.mode === 'create' && state.saved}
  <section class="recovery-panel" aria-label="Continue with this person">
    <a href={resolve('/manage/people/[id]', { id: state.saved.id })}>Edit saved person</a>
    <a href={resolve('/people/[id]', { id: state.saved.id })}>Review public record</a>
  </section>
{/if}

<form
  id="person-editor-form"
  aria-label={state.mode === 'create' ? 'Create person' : 'Edit person'}
  onsubmit={(event) => {
    event.preventDefault();
    onSubmit();
  }}
>
  <div class="field">
    <label for="person-name">Professional name · Required</label>
    <input
      id="person-name"
      value={state.draft.name ?? ''}
      required
      disabled={inputDisabled}
      aria-invalid={!!errorFor('person-name')}
      aria-describedby="person-name-help person-name-error"
      oninput={(event) => onDraftChange('name', event.currentTarget.value)}
    />
    <small id="person-name-help"
      >{nameBound.minCodePoints}–{nameBound.maxCodePoints} Unicode code points. Mononyms
      and punctuation stay intact.</small
    >
    {#if errorFor('person-name')}<p class="field-error" id="person-name-error">
        {errorFor('person-name')}
      </p>{/if}
  </div>
  <div class="field">
    <label for="person-biography">Biography · Optional</label>
    <textarea
      id="person-biography"
      rows="4"
      disabled={inputDisabled}
      aria-invalid={!!errorFor('person-biography')}
      value={state.draft.biography ?? ''}
      oninput={(event) => onDraftChange('biography', event.currentTarget.value)}
    ></textarea>
    {#if errorFor('person-biography')}<p class="field-error">
        {errorFor('person-biography')}
      </p>{/if}
  </div>
  <Button
    type="submit"
    pending={state.phase === 'submitting'}
    disabledReason={submitDisabled
      ? 'Review current changes before saving again.'
      : ''}>{submitLabel}</Button
  >
</form>

<style>
  .recovery-panel { display: grid; gap: var(--primitive-space-3); padding: var(--primitive-space-4); border: 1px solid var(--line); border-radius: var(--radius); margin-block: var(--primitive-space-4); }
  .recovery-panel ul { display: grid; gap: var(--primitive-space-3); padding-inline-start: var(--primitive-space-6); }
  .recovery-panel li { display: grid; gap: var(--primitive-space-2); }
  .recovery-panel dl { margin: 0; }
  .recovery-panel dd { margin-inline-start: 0; margin-block-end: var(--primitive-space-3); white-space: pre-wrap; }
  .review-check { display: flex; gap: var(--primitive-space-2); align-items: start; }
  .review-check input { width: auto; }

  .field {
    margin-bottom: 1rem;
  }
  .field-error {
    color: var(--danger);
  }
</style>
