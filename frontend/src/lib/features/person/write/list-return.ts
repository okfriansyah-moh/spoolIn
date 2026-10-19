import { isRecordId } from '../../../components/safe-url';

export type PersonListReturn = Readonly<{
  searchText: string;
  appliedSearch: string;
  rowCount: number;
  scrollY: number;
}>;

export function rememberPersonList(
  token: string,
  state: PersonListReturn,
): void {
  if (!isRecordId(token)) return;
  try {
    sessionStorage.setItem(
      `person-list-return:${token}`,
      JSON.stringify(state),
    );
  } catch {
    // Browsing remains available when session storage is disabled or full.
  }
}

export function readPersonListReturn(token: string): PersonListReturn | null {
  if (!isRecordId(token)) return null;
  try {
    const value: unknown = JSON.parse(
      sessionStorage.getItem(`person-list-return:${token}`) ?? 'null',
    );
    if (!value || typeof value !== 'object') return null;
    const state = Object.fromEntries(Object.entries(value));
    if (!validText(state) || !validPosition(state)) return null;
    return {
      searchText: state.searchText,
      appliedSearch: state.appliedSearch,
      rowCount: state.rowCount,
      scrollY: state.scrollY,
    };
  } catch {
    return null;
  }
}

function validText(
  state: Record<string, unknown>,
): state is Record<string, unknown> &
  Pick<PersonListReturn, 'searchText' | 'appliedSearch'> {
  return !(
    typeof state.searchText !== 'string' ||
    typeof state.appliedSearch !== 'string'
  );
}

function validPosition(
  state: Record<string, unknown>,
): state is Record<string, unknown> &
  Pick<PersonListReturn, 'rowCount' | 'scrollY'> {
  return !(
    typeof state.rowCount !== 'number' ||
    !Number.isSafeInteger(state.rowCount) ||
    state.rowCount < 0 ||
    typeof state.scrollY !== 'number' ||
    !Number.isFinite(state.scrollY) ||
    state.scrollY < 0
  );
}
