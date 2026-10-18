export type ArtworkStage =
  'idle' | 'transferring' | 'processing' | 'saved' | 'failed' | 'uncertain';

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

export function transferCompleteIsNotSaved(
  transferred: number,
  total: number,
  attached: boolean,
): boolean {
  return total > 0 && transferred >= total && !attached;
}

export function artworkFailureMessage(code: string | null): string {
  switch (code) {
    case 'FILE_INVALID':
    case 'FIELD_INVALID':
    case 'INVALID_IMAGE_DIMENSIONS':
      return 'Artwork was rejected. Use a static JPEG, PNG, or WebP image, at most 8192 pixels per side and 40 million pixels in total.';
    case 'CONFLICT':
      return 'The movie changed before artwork could be attached. Refresh the movie before uploading again.';
    case 'RATE_LIMITED':
      return 'Artwork upload was rate limited. Wait before trying again.';
    case 'UNAUTHENTICATED':
    case 'FORBIDDEN':
      return 'Artwork upload was not authorized. Sign in again and refresh the movie.';
    case 'RESOURCE_EXHAUSTED':
      return 'The upload exceeded the request limit. Choose a smaller image.';
    case 'ARTWORK_UNAVAILABLE':
      return 'Artwork processing could not finish. The existing artwork is unchanged. Try a smaller static image.';
    default:
      return 'Artwork could not be attached. The existing artwork is unchanged.';
  }
}
