import { expect, it, vi } from 'vitest';
import { newDraftState } from './retained-drafts';

it.each([
  { draftChoices: [], phase: 'idle' },
  { draftChoices: ['retained'], phase: 'uncertain' },
])('preserves the current draft when a fresh start is blocked: %j', (selection) => {
  const reset = vi.fn();
  const project = vi.fn((saved: string) => saved.toUpperCase());
  expect(newDraftState({ ...selection, saved: 'saved' }, { reset }, project, 'empty')).toBeNull();
  expect(reset).not.toHaveBeenCalled();
  expect(project).not.toHaveBeenCalled();
});

it('starts from saved values after deliberately abandoning a retained selection', () => {
  const reset = vi.fn();
  const next = newDraftState(
    { saved: 'saved', draftChoices: ['retained'], phase: 'idle' },
    { reset }, (saved) => saved.toUpperCase(), 'empty',
  );
  expect(reset).toHaveBeenCalledOnce();
  expect(next).toMatchObject({ draft: 'SAVED', draftChoices: [], phase: 'idle', draftBaseKnown: true });
});

it('starts an unsaved record with empty values without projecting a missing record', () => {
  const project = vi.fn((saved: string) => saved.toUpperCase());
  const next = newDraftState(
    { saved: null, draftChoices: ['retained'], phase: 'idle' },
    { reset: vi.fn() }, project, 'empty',
  );
  expect(next?.draft).toBe('empty');
  expect(project).not.toHaveBeenCalled();
});
