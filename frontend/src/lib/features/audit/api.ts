import type { GraphqlRequest } from '../../transport/graphql';
import { record } from '../session/parse';

const AUDIT_QUERY = `query Audit($first: Int = 24, $after: String, $subjectId: UUID) {
  audit(first: $first, after: $after, subjectId: $subjectId) {
    nodes {
      id actorRef action subjectKind subjectId outcome createdAt changes
    }
    pageInfo { endCursor hasNextPage }
  }
}`;

export type AuditRow = Readonly<{
  id: string;
  actorRef: string | null;
  action: string;
  subjectKind: string;
  subjectId: string | null;
  outcome: string;
  createdAt: string;
  changes: string;
}>;

export async function fetchAuditPage(
  request: GraphqlRequest,
  csrf: string,
  signal: AbortSignal,
): Promise<readonly AuditRow[]> {
  const data = record(await request(AUDIT_QUERY, {}, { csrf, signal }));
  const audit = record(data.audit);
  const nodes = Array.isArray(audit.nodes) ? audit.nodes : [];
  return Object.freeze(
    nodes.map((node) => {
      const row = record(node);
      return Object.freeze({
        id: String(row.id),
        actorRef: typeof row.actorRef === 'string' ? row.actorRef : null,
        action: String(row.action),
        subjectKind: String(row.subjectKind),
        subjectId: typeof row.subjectId === 'string' ? row.subjectId : null,
        outcome: String(row.outcome),
        createdAt: String(row.createdAt),
        changes: String(row.changes ?? ''),
      });
    }),
  );
}
