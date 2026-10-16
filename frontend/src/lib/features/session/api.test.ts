import { expect, it, vi } from 'vitest';
import { createSessionSource } from './api';
import type { GraphqlRequest } from '../../transport/graphql';
const id = '10000000-0000-4000-8000-000000000001';
const operation = {
  id,
  state: 'SUCCEEDED',
  resultExpired: false,
  problem: null,
};
it('named account adapters use exact submitted values and original owner lookup without exposing private receipt fields', async () => {
  const reply = { operation, problem: null };
  const request = vi
    .fn<GraphqlRequest>()
    .mockResolvedValueOnce({ register: reply })
    .mockResolvedValueOnce({ signIn: reply })
    .mockResolvedValueOnce({ signOut: reply })
    .mockResolvedValueOnce({ operation })
    .mockResolvedValueOnce({ operation: null });
  const api = createSessionSource(request);
  const signal = new AbortController().signal;
  await api.submit(
    {
      kind: 'register',
      name: 'A Person',
      email: 'a@example.test',
      password: ' exact ',
    },
    id,
    'csrf',
    signal,
  );
  await api.submit(
    { kind: 'signIn', email: 'a@example.test', password: ' old ' },
    id,
    'csrf',
    signal,
  );
  await api.signOut(id, 'csrf', signal);
  expect((await api.operation(id, 'csrf', signal))?.operationId).toBe(id);
  expect(await api.operation(id, 'csrf', signal)).toBeNull();
  expect(request.mock.calls[0]?.[1]).toEqual({
    operationId: id,
    input: { name: 'A Person', email: 'a@example.test', password: ' exact ' },
  });
  expect(request.mock.calls[1]?.[1]).toEqual({
    operationId: id,
    email: 'a@example.test',
    password: ' old ',
  });
  expect(request.mock.calls[2]?.[2]).toMatchObject({
    mutation: true,
    csrf: 'csrf',
    signal,
  });
  expect(request.mock.calls[3]?.[0]).toContain('owner: CORE');
  expect(request.mock.calls[3]?.[2]).toEqual({ csrf: 'csrf', signal });
});
