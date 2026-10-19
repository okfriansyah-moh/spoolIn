import { isRecordId } from '../../components/safe-url';

export type MovieListReturn = Readonly<{
  searchText: string;
  appliedSearch: string;
  rowCount: number;
  scrollY: number;
}>;

export function rememberMovieList(token: string, state: MovieListReturn): void {
  if (!isRecordId(token)) return;
  try {
    sessionStorage.setItem(`movie-list-return:${token}`, JSON.stringify(state));
  } catch {
    // Browsing remains available when session storage is disabled or full.
  }
}

export function readMovieListReturn(token: string): MovieListReturn | null {
  if (!isRecordId(token)) return null;
  try {
    const value: unknown = JSON.parse(
      sessionStorage.getItem(`movie-list-return:${token}`) ?? 'null',
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
  Pick<MovieListReturn, 'searchText' | 'appliedSearch'> {
  return !(
    typeof state.searchText !== 'string' ||
    typeof state.appliedSearch !== 'string'
  );
}

function validPosition(
  state: Record<string, unknown>,
): state is Record<string, unknown> &
  Pick<MovieListReturn, 'rowCount' | 'scrollY'> {
  return !(
    typeof state.rowCount !== 'number' ||
    !Number.isSafeInteger(state.rowCount) ||
    state.rowCount < 0 ||
    typeof state.scrollY !== 'number' ||
    !Number.isFinite(state.scrollY) ||
    state.scrollY < 0
  );
}
