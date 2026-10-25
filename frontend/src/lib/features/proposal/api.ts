import { record } from '../session/parse';
import { TransportFailure } from '../../transport/graphql';
import { safeArtwork } from '../../components/safe-url';

export type ProposalReview = Readonly<{
  proposalId: string;
  revision: number;
  contentHash: string;
  challenge: string;
  expiresAt: string;
  state: string;
  operationId: string;
  values: Record<string, unknown>;
  credits: Record<string, unknown>[];
  sources: { field: string; url: string; note: string }[];
  poster: {
    url: string;
    width: number;
    height: number;
    mediaType: string;
  } | null;
}>;

function text(value: unknown): string {
  if (typeof value !== 'string') throw new TransportFailure('unavailable');
  return value;
}

export function parseProposal(value: unknown): ProposalReview {
  const data = record(value);
  if (!Number.isInteger(data.revision) || Number(data.revision) < 1)
    throw new TransportFailure('unavailable');
  if (!Array.isArray(data.sources)) throw new TransportFailure('unavailable');
  return {
    proposalId: text(data.proposalId),
    revision: Number(data.revision),
    contentHash: text(data.contentHash),
    challenge: text(data.challenge),
    expiresAt: text(data.expiresAt),
    state: text(data.state),
    operationId: text(data.operationId),
    values: record(data.values),
    credits: Array.isArray(data.credits) ? data.credits.map(record) : [],
    sources: data.sources.map(parseSource),
    poster: parsePoster(data.poster),
  };
}

function proposalPosterUrl(value: unknown): string | undefined {
  const safe = safeArtwork(value);
  if (safe) return safe;
  if (
    typeof value === 'string' &&
    /^\/api\/movie-posters\/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/.test(
      value,
    )
  )
    return value;
  return undefined;
}

function parsePoster(value: unknown): ProposalReview['poster'] {
  if (value == null) return null;
  const poster = record(value);
  const url = proposalPosterUrl(poster.url);
  if (
    !url ||
    typeof poster.width !== 'number' ||
    typeof poster.height !== 'number'
  )
    throw new TransportFailure('unavailable');
  return {
    url,
    width: poster.width,
    height: poster.height,
    mediaType: text(poster.mediaType),
  };
}

function parseSource(value: unknown): ProposalReview['sources'][number] {
  const source = record(value);
  const note = [
    source.note,
    source.reuseBasis,
    source.retrievedAt,
    source.precision,
  ]
    .filter(
      (item): item is string => typeof item === 'string' && item.length > 0,
    )
    .join(' · ');
  return { field: text(source.field), url: text(source.url), note };
}

function responseFailure(status: number, write: boolean): TransportFailure {
  const code =
    status === 401
      ? 'UNAUTHENTICATED'
      : status === 403
        ? 'FORBIDDEN'
        : status === 409
          ? 'CONFLICT'
          : 'UNAVAILABLE';
  return new TransportFailure(
    write && status >= 500 ? 'uncertain' : 'rejected',
    code,
  );
}

export async function proposalRequest(
  id: string,
  suffix = '',
  options: {
    signal?: AbortSignal;
    csrf?: string;
    body?: Record<string, unknown>;
  } = {},
): Promise<unknown> {
  const headers: Record<string, string> = {
    Accept: 'application/json',
    'X-Spoolin-Client': 'web',
  };
  if (options.body) {
    if (!options.csrf) throw new TransportFailure('rejected', 'FORBIDDEN');
    headers['Content-Type'] = 'application/json';
    headers['X-CSRF-Token'] = options.csrf;
  }
  const response = await fetch(
    `/api/movie-proposals/${encodeURIComponent(id)}${suffix}`,
    {
      method: options.body ? 'POST' : 'GET',
      credentials: 'same-origin',
      cache: 'no-store',
      headers,
      ...(options.signal ? { signal: options.signal } : {}),
      ...(options.body ? { body: JSON.stringify(options.body) } : {}),
    },
  );
  if (!response.ok) throw responseFailure(response.status, !!options.body);
  return response.json();
}

export function proposalValue(value: unknown): string {
  if (value == null || value === '') return 'Unknown';
  if (Array.isArray(value))
    return value.map(proposalValue).join(', ') || 'None';
  if (typeof value === 'object') {
    const date = record(value);
    if (typeof date.year === 'number')
      return [date.year, date.month, date.day]
        .filter((part) => part != null && part !== 0)
        .map((part, index) =>
          index ? String(part).padStart(2, '0') : String(part),
        )
        .join('-');
    return JSON.stringify(value);
  }
  return String(value);
}

export function parseDecision(value: unknown): {
  state: string;
  problemCode: string | null;
} {
  const result = record(value);
  const state = text(result.state);
  if (!['PENDING', 'SUCCEEDED', 'FAILED', 'EXPIRED'].includes(state))
    throw new TransportFailure('uncertain');
  const problem = result.problem == null ? null : record(result.problem);
  return { state, problemCode: problem ? text(problem.code) : null };
}

export function posterStillProcessing(value: unknown): boolean {
  return record(value).state === 'PROCESSING';
}
