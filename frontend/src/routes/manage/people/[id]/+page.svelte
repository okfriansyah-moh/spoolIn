<script lang="ts">
  import { onMount } from 'svelte';
  import ArchiveAction from '../../../../lib/features/archive/ArchiveAction.svelte';
  import { page } from '$app/state';
  import { goto } from '$app/navigation';
  import { resolve } from '$app/paths';
  import Notice from '../../../../lib/components/Notice.svelte';
  import { createTransport } from '../../../../lib/transport/graphql';
  import { sessionController } from '../../../../lib/features/session/context';
  import { createDraftSource } from '../../../../lib/features/draft/api';
  import { createPersonEditorController } from '../../../../lib/features/person/write/editor-controller.svelte';
  import { loadWriteContext } from '../../../../lib/features/movie/write-context';
  import PersonEditorForm from '../../../../lib/features/person/write/PersonEditorForm.svelte';
  import EditorPageHeading from '../../../../lib/components/EditorPageHeading.svelte';
  import type { EditorState } from '../../../../lib/features/person/write/types';
  import { isRecordId } from '../../../../lib/components/safe-url';

  let controller = $state.raw<ReturnType<
    typeof createPersonEditorController
  > | null>(null);
  const sessionCtl = sessionController();
  const editorState = $derived<EditorState | null>(
    controller?.snapshot ?? null,
  );
  let ready = $state(false);
  const personId = $derived(page.params.id ?? '');
  const personReturnToken = $derived(page.url.searchParams.get('return') ?? '');
  let finishing = $state(false);
  let finishRequested = false;
  let completionError = $state('');

  async function finishPerson() {
    if (!controller || finishing) return;
    finishing = true;
    finishRequested = true;
    completionError = '';
    try {
      const result = await controller.finishEditing();
      if (result === 'blocked') return;
      const params = new URLSearchParams({ saved: personId });
      if (isRecordId(personReturnToken))
        params.set('restore', personReturnToken);
      await goto(resolve('/manage/people') + '?' + params.toString());
    } catch {
      completionError =
        'Person completion could not finish. Retry Save person to confirm the saved record.';
    } finally {
      finishing = false;
    }
  }

  async function checkSavedResult() {
    await controller?.checkOriginalResult();
    if (
      finishRequested &&
      (editorState?.phase === 'resolved' || editorState?.phase === 'saved')
    )
      await finishPerson();
  }

  let mounted = $state(false);
  let bootstrapError = $state('');
  let cleanupEditor = () => {};
  onMount(() => {
    mounted = true;
    return () => cleanupEditor();
  });
  $effect(() => {
    if (mounted) loadEditor(personId);
  });

  function loadEditor(id: string) {
    cleanupEditor();
    ready = false;
    controller = null;
    bootstrapError = '';
    finishRequested = false;
    completionError = '';
    if (!isRecordId(id)) {
      ready = true;
      return;
    }
    const transport = createTransport();
    const next = createPersonEditorController(
      transport,
      createDraftSource(transport),
      'edit',
      id,
    );
    controller = next;
    const abort = new AbortController();
    cleanupEditor = () => {
      abort.abort();
      next.dispose();
    };
    void bootstrapEditor(next, abort.signal);
  }

  async function bootstrapEditor(
    next: NonNullable<typeof controller>,
    signal: AbortSignal,
  ) {
    try {
      const context = await loadWriteContext(signal);
      const policy = sessionCtl.snapshot.policy;
      if (signal.aborted) return;
      if (!context || !policy) {
        bootstrapError =
          'Check your Maintainer session before loading this editor.';
        return;
      }
      await next.bootstrap(policy, context, signal);
      if (!signal.aborted) ready = true;
    } catch {
      if (!signal.aborted)
        bootstrapError = 'Person editor could not load. Retry the read.';
    }
  }
</script>

<svelte:head><title>Edit person · SpoolIn</title></svelte:head>
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
      title={editorState?.saved?.name ?? 'Edit person'}
      description="Update this person’s public name and biography."
      actionHref={editorState?.saved
        ? `people/${encodeURIComponent(editorState.saved.id)}`
        : undefined}
      actionLabel={editorState?.saved ? 'Review public record' : undefined}
    />

    {#if !isRecordId(personId)}
      <p role="alert">This person link is not valid.</p>
    {:else if ready && editorState && sessionCtl.snapshot.policy}
      {#if completionError}<Notice title="Person completion" tone="error"
          ><p>{completionError}</p></Notice
        >{/if}
      <PersonEditorForm
        state={editorState}
        policy={sessionCtl.snapshot.policy}
        disabled={editorState.phase === 'target-changed' || finishing}
        onSubmit={() => void finishPerson()}
        onCheckOriginal={() => void checkSavedResult()}
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
      {#if editorState.saved?.state === 'ACTIVE'}
        <ArchiveAction
          kind="person"
          id={editorState.saved.id}
          version={editorState.saved.version}
          disabled={editorState.phase === 'submitting'}
        />
      {/if}
    {:else if bootstrapError}
      <p role="alert">{bootstrapError}</p>
      <button type="button" onclick={() => loadEditor(personId)}
        >Retry person editor</button
      >
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
