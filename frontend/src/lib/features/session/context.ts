import { getContext } from 'svelte';
import type { SessionController } from './controller.svelte';
export const sessionContext = Symbol('spoolin-session');
export function sessionController(): SessionController {
  const controller = getContext<SessionController | undefined>(sessionContext);
  if (!controller) throw new Error('Session boundary unavailable.');
  return controller;
}
