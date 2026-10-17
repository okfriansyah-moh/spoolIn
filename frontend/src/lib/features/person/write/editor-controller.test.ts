import { describe, expect, it, vi } from 'vitest';
import { specimenPolicy } from '../../../../../../build-tools/verification-tests/frontend/src/routes/policy/fixture';

vi.mock('./api', () => ({
  fetchManagedPerson: vi.fn(async () => null),
  createPerson: vi.fn(),
  updatePerson: vi.fn(),
  recoverCatalogOperation: vi.fn(),
}));

import { PersonEditorController } from './editor-controller';

const policy = specimenPolicy();
const context = Object.freeze({
  accountId: '10000000-0000-4000-8000-000000000001',
  csrf: 'A'.repeat(43),
  policyRevision: policy.revision,
});

describe('PersonEditorController', () => {
  it('rejects empty name before submit', async () => {
    const controller = new PersonEditorController(
      async () => ({}),
      'create',
      null,
    );
    let phase = 'idle';
    controller.subscribe((state) => {
      phase = state.phase;
    });
    await controller.bootstrap(policy, context, new AbortController().signal);
    await controller.submit();
    expect(phase).toBe('failed');
  });
});
