<script lang="ts">
  import { untrack } from 'svelte';
  import Field from '../../components/Field.svelte';
  import Button from '../../components/Button.svelte';
  import DraftNotice from './DraftNotice.svelte';
  import { DraftController } from './controller';
  import type { PersonDraft } from '../../generated/json-types';
  import type { DraftSession, DraftSource, DraftTarget, DraftViewState } from './types';
  let {
    controller = new DraftController({} as DraftSource),
    session,
    target = { kind: 'PERSON', id: null },
    catalogOperationId = null,
    initialName = '',
  }: {
    controller?: DraftController;
    session: DraftSession;
    target?: DraftTarget;
    catalogOperationId?: string | null;
    initialName?: string;
  } = $props();
  let draftView = $state<DraftViewState>(controller.snapshot);
  let localName = $state(initialName);
  $effect(() => {
    controller.bindSession(session);
  });
  $effect(() => {
    untrack(() => {
      controller.beginEdit(target, {
        catalogOperationId,
        initial: { name: initialName, biography: null },
      });
      localName = initialName;
    });
    return controller.subscribe((next) => {
      draftView = next;
      if (document.activeElement?.id !== 'draft-name') {
        localName = (next.layers?.draft as PersonDraft | undefined)?.name ?? '';
      }
    });
  });
  $effect(() => {
    controller.setName(localName);
  });
  $effect(() => {
    return () => controller.dispose();
  });
</script>

<section aria-labelledby="draft-editor-heading">
  <h2 id="draft-editor-heading">Person draft</h2>
  {#if draftView.concealed}
    <p id="draft-concealed">
      {draftView.message || 'Protected draft is concealed.'}
    </p>
  {:else}
    <DraftNotice state={draftView} />
    <form
      role="form"
      aria-label="Person draft"
      onsubmit={(event) => event.preventDefault()}
    >
      <Field id="draft-name" label="Name" bind:value={localName} required />
      <Button type="button" onclick={() => controller.discard()}
        >Discard draft</Button
      >
    </form>
  {/if}
</section>

<style>
  section {
    display: grid;
    gap: 1rem;
    max-width: 36rem;
  }
</style>
