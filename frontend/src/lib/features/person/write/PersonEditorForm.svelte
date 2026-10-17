<script lang="ts">
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
    onDraftChange,
  }: {
    state: EditorState;
    policy: Policy;
    disabled?: boolean;
    onSubmit: () => void;
    onCheckOriginal: () => void;
    onReviewStale: () => void;
    onDraftChange: (field: 'name' | 'biography', value: string) => void;
  } = $props();

  const nameBound = $derived(boundFor(policy, 'personName'));
  const errorFor = (id: string) => state.fieldErrors.find((error) => error.id === id)?.message ?? '';
  const submitLabel = $derived(state.mode === 'create' ? 'Save person' : 'Save person');
  const submitDisabled = $derived(
    disabled ||
      state.phase === 'submitting' ||
      state.phase === 'uncertain' ||
      state.phase === 'stale' ||
      state.phase === 'target-changed',
  );
</script>

<PersonSaveFeedback {state} {onCheckOriginal} {onReviewStale} />

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
      disabled={disabled || submitDisabled}
      aria-invalid={!!errorFor('person-name')}
      aria-describedby="person-name-help person-name-error"
      oninput={(event) => onDraftChange('name', event.currentTarget.value)}
    />
    <small id="person-name-help"
      >{nameBound.minCodePoints}–{nameBound.maxCodePoints} Unicode code points. Mononyms and punctuation stay
      intact.</small
    >
    {#if errorFor('person-name')}<p class="field-error" id="person-name-error">{errorFor('person-name')}</p>{/if}
  </div>
  <div class="field">
    <label for="person-biography">Biography · Optional</label>
    <textarea
      id="person-biography"
      rows="4"
      disabled={disabled || submitDisabled}
      aria-invalid={!!errorFor('person-biography')}
      value={state.draft.biography ?? ''}
      oninput={(event) => onDraftChange('biography', event.currentTarget.value)}
    ></textarea>
    {#if errorFor('person-biography')}<p class="field-error">{errorFor('person-biography')}</p>{/if}
  </div>
  <Button type="submit" disabled={submitDisabled}>{submitLabel}</Button>
</form>

<style>
  .field {
    margin-bottom: 1rem;
  }
  .field-error {
    color: var(--color-danger, #c0392b);
  }
</style>
