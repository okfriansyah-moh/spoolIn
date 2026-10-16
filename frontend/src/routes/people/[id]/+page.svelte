<script lang="ts">
  /**
   * T013 – Public person detail page (/people/[id]).
   *
   * Shows person name, biography (if available) and their public credits.
   * Unavailable (archived/deleted) → safe message, no archived data.
   * AC-022-C01/AC-004: only active people shown; same names never collapse identity.
   */
  import { onMount } from 'svelte';
  import { page } from '$app/state';
  import { resolve } from '$app/paths';
  import { createTransport } from '../../../lib/transport/graphql';
  import { fetchPerson, fetchPersonCredits } from '../../../lib/features/person/read/api';
  import { isRecordId } from '../../../lib/components/safe-url';
  import type { PublicPersonDetail, PublicPersonCredit } from '../../../lib/features/person/read/api';

  type State =
    | { kind: 'loading' }
    | { kind: 'unavailable'; id: string }
    | { kind: 'failure' }
    | { kind: 'person'; person: PublicPersonDetail; credits: PublicPersonCredit[] };

  let state = $state<State>({ kind: 'loading' });

  onMount(() => {
    const id = page.params.id;
    if (!isRecordId(id)) {
      state = { kind: 'unavailable', id };
      return;
    }

    const transport = createTransport();
    const controller = new AbortController();

    Promise.all([
      fetchPerson(transport, { id }, controller.signal),
      fetchPersonCredits(transport, { id }, controller.signal),
    ])
      .then(([person, creditsResult]) => {
        if (controller.signal.aborted) return;
        // Public API returns null for archived/unknown people
        if (!person || person.state === 'ARCHIVED') {
          state = { kind: 'unavailable', id };
          return;
        }
        state = {
          kind: 'person',
          person,
          credits: [...creditsResult.nodes],
        };
      })
      .catch((err) => {
        if (controller.signal.aborted) return;
        void err;
        state = { kind: 'failure' };
      });

    return () => controller.abort();
  });

  const catalogHref = `${resolve('/')}`;
  const peopleHref = `${resolve('/')}people`;
</script>

<svelte:head>
  <title>
    {#if state.kind === 'person'}{state.person.name} · SpoolIn
    {:else if state.kind === 'loading'}Loading… · SpoolIn
    {:else}Unavailable · SpoolIn
    {/if}
  </title>
</svelte:head>

<nav aria-label="Breadcrumb">
  <a href={catalogHref}>Catalog</a> /
  <a href={peopleHref}>People</a>
</nav>

{#if state.kind === 'loading'}
  <div role="status" aria-live="polite">
    <h1 tabindex="-1">Loading…</h1>
  </div>

{:else if state.kind === 'unavailable'}
  <div class="unavailable">
    <h1 tabindex="-1">This person is unavailable</h1>
    <p>The person you requested is no longer available.</p>
    <a href={peopleHref}>Return to people</a>
  </div>

{:else if state.kind === 'failure'}
  <div role="alert" class="failure">
    <h1 tabindex="-1">Could not load person</h1>
    <p>The person could not be loaded. Please try again.</p>
    <a href={peopleHref}>Return to people</a>
  </div>

{:else if state.kind === 'person'}
  <article id="person-detail" aria-label={state.person.name}>
    <h1 tabindex="-1">{state.person.name}</h1>

    {#if state.person.biography}
      <section aria-label="Biography">
        <h2>Biography</h2>
        <p>{state.person.biography}</p>
      </section>
    {/if}

    <section aria-label="Credits">
      <h2>Credits</h2>
      {#if state.credits.length === 0}
        <p>No credits known.</p>
      {:else}
        <ul>
          {#each state.credits as credit (credit.id)}
            <li>
              <a href={`${resolve('/')}titles/${encodeURIComponent(credit.titleId)}`}>
                {credit.titleName}
              </a>
              — {credit.role.label}
              {#if credit.characters.length > 0}
                as {credit.characters.join(', ')}
              {/if}
            </li>
          {/each}
        </ul>
      {/if}
    </section>
  </article>
{/if}
