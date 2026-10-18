import { readFileSync } from 'node:fs';
import { buildASTSchema, graphql, Kind, parse, visit } from 'graphql';
import { describe, expect, it } from 'vitest';
import { TransportFailure, type GraphqlRequest } from '../transport/graphql';
import {
  fetchTitles,
  fetchTitleCredits as fetchPublicTitleCredits,
} from '../features/catalog/api';
import { fetchPeople, fetchPersonCredits } from '../features/person/read/api';
import { fetchManagedPeoplePage } from '../features/person/write/api';
import { fetchManagedTitlesPage } from '../features/movie/api';
import {
  fetchTitleCreditsPage,
  fetchActivePeoplePage,
} from '../features/credit/api';
import { fetchArchivedPage } from '../features/archive/api';
import { fetchAuditPage } from '../features/audit/api';

const id = '30000000-0000-4000-8000-000000000001';
type Journey = Readonly<{
  name: string;
  field: string;
  read: (request: GraphqlRequest, after: string | null) => Promise<unknown>;
}>;
const journeys: readonly Journey[] = [
  {
    name: 'public titles',
    field: 'titles',
    read: (r, after) => fetchTitles(r, { filter: {}, after }),
  },
  {
    name: 'public title credits',
    field: 'titleCredits',
    read: (r, after) => fetchPublicTitleCredits(r, id, after),
  },
  {
    name: 'public people',
    field: 'people',
    read: (r, after) => fetchPeople(r, { after }),
  },
  {
    name: 'public person credits',
    field: 'personCredits',
    read: (r, after) => fetchPersonCredits(r, { id, after }),
  },
  {
    name: 'managed titles',
    field: 'managedTitles',
    read: (r, after) => fetchManagedTitlesPage(r, 'csrf', after),
  },
  {
    name: 'managed people',
    field: 'managedPeople',
    read: (r, after) => fetchManagedPeoplePage(r, 'csrf', after),
  },
  {
    name: 'managed credits',
    field: 'managedCredits',
    read: (r, after) => fetchTitleCreditsPage(r, id, 'csrf', after),
  },
  {
    name: 'credit person picker',
    field: 'managedPeople',
    read: (r, after) => fetchActivePeoplePage(r, 'csrf', after),
  },
  {
    name: 'archived titles',
    field: 'managedTitles',
    read: (r, after) => fetchArchivedPage(r, 'movies', 'csrf', after),
  },
  {
    name: 'archived people',
    field: 'managedPeople',
    read: (r, after) => fetchArchivedPage(r, 'people', 'csrf', after),
  },
  {
    name: 'audit',
    field: 'audit',
    read: (r, after) =>
      fetchAuditPage(r, 'csrf', new AbortController().signal, after),
  },
];
const source = readFileSync(
  new URL('../../../../contracts/graphql/schema.graphql', import.meta.url),
  'utf8',
);

// Controlled GraphQL coercion boundary only; actual Core/Catalog/DB proof remains remote integration.
function policyServer(journey: Journey, defaultPage: number) {
  const schema = buildASTSchema(
    visit(parse(source), {
      InputValueDefinition(node) {
        return node.name.value === 'first'
          ? {
              ...node,
              defaultValue: { kind: Kind.INT, value: String(defaultPage) },
            }
          : undefined;
      },
    }),
  );
  let revision = 'policy-one';
  const firstValues: unknown[] = [];
  const request: GraphqlRequest = async (document, variables) => {
    expect(variables).not.toHaveProperty('first');
    const rootValue = {
      [journey.field]: ({
        first,
        after,
      }: {
        first: number;
        after?: string | null;
      }) => {
        firstValues.push(first);
        if (after && after !== revision)
          throw new Error('Cursor policy changed. Reload this list.');
        return {
          nodes: [],
          pageInfo: {
            endCursor: after ? `${revision}-next` : revision,
            hasNextPage: true,
          },
        };
      },
    };
    const result = await graphql({
      schema,
      source: document,
      variableValues: variables,
      rootValue,
    });
    if (result.errors?.length)
      throw new TransportFailure('rejected', 'VALIDATION_FAILED');
    return result.data;
  };
  return {
    request,
    firstValues,
    changePolicy() {
      revision = 'policy-two';
    },
  };
}

describe('production pagination requests use the effective server default', () => {
  for (const journey of journeys) {
    it.each([1, 3])(
      `${journey.name}: omitted first honors configured default %s across its cursor`,
      async (size) => {
        const server = policyServer(journey, size);
        await journey.read(server.request, null);
        await journey.read(server.request, 'policy-one');
        expect(server.firstValues).toEqual([size, size]);
      },
    );
    it(`${journey.name}: a stale-policy cursor is rejected and a fresh list can reload`, async () => {
      const server = policyServer(journey, 1);
      await journey.read(server.request, null);
      server.changePolicy();
      await expect(
        journey.read(server.request, 'policy-one'),
      ).rejects.toMatchObject({ code: 'VALIDATION_FAILED' });
      await expect(journey.read(server.request, null)).resolves.toBeDefined();
    });
  }
});
