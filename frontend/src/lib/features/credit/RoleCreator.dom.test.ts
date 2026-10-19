import {
  cleanup,
  fireEvent,
  render,
  screen,
  waitFor,
} from '@testing-library/svelte';
import { afterEach, expect, it, vi } from 'vitest';
import RoleCreator from './RoleCreator.svelte';
import { createCreditRole } from './role-api';
import { TransportFailure } from '../../transport/graphql';
import { specimenPolicy } from '../../../../../build-tools/verification-tests/frontend/src/routes/policy/fixture';
vi.mock('./role-api', () => ({
  createCreditRole: vi.fn(),
  recoverCreditRole: vi.fn(),
}));
afterEach(() => {
  cleanup();
  vi.clearAllMocks();
});
it.each([
  ['RATE_LIMITED', 'Role creation was rate limited. Wait before trying again.'],
  [
    'FORBIDDEN',
    'Role creation was rejected. Review the name and sign-in before trying again.',
  ],
])('allows correction after confirmed %s rejection', async (code, message) => {
  const policy = specimenPolicy();
  vi.mocked(createCreditRole).mockRejectedValueOnce(
    new TransportFailure('rejected', code),
  );
  const onCreated = vi.fn(async () => {});
  const { container } = render(RoleCreator, {
    props: {
      policy,
      context: {
        accountId: '10000000-0000-4000-8000-000000000001',
        csrf: 'A'.repeat(43),
        policyRevision: policy.revision,
      },
      inheritedCategory: 'CAST',
      onCreated,
    },
  });
  container.querySelector('details')?.setAttribute('open', '');
  const input = screen.getByLabelText('Custom role name · Required');
  await fireEvent.input(input, { target: { value: 'Stunt performer' } });
  await fireEvent.submit(container.querySelector('form')!);
  await screen.findByText(message);
  await waitFor(() => expect(input.hasAttribute('disabled')).toBe(false));
  expect(onCreated).not.toHaveBeenCalled();
});
