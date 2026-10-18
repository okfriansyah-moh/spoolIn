/**
 * T013 – Public people GraphQL transport.
 * Wraps the People, Person and PersonCredits queries.
 */
import { readPageInfo } from '../../../state/pagination';
import type { GraphqlRequest } from '../../../transport/graphql';
import { withSignal } from '../../../transport/request-options';
import type {
  PeopleQuery,
  PeopleQueryVariables,
  PersonQuery,
  PersonQueryVariables,
  PersonCreditsQuery,
  PersonCreditsQueryVariables,
} from '../../../generated/graphql';

const PEOPLE_QUERY = `query People($text: String, $first: Int, $after: Cursor) {
  people(text: $text, first: $first, after: $after) {
    nodes { id name biography state version }
    pageInfo { endCursor hasNextPage }
  }
}`;

const PERSON_QUERY = `query Person($id: UUID!) {
  person(id: $id) {
    id name biography state version
  }
}`;

const PERSON_CREDITS_QUERY = `query PersonCredits($id: UUID!, $first: Int, $after: Cursor) {
  personCredits(id: $id, first: $first, after: $after) {
    nodes {
      id personId personName titleId titleName
      role { id label category allowsCharacters }
      characters version
    }
    pageInfo { endCursor hasNextPage }
  }
}`;

export type PublicPerson = PeopleQuery['people']['nodes'][number];
export type PublicPersonDetail = NonNullable<PersonQuery['person']>;
export type PublicPersonCredit =
  PersonCreditsQuery['personCredits']['nodes'][number];

function record(value: unknown): Record<string, unknown> {
  if (value === null || typeof value !== 'object' || Array.isArray(value))
    throw new Error('Unexpected response shape');
  return Object.fromEntries(Object.entries(value));
}

export async function fetchPeople(
  request: GraphqlRequest,
  variables: PeopleQueryVariables,
  signal?: AbortSignal,
): Promise<PeopleQuery['people']> {
  const data = record(
    await request(
      PEOPLE_QUERY,
      variables as Record<string, unknown>,
      withSignal(signal),
    ),
  );
  const result = (data as { people: PeopleQuery['people'] }).people;
  if (!Array.isArray(result?.nodes)) throw new Error('People unavailable.');
  return {
    ...result,
    pageInfo: readPageInfo(result.pageInfo, variables.after ?? null),
  };
}

export async function fetchPerson(
  request: GraphqlRequest,
  variables: PersonQueryVariables,
  signal?: AbortSignal,
): Promise<PublicPersonDetail | null> {
  const data = record(
    await request(
      PERSON_QUERY,
      variables as Record<string, unknown>,
      withSignal(signal),
    ),
  );
  return (data as { person: PublicPersonDetail | null }).person;
}

export async function fetchPersonCredits(
  request: GraphqlRequest,
  variables: PersonCreditsQueryVariables,
  signal?: AbortSignal,
): Promise<PersonCreditsQuery['personCredits']> {
  const data = record(
    await request(
      PERSON_CREDITS_QUERY,
      variables as Record<string, unknown>,
      withSignal(signal),
    ),
  );
  const result = (
    data as { personCredits: PersonCreditsQuery['personCredits'] }
  ).personCredits;
  if (!Array.isArray(result?.nodes)) throw new Error('Credits unavailable.');
  return {
    ...result,
    pageInfo: readPageInfo(result.pageInfo, variables.after ?? null),
  };
}
