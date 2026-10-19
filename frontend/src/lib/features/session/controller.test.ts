import { expect, it, vi } from 'vitest';
import { specimenPolicy } from '../../../../../build-tools/verification-tests/frontend/src/routes/policy/fixture';
import { SessionController } from './controller.svelte';
import type { SessionBootstrap, SessionSource } from './types';
const firstId = '10000000-0000-4000-8000-000000000001';
const secondId = '20000000-0000-4000-8000-000000000002';
function response(
  accountId = firstId,
  expiresAt = '2099-01-01T00:00:00Z',
): SessionBootstrap {
  return {
    policy: specimenPolicy(),
    session: {
      authenticated: true,
      role: 'MAINTAINER',
      accountId,
      displayName: 'A Person',
      expiresAt,
      csrfToken: 'A'.repeat(43),
    },
  };
}
function pending<T>() {
  let resolve: (value: T) => void = () => {
    throw new Error('Uninitialized barrier');
  };
  const promise = new Promise<T>((accept) => {
    resolve = accept;
  });
  return { promise, resolve };
}
it('conceals authority until the actual bootstrap finishes and then uses its assigned role', async () => {
  const held = pending<SessionBootstrap>();
  const controller = new SessionController({ bootstrap: () => held.promise });
  const checking = controller.check();
  expect(controller.snapshot).toMatchObject({
    kind: 'checking',
    identity: null,
  });
  held.resolve(response());
  await checking;
  expect(controller.snapshot).toMatchObject({
    kind: 'verified',
    identity: { accountId: firstId, role: 'MAINTAINER' },
  });
  expect(controller.snapshot).not.toHaveProperty('csrfToken');
});
it('an older account-bound reply cannot overwrite a newer verified generation', async () => {
  const held = pending<SessionBootstrap>();
  const bootstrap = vi
    .fn<SessionSource['bootstrap']>()
    .mockReturnValueOnce(held.promise)
    .mockResolvedValueOnce(response(secondId));
  const controller = new SessionController({ bootstrap });
  const old = controller.check();
  await controller.check();
  held.resolve(response(firstId));
  await old;
  expect(controller.snapshot.identity?.accountId).toBe(secondId);
  expect(bootstrap.mock.calls[0]?.[0].aborted).toBe(true);
});
it('identity outage conceals the known account and stays distinct from confirmed anonymous state', async () => {
  const bootstrap = vi
    .fn<SessionSource['bootstrap']>()
    .mockResolvedValueOnce(response())
    .mockRejectedValueOnce(new Error('network'));
  const controller = new SessionController({ bootstrap });
  await controller.check();
  await expect(controller.check()).resolves.toBeUndefined();
  expect(controller.snapshot).toMatchObject({
    kind: 'unavailable',
    identity: null,
  });
});
it('conceals an expired session and cancels an older account expiry', async () => {
  vi.useFakeTimers();
  try {
    const initial = Date.now();
    const first = response(firstId, new Date(initial + 1000).toISOString());
    const second = response(secondId, new Date(initial + 3000).toISOString());
    const bootstrap = vi
      .fn<SessionSource['bootstrap']>()
      .mockResolvedValueOnce(first)
      .mockResolvedValueOnce(second);
    const controller = new SessionController({ bootstrap });
    await controller.check();
    await vi.advanceTimersByTimeAsync(500);
    await controller.check();
    await vi.advanceTimersByTimeAsync(500);
    expect(controller.snapshot.identity?.accountId).toBe(secondId);
    await vi.advanceTimersByTimeAsync(2000);
    expect(controller.snapshot).toMatchObject({
      kind: 'expired',
      identity: null,
    });
    controller.dispose();
  } finally {
    vi.useRealTimers();
  }
});

it('an expired successful registration receipt stays uncertain without announcing a new account', async () => {
  const current = response();
  const anonymous: SessionBootstrap = {
    ...current,
    session: {
      ...current.session,
      authenticated: false,
      role: null,
      accountId: null,
      displayName: null,
      expiresAt: null,
    },
  };
  const controller = new SessionController({
    bootstrap: async () => anonymous,
    submit: async (_input, operationId) => ({
      operationId,
      state: 'SUCCEEDED',
      resultExpired: true,
      problem: null,
    }),
  });
  await controller.check();
  const outcome = await controller.submit({
    kind: 'register',
    name: 'A Person',
    email: 'a@example.invalid',
    password: 'A sufficiently long password',
  });
  expect(outcome.kind).toBe('uncertain');
  expect(controller.hasPendingIdentity).toBe(true);
  expect(controller.snapshot.identity).toBeNull();
  controller.dispose();
});

it('keeps instances isolated and clears credentials synchronously before concealment is rendered', async () => {
  const first = new SessionController({
    bootstrap: async () => response(firstId),
  });
  const second = new SessionController({
    bootstrap: async () => response(secondId),
  });
  const reset = vi.fn();
  first.onCredentialReset = reset;
  try {
    await first.check();
    await second.check();
    expect(reset).toHaveBeenLastCalledWith(true);
    reset.mockClear();
    first.conceal();
    expect(reset).toHaveBeenCalledWith(false);
    expect(first.snapshot.identity).toBeNull();
    expect(second.snapshot.identity?.accountId).toBe(secondId);
    first.dispose();
    expect(first.onCredentialReset).toBeUndefined();
  } finally {
    first.dispose();
    second.dispose();
  }
});
