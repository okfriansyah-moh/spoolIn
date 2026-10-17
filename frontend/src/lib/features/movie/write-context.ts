import { createSessionSource } from '../session/api';
import { identityOf } from '../session/types';
import type { WriteContext } from './types';

export async function loadWriteContext(
  signal: AbortSignal,
): Promise<WriteContext | null> {
  const source = createSessionSource();
  const bootstrap = await source.bootstrap(signal);
  const identity = identityOf(bootstrap.session);
  if (!identity || identity.role !== 'MAINTAINER') return null;
  return Object.freeze({
    accountId: identity.accountId,
    csrf: bootstrap.session.csrfToken,
    policyRevision: bootstrap.policy.revision,
  });
}
