import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { readMovieListReturn, rememberMovieList } from './list-return';
import {
  readPersonListReturn,
  rememberPersonList,
} from '../person/write/list-return';

const token = '10000000-0000-4000-8000-000000000001';
const state = {
  searchText: ' Last Light ',
  appliedSearch: 'Last Light',
  rowCount: 3,
  scrollY: 120,
};
let storage: Map<string, string>;
beforeEach(() => {
  storage = new Map();
  vi.stubGlobal('sessionStorage', {
    getItem: (key: string) => storage.get(key) ?? null,
    setItem: (key: string, value: string) => storage.set(key, value),
  });
});
afterEach(() => vi.unstubAllGlobals());

describe.each([
  { kind: 'movie', read: readMovieListReturn, remember: rememberMovieList },
  { kind: 'person', read: readPersonListReturn, remember: rememberPersonList },
])('$kind list return', ({ kind, read, remember }) => {
  it('round-trips the exact search and position, including zero', () => {
    remember(token, state);
    expect(read(token)).toEqual(state);
    remember(token, { ...state, rowCount: 0, scrollY: 0 });
    expect(read(token)).toEqual({ ...state, rowCount: 0, scrollY: 0 });
  });
  it.each([
    null,
    [],
    { ...state, searchText: null },
    { ...state, appliedSearch: 1 },
    { ...state, rowCount: -1 },
    { ...state, rowCount: 1.5 },
    { ...state, rowCount: '3' },
    { ...state, scrollY: -1 },
    { ...state, scrollY: null },
    { ...state, scrollY: '120' },
  ])('ignores malformed stored state %#', (value) => {
    storage.set(`${kind}-list-return:${token}`, JSON.stringify(value));
    expect(read(token)).toBeNull();
  });
  it('ignores invalid tokens, missing entries, corrupt JSON and unavailable storage', () => {
    remember('invalid', state);
    expect(storage.size).toBe(0);
    expect(read('invalid')).toBeNull();
    expect(read(token)).toBeNull();
    storage.set(`${kind}-list-return:${token}`, '{');
    expect(read(token)).toBeNull();
    vi.stubGlobal('sessionStorage', {
      getItem: () => {
        throw new Error('Storage unavailable');
      },
      setItem: () => {
        throw new Error('Storage unavailable');
      },
    });
    expect(() => remember(token, state)).not.toThrow();
    expect(read(token)).toBeNull();
  });
});
