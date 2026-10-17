import { boundFor, checkText } from '../../policy/rules';
import type { Policy } from '../../policy/types';

export function validateCharacterNames(
  names: readonly string[],
  policy: Policy,
  allowsCharacters: boolean,
): string | null {
  if (!allowsCharacters && names.length) return 'This role does not accept character names.';
  const bound = boundFor(policy, 'characterName');
  const seen = new Set<string>();
  for (const name of names) {
    const trimmed = name.trim();
    if (!trimmed) return 'Each character name must be a complete non-empty string.';
    const result = checkText(name, bound);
    if (result.error) return result.error;
    if (seen.has(trimmed)) return 'Remove duplicate character names.';
    seen.add(trimmed);
  }
  return null;
}
