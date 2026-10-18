import type { Policy } from '../../policy/types';

const formatByMime: Readonly<Record<string, string>> = Object.freeze({
  'image/jpeg': 'JPEG',
  'image/png': 'PNG',
  'image/webp': 'WEBP',
});

export function validateArtworkFile(
  file: Pick<File, 'size' | 'type'>,
  policy: Policy['artwork'] | undefined,
): string {
  if (!policy || !/^[1-9][0-9]*$/.test(policy.maxInputBytes))
    return 'Current artwork rules are unavailable. Refresh the page before selecting a file.';
  if (!Number.isSafeInteger(file.size) || file.size <= 0)
    return 'Select a nonempty image file.';
  if (BigInt(file.size) > BigInt(policy.maxInputBytes))
    return 'This file exceeds the current artwork byte limit.';
  const format = formatByMime[file.type.toLowerCase()];
  if (!format || !policy.formats.includes(format))
    return 'Choose an image format allowed by the current artwork rules.';
  return '';
}
