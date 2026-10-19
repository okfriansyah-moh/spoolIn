import {
  cleanup,
  fireEvent,
  render,
  screen,
  waitFor,
} from '@testing-library/svelte';
import { afterEach, expect, it, vi } from 'vitest';
import DraftEditor from './DraftEditor.svelte';
import { DraftController } from './controller.svelte';
import type { DraftSource } from './types';
import {
  accountA,
  accountB,
  draftSession,
} from '../../../../../build-tools/verification-tests/frontend/src/routes/draft/fixture';

afterEach(() => cleanup());

it('renders reactive draft changes, preserves focused typing and conceals another account', async () => {
  const source: DraftSource = {
    listDrafts: async () => [],
    recoverOperation: async () => null,
    saveDraft: vi.fn(),
    discardDraft: vi.fn(),
  };
  const controller = new DraftController(source);
  const { rerender } = render(DraftEditor, {
    props: {
      controller,
      session: draftSession(accountA),
      initialName: 'Initial person',
    },
  });
  try {
    const field = await screen.findByLabelText(/^Name/);
    expect((field as HTMLInputElement).value).toBe('Initial person');
    field.focus();
    await fireEvent.input(field, { target: { value: 'Focused draft' } });
    expect(controller.snapshot.layers?.draft).toMatchObject({
      name: 'Focused draft',
    });
    expect((field as HTMLInputElement).value).toBe('Focused draft');
    field.blur();
    controller.setName('Recovered draft');
    await waitFor(() =>
      expect((field as HTMLInputElement).value).toBe('Recovered draft'),
    );
    await rerender({
      controller,
      session: draftSession(accountB),
      initialName: 'Initial person',
    });
    await waitFor(() => expect(screen.queryByLabelText(/^Name/)).toBeNull());
    expect(screen.getByText(/another account/)).toBeTruthy();
    expect(source.saveDraft).not.toHaveBeenCalled();
  } finally {
    controller.dispose();
  }
});
