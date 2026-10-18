<script lang="ts">
  import type { MovieDraft } from '../../generated/json-types';
  import type { MovieDraftField } from './draft-field-update';
  let { draft, disabled, errorFor, onChange }: {
    draft: MovieDraft; disabled: boolean; errorFor: (id: string) => string;
    onChange: (field: MovieDraftField, value: string) => void;
  } = $props();
  const fields: readonly { field: MovieDraftField; label: string; help: string; numeric?: boolean }[] = [
    { field: 'releaseMonth', label: 'Release month', help: 'Optional, 1–12. Leave blank when only the year is known.', numeric: true },
    { field: 'releaseDay', label: 'Release day', help: 'Optional calendar day; requires a release month.', numeric: true },
    { field: 'runtimeMinutes', label: 'Runtime in minutes', help: 'Optional, 1–100000 minutes. Unknown runtime may stay blank.', numeric: true },
    { field: 'originalLanguage', label: 'Original language', help: 'Optional BCP47 language tag, for example id or en-US.' },
    { field: 'synopsisLanguage', label: 'Synopsis language', help: 'BCP47 language tag. Use und when the language is unknown.' },
    { field: 'countries', label: 'Countries', help: 'Optional uppercase ISO country codes separated by commas, for example ID, GB.' },
  ];
  function value(field: MovieDraftField): string {
    if (field === 'releaseMonth') return String(draft.release?.month ?? '');
    if (field === 'releaseDay') return String(draft.release?.day ?? '');
    if (field === 'countries') return (draft.countries ?? []).join(', ');
    return String(draft[field as keyof MovieDraft] ?? '');
  }
</script>

{#each fields as field (field.field)}
  <div class="field">
    <label for="movie-{field.field}">{field.label}</label>
    <input id="movie-{field.field}" value={value(field.field)} {disabled}
      inputmode={field.numeric ? 'numeric' : 'text'}
      aria-invalid={!!errorFor(`movie-${field.field}`)}
      aria-describedby="movie-{field.field}-help movie-{field.field}-error"
      oninput={(event) => onChange(field.field, event.currentTarget.value)} />
    <small id="movie-{field.field}-help">{field.help}</small>
    <p id="movie-{field.field}-error" class="field-error">{errorFor(`movie-${field.field}`)}</p>
  </div>
{/each}

<style>
  .field { display: grid; gap: var(--primitive-space-2); }
  input { font: inherit; color: var(--text); background: var(--surface); border: 1px solid var(--line); border-radius: var(--radius); padding: var(--primitive-space-3); }
  .field-error { color: var(--danger); margin: 0; }
</style>
