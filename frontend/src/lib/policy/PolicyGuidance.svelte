<script lang="ts">
  import { untrack } from 'svelte';
  import { resolve } from '$app/paths';
  import Field from '../components/Field.svelte';
  import ErrorSummary from '../components/ErrorSummary.svelte';
  import Button from '../components/Button.svelte';
  import Notice from '../components/Notice.svelte';
  import type { Policy } from './types';
  import { boundFor, checkText, count } from './rules';
  import {
    registrationFeedback,
    optionalFeedback,
    draftFeedback,
    type PolicyFeedback,
  } from './feedback';
  let {
    view,
    policy,
    initialDraft = '',
    initialName = '',
    initialOriginalTitle = '',
    plainTextExample = '',
    revisedMaximum = 100,
  }: {
    view:
      | 'text-boundaries'
      | 'optional-clearing'
      | 'policy-changed'
      | 'policy-unavailable';
    policy: Policy;
    initialDraft?: string;
    initialName?: string;
    plainTextExample?: string;
    initialOriginalTitle?: string;
    revisedMaximum?: number;
  } = $props();
  let name = $state(untrack(() => initialName));
  let originalTitle = $state(untrack(() => initialOriginalTitle));
  let synopsis = $state(untrack(() => initialDraft));
  let feedback = $state<PolicyFeedback>({ errors: [], outcome: '' });
  const errors = $derived(feedback.errors);
  const outcome = $derived(feedback.outcome);
  let changed = $state(false);
  const person = $derived(boundFor(policy, 'personName'));
  const registration = $derived(boundFor(policy, 'registrationName'));
  const personSample = $derived(checkText('V', person));
  const titleSample = $derived(checkText('A', boundFor(policy, 'title')));
  const tightened = $derived({
    ...boundFor(policy, 'synopsis'),
    maxCodePoints: revisedMaximum,
  });
  function validateName() {
    feedback = registrationFeedback(name, registration);
  }
  function inspectClearing() {
    feedback = optionalFeedback(
      changed,
      originalTitle,
      boundFor(policy, 'originalTitle'),
    );
  }
  function reviewDraft() {
    feedback = draftFeedback(synopsis, tightened);
  }
</script>

<div class="policy-guidance">
  {#if view === 'text-boundaries'}
    <h1>Names and text stay intact</h1>
    <div class="table-scroll">
      <table>
        <caption>Unicode code points and current input rules</caption>
        <thead
          ><tr
            ><th scope="col">Input</th><th scope="col">Count / rule</th><th
              scope="col">Outcome</th
            ></tr
          ></thead
        >
        <tbody>
          <tr
            ><td>V</td><td
              >1 code point · person {person.minCodePoints}–{person.maxCodePoints}</td
            ><td>{personSample.error || 'Valid professional name'}</td></tr
          >
          <tr
            ><td>V</td><td
              >1 code point · registration {registration.minCodePoints}–{registration.maxCodePoints}</td
            ><td>Review the configured minimum</td></tr
          >
          <tr
            ><td>é / é</td><td>1 / 2 Unicode code points</td><td
              >Count code points, not bytes</td
            ></tr
          >
          <tr
            ><td>O’Connor · 李 · Jean-Luc</td><td
              >Legitimate names and internal spacing</td
            ><td>Preserve characters</td></tr
          >
          <tr
            ><td>A</td><td>1-character title</td><td
              >{titleSample.error ||
                'Accepted under the current title rule'}</td
            ></tr
          >
          <tr
            ><td>{plainTextExample}</td><td>Plain text fixture</td><td
              >Rendered as text; never executed</td
            ></tr
          >
        </tbody>
      </table>
    </div>
    <Notice title="Inclusive limits, no silent truncation">
      <p>
        Title {boundFor(policy, 'title').minCodePoints}–{boundFor(
          policy,
          'title',
        ).maxCodePoints}, synopsis {boundFor(policy, 'synopsis')
          .minCodePoints}–{boundFor(policy, 'synopsis').maxCodePoints}, role {boundFor(
          policy,
          'creditRole',
        ).minCodePoints}–{boundFor(policy, 'creditRole').maxCodePoints} and character
        {boundFor(policy, 'characterName').minCodePoints}–{boundFor(
          policy,
          'characterName',
        ).maxCodePoints} use Unicode code points. Each character name is checked separately.
        Meaningful synopsis line breaks remain.
      </p>
    </Notice>
    <form
      novalidate
      onsubmit={(event) => {
        event.preventDefault();
        validateName();
      }}
    >
      <ErrorSummary {errors} />
      <Field
        id="policy-name"
        label="Registration name"
        bind:value={name}
        required
        help={`Use ${registration.minCodePoints} to ${registration.maxCodePoints} Unicode code points.`}
        error={errors.find((item) => item.id === 'policy-name')?.message ?? ''}
      />
      <Button type="submit">Check name</Button>
    </form>
  {:else if view === 'optional-clearing'}
    <h1>Optional movie information</h1>
    <form
      novalidate
      onsubmit={(event) => {
        event.preventDefault();
        inspectClearing();
      }}
    >
      <ErrorSummary {errors} />
      <div
        oninput={() => {
          changed = true;
        }}
      >
        <Field
          id="policy-original"
          label="Original title"
          bind:value={originalTitle}
          help="A supplied empty value clears this field. An omitted field leaves the saved value unchanged."
          error={errors.find((item) => item.id === 'policy-original')
            ?.message ?? ''}
        />
      </div>
      <div class="actions">
        <Button type="submit">Check field change</Button><Button
          variant="secondary"
          onclick={() => {
            changed = false;
            originalTitle = initialOriginalTitle;
            inspectClearing();
          }}>Leave unchanged</Button
        >
      </div>
    </form>
    <Notice title="Unknown remains unknown"
      ><p>
        Required whitespace-only values need correction. Optional unknown values
        may stay blank; no zero runtime, fabricated date, or guessed credit is
        supplied.
      </p></Notice
    >
    <p>Character names remain separate and ordered: Maya · Maya / Echo.</p>
  {:else if view === 'policy-changed'}
    <h1>Review the current rule</h1>
    <Notice title="Synopsis now exceeds the effective maximum" tone="warning"
      ><p>
        Your complete draft is retained. The new limit is displayed before
        another submission. Correct changed or new input; unchanged stored
        values remain valid to retain. No partial save or silent truncation
        occurs.
      </p></Notice
    >
    <form
      novalidate
      onsubmit={(event) => {
        event.preventDefault();
        reviewDraft();
      }}
    >
      <ErrorSummary {errors} />
      <Field
        id="policy-draft"
        label="Retained synopsis draft"
        multiline
        bind:value={synopsis}
        help={`Current maximum: ${revisedMaximum} Unicode code points. Existing stored content is not rewritten.`}
        error={errors.find((item) => item.id === 'policy-draft')?.message ?? ''}
      />
      <p class="muted">{count(synopsis)} Unicode code points retained.</p>
      <Button type="submit">Review draft</Button>
    </form>
  {:else}
    <h1>Editing is temporarily unavailable</h1>
    <Notice
      title="The current input rules could not be loaded safely"
      tone="warning"
      ><p>
        Authorized reads remain available where possible. Your permitted draft
        is retained. Try loading the current rules again before editing.
      </p></Notice
    >
    <Field
      id="policy-retained"
      label="Retained synopsis draft"
      multiline
      value={initialDraft}
      disabled
      help="Editing stays unavailable until the current rules can be loaded."
    />
    <div class="actions">
      <Button disabledReason="Current input rules are unavailable."
        >Review draft</Button
      ><a class="button secondary" href={resolve('/')}>Read the movies</a>
    </div>
  {/if}
  {#if outcome}<Notice title={outcome} tone="success" />{/if}
</div>

<style>
  .policy-guidance {
    display: grid;
    gap: var(--primitive-space-6);
    min-width: 0;
  }
  form {
    display: grid;
    gap: var(--primitive-space-4);
    min-width: 0;
  }
  .actions {
    display: flex;
    flex-wrap: wrap;
    gap: var(--primitive-space-3);
    align-items: start;
  }
  .table-scroll {
    overflow-x: auto;
    max-width: 100%;
  }
  table {
    width: 100%;
    border-collapse: collapse;
    table-layout: fixed;
  }
  th,
  td {
    text-align: left;
    vertical-align: top;
    padding: var(--primitive-space-3);
    overflow-wrap: anywhere;
    border-bottom: 1px solid var(--line);
  }
  caption {
    text-align: left;
    padding-block-end: var(--primitive-space-3);
  }
</style>
