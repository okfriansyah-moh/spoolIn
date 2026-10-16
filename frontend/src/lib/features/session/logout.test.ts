import { expect, it, vi } from 'vitest';
import { specimenPolicy } from '../../../../../build-tools/verification-tests/frontend/src/routes/policy/fixture';
import { SessionController } from './controller';
import { TransportFailure } from '../../transport/graphql';
import type { SessionBootstrap, SessionSource } from './types';
import type { AccountCommands, IdentityOutcome } from './commands';
const accountId = '10000000-0000-4000-8000-000000000001';
function bootstrap(authenticated = true): SessionBootstrap {
  return {
    policy: specimenPolicy(),
    session: authenticated
      ? {
          authenticated,
          role: 'VIEWER',
          accountId,
          displayName: 'A Person',
          expiresAt: '2099-01-01T00:00:00Z',
          csrfToken: 'B'.repeat(43),
        }
      : {
          authenticated,
          role: null,
          accountId: null,
          displayName: null,
          expiresAt: null,
          csrfToken: 'A'.repeat(43),
        },
  };
}
function result(
  operationId: string,
  state: IdentityOutcome['state'] = 'SUCCEEDED',
): IdentityOutcome {
  return {
    operationId,
    state,
    resultExpired: false,
    problem: state === 'FAILED' ? { code: 'FAILED', retryAfterMs: 0 } : null,
  };
}
it('confirmed logout conceals the account and reports signed out', async () => {
  const signOut = vi.fn<AccountCommands['signOut']>(async (id) => result(id));
  const controller = new SessionController({
    bootstrap: async () => bootstrap(),
    signOut,
  });
  await controller.check();
  await controller.signOut();
  expect(controller.snapshot).toMatchObject({
    kind: 'anonymous',
    identity: null,
    message: 'Signed out.',
  });
  expect(signOut).toHaveBeenCalledTimes(1);
});
it('a deleted cookie and anonymous read cannot prove revocation after a lost response body', async () => {
  const source = vi
    .fn<SessionSource['bootstrap']>()
    .mockResolvedValueOnce(bootstrap())
    .mockResolvedValue(bootstrap(false));
  const signOut = vi
    .fn<AccountCommands['signOut']>()
    .mockRejectedValue(new TransportFailure('uncertain'));
  const operation = vi
    .fn<AccountCommands['operation']>()
    .mockResolvedValue(null);
  const controller = new SessionController({
    bootstrap: source,
    signOut,
    operation,
  });
  await controller.check();
  await controller.signOut();
  await controller.reconcileSignOut();
  expect(controller.snapshot).toMatchObject({
    kind: 'uncertain',
    identity: null,
  });
  expect(controller.snapshot.message).not.toBe('Signed out.');
  expect(signOut).toHaveBeenCalledTimes(1);
  expect(operation.mock.calls[0]?.[0]).toBe(signOut.mock.calls[0]?.[0]);
  expect(operation.mock.calls[0]?.[1]).toBe('B'.repeat(43));
});
it('an authorized original receipt plus current session read resolves a lost reply without another signout UUID', async () => {
  const source = vi
    .fn<SessionSource['bootstrap']>()
    .mockResolvedValueOnce(bootstrap())
    .mockResolvedValue(bootstrap(false));
  const signOut = vi
    .fn<AccountCommands['signOut']>()
    .mockRejectedValue(new TransportFailure('uncertain'));
  const operation = vi.fn<AccountCommands['operation']>(async (id) =>
    result(id),
  );
  const controller = new SessionController({
    bootstrap: source,
    signOut,
    operation,
  });
  await controller.check();
  await controller.signOut();
  await controller.reconcileSignOut();
  expect(controller.snapshot).toMatchObject({
    kind: 'anonymous',
    identity: null,
    message: 'Signed out.',
  });
  expect(signOut).toHaveBeenCalledTimes(1);
  expect(operation.mock.calls[0]?.[0]).toBe(signOut.mock.calls[0]?.[0]);
});
it('a confirmed failure before revocation retains the known active identity', async () => {
  const signOut = vi.fn<AccountCommands['signOut']>(async (id) =>
    result(id, 'FAILED'),
  );
  const controller = new SessionController({
    bootstrap: async () => bootstrap(),
    signOut,
  });
  await controller.check();
  await controller.signOut();
  expect(controller.snapshot).toMatchObject({
    kind: 'verified',
    identity: { accountId },
    message: 'Sign-out did not complete.',
  });
});
