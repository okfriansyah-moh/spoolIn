<script lang="ts">
  import ArchiveAction from '../../../../lib/features/archive/ArchiveAction.svelte';
  import { page } from '$app/state';
  import { createTransport } from '../../../../lib/transport/graphql';
  import { sessionController } from '../../../../lib/features/session/context';
  import { createDraftSource } from '../../../../lib/features/draft/api';
  import { createMovieEditorController } from '../../../../lib/features/movie/editor-controller';
  import { loadWriteContext } from '../../../../lib/features/movie/write-context';
  import MovieEditorForm from '../../../../lib/features/movie/MovieEditorForm.svelte';
  import EditorPageHeading from '../../../../lib/components/EditorPageHeading.svelte';
  import type {
    EditorState,
    WriteContext,
  } from '../../../../lib/features/movie/types';
  import ConfirmDialog from '../../../../lib/components/ConfirmDialog.svelte';
  import { isRecordId } from '../../../../lib/components/safe-url';
  import CreditPanel from '../../../../lib/features/credit/CreditPanel.svelte';
  import ArtworkUploadField from '../../../../lib/features/artwork/ArtworkUploadField.svelte';
  import { applyMovieDraftField } from '../../../../lib/features/movie/draft-field-update';

  const sessionCtl = sessionController();
  let editorState = $state<EditorState | null>(null);
  let writeContext = $state<WriteContext | null>(null);
  let creditTitleVersion = $state<string | null>(null);
  let ready = $state(false);
  let controller: ReturnType<typeof createMovieEditorController> | null = null;
  const movieId = $derived(page.params.id ?? '');

  $effect(() => {
    const selectedId = movieId;
    ready = false;
    editorState = null;
    writeContext = null;
    creditTitleVersion = null;
    if (!isRecordId(selectedId)) {
      ready = true;
      return;
    }
    const transport = createTransport();
    controller = createMovieEditorController(
      transport,
      createDraftSource(transport),
      'edit',
      selectedId,
    );
    const unsubscribe = controller.subscribe((next) => {
      editorState = next;
    });
    const abort = new AbortController();
    void (async () => {
      const context = await loadWriteContext(abort.signal);
      const policy = sessionCtl.snapshot.policy;
      if (!context || !policy || abort.signal.aborted) return;
      writeContext = context;
      await controller?.bootstrap(policy, context, abort.signal);
      ready = true;
    })();
    return () => {
      abort.abort();
      unsubscribe();
      controller?.dispose();
    };
  });

  function draftChange(
    field: import('../../../../lib/features/movie/draft-field-update').MovieDraftField,
    value: string,
  ) {
    if (!controller || !editorState) return;
    const update = applyMovieDraftField(editorState.draft, field, value);
    controller.updateDraft(
      update.draft,
      update.originalTitleTouched ? { originalTitleTouched: true } : {},
    );
  }
</script>

<svelte:head><title>Edit movie · SpoolIn</title></svelte:head>
<section class="movie-editor-page">
  <aside class="workspace-rail" aria-label="Workspace">
    <h2>Workspace</h2>
    <span class="workspace-current">Movies</span>
    <p>Manage active movies and their credits.</p>
  </aside>
  <div class="workspace-body">
    <EditorPageHeading
      sectionLabel="Movies"
      sectionHref="manage/movies"
      title={editorState?.saved?.title ?? 'Edit movie'}
      description="Update the details people use to recognize and find this film."
      actionHref={editorState?.saved
        ? `titles/${encodeURIComponent(editorState.saved.id)}`
        : undefined}
      actionLabel={editorState?.saved ? 'Review public record' : undefined}
    />

    {#if !isRecordId(movieId)}
      <p role="alert">This movie link is not valid.</p>
    {:else if ready && editorState && sessionCtl.snapshot.policy}
      <MovieEditorForm
        state={editorState}
        policy={sessionCtl.snapshot.policy}
        disabled={editorState.phase === 'target-changed'}
        onSubmit={() => void controller?.submit()}
        onCheckOriginal={() => void controller?.checkOriginalResult()}
        onReviewStale={() => controller?.toggleStaleReview()}
        onRebase={(reviewed) => void controller?.rebaseAfterReview(reviewed)}
        onSelectDraft={(id) => void controller?.selectRetainedDraft(id)}
        onStartNewDraft={() => controller?.startNewDraft()}
        onDraftChange={draftChange}
        onGenreToggle={(id, checked) => controller?.toggleGenre(id, checked)}
        onDiscardOpen={() => controller?.openDiscardDialog()}
      />
      {#if editorState.saved && writeContext && sessionCtl.snapshot.policy}
        <ArtworkUploadField
          request={createTransport()}
          csrf={writeContext.csrf}
          titleId={editorState.saved.id}
          expectedVersion={editorState.saved.version}
          policyRevision={sessionCtl.snapshot.policy.revision}
          artworkPolicy={sessionCtl.snapshot.policy.artwork}
          disabled={editorState.phase === 'target-changed' ||
            editorState.phase === 'stale'}
          onAttached={async () => {
            await controller?.refreshExternalChange();
            creditTitleVersion = null;
          }}
        />
        {@const version = creditTitleVersion ?? editorState.saved.version}
        <CreditPanel
          titleId={editorState.saved.id}
          titleVersion={version}
          policy={sessionCtl.snapshot.policy}
          context={writeContext}
          draftSource={createDraftSource(createTransport())}
          onTitleVersionChange={(next) => {
            creditTitleVersion = next;
            void controller?.refreshExternalChange();
          }}
        />
      {/if}
      {#if editorState.saved?.state === 'ACTIVE'}
        <ArchiveAction
          kind="movie"
          id={editorState.saved.id}
          version={editorState.saved.version}
          disabled={editorState.phase === 'submitting'}
        />
      {/if}
    {:else}
      <p role="status">Loading movie editor…</p>
    {/if}

    <ConfirmDialog
      open={editorState?.discardDialogOpen ?? false}
      title="Leave this draft?"
      action="Discard draft"
      oncancel={() => controller?.closeDiscardDialog()}
      onconfirm={() => void controller?.confirmDiscard()}
    >
      <p>
        Leaving discards the current unsaved form values. Saved movie
        information stays unchanged.
      </p>
    </ConfirmDialog>
  </div>
</section>

<style>
  .movie-editor-page {
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
    .movie-editor-page {
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
