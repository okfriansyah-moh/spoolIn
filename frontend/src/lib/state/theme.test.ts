import { it, expect, vi } from 'vitest';
import { readTheme, selectTheme, browserStorage } from './theme';
const THEME_KEY = 'spoolin.theme';
const invalid = [
  'system',
  '',
  ' DARK ',
  'DARK',
  '0',
  'false',
  '{"theme":"dark"}',
  '__proto__',
  '<script>alert(1)</script>',
  'dark; color:red',
];
function fixture(value: string | null) {
  const data = new Map<string, string>();
  if (value !== null) data.set(THEME_KEY, value);
  return {
    data,
    getItem: vi.fn((key: string) => data.get(key) ?? null),
    setItem: vi.fn((key: string, next: string) => {
      data.set(key, next);
    }),
    removeItem: vi.fn((key: string) => {
      data.delete(key);
    }),
  };
}
for (const [stored, system, expected] of [
  ['dark', 'light', 'dark'],
  ['light', 'dark', 'light'],
] as const)
  it(`R01/R02 stored ${stored}`, () => {
    const storage = fixture(stored);
    expect(readTheme(storage, system)).toBe(expected);
    expect(storage.getItem.mock.calls).toEqual([[THEME_KEY]]);
    expect(storage.setItem).not.toHaveBeenCalled();
    expect(storage.removeItem).not.toHaveBeenCalled();
  });
for (const system of ['dark', 'light'] as const)
  it(`R03 absent follows ${system}`, () => {
    const storage = fixture(null);
    expect(readTheme(storage, system)).toBe(system);
    expect(storage.data.size).toBe(0);
    expect(storage.removeItem).not.toHaveBeenCalled();
    expect(storage.setItem).not.toHaveBeenCalled();
  });
for (const value of invalid)
  for (const system of ['dark', 'light'] as const)
    it(`R04 invalid ${JSON.stringify(value)} follows ${system}`, () => {
      const storage = fixture(value);
      expect(readTheme(storage, system)).toBe(system);
      expect(storage.getItem.mock.calls).toEqual([[THEME_KEY]]);
      expect(storage.removeItem.mock.calls).toEqual([[THEME_KEY]]);
      expect(storage.setItem).not.toHaveBeenCalled();
      expect(storage.data.size).toBe(0);
    });
it('R05 unreadable storage remains light', () => {
  const storage = fixture('dark');
  storage.getItem.mockImplementation(() => {
    throw Error('denied');
  });
  expect(readTheme(storage, 'light')).toBe('light');
  expect(storage.setItem).not.toHaveBeenCalled();
  expect(storage.removeItem).not.toHaveBeenCalled();
});
it('R06 removal failure does not select invalid value', () => {
  const storage = fixture('DARK');
  storage.removeItem.mockImplementation(() => {
    throw Error('denied');
  });
  expect(readTheme(storage, 'light')).toBe('light');
  expect(storage.setItem).not.toHaveBeenCalled();
});
for (const theme of ['dark', 'light'] as const)
  it(`R07 valid selection ${theme}`, () => {
    const storage = fixture(null);
    expect(selectTheme('light', theme, storage)).toBe(theme);
    expect(storage.setItem.mock.calls).toEqual([[THEME_KEY, theme]]);
    expect(storage.getItem).not.toHaveBeenCalled();
    expect(storage.removeItem).not.toHaveBeenCalled();
  });
it('R08 failed persistence keeps valid memory selection', () => {
  const storage = fixture('dark');
  storage.setItem.mockImplementation(() => {
    throw Error('denied');
  });
  expect(selectTheme('dark', 'light', storage)).toBe('light');
  expect(storage.data.get(THEME_KEY)).toBe('dark');
  expect(storage.setItem.mock.calls).toEqual([[THEME_KEY, 'light']]);
});
for (const value of [...invalid, null, undefined, false, 0, {}, ['dark']])
  it(`R09 rejected selection ${JSON.stringify(value)}`, () => {
    const storage = fixture('dark');
    expect(selectTheme('light', value, storage)).toBe('light');
    expect(storage.setItem).not.toHaveBeenCalled();
    expect(storage.data.get(THEME_KEY)).toBe('dark');
  });
it('R10 prototype key is not an alias', () => {
  const storage = fixture(null);
  storage.data.set('movie-design-theme', 'dark');
  expect(readTheme(storage, 'light')).toBe('light');
  expect(storage.getItem.mock.calls).toEqual([[THEME_KEY]]);
});
it('works without a storage object', () => {
  expect(readTheme(undefined, 'dark')).toBe('dark');
  expect(selectTheme('dark', 'light')).toBe('light');
  expect(browserStorage()).toBeUndefined();
});
it('catches a denied localStorage getter and returns accessible storage', () => {
  const storage = fixture('dark');
  vi.stubGlobal('window', { localStorage: storage });
  expect(browserStorage()).toBe(storage);
  vi.stubGlobal(
    'window',
    Object.defineProperty({}, 'localStorage', {
      get() {
        throw Error('denied');
      },
    }),
  );
  expect(browserStorage()).toBeUndefined();
  vi.unstubAllGlobals();
});
