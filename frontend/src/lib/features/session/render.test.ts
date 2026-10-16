import { render } from 'svelte/server';
import { expect, it } from 'vitest';
import AccountForm from './AccountForm.svelte';
import SessionMenu from './SessionMenu.svelte';
import { SessionController } from './controller';
import { sessionContext } from './context';
import { specimenPolicy } from '../../../../../build-tools/verification-tests/frontend/src/routes/policy/fixture';
async function visitor() {
  const controller = new SessionController({
    bootstrap: async () => ({
      policy: specimenPolicy(),
      session: {
        authenticated: false,
        role: null,
        accountId: null,
        displayName: null,
        expiresAt: null,
        csrfToken: 'A'.repeat(43),
      },
    }),
  });
  await controller.check();
  return controller;
}
it('registration renders labelled new-password controls without a role selector', async () => {
  const controller = await visitor();
  const html = render(AccountForm, {
    props: { mode: 'register' },
    context: new Map([[sessionContext, controller]]),
  }).body;
  expect(html).toContain('autocomplete="new-password"');
  expect(html).toContain('autocomplete="email"');
  expect(html).toContain('for="account-name"');
  expect(html).not.toContain('<select');
  expect(html).not.toContain('Maintainer');
  controller.dispose();
});
it('first-success status exists before the header has any announcement', async () => {
  const controller = await visitor();
  const html = render(SessionMenu, {
    props: { controller, session: controller.snapshot },
  }).body;
  expect(html).toContain('role="status"');
  expect(html).toContain('aria-live="polite"');
  expect(html).not.toContain('Signed out.');
  controller.dispose();
});
it('checking conceals credentials and private projections', () => {
  const controller = new SessionController({
    bootstrap: async () => {
      throw new Error('unavailable');
    },
  });
  const html = render(AccountForm, {
    props: { mode: 'signIn' },
    context: new Map([[sessionContext, controller]]),
  }).body;
  expect(html).toContain('Checking your session');
  expect(html).not.toContain('type="password"');
  expect(html).not.toContain('data-private');
  controller.dispose();
});
