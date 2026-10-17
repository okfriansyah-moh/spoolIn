<script lang="ts">
  import { onMount } from 'svelte';
  import { resolve } from '$app/paths';
  import { createTransport } from '../../../../lib/transport/graphql';
  import { sessionController } from '../../../../lib/features/session/context';
  import { createDraftSource } from '../../../../lib/features/draft/api';
  import { createMovieEditorController } from '../../../../lib/features/movie/editor-controller';
  import { loadWriteContext } from '../../../../lib/features/movie/write-context';
  import MovieEditorForm from '../../../../lib/features/movie/MovieEditorForm.svelte';
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
    const unsubscribe = controller.subscribe((next) => {
      editorState = next;
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
    field: 'title' | 'synopsis' | 'originalTitle' | 'releaseYear',
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
<h1 tabindex="-1">Create movie</h1>
<p class="muted"><a href={resolve('/manage/movies')}>Back to movies</a></p>

{#if ready && editorState && sessionCtl.snapshot.policy}
  <MovieEditorForm
    state={editorState}
    policy={sessionCtl.snapshot.policy}
    onSubmit={() => void controller?.submit()}
    onCheckOriginal={() => void controller?.checkOriginalResult()}
    onReviewStale={() => controller?.toggleStaleReview()}
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
    Leaving discards the current unsaved form values. Saved movie information
    stays unchanged.
  </p>
</ConfirmDialog>

<style>
  .muted {
    margin-bottom: 1rem;
  }
</style>
