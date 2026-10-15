<script lang="ts">
  import { afterNavigate } from '$app/navigation';
  import { focusContentHeading } from '../../../../../frontend/src/lib/state/navigation';
  import type { Snippet } from 'svelte';
  import AppShell from '../../../../../frontend/src/lib/components/AppShell.svelte';
  import '../../../../../frontend/src/lib/generated/tokens.css';
  import '../../../../../frontend/src/lib/design-system/base.css';
  let { children }: { children: Snippet } = $props();
  afterNavigate(focusContentHeading);
</script>

<AppShell>{@render children()}</AppShell>
