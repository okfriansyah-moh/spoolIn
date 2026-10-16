import { expect, it, vi } from 'vitest';
import { specimenPolicy } from '../../../../../build-tools/verification-tests/frontend/src/routes/policy/fixture';
import { SessionController } from './controller';
import type { SessionBootstrap, SessionSource } from './types';
import type { AccountCommands, IdentityOutcome } from './commands';
const accountId = '10000000-0000-4000-8000-000000000001';
function bootstrap(authenticated = false): SessionBootstrap {
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
function succeeded(operationId: string): IdentityOutcome {
  return {
    operationId,
    state: 'SUCCEEDED',
    resultExpired: false,
    problem: null,
  };
}
it('registration uses anonymous CSRF and clears the secret from its result without automatically signing in', async () => {
  const submit = vi.fn<AccountCommands['submit']>(async (_input, id) =>
    succeeded(id),
  );
  const controller = new SessionController({
    bootstrap: async () => bootstrap(),
    submit,
  });
  await controller.check();
  const password = '  exact e\u0301 🦉 credential  ';
  const outcome = await controller.submit({
    kind: 'register',
    name: 'Li',
    email: 'li@example.test',
    password,
  });
  expect(outcome.kind).toBe('registered');
  expect(controller.snapshot).toMatchObject({
    kind: 'anonymous',
    identity: null,
  });
  expect(submit.mock.calls[0]?.[0].password).toBe(password);
  expect(submit.mock.calls[0]?.[2]).toBe('A'.repeat(43));
  expect(JSON.stringify({ outcome, state: controller.snapshot })).not.toContain(
    password,
  );
});
it('confirmed sign-in rechecks the actual session and follows its assigned Viewer role', async () => {
  const source = vi
    .fn<SessionSource['bootstrap']>()
    .mockResolvedValueOnce(bootstrap())
    .mockResolvedValueOnce(bootstrap(true));
  const submit = vi.fn<AccountCommands['submit']>(async (_input, id) =>
    succeeded(id),
  );
  const controller = new SessionController({ bootstrap: source, submit });
  await controller.check();
  const result = await controller.submit({
    kind: 'signIn',
    email: 'a@example.test',
    password: 'old short credential',
  });
  expect(result).toMatchObject({ kind: 'signedIn', role: 'VIEWER' });
  expect(controller.snapshot.identity?.accountId).toBe(accountId);
  expect(controller.snapshot.message).toBe('Signed in as Viewer.');
  expect(source).toHaveBeenCalledTimes(2);
});
it('a signed-in account cannot use the visitor registration controller', async () => {
  const submit = vi.fn<AccountCommands['submit']>();
  const controller = new SessionController({
    bootstrap: async () => bootstrap(true),
    submit,
  });
  await controller.check();
  expect(
    (
      await controller.submit({
        kind: 'register',
        name: 'Li',
        email: 'li@example.test',
        password: 'x'.repeat(15),
      })
    ).kind,
  ).toBe('failed');
  expect(submit).not.toHaveBeenCalled();
  expect(controller.snapshot.identity?.accountId).toBe(accountId);
});
