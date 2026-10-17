import { uuid } from '../session/parse';

export function optionalUuid(value: unknown): string | null {
  if (value === null) return null;
  return typeof value === 'string' ? uuid(value) : null;
}

export function optionalString(value: unknown): string | null {
  if (value === null) return null;
  return typeof value === 'string' ? value : null;
}

export function requiredString(value: unknown): string {
  return typeof value === 'string' ? value : '';
}
