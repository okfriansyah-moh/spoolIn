export type ArtworkStage = 'idle' | 'transferring' | 'processing' | 'saved' | 'failed' | 'uncertain';

export function stageLabel(stage: ArtworkStage): string {
  switch (stage) {
    case 'transferring':
      return 'Transferring…';
    case 'processing':
      return 'Processing…';
    case 'saved':
      return 'Saved';
    case 'failed':
      return 'Upload failed';
    case 'uncertain':
      return 'Checking result…';
    default:
      return 'Select artwork';
  }
}

export function transferCompleteIsNotSaved(transferred: number, total: number, attached: boolean): boolean {
  return total > 0 && transferred >= total && !attached;
}
