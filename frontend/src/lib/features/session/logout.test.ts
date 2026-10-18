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
it.each([true, false])(
  'confirmed HTTP rejection rechecks actual authority (%s)',
  async (authenticated) => {
    const source = vi
      .fn<SessionSource['bootstrap']>()
      .mockResolvedValueOnce(bootstrap())
      .mockResolvedValue(bootstrap(authenticated));
    const controller = new SessionController({
      bootstrap: source,
      signOut: async () => {
        throw new TransportFailure('rejected');
      },
    });
    await controller.check();
    await controller.signOut();
    expect(controller.snapshot.kind).toBe(
      authenticated ? 'verified' : 'expired',
    );
    expect(controller.snapshot.message).not.toBe('Signed out.');
    controller.dispose();
  },
);
it('an unavailable rejection recheck keeps logout uncertain', async () => {
  const source = vi
    .fn<SessionSource['bootstrap']>()
    .mockResolvedValueOnce(bootstrap())
    .mockRejectedValue(new Error('offline'));
  const controller = new SessionController({
    bootstrap: source,
    signOut: async () => {
      throw new TransportFailure('rejected');
    },
  });
  await controller.check();
  await controller.signOut();
  expect(controller.snapshot.kind).toBe('uncertain');
  controller.dispose();
});
it('a failed original logout receipt restores only the same current account', async () => {
  const controller = new SessionController({
    bootstrap: async () => bootstrap(),
    signOut: async (id) => result(id, 'PENDING'),
    operation: async (id) => result(id, 'FAILED'),
  });
  await controller.check();
  await controller.signOut();
  await controller.check();
  expect(controller.snapshot).toMatchObject({
    kind: 'verified',
    identity: { accountId },
    message: 'Sign-out did not complete.',
  });
  controller.dispose();
});
it.each([true, false])(
  'fresh sign-in preparation keeps old logout unconfirmed even when authenticated=%s',
  async (authenticated) => {
    const source = vi
      .fn<SessionSource['bootstrap']>()
      .mockResolvedValueOnce(bootstrap())
      .mockResolvedValue(bootstrap(authenticated));
    const controller = new SessionController({
      bootstrap: source,
      signOut: async (id) => result(id, 'PENDING'),
    });
    await controller.check();
    await controller.signOut();
    await controller.prepareSignIn();
    expect(controller.snapshot.identity).toBeNull();
    expect(controller.snapshot.message).not.toBe('Signed out.');
    expect(controller.hasUnconfirmedLogout).toBe(true);
    controller.dispose();
  },
);
it('unavailable sign-in preparation and receipt lookup preserve concealed uncertain logout', async () => {
  const source = vi
    .fn<SessionSource['bootstrap']>()
    .mockResolvedValueOnce(bootstrap())
    .mockRejectedValue(new Error('offline'));
  const controller = new SessionController({
    bootstrap: source,
    signOut: async (id) => result(id, 'PENDING'),
    operation: async () => {
      throw new Error('offline');
    },
  });
  await controller.check();
  await controller.signOut();
  await controller.prepareSignIn();
  await controller.reconcileSignOut();
  expect(controller.snapshot).toMatchObject({
    kind: 'uncertain',
    identity: null,
  });
  controller.dispose();
});

it('an expired successful receipt cannot prove logout even when the current cookie is anonymous', async () => {
  const source = vi.fn<SessionSource['bootstrap']>().mockResolvedValueOnce(bootstrap()).mockResolvedValue(bootstrap(false));
  const signOut = vi.fn<AccountCommands['signOut']>(async (id) => ({ ...result(id), resultExpired: true }));
  const operation = vi.fn<AccountCommands['operation']>(async (id) => ({ ...result(id), resultExpired: true }));
  const controller = new SessionController({ bootstrap: source, signOut, operation });
  await controller.check();
  await controller.signOut();
  expect(controller.snapshot.kind).toBe('uncertain');
  expect(controller.hasUnconfirmedLogout).toBe(true);
  await controller.reconcileSignOut();
  expect(controller.snapshot.kind).toBe('uncertain');
  expect(controller.snapshot.message).not.toBe('Signed out.');
});
