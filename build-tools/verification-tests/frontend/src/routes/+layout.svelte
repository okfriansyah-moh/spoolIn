<script lang="ts">
  import { page } from '$app/state';
  import SessionMenu from '../../../../../frontend/src/lib/features/session/SessionMenu.svelte';
  import { SessionController } from '../../../../../frontend/src/lib/features/session/controller.svelte';
  import { createSessionSource } from '../../../../../frontend/src/lib/features/session/api';
  import type { SessionState } from '../../../../../frontend/src/lib/features/session/types';
  import { afterNavigate } from '$app/navigation';
  import { focusContentHeading } from '../../../../../frontend/src/lib/state/navigation';
  import type { Snippet } from 'svelte';
  import AppShell from '../../../../../frontend/src/lib/components/AppShell.svelte';
  import '../../../../../frontend/src/lib/generated/tokens.css';
  import '../../../../../frontend/src/lib/design-system/base.css';
  let { children }: { children: Snippet } = $props();
  const controller = new SessionController(createSessionSource());
  const session = $derived<SessionState>({
    kind: 'anonymous',
    identity: null,
    policy: null,
    message: page.url.searchParams.has('session-feedback') ? 'Signed out.' : '',
  });
  afterNavigate(focusContentHeading);
</script>

<AppShell>
  {#snippet account()}<SessionMenu {controller} {session} />{/snippet}
  {@render children()}
</AppShell>
