import { expect, it } from 'vitest';
import { specimenPolicy } from '../../../../../build-tools/verification-tests/frontend/src/routes/policy/fixture';
import { session } from './parse';
import { createSessionSource } from './api';
const anonymous = {
  authenticated: false,
  role: null,
  accountId: null,
  displayName: null,
  expiresAt: null,
  csrfToken: 'A'.repeat(43),
};
const verified = {
  ...anonymous,
  authenticated: true,
  role: 'VIEWER',
  accountId: '10000000-0000-4000-8000-000000000001',
  displayName: 'A Person',
  expiresAt: '2099-01-01T00:00:00Z',
};
it('parses only a coherent authoritative session and permits readable anti-forgery data without granting authority', () => {
  expect(session(anonymous)).toEqual(anonymous);
  expect(session(verified)).toEqual(verified);
  expect(session({ ...verified, role: 'MAINTAINER' }).role).toBe('MAINTAINER');
});
it.each([
  null,
  [],
  {},
  { ...anonymous, role: 'VIEWER' },
  { ...anonymous, accountId: verified.accountId },
  { ...verified, authenticated: 'true' },
  { ...verified, role: 'Actor' },
  { ...verified, accountId: 'foreign' },
  { ...verified, displayName: '' },
  { ...verified, displayName: '\uD800' },
  { ...verified, expiresAt: 'bad-date' },
  { ...verified, csrfToken: 'short' },
  { ...verified, credential: 'unexpected-private-value' },
])(
  'rejects malformed or contradictory session input without publishing an identity',
  (value) => {
    expect(() => session(value)).toThrow('The request could not be completed.');
  },
);
it('production adapter uses the generated named bootstrap operation and validates both projections', async () => {
  const source = createSessionSource(async (query, variables, options) => {
    expect(query).toContain('query AccountBootstrap');
    expect(variables).toEqual({});
    expect(options).toMatchObject({ identity: true });
    return { session: verified, policy: specimenPolicy() };
  });
  const bootstrap = await source.bootstrap(new AbortController().signal);
  expect(bootstrap.session.accountId).toBe(verified.accountId);
  expect(bootstrap.policy.textBounds.registrationName?.minCodePoints).toBe(2);
});
