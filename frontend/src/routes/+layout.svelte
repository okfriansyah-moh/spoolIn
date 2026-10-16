<script lang="ts">
  import { onMount, setContext, flushSync, type Snippet } from 'svelte';
  import { SessionController } from '../lib/features/session/controller';
  import { createSessionSource } from '../lib/features/session/api';
  import { sessionContext } from '../lib/features/session/context';
  import SessionMenu from '../lib/features/session/SessionMenu.svelte';
  import { page } from '$app/state';
  import { afterNavigate } from '$app/navigation';
  import { focusContentHeading } from '../lib/state/navigation';
  import AppShell from '../lib/components/AppShell.svelte';
  import '../lib/generated/tokens.css';
  import '../lib/design-system/base.css';
  let { children }: { children: Snippet } = $props();
  const controller = new SessionController(createSessionSource());
  setContext(sessionContext, controller);
  let session = $state(controller.snapshot);
  const capabilities = $derived({
    verified: session.kind === 'verified',
    manageCatalog: session.identity?.role === 'MAINTAINER',
  });
  onMount(() => {
    const unsubscribe = controller.subscribe((state) => {
      session = state;
    });
    void controller.check();
    const hide = () => {
      flushSync(() => {
        controller.conceal();
      });
    };
    const show = (event: Event) => {
      if ('persisted' in event && event.persisted === true)
        void controller.check();
    };
    window.addEventListener('pagehide', hide);
    window.addEventListener('pageshow', show);
    return () => {
      window.removeEventListener('pagehide', hide);
      window.removeEventListener('pageshow', show);
      unsubscribe();
      controller.dispose();
    };
  });
  afterNavigate(focusContentHeading);
</script>

<AppShell currentPath={page.url.pathname} {capabilities}>
  {#snippet account()}<SessionMenu {controller} {session} />{/snippet}
  {@render children()}
</AppShell>
