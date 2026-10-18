import type {
  MovieContext,
  MovieValues,
  Outcome,
  PartialDate,
  ProposalRevision,
} from '../generated/spoolin/v1/spoolin.js';

function dateParts(value: PartialDate | undefined) {
  return {
    year: value?.year ?? null,
    month: value?.month ?? null,
    day: value?.day ?? null,
  };
}
function datePrecision(value: PartialDate | undefined) {
  if (value?.day) return 'DAY';
  if (value?.month) return 'MONTH';
  if (value?.year) return 'YEAR';
  return 'UNKNOWN';
}
export function dateProjection(value: PartialDate | undefined) {
  return { ...dateParts(value), precision: datePrecision(value) };
}
export function contextProjection(value: MovieContext) {
  return {
    titles: (value.titles?.nodes ?? []).map((title) => ({
      id: title.id,
      title: title.values?.title ?? '',
      release: dateProjection(title.values?.release),
      genreIds: title.values?.genreIds ?? [],
    })),
    genres: value.genres?.nodes ?? [],
    roles: value.roles?.nodes ?? [],
    policyRevision: value.policy?.revision ?? '',
  };
}
function movieMetadata(value: Partial<MovieValues>) {
  return {
    originalTitle: value.originalTitle ?? null,
    runtimeMinutes: value.runtimeMinutes ?? null,
    originalLanguage: value.originalLanguage ?? null,
    synopsisLanguage: value.synopsisLanguage ?? '',
    countries: value.countries ?? [],
  };
}
export function movieProjection(value: MovieValues | undefined) {
  const movie: Partial<MovieValues> = value ?? {};
  const metadata = movieMetadata(movie);
  return {
    title: movie.title ?? '',
    originalTitle: metadata.originalTitle,
    synopsis: movie.synopsis ?? '',
    genreIds: movie.genreIds ?? [],
    release: dateParts(movie.release),
    runtimeMinutes: metadata.runtimeMinutes,
    originalLanguage: metadata.originalLanguage,
    synopsisLanguage: metadata.synopsisLanguage,
    countries: metadata.countries,
  };
}
export function proposalProjection(
  value: ProposalRevision,
  policyRevision: string,
) {
  if (!/^[A-Za-z0-9+/]{43}=$/.test(value.contentHash))
    throw new Error('Invalid proposal digest');
  const digest = Buffer.from(value.contentHash, 'base64');
  if (digest.length !== 32 || digest.toString('base64') !== value.contentHash)
    throw new Error('Invalid proposal digest');
  return {
    proposalId: value.proposalId,
    revision: value.revision,
    contentHash: digest.toString('hex'),
    expiresAt: new Date(
      Number(value.expiresAt?.seconds ?? 0) * 1000,
    ).toISOString(),
    normalizedMovie: movieProjection(value.values),
    policyRevision,
    status: 'REVIEW',
    reviewResourceUri: `spoolin://proposal/${value.proposalId}/${value.revision}`,
  };
}
export function outcomeProjection(value: Outcome) {
  return {
    operationId: value.operationId,
    state: value.state,
    resultExpired: value.resultExpired,
    ...(value.resultId ? { resultId: value.resultId } : {}),
    ...(value.version ? { version: value.version } : {}),
    ...(value.problem
      ? {
          problem: {
            code: value.problem.code,
            message: 'The operation could not be completed.',
            fields: value.problem.fields,
            retryAfterMs: value.problem.retryAfterMs ?? null,
          },
        }
      : {}),
  };
}
