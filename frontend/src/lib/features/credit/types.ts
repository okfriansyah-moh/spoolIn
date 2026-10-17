export type CreditRoleOption = Readonly<{
  id: string;
  label: string;
  category: string;
  allowsCharacters: boolean;
}>;

export type ManagedCredit = Readonly<{
  id: string;
  personId: string;
  personName: string;
  role: CreditRoleOption;
  characters: readonly string[];
  version: string;
  personArchived?: boolean;
}>;

export type PersonOption = Readonly<{ id: string; name: string }>;
