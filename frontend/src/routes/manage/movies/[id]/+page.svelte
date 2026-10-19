<script lang="ts">
  import Notice from '../../../../lib/components/Notice.svelte';
  import ArchiveAction from '../../../../lib/features/archive/ArchiveAction.svelte';
  import { page } from '$app/state';
  import { goto } from '$app/navigation';
  import { resolve } from '$app/paths';
  import { createTransport } from '../../../../lib/transport/graphql';
  import { sessionController } from '../../../../lib/features/session/context';
  import { createDraftSource } from '../../../../lib/features/draft/api';
  import { createMovieEditorController } from '../../../../lib/features/movie/editor-controller.svelte';
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

  let controller = $state.raw<ReturnType<
    typeof createMovieEditorController
  > | null>(null);
  const sessionCtl = sessionController();
  const editorState = $derived<EditorState | null>(
    controller?.snapshot ?? null,
  );
  let writeContext = $state<WriteContext | null>(null);
  let creditTitleVersion = $state<string | null>(null);
  let ready = $state(false);
  const movieId = $derived(page.params.id ?? '');
  const movieReturnToken = $derived(page.url.searchParams.get('return') ?? '');
  const creditsStep = $derived(page.url.searchParams.get('step') === 'credits');
  const createdFlow = $derived(
    creditsStep && page.url.searchParams.get('created') === '1',
  );

  let artworkBlocked = $state(false);
  let artworkBusy = $state(false);
  let artworkMessage = $state('');
  let finishing = $state(false);
  let finishRequested = false;
  let completionError = $state('');
  const finishBlocked = $derived(
    artworkBlocked ||
      finishing ||
      !editorState?.saved ||
      !!editorState?.draftChoices.length ||
      ['submitting', 'uncertain', 'stale', 'target-changed'].includes(
        editorState?.phase ?? 'idle',
      ),
  );

  async function finishMovie() {
    if (finishBlocked || !controller || creditsStep) return;
    finishing = true;
    finishRequested = true;
    completionError = '';
    try {
      const result = await controller.finishEditing();
      if (result === 'blocked') return;
      const params = new URLSearchParams({ saved: movieId });
      if (isRecordId(movieReturnToken)) params.set('restore', movieReturnToken);
      await goto(resolve('/manage/movies') + '?' + params.toString());
    } catch {
      completionError =
        'Completion could not finish. Your saved artwork is retained. Retry Save movie.';
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
      await finishMovie();
  }

  $effect(() => {
    const selectedId = movieId;
    ready = false;
    controller = null;
    writeContext = null;
    creditTitleVersion = null;
    artworkBlocked = false;
    artworkBusy = false;
    artworkMessage = '';
    completionError = '';
    finishRequested = false;
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
      title={creditsStep
        ? `Cast and creators · ${editorState?.saved?.title ?? 'Movie'}`
        : (editorState?.saved?.title ?? 'Edit movie')}
      description={creditsStep
        ? createdFlow
          ? 'Movie created. Add the people behind it, or skip this step for now.'
          : 'Manage the people who contributed to this movie.'
        : 'Update the details people use to recognize and find this film.'}
      actionHref={editorState?.saved
        ? `titles/${encodeURIComponent(editorState.saved.id)}`
        : undefined}
      actionLabel={editorState?.saved ? 'Review public record' : undefined}
    />

    {#if !isRecordId(movieId)}
      <p role="alert">This movie link is not valid.</p>
    {:else if ready && editorState && sessionCtl.snapshot.policy}
      {#if !creditsStep}
        <MovieEditorForm
          state={editorState}
          policy={sessionCtl.snapshot.policy}
          disabled={editorState.phase === 'target-changed' ||
            artworkBusy ||
            finishing}
          showActions={false}
          onSubmit={() => void finishMovie()}
          onCheckOriginal={() => void checkSavedResult()}
          onReviewStale={() => controller?.toggleStaleReview()}
          onRebase={(reviewed) => void controller?.rebaseAfterReview(reviewed)}
          onSelectDraft={(id) => void controller?.selectRetainedDraft(id)}
          onStartNewDraft={() => controller?.startNewDraft()}
          onDraftChange={draftChange}
          onGenreToggle={(id, checked) => controller?.toggleGenre(id, checked)}
          onDiscardOpen={() => controller?.openDiscardDialog()}
        />
      {/if}
      {#if editorState.saved && writeContext && sessionCtl.snapshot.policy}
        {#if !creditsStep}
          <ArtworkUploadField
            request={createTransport()}
            csrf={writeContext.csrf}
            titleId={editorState.saved.id}
            expectedVersion={editorState.saved.version}
            policyRevision={sessionCtl.snapshot.policy.revision}
            artworkPolicy={sessionCtl.snapshot.policy.artwork}
            disabled={finishing ||
              editorState.phase === 'submitting' ||
              editorState.phase === 'uncertain' ||
              editorState.phase === 'target-changed' ||
              editorState.phase === 'stale'}
            onStateChange={(state) => {
              artworkBlocked = state.blocked;
              artworkBusy = state.busy;
              artworkMessage = state.message;
            }}
            onAttached={async () => {
              if (!(await controller?.refreshAfterArtwork()))
                throw new Error('Movie details refresh is not confirmed.');
              creditTitleVersion = null;
            }}
          />
          {#if completionError}<Notice title="Movie completion" tone="error"
              ><p>{completionError}</p></Notice
            >{/if}
          <div class="finish-actions">
            <button
              type="button"
              class="secondary"
              disabled={artworkBlocked ||
                finishing ||
                editorState.phase === 'submitting' ||
                editorState.phase === 'uncertain'}
              onclick={() => controller?.openDiscardDialog()}
              >Leave draft</button
            >
            <button
              type="submit"
              form="movie-editor-form"
              disabled={finishBlocked}
              aria-busy={finishing}
              aria-describedby="movie-finish-help"
              >{finishing ? 'Saving…' : 'Save movie'}</button
            >
          </div>
          <p id="movie-finish-help" role="status">
            {artworkMessage ||
              (editorState.phase === 'stale'
                ? 'Review the current movie values above before saving.'
                : editorState.phase === 'uncertain'
                  ? 'Check the original movie save result above before finishing.'
                  : editorState.draftChoices.length
                    ? 'Choose a retained draft above before saving.'
                    : editorState.phase === 'target-changed'
                      ? 'This movie is no longer available for editing.'
                      : 'Save your movie details and return to Movies. Artwork is saved separately when its upload completes.')}
          </p>
        {/if}
        {#if creditsStep}
          {@const version = creditTitleVersion ?? editorState.saved.version}
          <CreditPanel
            titleId={editorState.saved.id}
            titleVersion={version}
            showSkip={createdFlow}
            onFinish={() => {
              void goto(
                resolve('/manage/movies') +
                  (createdFlow
                    ? '?created=' + encodeURIComponent(movieId)
                    : isRecordId(movieReturnToken)
                      ? '?restore=' + encodeURIComponent(movieReturnToken)
                      : ''),
              );
            }}
            policy={sessionCtl.snapshot.policy}
            context={writeContext}
            draftSource={createDraftSource(createTransport())}
            onTitleVersionChange={(next) => {
              creditTitleVersion = next;
              void controller?.refreshExternalChange();
            }}
          />
        {/if}
      {/if}
      {#if !creditsStep && editorState.saved?.state === 'ACTIVE'}
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
  .finish-actions {
    display: flex;
    flex-wrap: wrap;
    gap: var(--primitive-space-3);
    margin-top: var(--primitive-space-4);
  }
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
