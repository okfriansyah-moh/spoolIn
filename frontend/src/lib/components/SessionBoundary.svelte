<script lang="ts">
  import type { Snippet } from 'svelte';
  let {
    state,
    children,
  }: {
    state: 'checking' | 'verified' | 'expired' | 'unavailable' | 'denied';
    children: Snippet;
  } = $props();
</script>

{#if state === 'verified'}{@render children()}{:else}<div
    role="status"
    class="notice warning"
  >
    <strong
      >{state === 'checking'
        ? 'Checking your session'
        : state === 'expired'
          ? 'Your session expired'
          : state === 'denied'
            ? 'You do not have access'
            : 'Sign-in is temporarily unavailable'}</strong
    >
    <p>
      Protected work is concealed until your session and access are verified.
    </p>
  </div>{/if}
