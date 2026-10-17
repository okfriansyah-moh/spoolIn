/**
 * T013 – Public people GraphQL transport.
 * Wraps the People, Person and PersonCredits queries.
 */
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

const PEOPLE_QUERY = `query People($text: String, $first: Int = 24, $after: Cursor) {
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

const PERSON_CREDITS_QUERY = `query PersonCredits($id: UUID!, $first: Int = 24, $after: Cursor) {
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
  return (data as { people: PeopleQuery['people'] }).people;
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
  return (data as { personCredits: PersonCreditsQuery['personCredits'] })
    .personCredits;
}
