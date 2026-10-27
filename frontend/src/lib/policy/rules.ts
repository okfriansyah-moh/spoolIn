import type { Policy, TextBound, TextField } from './types';
import { PolicyUnavailable } from './types';
export function boundFor(policy: Policy, field: TextField): TextBound {
  const bound = policy.textBounds[field];
  if (!bound) throw new PolicyUnavailable();
  return bound;
}
export function count(value: string): number {
  return Array.from(value).length;
}
export function trimOrdinary(value: string): string {
  let start = 0;
  let end = value.length;
  const whitespace = /\p{White_Space}/u;
  while (start < end && whitespace.test(value[start]!)) start += 1;
  while (end > start && whitespace.test(value[end - 1]!)) end -= 1;
  return value.slice(start, end);
}
export function checkText(
  value: string,
  bound: TextBound,
): { value: string; error: string } {
  if (value.includes(String.fromCharCode(0)) || /[\uD800-\uDFFF]/u.test(value))
    return { value, error: 'Remove invalid Unicode characters.' };
  const checked = bound.normalization === 'EXACT' ? value : trimOrdinary(value);
  if (
    bound.normalization === 'EXACT' &&
    new TextEncoder().encode(checked).length > 512
  )
    return { value, error: 'Use at most 512 UTF-8 bytes.' };
  const size = count(checked);
  const error =
    size < bound.minCodePoints || size > bound.maxCodePoints
      ? `Use ${bound.minCodePoints} to ${bound.maxCodePoints} Unicode code points.`
      : '';
  return { value: error ? value : checked, error };
}
export type OptionalIntent =
  { kind: 'omitted' } | { kind: 'clear' } | { kind: 'value'; value: string };
export function optionalIntent(
  present: boolean,
  value: string | null,
): OptionalIntent {
  if (!present) return { kind: 'omitted' };
  if (value === null || trimOrdinary(value) === '') return { kind: 'clear' };
  return { kind: 'value', value: trimOrdinary(value) };
}
export function changedText(
  stored: string,
  proposed: string,
  bound: TextBound,
) {
  return proposed === stored
    ? { value: stored, error: '' }
    : checkText(proposed, bound);
}
