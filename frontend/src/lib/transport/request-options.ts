import type { RequestOptions } from './graphql';

export function withSignal(signal?: AbortSignal): RequestOptions {
  if (signal === undefined) return {};
  return { signal };
}

export function withCsrfAndSignal(
  csrf: string,
  signal?: AbortSignal,
): RequestOptions {
  if (signal === undefined) return { csrf };
  return { csrf, signal };
}
