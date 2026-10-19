import { cleanup, fireEvent, render, screen } from '@testing-library/svelte';
import { afterEach, expect, it, vi } from 'vitest';
import PersonSearchSelect from './PersonSearchSelect.svelte';

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
  vi.restoreAllMocks();
});
function mount(
  options = [
    { id: 'one', name: 'Alex' },
    { id: 'two', name: 'Blair' },
  ],
) {
  Object.defineProperty(HTMLElement.prototype, 'scrollIntoView', {
    configurable: true,
    value: vi.fn(),
  });
  vi.stubGlobal('requestAnimationFrame', (callback: FrameRequestCallback) => {
    callback(0);
    return 1;
  });
  const onSelect = vi.fn();
  const onSearch = vi.fn();
  render(PersonSearchSelect, {
    props: {
      options,
      selected: null,
      loading: false,
      hasMore: false,
      error: '',
      feedback: '',
      onSearch,
      onSelect,
      onMore: vi.fn(),
      onRetry: vi.fn(),
    },
  });
  return { onSelect, onSearch, input: screen.getByRole('combobox') };
}
it('selects the highlighted person with the keyboard and closes without searching', async () => {
  const { input, onSelect, onSearch } = mount();
  await fireEvent.focus(input);
  onSearch.mockClear();
  await fireEvent.keyDown(input, { key: 'ArrowDown' });
  await fireEvent.keyDown(input, { key: 'Enter' });
  expect(onSelect).toHaveBeenCalledWith({ id: 'one', name: 'Alex' });
  expect(onSearch).not.toHaveBeenCalled();
  expect(input.getAttribute('aria-expanded')).toBe('false');
});
it('searches when Enter has no highlighted match and Escape closes the chooser', async () => {
  const { input, onSelect, onSearch } = mount([]);
  await fireEvent.focus(input);
  await fireEvent.input(input, { target: { value: 'No match' } });
  await fireEvent.keyDown(input, { key: 'Enter' });
  expect(onSearch).toHaveBeenCalledWith('No match');
  expect(onSelect).not.toHaveBeenCalled();
  await fireEvent.keyDown(input, { key: 'Escape' });
  expect(input.getAttribute('aria-expanded')).toBe('false');
});
