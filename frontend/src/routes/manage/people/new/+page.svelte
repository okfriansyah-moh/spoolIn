<script lang="ts">
  import { onMount } from 'svelte';
  import { createTransport } from '../../../../lib/transport/graphql';
  import { sessionController } from '../../../../lib/features/session/context';
  import { createDraftSource } from '../../../../lib/features/draft/api';
  import { createPersonEditorController } from '../../../../lib/features/person/write/editor-controller';
  import { loadWriteContext } from '../../../../lib/features/movie/write-context';
  import PersonEditorForm from '../../../../lib/features/person/write/PersonEditorForm.svelte';
  import EditorPageHeading from '../../../../lib/components/EditorPageHeading.svelte';
  import type { EditorState } from '../../../../lib/features/person/write/types';

  const sessionCtl = sessionController();
  let editorState = $state<EditorState | null>(null);
  let ready = $state(false);
  let bootstrapError = $state('');
  let retryBootstrap = async () => {};
  let controller: ReturnType<typeof createPersonEditorController> | null = null;

  onMount(() => {
    const transport = createTransport();
    controller = createPersonEditorController(
      transport,
      createDraftSource(transport),
      'create',
      null,
    );
    const unsubscribe = controller.subscribe((next) => {
      editorState = next;
    });
    const abort = new AbortController();
    retryBootstrap = async () => {
      bootstrapError = '';
      try {
        const context = await loadWriteContext(abort.signal);
        const policy = sessionCtl.snapshot.policy;
        if (abort.signal.aborted) return;
        if (!context || !policy) {
          bootstrapError =
            'Check your Maintainer session before loading this editor.';
          return;
        }
        await controller?.bootstrap(policy, context, abort.signal);
        if (!abort.signal.aborted) ready = true;
      } catch {
        if (!abort.signal.aborted)
          bootstrapError = 'Person editor could not load. Retry the read.';
      }
    };
    void retryBootstrap();
    return () => {
      abort.abort();
      unsubscribe();
      controller?.dispose();
    };
  });
</script>

<svelte:head><title>Create person · SpoolIn</title></svelte:head>
<section class="person-editor-page">
  <aside class="workspace-rail" aria-label="Workspace">
    <h2>Workspace</h2>
    <span class="workspace-current">People</span>
    <p>Manage shared names and biographies.</p>
  </aside>
  <div class="workspace-body">
    <EditorPageHeading
      sectionLabel="People"
      sectionHref="manage/people"
      title="Create person"
      description="Add someone who has contributed to a movie in the catalog."
    />

    {#if bootstrapError}
      <p role="alert">{bootstrapError}</p>
      <button type="button" onclick={() => void retryBootstrap()}
        >Retry person editor</button
      >
    {:else if ready && editorState && sessionCtl.snapshot.policy}
      <PersonEditorForm
        state={editorState}
        policy={sessionCtl.snapshot.policy}
        onSubmit={() => void controller?.submit()}
        onCheckOriginal={() => void controller?.checkOriginalResult()}
        onReviewStale={() => controller?.toggleStaleReview()}
        onRebase={(reviewed) => void controller?.rebaseAfterReview(reviewed)}
        onSelectDraft={(id) => void controller?.selectRetainedDraft(id)}
        onStartNewDraft={() => controller?.startNewDraft()}
        onDiscardOpen={() => controller?.openDiscardDialog()}
        onDiscardClose={() => controller?.closeDiscardDialog()}
        onDiscardConfirm={() => void controller?.confirmDiscard()}
        onDraftChange={(field, value) => {
          if (!controller || !editorState) return;
          controller.updateDraft({ ...editorState.draft, [field]: value });
        }}
      />
    {:else}
      <p role="status">Loading person editor…</p>
    {/if}
  </div>
</section>

<style>
  .person-editor-page {
    display: grid;
    grid-template-columns: 185px minmax(0, 1fr);
    align-items: start;
    gap: clamp(var(--primitive-space-6), 4vw, var(--primitive-space-8));
    width: 100%;
  }
  .workspace-body {
    min-width: 0;
  }
  .workspace-rail h2 {
    font-size: 19px;
  }
  .workspace-current {
    display: block;
    padding: var(--primitive-space-2) var(--primitive-space-3);
    margin-block: var(--primitive-space-4);
    border-radius: var(--radius);
    background: var(--selected);
    font-size: 14px;
  }
  .workspace-rail p {
    color: var(--muted);
    font-size: var(--primitive-type-small);
  }
  .workspace-body :global(.editor-page-heading h1) {
    font-size: clamp(30px, 3vw, 36px);
  }
  .workspace-body :global(.editor-page-heading p) {
    font-size: 14px;
  }
  @media (max-width: 899px) {
    .person-editor-page {
      grid-template-columns: minmax(0, 1fr);
      gap: var(--primitive-space-6);
    }
    .workspace-rail {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      gap: var(--primitive-space-3);
      padding-bottom: var(--primitive-space-3);
      border-bottom: 1px solid var(--line);
    }
    .workspace-rail h2 {
      margin: 0;
      font-size: 17px;
    }
    .workspace-current {
      margin: 0;
    }
    .workspace-rail p {
      display: none;
    }
  }
</style>
