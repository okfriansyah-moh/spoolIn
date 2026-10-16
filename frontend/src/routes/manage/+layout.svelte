<script lang="ts">
  import { onMount, type Snippet } from 'svelte';
  import { resolve } from '$app/paths';
  import { page } from '$app/state';
  import { sessionController } from '../../lib/features/session/context';
  import SessionBoundary from '../../lib/components/SessionBoundary.svelte';
  let { children }: { children: Snippet } = $props();
  const controller = sessionController();
  let session = $state(controller.snapshot);
  let ready = $state(false);
  const boundary = $derived(
    !ready
      ? 'checking'
      : session.kind === 'verified'
        ? session.identity?.role === 'MAINTAINER'
          ? 'verified'
          : 'denied'
        : session.kind === 'expired'
          ? 'expired'
          : session.kind === 'anonymous'
            ? 'denied'
            : session.kind === 'checking'
              ? 'checking'
              : 'unavailable',
  );
  onMount(() => {
    const unsubscribe = controller.subscribe((next) => {
      session = next;
    });
    void controller.check().finally(() => {
      ready = true;
    });
    return unsubscribe;
  });
</script>

<SessionBoundary state={boundary}
  >{#if boundary === 'verified'}<section data-private>
      {@render children()}
    </section>{/if}</SessionBoundary
>
{#if boundary !== 'verified'}
  <div class="actions">
    <a
      href={resolve(
        `/sign-in?returnTo=${encodeURIComponent(page.url.pathname + page.url.search)}`,
      )}>Sign in</a
    ><a href={resolve('/')}>Browse catalog</a><button
      class="secondary"
      onclick={() => {
        void controller.check();
      }}>Check session</button
    >
  </div>
{/if}
