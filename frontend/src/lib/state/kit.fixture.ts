export const page = { url: new URL('http://localhost/') };
export function afterNavigate(callback: () => void): void {
  void callback;
}
