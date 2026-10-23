import {
  readListReturn,
  rememberListReturn,
  type ListReturn,
} from '../../../state/list-return';

export type PersonListReturn = ListReturn;

export function rememberPersonList(
  token: string,
  state: PersonListReturn,
): void {
  rememberListReturn('person', token, state);
}

export function readPersonListReturn(token: string): PersonListReturn | null {
  return readListReturn('person', token);
}
