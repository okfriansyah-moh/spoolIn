import type { MovieContext, MovieValues, Outcome, PartialDate, ProposalRevision } from '../generated/spoolin/v1/spoolin.js';

export function dateProjection(value: PartialDate | undefined) {
  return { year: value?.year ?? null, month: value?.month ?? null, day: value?.day ?? null,
    precision: value?.day ? 'DAY' : value?.month ? 'MONTH' : value?.year ? 'YEAR' : 'UNKNOWN' };
}
export function contextProjection(value: MovieContext) {
  return { titles: (value.titles?.nodes ?? []).map((title) => ({ id: title.id, title: title.values?.title ?? '',
    release: dateProjection(title.values?.release), genreIds: title.values?.genreIds ?? [] })),
    genres: value.genres?.nodes ?? [], roles: value.roles?.nodes ?? [], policyRevision: value.policy?.revision ?? '' };
}
export function movieProjection(value: MovieValues | undefined) {
  return { title: value?.title ?? '', originalTitle: value?.originalTitle ?? null, synopsis: value?.synopsis ?? '',
    genreIds: value?.genreIds ?? [], release: { year: value?.release?.year ?? null, month: value?.release?.month ?? null, day: value?.release?.day ?? null }, runtimeMinutes: value?.runtimeMinutes ?? null,
    originalLanguage: value?.originalLanguage ?? null, synopsisLanguage: value?.synopsisLanguage ?? '', countries: value?.countries ?? [] };
}
export function proposalProjection(value: ProposalRevision, policyRevision: string) {
  return { proposalId: value.proposalId, revision: value.revision, contentHash: value.contentHash,
    expiresAt: new Date(Number(value.expiresAt?.seconds ?? 0) * 1000).toISOString(), normalizedMovie: movieProjection(value.values),
    policyRevision, status: 'REVIEW', reviewResourceUri: `spoolin://proposal/${value.proposalId}/${value.revision}` };
}
export function outcomeProjection(value: Outcome) {
  return { operationId: value.operationId, state: value.state, resultExpired: value.resultExpired,
    ...(value.resultId ? { resultId: value.resultId } : {}), ...(value.version ? { version: value.version } : {}),
    ...(value.problem ? { problem: { code: value.problem.code, message: 'The operation could not be completed.',
      fields: value.problem.fields, retryAfterMs: value.problem.retryAfterMs ?? null } } : {}) };
}
