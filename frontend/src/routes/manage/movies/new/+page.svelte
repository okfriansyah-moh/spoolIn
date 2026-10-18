<script lang="ts">
  import { onMount } from 'svelte';
  import { goto } from '$app/navigation';
  import { resolve } from '$app/paths';
  import { createTransport } from '../../../../lib/transport/graphql';
  import { sessionController } from '../../../../lib/features/session/context';
  import { createDraftSource } from '../../../../lib/features/draft/api';
  import { createMovieEditorController } from '../../../../lib/features/movie/editor-controller';
  import { loadWriteContext } from '../../../../lib/features/movie/write-context';
  import MovieEditorForm from '../../../../lib/features/movie/MovieEditorForm.svelte';
  import EditorPageHeading from '../../../../lib/components/EditorPageHeading.svelte';
  import type { EditorState } from '../../../../lib/features/movie/types';
  import ConfirmDialog from '../../../../lib/components/ConfirmDialog.svelte';
  import { applyMovieDraftField } from '../../../../lib/features/movie/draft-field-update';

  const sessionCtl = sessionController();
  let editorState = $state<EditorState | null>(null);
  let ready = $state(false);
  let controller: ReturnType<typeof createMovieEditorController> | null = null;

  onMount(() => {
    const transport = createTransport();
    controller = createMovieEditorController(
      transport,
      createDraftSource(transport),
      'create',
      null,
    );
    let navigating = false;
    const unsubscribe = controller.subscribe((next) => {
      editorState = next;
      if (
        !navigating &&
        next.saved &&
        (next.phase === 'saved' || next.phase === 'resolved')
      ) {
        navigating = true;
        const destination =
          resolve('/manage/movies/[id]', { id: next.saved.id }) +
          '?step=credits';
        void goto(destination, { replaceState: true }).catch(() => {
          navigating = false;
        });
      }
    });
    const abort = new AbortController();
    void (async () => {
      const context = await loadWriteContext(abort.signal);
      const policy = sessionCtl.snapshot.policy;
      if (!context || !policy || abort.signal.aborted) return;
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

<svelte:head><title>Create movie · SpoolIn</title></svelte:head>
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
      title="Create movie"
      description="Start with the details people use to recognize and find this film."
    />

    {#if ready && editorState && sessionCtl.snapshot.policy}
      <MovieEditorForm
        state={editorState}
        policy={sessionCtl.snapshot.policy}
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
    {:else}
      <p role="status">Loading create form…</p>
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
