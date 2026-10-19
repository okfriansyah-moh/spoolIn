<script lang="ts">
  import { onMount, tick } from 'svelte';
  import { page } from '$app/state';
  import { goto } from '$app/navigation';
  import { resolve } from '$app/paths';
  import Field from '../../components/Field.svelte';
  import ErrorSummary from '../../components/ErrorSummary.svelte';
  import { boundFor } from '../../policy/rules';
  import { sessionController } from './context';
  import { safeDestination } from './routing';
  import { validateSubmission, type FieldError } from './validation';
  import type { AccountSubmission, FormOutcome } from './commands';
  let { mode }: { mode: 'signIn' | 'register' } = $props();
  const controller = sessionController();
  const session = $derived(controller.snapshot);
  let name = $state('');
  let email = $state('');
  let password = $state('');
  let errors = $state<FieldError[]>([]);
  let outcome = $state<FormOutcome | null>(null);
  let busy = $state(false);
  let coolingDown = $state(false);
  let retryTimer: ReturnType<typeof globalThis.setTimeout> | undefined;
  const policy = $derived(session.policy);
  const heading = $derived(
    outcome?.kind === 'registered'
      ? 'Your Viewer account is ready'
      : session.identity
        ? `Signed in as ${session.identity.role === 'VIEWER' ? 'Viewer' : 'Maintainer'}`
        : mode === 'register'
          ? 'Create a Viewer account'
          : 'Sign in',
  );
  const canEdit = $derived(
    session.kind === 'anonymous' || session.kind === 'expired',
  );
  const registration = $derived(
    policy ? boundFor(policy, 'registrationName') : null,
  );
  const newPassword = $derived(policy ? boundFor(policy, 'password') : null);
  const emailRule = $derived(
    mode === 'register' && policy ? boundFor(policy, 'email') : null,
  );
  const feedback = $derived(
    outcome?.kind === 'registered' ? outcome.message : '',
  );
  onMount(() => {
    controller.onCredentialReset = (clearIdentity) => {
      password = '';
      if (clearIdentity) {
        name = '';
        email = '';
      }
    };
    if (session.kind !== 'anonymous' || session.identity)
      controller.onCredentialReset(session.identity !== null);
    if (session.kind !== 'checking') {
      if (mode === 'signIn') void controller.prepareSignIn();
      else void controller.check();
    }
    return () => {
      controller.onCredentialReset = undefined;
      password = '';
      globalThis.clearTimeout(retryTimer);
    };
  });
  function errorFor(id: string): string {
    return errors.find((error) => error.id === id)?.message ?? '';
  }
  function cooldown(milliseconds: number) {
    globalThis.clearTimeout(retryTimer);
    coolingDown = milliseconds > 0;
    if (coolingDown)
      retryTimer = globalThis.setTimeout(() => {
        coolingDown = false;
      }, milliseconds);
  }
  async function apply(result: FormOutcome) {
    outcome = result;
    cooldown(result.retryAfterMs);
    if (result.kind === 'failed')
      errors = [{ id: 'account-feedback', message: result.message }];
    if (result.kind === 'signedIn' && result.role) {
      const destination = safeDestination(
        page.url.searchParams.get('returnTo'),
        result.role,
      );
      await navigate(destination);
    }
  }
  async function navigate(destination: string) {
    const target = new globalThis.URL(destination, page.url.href);
    const suffix = target.searchParams.toString() + target.hash;
    if (target.pathname === '/') {
      await goto(resolve(`/?${suffix}`));
      return;
    }
    if (target.pathname.startsWith('/manage')) {
      await goto(
        resolve(`/manage/[...section]?${suffix}`, {
          section: target.pathname.slice('/manage/'.length),
        }),
      );
      return;
    }
    window.location.assign(`${resolve('/')}${destination.slice(1)}`);
  }
  async function submit(event: SubmitEvent) {
    event.preventDefault();
    if (busy || coolingDown || !policy) return;
    const input: AccountSubmission =
      mode === 'register'
        ? { kind: mode, name, email, password }
        : { kind: mode, email, password };
    password = '';
    errors = validateSubmission(input, policy);
    outcome = null;
    if (errors.length) return;
    busy = true;
    await tick();
    try {
      await apply(await controller.submit(input));
    } catch {
      errors = [
        {
          id: 'account-feedback',
          message:
            'The request could not be completed. Check your current session.',
        },
      ];
    } finally {
      busy = false;
    }
  }
  async function reconcile() {
    if (busy) return;
    busy = true;
    errors = [];
    try {
      await apply(await controller.reconcileIdentity());
    } finally {
      busy = false;
    }
  }
  function refresh() {
    password = '';
    errors = [];
    void controller.check();
  }
</script>

<svelte:head><title>{heading} · SpoolIn</title></svelte:head>
<section class="account-layout stack" aria-labelledby="account-title">
  <h1 id="account-title" tabindex="-1">{heading}</h1>
  <p id="account-success" role="status" aria-live="polite" aria-atomic="true">
    {feedback}
  </p>
  {#if session.identity && session.kind === 'verified'}
    <div class="panel stack" data-private>
      <p>
        {session.identity.displayName}, your verified account can browse the
        public catalog.
      </p>
      {#if session.identity.role === 'MAINTAINER'}<a href={resolve('/manage')}
          >Open management</a
        >{/if}
      <a href={resolve('/')}>Browse catalog</a>
    </div>
  {:else if outcome?.kind === 'registered'}
    <div class="notice success">
      <p>Viewer is assigned to this account. You can sign in immediately.</p>
      <a href={resolve('/sign-in')}>Sign in</a>
    </div>
  {:else if session.kind === 'checking'}
    <p role="status" aria-live="polite">
      {busy
        ? mode === 'register'
          ? 'Creating your account…'
          : 'Signing in…'
        : 'Checking your session'}
    </p>
  {:else if session.kind === 'unavailable'}
    <div class="notice warning">
      <h2>Sign-in is temporarily unavailable</h2>
      <p>Protected work stays concealed. Public browsing remains available.</p>
      <button class="secondary" onclick={refresh}>Check again</button><a
        href={resolve('/')}>Browse catalog</a
      >
    </div>
  {:else if session.kind === 'uncertain'}
    <div class="notice warning">
      <h2>Check before continuing</h2>
      <p>{session.message}</p>
      {#if controller.hasPendingIdentity}<button
          class="secondary"
          disabled={busy}
          onclick={reconcile}>Check original request</button
        >{/if}
      {#if controller.hasUnconfirmedLogout}<button
          class="secondary"
          onclick={() => {
            void controller.reconcileSignOut();
          }}>Check sign-out</button
        ><button
          class="secondary"
          onclick={() => {
            void controller.prepareSignIn();
          }}>Sign in again</button
        >{/if}
      <a href={resolve('/')}>Browse catalog</a>
    </div>
  {:else if canEdit && policy}
    {#if session.kind === 'expired'}<div class="notice warning" role="status">
        <h2>Your session expired</h2>
        <p>Sign in again before continuing protected work.</p>
      </div>{/if}
    <ErrorSummary {errors} />
    <form
      class="panel stack"
      novalidate
      onsubmit={submit}
      aria-label={mode === 'register' ? 'Create Viewer account' : 'Sign in'}
    >
      {#if mode === 'register' && registration}
        <Field
          id="account-name"
          label="Name"
          bind:value={name}
          required
          autocomplete="name"
          error={errorFor('account-name')}
          help={`Use ${registration.minCodePoints} to ${registration.maxCodePoints} Unicode code points after surrounding spaces are trimmed.`}
        />
      {/if}
      <Field
        id="account-email"
        label="Email"
        type="email"
        bind:value={email}
        required
        autocomplete="email"
        error={errorFor('account-email')}
        help={`Use a valid email address${emailRule ? `, up to ${emailRule.maxCodePoints} Unicode code points` : ''}.`}
      />
      <Field
        id="account-password"
        label="Password"
        type="password"
        bind:value={password}
        required
        autocomplete={mode === 'register' ? 'new-password' : 'current-password'}
        error={errorFor('account-password')}
        help={mode === 'register' && newPassword
          ? `Use ${newPassword.minCodePoints} to ${newPassword.maxCodePoints} Unicode code points, within 512 UTF-8 bytes. Spaces and letterforms stay exactly as entered.`
          : 'Use your existing password. New-password length rules do not apply to sign-in.'}
      />
      <div id="account-feedback" tabindex="-1">
        {outcome?.kind === 'failed' ? outcome.message : ''}
      </div>
      {#if coolingDown}<p role="status">
          Wait before trying again. Your name and email stay in this form; the
          password is cleared.
        </p>{/if}
      <div class="actions">
        <button
          type="submit"
          disabled={busy || coolingDown || controller.hasPendingIdentity}
          aria-busy={busy}
          >{mode === 'register' ? 'Create Viewer account' : 'Sign in'}</button
        ><button type="button" class="secondary" onclick={refresh}
          >Refresh input rules</button
        >
      </div>
      {#if controller.hasPendingIdentity}<button
          type="button"
          class="secondary"
          onclick={reconcile}>Check original request</button
        >{/if}
      <p>
        {mode === 'register'
          ? 'Registration creates a Viewer who can sign in immediately.'
          : 'Use an existing account or create a Viewer account.'}
      </p>
      <div class="actions">
        <a href={resolve(mode === 'register' ? '/sign-in' : '/register')}
          >{mode === 'register' ? 'Sign in' : 'Create a Viewer account'}</a
        ><a href={resolve('/')}>Browse catalog</a>
      </div>
    </form>
  {/if}
</section>

<style>
  .account-layout {
    max-inline-size: 42rem;
    margin-inline: auto;
    padding-block: var(--primitive-space-8);
  }
  .account-layout h1 {
    margin: 0;
  }
  #account-success:empty,
  #account-feedback:empty {
    margin: 0;
  }
</style>
