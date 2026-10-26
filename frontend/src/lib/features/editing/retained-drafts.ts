import type { DraftRecord } from '../draft/types';

type DraftLoader<Context> = { load(context: Context, signal: AbortSignal): Promise<readonly DraftRecord[]> };

export async function selectRetainedDraft<Context>(
  id: string, context: Context | null, phase: string, active: AbortSignal | undefined,
  source: DraftLoader<Context>, apply: (draft: DraftRecord, signal: AbortSignal) => Promise<void>, report: (message: string) => void,
): Promise<void> {
  if (!context || phase === 'uncertain' || phase === 'submitting') return;
  const signal = active ?? new AbortController().signal;
  try {
    const drafts = await source.load(context, signal);
    const selected = drafts.find((draft) => draft.id === id);
    if (!selected || signal.aborted) {
      report('That retained draft is no longer available. Choose another draft or start again.');
      return;
    }
    await apply(selected, signal);
  } catch { report('The retained draft could not be checked. Retry before continuing.'); }
}

export async function reconcileRetainedDraft(
  draft: DraftRecord, signal: AbortSignal, sync: { adopt(draft: DraftRecord): void },
  pending: (id: string) => void, check: () => Promise<void>, phase: () => string,
): Promise<boolean> {
  sync.adopt(draft);
  if (!draft.operationId) return true;
  pending(draft.operationId);
  await check();
  return !signal.aborted && phase() === 'failed';
}

export function newDraftState<Saved, Draft>(saved: Saved | null, project: (saved: Saved) => Draft, empty: Draft) {
  return { draftChoices: Object.freeze([]), draftBaseKnown: true, phase: 'idle' as const,
    draft: saved ? project(saved) : empty,
    message: 'Started a new draft. Other retained drafts remain available after reload.' };
}

export function submissionBlocked(pending: string | null, choices: number, phase: string, mode: string, saved: unknown): boolean {
  return !!pending || choices > 0 || ['submitting', 'uncertain', 'stale', 'target-changed'].includes(phase) || (mode === 'create' && saved !== null);
}


type RecoveryNotice = Readonly<{
  draftChoices?: readonly never[];
  phase?: 'uncertain';
  pendingOperationId?: string;
  message?: string;
}>;

type RecoveryPort = Readonly<{
  sync: { adopt(draft: DraftRecord): void };
  update: (notice: RecoveryNotice) => void;
  check: () => Promise<void>;
  state: () => Readonly<{ phase: string; saved: Readonly<{ version: string }> | null }>;
}>;

export async function recoverRetainedDraft(
  draft: DraftRecord,
  signal: AbortSignal,
  staleMessage: string,
  port: RecoveryPort,
) {
  port.update({ draftChoices: Object.freeze([]) });
  const ready = await reconcileRetainedDraft(
    draft, signal, port.sync,
    (id) => port.update({
      phase: 'uncertain', pendingOperationId: id,
      message: 'Check the original save before continuing this retained draft.',
    }),
    port.check, () => port.state().phase,
  );
  if (!ready) return null;
  const version = port.state().saved?.version;
  const stale = version !== undefined && draft.baseVersion !== version;
  return {
    phase: stale ? 'stale' as const : 'idle' as const,
    pendingOperationId: null,
    draftBaseKnown: !stale,
    message: stale ? staleMessage : 'Saved draft recovered.',
    staleReviewOpen: stale,
  };
}
