<script lang="ts">
  import { resolve } from '$app/paths';
  import ConfirmDialog from '../../components/ConfirmDialog.svelte';
  import type { SessionController } from './controller.svelte';
  import type { SessionState } from './types';
  let {
    controller,
    session,
  }: { controller: SessionController; session: SessionState } = $props();
  let confirm = $state(false);
  const announcement = $derived(
    session.kind === 'verified' ||
      ['uncertain', 'expired', 'unavailable'].includes(session.kind) ||
      session.message === 'Signed out.'
      ? session.message
      : '',
  );
  function signOut() {
    confirm = false;
    void controller.signOut();
  }
</script>

<div class="session-menu">
  {#if session.kind === 'verified' && session.identity}
    <span data-private
      >{session.identity.displayName} · {session.identity.role === 'VIEWER'
        ? 'Viewer'
        : 'Maintainer'}</span
    >
    <button
      class="secondary"
      onclick={() => {
        confirm = true;
      }}>Sign out</button
    >
  {:else}
    <a class="button secondary" href={resolve('/sign-in')}
      >{controller.hasUnconfirmedLogout ? 'Sign in again' : 'Sign in'}</a
    >
    {#if !controller.hasUnconfirmedLogout}<a
        class="button secondary"
        href={resolve('/register')}>Register</a
      >{/if}
    {#if session.kind === 'uncertain' && controller.hasUnconfirmedLogout}<button
        class="secondary"
        onclick={() => {
          void controller.reconcileSignOut();
        }}>Check sign-out</button
      >{/if}
  {/if}
</div>
<p
  role="status"
  aria-live="polite"
  aria-atomic="true"
  class="session-announcement"
  class:idle={!announcement}
>
  {announcement}
</p>
<ConfirmDialog
  bind:open={confirm}
  title="Sign out?"
  action="Sign out"
  onconfirm={signOut}
>
  <p>
    This ends the session and makes protected work unavailable to the next
    account. Committed changes remain saved.
  </p>
</ConfirmDialog>

<style>
  .session-menu {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: var(--primitive-space-3);
    max-width: 100%;
  }
  .session-menu span {
    overflow-wrap: anywhere;
  }
  .session-menu a {
    min-height: 44px;
    display: inline-flex;
    align-items: center;
  }
  .session-announcement {
    flex-basis: 100%;
    margin: 0;
  }
  .session-announcement.idle {
    position: absolute;
    width: 1px;
    height: 1px;
    margin: -1px;
    overflow: hidden;
    clip-path: inset(50%);
    white-space: nowrap;
  }
</style>
