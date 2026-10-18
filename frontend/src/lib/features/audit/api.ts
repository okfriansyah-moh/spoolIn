import type { GraphqlRequest } from '../../transport/graphql';
import { record } from '../session/parse';

const AUDIT_QUERY = `query Audit($first: Int = 24, $after: String, $subjectId: UUID) {
  audit(first: $first, after: $after, subjectId: $subjectId) {
    nodes {
      id actorRef action subjectKind subjectId outcome at changes
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
  at: string;
  changes: Readonly<Record<string, unknown>>;
}>;

export type AuditPage = Readonly<{
  nodes: readonly AuditRow[];
  endCursor: string | null;
  hasNextPage: boolean;
}>;

export async function fetchAuditPage(
  request: GraphqlRequest,
  csrf: string,
  signal: AbortSignal,
  after: string | null = null,
): Promise<AuditPage> {
  const data = record(await request(AUDIT_QUERY, { after }, { csrf, signal }));
  const audit = record(data.audit);
  const nodes = Array.isArray(audit.nodes) ? audit.nodes : [];
  const rows = Object.freeze(
    nodes.map((node) => {
      const row = record(node);
      return Object.freeze({
        id: String(row.id),
        actorRef: typeof row.actorRef === 'string' ? row.actorRef : null,
        action: String(row.action),
        subjectKind: String(row.subjectKind),
        subjectId: typeof row.subjectId === 'string' ? row.subjectId : null,
        outcome: String(row.outcome),
        at: String(row.at),
        changes: Object.freeze(record(row.changes)),
      });
    }),
  );
  const pageInfo = record(audit.pageInfo);
  const endCursor = typeof pageInfo.endCursor === 'string' ? pageInfo.endCursor : null;
  if (pageInfo.hasNextPage === true && !endCursor) {
    throw new Error('Audit pagination cursor is missing.');
  }
  return Object.freeze({ nodes: rows, endCursor, hasNextPage: pageInfo.hasNextPage === true });
}
