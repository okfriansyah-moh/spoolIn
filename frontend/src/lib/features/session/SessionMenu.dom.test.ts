import { cleanup, render, screen } from '@testing-library/svelte';
import { afterEach, expect, it, vi } from 'vitest';
import { specimenPolicy } from '../../../../../build-tools/verification-tests/frontend/src/routes/policy/fixture';
import SessionMenu from './SessionMenu.svelte';
import { SessionController } from './controller.svelte';
import type { SessionBootstrap, SessionSource } from './types';

afterEach(() => cleanup());

it('keeps one polite status node while empty feedback becomes visible and clears again', async () => {
  const anonymous: SessionBootstrap = {
    policy: specimenPolicy(),
    session: {
      authenticated: false,
      role: null,
      accountId: null,
      displayName: null,
      expiresAt: null,
      csrfToken: 'A'.repeat(43),
    },
  };
  const bootstrap = vi.fn<SessionSource['bootstrap']>();
  bootstrap.mockResolvedValueOnce(anonymous);
  bootstrap.mockRejectedValueOnce(new Error('Session service unavailable'));
  bootstrap.mockResolvedValueOnce(anonymous);
  const controller = new SessionController({ bootstrap });
  try {
    await controller.check();
    const { rerender } = render(SessionMenu, {
      props: { controller, session: controller.snapshot },
    });
    const status = screen.getByRole('status');
    expect(status.getAttribute('aria-live')).toBe('polite');
    expect(status.getAttribute('aria-atomic')).toBe('true');
    expect(status.textContent?.trim()).toBe('');
    expect(status.classList.contains('idle')).toBe(true);

    await controller.check();
    await rerender({ controller, session: controller.snapshot });
    expect(screen.getByRole('status')).toBe(status);
    expect(status.textContent).toContain('Sign-in is temporarily unavailable');
    expect(status.classList.contains('idle')).toBe(false);

    await controller.check();
    await rerender({ controller, session: controller.snapshot });
    expect(screen.getByRole('status')).toBe(status);
    expect(status.textContent?.trim()).toBe('');
    expect(status.classList.contains('idle')).toBe(true);
  } finally {
    controller.dispose();
  }
});
