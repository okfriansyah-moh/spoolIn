<script lang="ts">
  import type { Snippet } from 'svelte';
  let {
    title,
    children,
    tone = 'info',
    urgent = false,
  }: {
    title: string;
    children?: Snippet;
    tone?: 'info' | 'success' | 'error' | 'warning';
    urgent?: boolean;
  } = $props();
</script>

<div
  class={`notice ${tone}`}
  role={urgent ? 'alert' : 'status'}
  aria-atomic="true"
>
  <strong>{title}</strong>{#if children}<div>{@render children()}</div>{/if}
</div>
