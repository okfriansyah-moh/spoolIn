<script lang="ts">
  import type { Snippet } from 'svelte';
  import { page } from '$app/state';
  import { afterNavigate } from '$app/navigation';
  import { focusContentHeading } from '../lib/state/navigation';
  import AppShell from '../lib/components/AppShell.svelte';
  import '../lib/generated/tokens.css';
  import '../lib/design-system/base.css';
  let { children }: { children: Snippet } = $props();
  afterNavigate(focusContentHeading);
</script>

<AppShell currentPath={page.url.pathname}>{@render children()}</AppShell>
