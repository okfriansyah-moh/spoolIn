<script lang="ts">
  import { resolve } from '$app/paths';
  import Button from '../../components/Button.svelte';
  import type { Policy } from '../../policy/types';
  import type { EditorState } from './types';
  import { policyGuidance } from './types';
  import MovieMetadataFields from './MovieMetadataFields.svelte';
  import MovieSaveFeedback from './MovieSaveFeedback.svelte';
  let {
    state: editorState,
    policy,
    disabled = false,
    onSubmit,
    onCheckOriginal,
    onReviewStale,
    onRebase,
    onSelectDraft,
    onStartNewDraft,
    onDraftChange,
    onGenreToggle,
    onDiscardOpen,
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
    onDraftChange: (
      field: import('./draft-field-update').MovieDraftField,
      value: string,
    ) => void;
    onGenreToggle: (genreId: string, checked: boolean) => void;
    onDiscardOpen: () => void;
  } = $props();
  const titleHelp = $derived(policyGuidance(policy, 'title'));
  const synopsisHelp = $derived(policyGuidance(policy, 'synopsis'));
  const errorFor = (id: string) =>
    editorState.fieldErrors.find((error) => error.id === id)?.message ?? '';
  const submitLabel = $derived(
    editorState.mode === 'create' ? 'Create movie' : 'Save movie',
  );
  const submitDisabled = $derived(
    disabled ||
      editorState.phase === 'submitting' ||
      editorState.phase === 'uncertain' ||
      editorState.phase === 'stale' ||
      editorState.phase === 'target-changed',
  );
  const reviewId = $props.id();
  let reviewedCurrent = $state(false);
  $effect(() => {
    const version = editorState.current?.version;
    const open = editorState.staleReviewOpen;
    void version;
    void open;
    reviewedCurrent = false;
  });
  const inputDisabled = $derived(
    disabled ||
      editorState.draftChoices.length > 0 ||
      editorState.phase === 'submitting' ||
      editorState.phase === 'uncertain' ||
      editorState.phase === 'target-changed' ||
      (editorState.mode === 'create' && editorState.saved !== null),
  );
</script>

<MovieSaveFeedback state={editorState} {onCheckOriginal} {onReviewStale} />

{#if editorState.phase === 'idle' && editorState.message && !editorState.draftChoices.length}<p
    role="status"
  >
    {editorState.message}
  </p>{/if}
{#if editorState.draftChoices.length}
  <section class="recovery-panel" aria-labelledby="{reviewId}-drafts">
    <h2 id="{reviewId}-drafts">Choose a retained draft</h2>
    <p role="status">{editorState.message}</p>
    <ul>
      {#each editorState.draftChoices as choice (choice.id)}
        <li>
          <Button
            type="button"
            variant="secondary"
            onclick={() => onSelectDraft(choice.id)}
            >Resume {choice.label}</Button
          >
          <small
            >Available until <time datetime={choice.expiresAt}
              >{choice.expiresAt}</time
            ></small
          >
        </li>
      {/each}
    </ul>
    <Button type="button" variant="secondary" onclick={onStartNewDraft}
      >Start a new draft from current values</Button
    >
  </section>
{/if}
{#if editorState.phase === 'stale' && editorState.staleReviewOpen && editorState.current}
  <section class="recovery-panel" aria-labelledby="{reviewId}-current">
    <h2 id="{reviewId}-current">Review current saved movie values</h2>
    <dl>
      <dt>Title</dt>
      <dd>{editorState.current.title}</dd>
      <dt>Synopsis</dt>
      <dd>{editorState.current.synopsis}</dd>
      <dt>Original title</dt>
      <dd>{editorState.current.originalTitle ?? 'Unknown'}</dd>
      <dt>Release year</dt>
      <dd>{editorState.current.releaseYear || 'Unknown'}</dd>
      <dt>Release month / day</dt><dd>{editorState.current.releaseMonth ?? 'Unknown'} / {editorState.current.releaseDay ?? 'Unknown'}</dd>
      <dt>Runtime</dt><dd>{editorState.current.runtimeMinutes ?? 'Unknown'}</dd>
      <dt>Original language</dt><dd>{editorState.current.originalLanguage ?? 'Unknown'}</dd>
      <dt>Synopsis language</dt><dd>{editorState.current.synopsisLanguage ?? 'und'}</dd>
      <dt>Countries</dt><dd>{editorState.current.countries?.join(', ') || 'Unknown'}</dd>
      <dt>Genres</dt>
      <dd>
        {editorState.current.genreIds
          .map(
            (id) =>
              editorState.genres.find((genre) => genre.id === id)?.label ?? id,
          )
          .join(', ')}
      </dd>
    </dl>
    {#if editorState.draftBaseKnown}
      <p>
        Compare these values with your retained form edits. Continuing keeps
        your changed fields and adopts current values for fields you did not
        change.
      </p>
    {:else}
      <p>
        The older base values are unavailable after reload. Continuing applies
        every field shown in your retained form. Review each field carefully.
      </p>
    {/if}
    <label class="review-check" for="{reviewId}-reviewed"
      ><input
        id="{reviewId}-reviewed"
        type="checkbox"
        bind:checked={reviewedCurrent}
      />I reviewed the current values and my retained edits.</label
    >
    <Button
      type="button"
      variant="secondary"
      disabledReason={reviewedCurrent ? '' : 'Review the current values first.'}
      onclick={() => onRebase(reviewedCurrent)}
      >Use reviewed version for my edits</Button
    >
  </section>
{/if}
{#if editorState.mode === 'create' && editorState.saved}
  <section class="recovery-panel" aria-label="Continue with this movie">
    <a href={resolve('/manage/movies/[id]', { id: editorState.saved.id })}
      >Edit saved movie</a
    >
    <a href={resolve('/titles/[id]', { id: editorState.saved.id })}
      >Review public record</a
    >
  </section>
{/if}

<form
  id="movie-editor-form"
  aria-label={editorState.mode === 'create' ? 'Create movie' : 'Edit movie'}
  onsubmit={(event) => {
    event.preventDefault();
    onSubmit();
  }}
>
  <div class="field">
    <label for="movie-title">Title · Required</label>
    <input
      id="movie-title"
      value={editorState.draft.title ?? ''}
      required
      disabled={inputDisabled}
      aria-invalid={!!errorFor('movie-title')}
      aria-describedby="movie-title-help movie-title-error"
      oninput={(event) => onDraftChange('title', event.currentTarget.value)}
    />
    <small id="movie-title-help">{titleHelp}</small>
    {#if errorFor('movie-title')}<p class="field-error" id="movie-title-error">
        {errorFor('movie-title')}
      </p>{/if}
  </div>
  <div class="field">
    <label for="movie-synopsis">Synopsis · Required</label>
    <textarea
      id="movie-synopsis"
      rows="4"
      required
      disabled={inputDisabled}
      aria-invalid={!!errorFor('movie-synopsis')}
      aria-describedby="movie-synopsis-help movie-synopsis-error"
      value={editorState.draft.synopsis ?? ''}
      oninput={(event) => onDraftChange('synopsis', event.currentTarget.value)}
    ></textarea>
    <small id="movie-synopsis-help"
      >{synopsisHelp} Line breaks are preserved.</small
    >
    {#if errorFor('movie-synopsis')}<p
        class="field-error"
        id="movie-synopsis-error"
      >
        {errorFor('movie-synopsis')}
      </p>{/if}
  </div>
  <div class="field">
    <label for="movie-release-year">Release year · Required</label>
    <input
      id="movie-release-year"
      inputmode="numeric"
      value={editorState.draft.release?.year
        ? String(editorState.draft.release.year)
        : ''}
      required
      disabled={inputDisabled}
      aria-invalid={!!errorFor('movie-release-year')}
      aria-describedby="movie-release-year-help movie-release-year-error"
      oninput={(event) =>
        onDraftChange('releaseYear', event.currentTarget.value)}
    />
    <small id="movie-release-year-help"
      >Use a release year from 1 to 9999 for new movies.</small
    >
    {#if errorFor('movie-release-year')}<p
        class="field-error"
        id="movie-release-year-error"
      >
        {errorFor('movie-release-year')}
      </p>{/if}
  </div>
  <fieldset id="movie-genres" aria-describedby="movie-genres-error">
    <legend>Genres · at least one required</legend>
    {#each editorState.genres as genre (genre.id)}
      <label class="check">
        <input
          type="checkbox"
          checked={(editorState.draft.genreIds ?? []).includes(genre.id)}
          disabled={inputDisabled}
          onchange={(event) =>
            onGenreToggle(genre.id, event.currentTarget.checked)}
        />
        {genre.label}
      </label>
    {/each}
    {#if errorFor('movie-genres')}
      <p class="field-error" id="movie-genres-error">
        {errorFor('movie-genres')}
      </p>
    {/if}
  </fieldset>
  <div class="field">
    <label for="movie-originalTitle">Original title · Optional</label>
    <input
      id="movie-originalTitle"
      value={editorState.draft.originalTitle ?? ''}
      disabled={inputDisabled}
      aria-invalid={!!errorFor('movie-originalTitle')}
      aria-describedby="movie-originalTitle-help movie-originalTitle-error"
      oninput={(event) =>
        onDraftChange('originalTitle', event.currentTarget.value)}
    />
    <small id="movie-originalTitle-help"
      >Clear this field to remove the original title. Unknown information may
      stay blank.</small
    >
    {#if errorFor('movie-originalTitle')}<p
        class="field-error"
        id="movie-originalTitle-error"
      >
        {errorFor('movie-originalTitle')}
      </p>{/if}
  </div>
  <MovieMetadataFields draft={editorState.draft} disabled={inputDisabled} {errorFor} onChange={onDraftChange} />
  <div class="editor-actions">
    <Button type="button" variant="secondary" onclick={onDiscardOpen}
      >Leave draft</Button
    >
    <Button
      type="submit"
      pending={editorState.phase === 'submitting'}
      disabledReason={submitDisabled
        ? 'Review current changes before saving again.'
        : ''}>{submitLabel}</Button
    >
  </div>
</form>

<style>
  .recovery-panel {
    display: grid;
    gap: var(--primitive-space-3);
    padding: var(--primitive-space-4);
    border: 1px solid var(--line);
    border-radius: var(--radius);
    margin-block: var(--primitive-space-4);
  }
  .recovery-panel ul {
    display: grid;
    gap: var(--primitive-space-3);
    padding-inline-start: var(--primitive-space-6);
  }
  .recovery-panel li {
    display: grid;
    gap: var(--primitive-space-2);
  }
  .recovery-panel dl {
    margin: 0;
  }
  .recovery-panel dd {
    margin-inline-start: 0;
    margin-block-end: var(--primitive-space-3);
    white-space: pre-wrap;
  }
  .review-check {
    display: flex;
    gap: var(--primitive-space-2);
    align-items: start;
  }
  .review-check input {
    width: auto;
  }

  form {
    display: grid;
    gap: 1rem;
    max-width: 42rem;
  }
  fieldset {
    border: 0;
    padding: 0;
    margin: 0;
    display: grid;
    gap: 0.5rem;
  }
  .check {
    display: flex;
    gap: 0.5rem;
    align-items: center;
  }
  .editor-actions {
    display: flex;
    flex-wrap: wrap;
    gap: 0.75rem;
  }
</style>
