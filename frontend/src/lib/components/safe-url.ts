export function safeUrl(value: unknown): string | undefined {
  if (typeof value !== 'string' || !/^https?:\/\//i.test(value))
    return undefined;
  if (
    [...value].some(
      (character) =>
        character.charCodeAt(0) <= 32 || character.charCodeAt(0) === 127,
    )
  )
    return undefined;
  try {
    const url = new URL(value);
    if (!url.hostname || url.username || url.password) return undefined;
    return value;
  } catch {
    return undefined;
  }
}

export function isRecordId(value: string): boolean {
  return /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/.test(
    value,
  );
}
export function safeArtwork(value: unknown): string | undefined {
  if (typeof value !== 'string' || !value.startsWith('/artwork/'))
    return undefined;
  return isRecordId(value.slice(9)) ? value : undefined;
}
