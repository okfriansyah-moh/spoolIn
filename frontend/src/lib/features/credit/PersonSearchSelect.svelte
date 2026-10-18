<script lang="ts">
  import { onDestroy } from 'svelte';
  import type { PersonOption } from './types';

  let {
    options,
    selected,
    disabled = false,
    loading,
    hasMore,
    error,
    feedback,
    onSearch,
    onSelect,
    onMore,
    onRetry,
  }: {
    options: readonly PersonOption[];
    selected: PersonOption | null;
    disabled?: boolean;
    loading: boolean;
    hasMore: boolean;
    error: string;
    feedback: string;
    onSearch: (query: string) => void;
    onSelect: (person: PersonOption) => void;
    onMore: () => void;
    onRetry: () => void;
  } = $props();
  let root: HTMLDivElement;
  let input: HTMLInputElement;
  let open = $state(false);
  let query = $state('');
  let active = $state(-1);
  let timer: ReturnType<typeof setTimeout> | undefined;
  let suppressFocus = false;
  $effect(() => {
    if (disabled) close();
  });
  const matches = $derived(
    options.filter((person) =>
      person.name
        .toLocaleLowerCase()
        .includes(query.trim().toLocaleLowerCase()),
    ),
  );
  const activeId = $derived(
    open && active >= 0 && matches[active]
      ? `credit-person-option-${matches[active].id}`
      : undefined,
  );

  function close() {
    clearTimeout(timer);
    open = false;
    active = -1;
  }
  onDestroy(() => clearTimeout(timer));

  function show() {
    if (disabled || open || suppressFocus) return;
    open = true;
    query = '';
    active = -1;
    onSearch('');
  }

  function type(event: Event) {
    if (disabled) return;
    open = true;
    query = (event.currentTarget as HTMLInputElement).value;
    active = -1;
    clearTimeout(timer);
    timer = setTimeout(() => onSearch(query), 250);
  }

  function select(person: PersonOption) {
    onSelect(person);
    close();
    suppressFocus = true;
    input.focus({ preventScroll: true });
    queueMicrotask(() => {
      suppressFocus = false;
    });
  }

  function move(direction: number) {
    if (!matches.length) return;
    active =
      active < 0
        ? direction > 0
          ? 0
          : matches.length - 1
        : (active + direction + matches.length) % matches.length;
    requestAnimationFrame(() => {
      if (activeId)
        document.getElementById(activeId)?.scrollIntoView({ block: 'nearest' });
    });
  }

  function keydown(event: KeyboardEvent) {
    if (event.isComposing) return;
    if (event.key === 'Escape') {
      event.preventDefault();
      close();
    } else if (event.key === 'ArrowDown' || event.key === 'ArrowUp') {
      event.preventDefault();
      if (!open) show();
      else move(event.key === 'ArrowDown' ? 1 : -1);
    } else if (event.key === 'Enter' && open) {
      event.preventDefault();
      clearTimeout(timer);
      if (active >= 0 && matches[active]) select(matches[active]);
      else onSearch(query);
    }
  }

  function outside(event: PointerEvent) {
    if (open && !event.composedPath().includes(root)) close();
  }
</script>

<svelte:window onpointerdown={outside} />
<div
  class="person-select"
  bind:this={root}
  onfocusout={(event) => {
    if (!root.contains(event.relatedTarget as Node | null)) close();
  }}
>
  <label for="credit-person">Person</label>
  <div class="selection-control">
    <input
      id="credit-person"
      bind:this={input}
      role="combobox"
      type="text"
      autocomplete="off"
      value={open ? query : (selected?.name ?? '')}
      placeholder="Select or search for a person"
      {disabled}
      aria-autocomplete="list"
      aria-expanded={open && !disabled}
      aria-controls="credit-person-options"
      aria-activedescendant={activeId}
      aria-describedby="credit-person-help"
      onfocus={show}
      onclick={show}
      oninput={type}
      onkeydown={keydown}
    />
    <span aria-hidden="true">▾</span>
  </div>
  <small id="credit-person-help"
    >Type to find a person, then select a name.</small
  >
  {#if open && !disabled}
    <div class="selection-menu">
      {#if selected}<p class="selected-name">Selected: {selected.name}</p>{/if}
      <ul
        id="credit-person-options"
        role="listbox"
        aria-label="People"
        aria-busy={loading}
        onscroll={(event) => {
          const list = event.currentTarget;
          if (
            hasMore &&
            !loading &&
            !error &&
            list.scrollHeight - list.scrollTop - list.clientHeight < 40
          )
            onMore();
        }}
      >
        {#each matches as person, index (person.id)}
          <li
            role="option"
            id={`credit-person-option-${person.id}`}
            aria-selected={selected?.id === person.id}
            class:active={active === index}
          >
            <button
              type="button"
              tabindex="-1"
              onmousedown={(event) => event.preventDefault()}
              onclick={() => select(person)}>{person.name}</button
            >
          </li>
        {/each}
      </ul>
      <p role="status">
        {loading
          ? 'Finding people…'
          : error
            ? ''
            : matches.length
              ? feedback
              : 'No matching people. Try another name.'}
      </p>
      {#if error}<p role="alert">{error}</p>
        <button
          class="secondary"
          type="button"
          onclick={() => {
            input.focus({ preventScroll: true });
            onRetry();
          }}>Retry names</button
        >{/if}
      {#if hasMore && !error}<button
          class="secondary"
          type="button"
          disabled={loading}
          onclick={() => {
            input.focus({ preventScroll: true });
            onMore();
          }}>Show more results</button
        >{/if}
    </div>
  {/if}
</div>

<style>
  .person-select {
    position: relative;
  }
  .selection-control {
    position: relative;
  }
  .selection-control input {
    width: 100%;
    padding-right: 2.5rem;
  }
  .selection-control span {
    position: absolute;
    right: 1rem;
    top: 50%;
    transform: translateY(-50%);
    pointer-events: none;
  }
  .selection-menu {
    position: absolute;
    z-index: 10;
    width: 100%;
    border: 1px solid var(--line);
    border-radius: var(--radius);
    background: var(--surface);
    box-shadow: 0 8px 20px #0003;
  }
  .selection-menu ul {
    max-height: 16rem;
    overflow-y: auto;
    margin: 0;
    padding: 0;
    list-style: none;
  }
  .selection-menu li button {
    display: block;
    width: 100%;
    text-align: left;
    border: 0;
    border-radius: 0;
    background: transparent;
    color: var(--text);
  }
  .selection-menu li.active,
  .selection-menu li:hover,
  .selection-menu li[aria-selected='true'] {
    background: var(--selected);
  }
  .selection-menu p {
    margin: 0.5rem 1rem;
  }
  .selection-menu > button {
    margin: 0.5rem 1rem;
  }
  small {
    color: var(--muted);
  }
</style>
