<script lang="ts">
  import { tick } from 'svelte';
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
      editorState.phase === 'target-changed' ||
      (editorState.mode === 'create' && editorState.saved !== null),
  );
  const savedCreate = $derived(
    editorState.mode === 'create' && editorState.saved !== null,
  );
  const submitDisabledReason = $derived(
    savedCreate
      ? 'This movie is saved. Use one of the links above to continue.'
      : submitDisabled
        ? 'Review the current changes before saving again.'
        : '',
  );
  const genreCount = $derived((editorState.draft.genreIds ?? []).length);
  const selectedGenres = $derived(
    editorState.genres.filter((genre) =>
      (editorState.draft.genreIds ?? []).includes(genre.id),
    ),
  );
  let genreSearch = $state('');
  let genreOpen = $state(false);
  let detailsOpen = $state(false);
  let lastErrorSignature = '';
  const optionalFieldErrorIds = new Set([
    'movie-originalTitle',
    'movie-releaseMonth',
    'movie-releaseDay',
    'movie-runtimeMinutes',
    'movie-originalLanguage',
    'movie-synopsisLanguage',
    'movie-countries',
  ]);
  const visibleGenres = $derived.by(() => {
    const query = genreSearch.trim().toLocaleLowerCase();
    return editorState.genres.filter((genre) =>
      genre.label.toLocaleLowerCase().includes(query),
    );
  });
  const reviewId = $props.id();
  let reviewedCurrent = $state(false);
  $effect(() => {
    const version = editorState.current?.version;
    const open = editorState.staleReviewOpen;
    void version;
    void open;
    reviewedCurrent = false;
  });
  $effect(() => {
    const errors = editorState.fieldErrors;
    const first = errors[0];
    const signature = errors.map((error) => error.id).join('|');
    if (!signature) {
      lastErrorSignature = '';
      return;
    }
    if (!first || signature === lastErrorSignature) return;
    lastErrorSignature = signature;
    if (errors.some((error) => error.id === 'movie-genres')) genreOpen = true;
    if (errors.some((error) => optionalFieldErrorIds.has(error.id)))
      detailsOpen = true;
    void tick().then(() => document.getElementById(first.id)?.focus());
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
      <dt>Release month / day</dt>
      <dd>
        {editorState.current.releaseMonth ?? 'Unknown'} / {editorState.current
          .releaseDay ?? 'Unknown'}
      </dd>
      <dt>Runtime</dt>
      <dd>{editorState.current.runtimeMinutes ?? 'Unknown'}</dd>
      <dt>Original language</dt>
      <dd>{editorState.current.originalLanguage ?? 'Unknown'}</dd>
      <dt>Synopsis language</dt>
      <dd>{editorState.current.synopsisLanguage ?? 'und'}</dd>
      <dt>Countries</dt>
      <dd>{editorState.current.countries?.join(', ') || 'Unknown'}</dd>
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
  <section
    class="recovery-panel saved-movie-actions"
    aria-label="Continue with this movie"
  >
    <a
      class="button secondary"
      href={resolve('/manage/movies/[id]', { id: editorState.saved.id })}
      >Edit saved movie</a
    >
    <a
      class="button secondary"
      href={resolve('/titles/[id]', { id: editorState.saved.id })}
      >Review public record</a
    >
  </section>
{/if}

<form
  id="movie-editor-form"
  class="movie-editor-form"
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
  <fieldset
    id="movie-genres"
    class="genre-fieldset"
    tabindex="-1"
    aria-describedby="movie-genres-help movie-genres-error"
  >
    <legend
      >Genres <span class="required-note">Required · choose at least one</span
      ></legend
    >
    <p id="movie-genres-help" class="field-help">
      Choose the categories that best help people find this film.
    </p>
    {#if selectedGenres.length}
      <ul class="selected-genres" aria-label="Selected genres">
        {#each selectedGenres as genre (genre.id)}
          <li>
            <span>{genre.label}</span>
            <button
              type="button"
              class="remove-genre"
              aria-label="Remove genre {genre.label}"
              disabled={inputDisabled}
              onclick={() => onGenreToggle(genre.id, false)}>×</button
            >
          </li>
        {/each}
      </ul>
    {/if}
    <details class="genre-disclosure" bind:open={genreOpen}>
      <summary>
        {genreCount ? 'Change genres' : 'Choose genres'}
        <span class="disclosure-count">{genreCount} selected</span>
      </summary>
      <div class="genre-panel">
        <label for="movie-genre-search">Find a genre</label>
        <input
          id="movie-genre-search"
          type="search"
          bind:value={genreSearch}
          placeholder="Search genres"
          disabled={inputDisabled}
        />
        <p class="genre-result-count" role="status" aria-live="polite">
          {visibleGenres.length}
          {visibleGenres.length === 1 ? 'genre' : 'genres'}
        </p>
        <div class="genre-options">
          {#each visibleGenres as genre (genre.id)}
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
          {:else}
            <p class="no-genres">
              No genres match that search. Try another name.
            </p>
          {/each}
        </div>
      </div>
    </details>
    {#if errorFor('movie-genres')}
      <p class="field-error" id="movie-genres-error">
        {errorFor('movie-genres')}
      </p>
    {/if}
  </fieldset>
  <details class="optional-details" bind:open={detailsOpen}>
    <summary>
      More movie details
      <span>Original title, release date, runtime, language and countries</span>
    </summary>
    <div class="optional-fields">
      <div class="field">
        <label for="movie-originalTitle"
          >Original title <span class="muted">Optional</span></label
        >
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
          >Leave this blank when the original title is unknown.</small
        >
        {#if errorFor('movie-originalTitle')}<p
            class="field-error"
            id="movie-originalTitle-error"
          >
            {errorFor('movie-originalTitle')}
          </p>{/if}
      </div>
      <MovieMetadataFields
        draft={editorState.draft}
        disabled={inputDisabled}
        {errorFor}
        onChange={onDraftChange}
      />
    </div>
  </details>
  <div class="editor-actions">
    <Button type="button" variant="secondary" onclick={onDiscardOpen}
      >Leave draft</Button
    >
    <Button
      type="submit"
      pending={editorState.phase === 'submitting'}
      disabledReason={submitDisabledReason}>{submitLabel}</Button
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
  .saved-movie-actions {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
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
    gap: var(--primitive-space-6);
    width: 100%;
    max-width: 48rem;
    padding: clamp(1rem, 3vw, 2rem);
    border: 1px solid var(--line);
    border-radius: var(--component-artwork-radius);
    background: var(--surface);
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
  .genre-fieldset {
    display: grid;
    gap: var(--primitive-space-3);
    padding: 0;
    border: 0;
  }
  .genre-fieldset legend {
    color: var(--text);
    font-size: var(--primitive-type-body);
    font-weight: 600;
  }
  .required-note {
    color: var(--muted);
    font-size: var(--primitive-type-small);
    font-weight: 400;
  }
  .field-help {
    margin: calc(-1 * var(--primitive-space-2)) 0 0;
    color: var(--muted);
    font-size: var(--primitive-type-small);
  }
  .selected-genres {
    display: flex;
    flex-wrap: wrap;
    gap: var(--primitive-space-2);
    padding: 0;
    margin: 0;
    list-style: none;
  }
  .selected-genres li {
    display: inline-flex;
    align-items: center;
    min-height: 44px;
    padding-left: var(--primitive-space-3);
    border: 1px solid var(--line);
    border-radius: 999px;
    background: var(--selected);
  }
  .remove-genre {
    width: 44px;
    height: 44px;
    min-height: 44px;
    padding: 0;
    border: 0;
    border-radius: 50%;
    background: transparent;
    color: var(--text);
    font-size: 1.35rem;
  }
  .remove-genre:hover {
    background: var(--raised);
    filter: none;
  }
  .genre-disclosure,
  .optional-details {
    border: 1px solid var(--line);
    border-radius: var(--radius);
    background: var(--bg);
  }
  .genre-disclosure summary,
  .optional-details summary {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: var(--primitive-space-3);
    padding: var(--primitive-space-3) var(--primitive-space-4);
    font-weight: 600;
  }
  .disclosure-count,
  .optional-details summary span {
    color: var(--muted);
    font-size: var(--primitive-type-small);
    font-weight: 400;
    text-align: right;
  }
  .genre-panel {
    padding: 0 var(--primitive-space-4) var(--primitive-space-4);
  }
  .genre-panel label[for='movie-genre-search'] {
    display: block;
    margin-bottom: var(--primitive-space-2);
  }
  .genre-result-count {
    margin: var(--primitive-space-2) 0;
    color: var(--muted);
    font-size: var(--primitive-type-small);
  }
  .genre-options {
    max-height: 16rem;
    overflow-y: auto;
    overscroll-behavior: contain;
    padding: var(--primitive-space-2);
    border: 1px solid var(--line);
    border-radius: var(--radius);
  }
  .genre-options .check {
    padding: var(--primitive-space-1) var(--primitive-space-2);
    border-radius: var(--radius);
  }
  .genre-options .check:hover {
    background: var(--raised);
  }
  .no-genres {
    margin: var(--primitive-space-3);
    color: var(--muted);
  }
  .optional-fields {
    display: grid;
    gap: var(--primitive-space-6);
    padding: 0 var(--primitive-space-4) var(--primitive-space-4);
  }
  .optional-details > summary {
    display: grid;
    justify-content: stretch;
    text-align: left;
  }
  .editor-actions {
    display: flex;
    flex-wrap: wrap;
    gap: 0.75rem;
    justify-content: space-between;
    align-items: end;
    padding-top: var(--primitive-space-4);
    border-top: 1px solid var(--line);
  }
  @media (max-width: 699px) {
    form {
      padding: var(--primitive-space-4);
    }
    .editor-actions {
      display: grid;
      grid-template-columns: minmax(0, 1fr);
    }
    .editor-actions :global(.button-group),
    .editor-actions :global(button) {
      width: 100%;
    }
    .genre-disclosure summary,
    .optional-details summary {
      align-items: flex-start;
      flex-direction: column;
    }
    .disclosure-count,
    .optional-details summary span {
      text-align: left;
    }
  }
</style>
