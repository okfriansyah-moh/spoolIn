export type Theme = 'dark' | 'light';
export const THEME_KEY = 'spoolin.theme';
export type ThemeStorage = Pick<Storage, 'getItem' | 'setItem' | 'removeItem'>;
export function isTheme(value: unknown): value is Theme {
  return value === 'dark' || value === 'light';
}
export function readThemePreference(
  storage: ThemeStorage | undefined,
): Theme | undefined {
  try {
    const value = storage?.getItem(THEME_KEY);
    if (isTheme(value)) return value;
    if (value !== null && value !== undefined) storage?.removeItem(THEME_KEY);
  } catch {
    /* Appearance remains usable when storage is unavailable. */
  }
  return undefined;
}
export function readTheme(
  storage: ThemeStorage | undefined,
  system: Theme,
): Theme {
  return readThemePreference(storage) ?? system;
}
export function selectTheme(
  current: Theme,
  value: unknown,
  storage?: ThemeStorage,
): Theme {
  if (!isTheme(value)) return current;
  try {
    storage?.setItem(THEME_KEY, value);
  } catch {
    /* Keep explicit selection in memory. */
  }
  return value;
}
export function browserStorage(): ThemeStorage | undefined {
  try {
    return window.localStorage;
  } catch {
    return undefined;
  }
}
