import {
  cleanup,
  fireEvent,
  render,
  screen,
  waitFor,
} from '@testing-library/svelte';
import { afterEach, expect, it, vi } from 'vitest';
import AccountForm from './AccountForm.svelte';
import { SessionController } from './controller';
import { sessionContext } from './context';
import type { AccountCommands } from './commands';
import { specimenPolicy } from '../../../../../build-tools/verification-tests/frontend/src/routes/policy/fixture';
function mount(
  submit: AccountCommands['submit'],
  mode: 'signIn' | 'register' = 'register',
  operation?: AccountCommands['operation'],
) {
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
    submit,
    ...(operation ? { operation } : {}),
  });
  render(AccountForm, {
    props: { mode },
    context: new Map([[sessionContext, controller]]),
  });
  void controller.check();
  return controller;
}
afterEach(() => cleanup());
it('real form validation focuses the summary and never dispatches invalid values', async () => {
  const submit = vi.fn<AccountCommands['submit']>();
  const controller = mount(submit);
  const form = await screen.findByRole('form', {
    name: 'Create Viewer account',
  });
  await fireEvent.submit(form);
  await waitFor(() =>
    expect(document.activeElement?.getAttribute('role')).toBe('alert'),
  );
  const passwordError = screen.getByRole('link', { name: /^Password:/ });
  expect(passwordError.getAttribute('href')).toBe('#account-password');
  expect(submit).not.toHaveBeenCalled();
  controller.dispose();
});
it('registration clears the password before dispatch and announces first success', async () => {
  const submit = vi.fn<AccountCommands['submit']>(async (_input, id) => ({
    operationId: id,
    state: 'SUCCEEDED',
    resultExpired: false,
    problem: null,
  }));
  const controller = mount(submit);
  await screen.findByRole('form', { name: 'Create Viewer account' });
  await fireEvent.input(screen.getByLabelText(/^Name\b/), {
    target: { value: 'A Person' },
  });
  await fireEvent.input(screen.getByLabelText(/^Email\b/), {
    target: { value: 'a@example.test' },
  });
  const password = screen.getByLabelText(/^Password\b/);
  await fireEvent.input(password, {
    target: { value: '  password exact as entered  ' },
  });
  await fireEvent.submit(
    screen.getByRole('form', { name: 'Create Viewer account' }),
  );
  await screen.findByRole('heading', { name: 'Your Viewer account is ready' });
  expect(submit).toHaveBeenCalledTimes(1);
  expect(password instanceof HTMLInputElement && password.value === '').toBe(
    true,
  );
  expect(document.querySelector('#account-success')?.textContent).toContain(
    'Your Viewer account is ready',
  );
  controller.dispose();
});
it('server denial preserves email and name while clearing password and focusing safe feedback', async () => {
  const controller = mount(async (_input, id) => ({
    operationId: id,
    state: 'FAILED',
    resultExpired: false,
    problem: { code: 'FAILED', retryAfterMs: 0 },
  }));
  await screen.findByRole('form', { name: 'Create Viewer account' });
  const name = screen.getByLabelText(/^Name\b/);
  const email = screen.getByLabelText(/^Email\b/);
  await fireEvent.input(name, { target: { value: 'A Person' } });
  await fireEvent.input(email, { target: { value: 'a@example.test' } });
  await fireEvent.input(screen.getByLabelText(/^Password\b/), {
    target: { value: 'existing exact password' },
  });
  await fireEvent.submit(
    screen.getByRole('form', { name: 'Create Viewer account' }),
  );
  await waitFor(() =>
    expect(document.activeElement?.getAttribute('role')).toBe('alert'),
  );
  const currentPassword = screen.getByLabelText(/^Password\b/);
  expect(
    currentPassword instanceof HTMLInputElement && currentPassword.value === '',
  ).toBe(true);
  expect(screen.getByDisplayValue('A Person')).toBeTruthy();
  expect(screen.getByDisplayValue('a@example.test')).toBeTruthy();
  await fireEvent.click(
    screen.getByRole('button', { name: 'Refresh input rules' }),
  );
  await screen.findByRole('form', { name: 'Create Viewer account' });
  controller.dispose();
});
it('uncertain form checks the original registration and never resubmits a password', async () => {
  const submit = vi.fn<AccountCommands['submit']>(async (_input, id) => ({
    operationId: id,
    state: 'PENDING',
    resultExpired: false,
    problem: null,
  }));
  const operation = vi.fn<AccountCommands['operation']>(async (id) => ({
    operationId: id,
    state: 'SUCCEEDED',
    resultExpired: false,
    problem: null,
  }));
  const controller = mount(submit, 'register', operation);
  await screen.findByRole('form', { name: 'Create Viewer account' });
  await fireEvent.input(screen.getByLabelText(/^Name\b/), {
    target: { value: 'A Person' },
  });
  await fireEvent.input(screen.getByLabelText(/^Email\b/), {
    target: { value: 'a@example.test' },
  });
  await fireEvent.input(screen.getByLabelText(/^Password\b/), {
    target: { value: 'existing exact password' },
  });
  await fireEvent.submit(
    screen.getByRole('form', { name: 'Create Viewer account' }),
  );
  const check = await screen.findByRole('button', { name: 'Check original request' });
  await waitFor(() => expect(check instanceof HTMLButtonElement && !check.disabled).toBe(true));
  await fireEvent.click(check);
  await screen.findByRole('heading', { name: 'Your Viewer account is ready' });
  expect(submit).toHaveBeenCalledTimes(1);
  expect(operation).toHaveBeenCalledTimes(1);
  controller.dispose();
});
