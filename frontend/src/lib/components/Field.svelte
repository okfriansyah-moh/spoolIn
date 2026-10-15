<script lang="ts">
  import type { HTMLInputAttributes } from 'svelte/elements';
  let {
    id,
    label,
    value = $bindable(''),
    help = '',
    error = '',
    required = false,
    disabled = false,
    multiline = false,
    type = 'text',
    autocomplete,
  }: {
    id: string;
    label: string;
    value?: string;
    help?: string;
    error?: string;
    required?: boolean;
    disabled?: boolean;
    multiline?: boolean;
    type?: 'text' | 'email' | 'password' | 'search';
    autocomplete?: HTMLInputAttributes['autocomplete'];
  } = $props();
  const describedBy = $derived(
    [help && `${id}-help`, error && `${id}-error`].filter(Boolean).join(' ') ||
      undefined,
  );
</script>

<div class="field">
  <label for={id}>{label}{required ? ' · Required' : ' · Optional'}</label
  >{#if multiline}<textarea
      {id}
      bind:value
      {required}
      {disabled}
      aria-invalid={!!error}
      aria-describedby={describedBy}
      rows="4"></textarea>{:else}<input
      {id}
      {type}
      bind:value
      {required}
      {disabled}
      {autocomplete}
      aria-invalid={!!error}
      aria-describedby={describedBy}
    />{/if}{#if help}<small id={`${id}-help`}>{help}</small>{/if}{#if error}<p
      class="field-error"
      id={`${id}-error`}
    >
      {error}
    </p>{/if}
</div>
