import {
  readListReturn,
  rememberListReturn,
  type ListReturn,
} from '../../state/list-return';

export type MovieListReturn = ListReturn;

export function rememberMovieList(token: string, state: MovieListReturn): void {
  rememberListReturn('movie', token, state);
}

export function readMovieListReturn(token: string): MovieListReturn | null {
  return readListReturn('movie', token);
}
