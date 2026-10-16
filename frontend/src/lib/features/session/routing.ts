import type { ActorRole } from '../../generated/graphql';
const id = '[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}';
const publicRoute = new RegExp(`^(?:/|/people(?:/${id})?|/titles/${id})$`);
const managementRoute = new RegExp(
  `^/manage(?:/(?:movies|people)(?:/(?:new|${id}))?|/(?:archives|audit))?$`,
);
function permitted(path: string, role: ActorRole): boolean {
  return (
    publicRoute.test(path) ||
    (role === 'MAINTAINER' && managementRoute.test(path))
  );
}
export function safeDestination(value: string | null, role: ActorRole): string {
  if (
    !value ||
    !value.startsWith('/') ||
    value.startsWith('//') ||
    value.includes('\\') ||
    /\p{Cc}/u.test(value)
  )
    return '/';
  try {
    const target = new URL(value, 'https://spoolin.invalid');
    if (
      target.origin !== 'https://spoolin.invalid' ||
      !permitted(target.pathname, role)
    )
      return '/';
    return target.pathname + target.search + target.hash;
  } catch {
    return '/';
  }
}
