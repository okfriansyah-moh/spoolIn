<script lang="ts">
  import type { Snippet } from 'svelte';
  let {
    children,
    variant = 'primary',
    pending = false,
    disabledReason = '',
    onclick,
    type = 'button',
    selected,
  }: {
    children: Snippet;
    variant?: 'primary' | 'secondary' | 'danger';
    pending?: boolean;
    disabledReason?: string;
    onclick?: () => void;
    type?: 'button' | 'submit';
    selected?: boolean;
  } = $props();
  const id = $props.id();
</script>

<span class="button-group"
  ><button
    {type}
    class={variant}
    disabled={pending || !!disabledReason}
    aria-busy={pending}
    aria-pressed={selected}
    aria-describedby={disabledReason ? id : undefined}
    onclick={(event) => {
      event.currentTarget.focus();
      onclick?.();
    }}
    >{#if pending}Saving…{:else}{@render children()}{/if}</button
  >{#if disabledReason}<small {id}>{disabledReason}</small>{/if}</span
>
