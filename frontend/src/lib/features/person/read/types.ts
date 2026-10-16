/**
 * T013 – Public person read state types.
 * Covers AC-022 (people list) and the detail view.
 */
export type PersonSummary = Readonly<{
  id: string;
  name: string;
  biography: string | null;
}>;

export type PersonDetail = Readonly<{
  id: string;
  name: string;
  biography: string | null;
  state: 'ACTIVE' | 'ARCHIVED';
}>;

export type CreditItem = Readonly<{
  id: string;
  titleId: string;
  titleName: string;
  roleLabel: string;
  roleCategory: 'CAST' | 'CREATOR';
  characters: readonly string[];
}>;

export type PeopleKind = 'loading' | 'people' | 'empty' | 'no-match' | 'failure';

export type PeopleState = Readonly<{
  kind: PeopleKind;
  text: string;
  people: readonly PersonSummary[];
  pageInfo: Readonly<{ endCursor: string | null; hasNextPage: boolean }> | null;
}>;

export type PersonReadKind = 'loading' | 'person' | 'unavailable' | 'failure';

export type PersonReadState = Readonly<{
  kind: PersonReadKind;
  person: PersonDetail | null;
  credits: readonly CreditItem[];
}>;
