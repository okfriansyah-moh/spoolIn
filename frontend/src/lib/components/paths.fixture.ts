export const resolve = (path: string, params: Record<string, string> = {}) =>
  path.replace(/\[([^\]]+)\]/g, (segment, key: string) =>
    params[key] === undefined ? segment : encodeURIComponent(params[key]),
  );
export const base = '';
