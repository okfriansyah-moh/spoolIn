export type Capabilities = { verified: boolean; manageCatalog: boolean };
export type NavItem = { href: string; label: string };
export function navigation(capabilities: Capabilities): NavItem[] {
  const items = [
    { href: '/', label: 'Catalog' },
    { href: '/people', label: 'People' },
  ];
  if (capabilities.verified && capabilities.manageCatalog)
    items.push(
      { href: '/manage/movies', label: 'Movies' },
      { href: '/manage/people', label: 'Manage people' },
      { href: '/manage/archives', label: 'Archives' },
      { href: '/manage/audit', label: 'History' },
    );
  return items;
}

export function focusContentHeading(): void {
  const heading = document.querySelector<HTMLElement>('main h1');
  heading?.setAttribute('tabindex', '-1');
  heading?.focus();
}
