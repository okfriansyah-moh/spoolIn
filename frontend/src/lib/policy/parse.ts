import { PolicyUnavailable, requestFields, textFields } from './types';
import type { Policy, TextBound } from './types';

function record(value: unknown): Record<string, unknown> {
  if (value === null || typeof value !== 'object' || Array.isArray(value))
    throw new PolicyUnavailable();
  return Object.fromEntries(Object.entries(value));
}
function exact(value: Record<string, unknown>, fields: readonly string[]) {
  if (
    Object.keys(value).length !== fields.length ||
    fields.some((name) => !(name in value))
  )
    throw new PolicyUnavailable();
}
function integer(value: unknown, min = 1): number {
  if (
    typeof value !== 'number' ||
    !Number.isInteger(value) ||
    value < min ||
    value > 2147483647
  )
    throw new PolicyUnavailable();
  return value;
}
function long(value: unknown): string {
  if (
    typeof value !== 'string' ||
    !/^[0-9]+$/.test(value) ||
    BigInt(value) < 1n ||
    BigInt(value) > 9223372036854775807n
  )
    throw new PolicyUnavailable();
  return value;
}
function textBound(value: unknown, field: string): TextBound {
  const input = record(value);
  exact(input, ['minCodePoints', 'maxCodePoints', 'required', 'normalization']);
  const minCodePoints = integer(input.minCodePoints, 0);
  const maxCodePoints = integer(input.maxCodePoints);
  if (
    minCodePoints > maxCodePoints ||
    typeof input.required !== 'boolean' ||
    (input.required && minCodePoints < 1)
  )
    throw new PolicyUnavailable();
  const normalization = field === 'password' ? 'EXACT' : 'TRIM_WHITE_SPACE';
  if (input.normalization !== normalization) throw new PolicyUnavailable();
  return Object.freeze({
    minCodePoints,
    maxCodePoints,
    required: input.required,
    normalization,
  });
}
function artwork(value: unknown): Policy['artwork'] {
  const input = record(value);
  exact(input, [
    'maxInputBytes',
    'formats',
    'maxDimensionPx',
    'maxPixels',
    'maxDecodedBytes',
    'maxOutputBytes',
  ]);
  if (
    !Array.isArray(input.formats) ||
    !input.formats.length ||
    input.formats.some((item) => !['JPEG', 'PNG', 'WEBP'].includes(item)) ||
    new Set(input.formats).size !== input.formats.length
  )
    throw new PolicyUnavailable();
  const formats: string[] = input.formats.filter(
    (item): item is string => typeof item === 'string',
  );
  return Object.freeze({
    maxInputBytes: long(input.maxInputBytes),
    formats: Object.freeze(formats),
    maxDimensionPx: integer(input.maxDimensionPx),
    maxPixels: long(input.maxPixels),
    maxDecodedBytes: long(input.maxDecodedBytes),
    maxOutputBytes: long(input.maxOutputBytes),
  });
}
export function parsePolicy(value: unknown): Policy {
  const input = record(value);
  exact(input, ['revision', 'textBounds', 'artwork', 'requestLimits']);
  if (
    typeof input.revision !== 'string' ||
    !/^[0-9a-f]{64}$/.test(input.revision)
  )
    throw new PolicyUnavailable();
  const text = record(input.textBounds);
  exact(text, textFields);
  const requests = record(input.requestLimits);
  exact(requests, requestFields);
  const textBounds = Object.freeze(
    Object.fromEntries(
      textFields.map((name): [string, TextBound] => [
        name,
        textBound(text[name], name),
      ]),
    ),
  );
  const requestLimits = Object.freeze(
    Object.fromEntries(
      requestFields.map((name): [string, number] => [
        name,
        integer(requests[name]),
      ]),
    ),
  );
  if ((requestLimits.defaultPage ?? 0) > (requestLimits.maxPage ?? 0))
    throw new PolicyUnavailable();
  return Object.freeze({
    revision: input.revision,
    textBounds,
    artwork: artwork(input.artwork),
    requestLimits,
  });
}
