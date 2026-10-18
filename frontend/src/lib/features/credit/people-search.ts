import { matchingPage } from '../../state/search-page';
import type { GraphqlRequest } from '../../transport/graphql';
import { fetchActivePeoplePage } from './api';

export async function fetchPeopleMatches(
  request: GraphqlRequest,
  csrf: string,
  after: string | null,
  query: string,
  signal: AbortSignal,
) {
  const page = await matchingPage(
    async (cursor) => {
      const result = await fetchActivePeoplePage(request, csrf, cursor, signal);
      return {
        nodes: result.nodes,
        pageInfo: { hasNextPage: !!result.next, endCursor: result.next },
      };
    },
    after,
    query,
    (person) => person.name,
    () => !signal.aborted,
  );
  return {
    nodes: page.nodes,
    next: page.pageInfo.hasNextPage ? page.pageInfo.endCursor : null,
  };
}
