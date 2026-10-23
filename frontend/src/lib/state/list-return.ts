import { isRecordId } from '../components/safe-url';

type ListKind = 'movie' | 'person';

export type ListReturn = Readonly<{
  searchText: string;
  appliedSearch: string;
  rowCount: number;
  scrollY: number;
}>;

export function rememberListReturn(
  kind: ListKind,
  token: string,
  state: ListReturn,
): void {
  if (!isRecordId(token)) return;
  try {
    sessionStorage.setItem(
      `${kind}-list-return:${token}`,
      JSON.stringify(state),
    );
  } catch {
    // Browsing remains available when session storage is disabled or full.
  }
}

export function readListReturn(
  kind: ListKind,
  token: string,
): ListReturn | null {
  if (!isRecordId(token)) return null;
  try {
    const value: unknown = JSON.parse(
      sessionStorage.getItem(`${kind}-list-return:${token}`) ?? 'null',
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
  Pick<ListReturn, 'searchText' | 'appliedSearch'> {
  return !(
    typeof state.searchText !== 'string' ||
    typeof state.appliedSearch !== 'string'
  );
}

function validPosition(
  state: Record<string, unknown>,
): state is Record<string, unknown> & Pick<ListReturn, 'rowCount' | 'scrollY'> {
  return !(
    typeof state.rowCount !== 'number' ||
    !Number.isSafeInteger(state.rowCount) ||
    state.rowCount < 0 ||
    typeof state.scrollY !== 'number' ||
    !Number.isFinite(state.scrollY) ||
    state.scrollY < 0
  );
}
