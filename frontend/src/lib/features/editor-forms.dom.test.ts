import {
  cleanup,
  fireEvent,
  render,
  screen,
  waitFor,
} from '@testing-library/svelte';
import { afterEach, expect, it, vi } from 'vitest';
import { specimenPolicy } from '../../../../build-tools/verification-tests/frontend/src/routes/policy/fixture';
import MovieForm from './movie/MovieEditorForm.svelte';
import PersonForm from './person/write/PersonEditorForm.svelte';
import type { EditorState as MovieState, ManagedMovie } from './movie/types';
import type { EditorState as PersonState } from './person/write/types';

vi.mock('$app/paths', () => ({
  resolve: (path: string, values: Record<string, string> = {}) =>
    Object.entries(values).reduce(
      (result, [key, value]) => result.replace(`[${key}]`, value),
      path,
    ),
}));

const id = '00000000-0000-4000-8000-000000000001';
const movie: ManagedMovie = {
  id,
  kind: 'MOVIE',
  state: 'ACTIVE',
  title: 'Current film',
  originalTitle: null,
  synopsis: 'Current story',
  releaseYear: '2000',
  genreIds: ['drama'],
  version: '3',
  artworkUrl: null,
  artworkUnavailable: false,
};
const person = {
  id,
  name: 'Current name',
  biography: null,
  state: 'ACTIVE' as const,
  version: '3',
};

function movieState(overrides: Partial<MovieState> = {}): MovieState {
  return {
    mode: 'create',
    phase: 'idle',
    movieId: null,
    saved: null,
    current: null,
    draft: {
      title: 'Film',
      synopsis: 'Story',
      genreIds: ['drama'],
      release: { year: 2000, month: null, day: null },
      countries: ['ID'],
      synopsisLanguage: 'und',
    },
    genres: [
      { id: 'drama', label: 'Drama' },
      { id: 'comedy', label: 'Comedy' },
    ],
    draftChoices: [],
    draftBaseKnown: true,
    fieldErrors: [],
    summaryTitle: '',
    message: '',
    pendingOperationId: null,
    staleReviewOpen: false,
    mediaOutage: false,
    discardDialogOpen: false,
    ...overrides,
  };
}
function personState(overrides: Partial<PersonState> = {}): PersonState {
  return {
    mode: 'create',
    phase: 'idle',
    personId: null,
    saved: null,
    current: null,
    draft: { name: 'Retained name', biography: 'Retained biography' },
    draftChoices: [],
    draftBaseKnown: true,
    fieldErrors: [],
    summaryTitle: '',
    message: '',
    pendingOperationId: null,
    staleReviewOpen: false,
    discardDialogOpen: false,
    discardPending: false,
    ...overrides,
  };
}
function actions() {
  return {
    onSubmit: vi.fn(),
    onCheckOriginal: vi.fn(),
    onReviewStale: vi.fn(),
    onRebase: vi.fn(),
    onSelectDraft: vi.fn(),
    onStartNewDraft: vi.fn(),
    onDraftChange: vi.fn(),
    onGenreToggle: vi.fn(),
    onDiscardOpen: vi.fn(),
    onDiscardClose: vi.fn(),
    onDiscardConfirm: vi.fn(),
  };
}
function renderMovie(state = movieState()) {
  const callbacks = actions();
  return {
    ...render(MovieForm, { state, policy: specimenPolicy(), ...callbacks }),
    callbacks,
  };
}
function renderPerson(state = personState()) {
  const callbacks = actions();
  return {
    ...render(PersonForm, { state, policy: specimenPolicy(), ...callbacks }),
    callbacks,
  };
}
afterEach(cleanup);

it('movie fields emit user-entered text and preserve partially typed country lists', async () => {
  const { callbacks } = renderMovie();
  for (const [label, field, value] of [
    ['Title · Required', 'title', 'New title'],
    ['Synopsis · Required', 'synopsis', 'New story'],
    ['Release year · Required', 'releaseYear', '2024'],
  ] as const) {
    await fireEvent.input(screen.getByLabelText(label), { target: { value } });
    expect(callbacks.onDraftChange).toHaveBeenCalledWith(field, value);
  }
  await fireEvent.click(screen.getByText('More movie details'));
  for (const [label, field, value] of [
    ['Original title', 'originalTitle', 'Original'],
    ['Release month', 'releaseMonth', '10'],
    ['Release day', 'releaseDay', '9'],
    ['Runtime in minutes', 'runtimeMinutes', '95'],
    ['Original language', 'originalLanguage', 'id'],
    ['Synopsis language', 'synopsisLanguage', 'en'],
    ['Countries', 'countries', 'ID, '],
  ] as const) {
    const input = screen.getByLabelText(label, {
      exact: label !== 'Original title',
    });
    await fireEvent.input(input, { target: { value } });
    expect(callbacks.onDraftChange).toHaveBeenCalledWith(field, value);
  }
  expect(screen.getByLabelText('Countries')).toHaveProperty('value', 'ID, ');
  await fireEvent.submit(screen.getByRole('form', { name: 'Create movie' }));
  expect(callbacks.onSubmit).toHaveBeenCalledOnce();
  await fireEvent.click(screen.getByRole('button', { name: 'Leave draft' }));
  expect(callbacks.onDiscardOpen).toHaveBeenCalledOnce();
});

it('genre search, selection, removal and Escape preserve the disclosure focus', async () => {
  const { callbacks } = renderMovie();
  await fireEvent.click(screen.getByText('Change genres'));
  const disclosure = screen.getByRole('searchbox').closest('details');
  if (!disclosure) throw new Error('Genre disclosure missing');
  await fireEvent(disclosure, new Event('toggle'));
  await fireEvent.input(
    screen.getByRole('searchbox', { name: 'Find a genre' }),
    { target: { value: 'COM' } },
  );
  await fireEvent.click(screen.getByRole('checkbox', { name: 'Comedy' }));
  expect(callbacks.onGenreToggle).toHaveBeenCalledWith('comedy', true);
  await fireEvent.input(screen.getByRole('searchbox'), {
    target: { value: 'missing' },
  });
  expect(
    screen.getByText('No genres match that search. Try another name.'),
  ).toBeTruthy();
  await fireEvent.keyDown(window, { key: 'Escape' });
  expect(
    screen.getByRole('searchbox', { hidden: true }).closest('details'),
  ).toHaveProperty('open', false);
  await fireEvent.click(
    screen.getByRole('button', { name: 'Remove genre Drama' }),
  );
  expect(callbacks.onGenreToggle).toHaveBeenCalledWith('drama', false);
});

it('movie validation opens the relevant details and focuses the first invalid field', async () => {
  renderMovie(
    movieState({
      fieldErrors: [
        { id: 'movie-releaseMonth', message: 'Month must be between 1 and 12' },
        { id: 'movie-genres', message: 'Choose a genre' },
      ],
    }),
  );
  const month = screen.getByLabelText('Release month');
  await waitFor(() => expect(document.activeElement).toBe(month));
  expect(month.closest('details')).toHaveProperty('open', true);
  expect(
    screen.getByRole('link', { name: 'Choose a genre' }).getAttribute('href'),
  ).toBe('#movie-genres');
  expect(screen.getByRole('searchbox').closest('details')).toHaveProperty(
    'open',
    true,
  );
});

it('movie retained drafts require an explicit choice before fields can be edited', async () => {
  const { callbacks } = renderMovie(
    movieState({
      draftChoices: [
        {
          id: 'draft-1',
          label: 'Film from yesterday',
          expiresAt: '2099-01-01T00:00:00Z',
        },
      ],
      message: 'Choose which edits to retain.',
    }),
  );
  expect(screen.getByLabelText('Title · Required')).toHaveProperty(
    'disabled',
    true,
  );
  await fireEvent.click(
    screen.getByRole('button', { name: 'Resume Film from yesterday' }),
  );
  expect(callbacks.onSelectDraft).toHaveBeenCalledWith('draft-1');
  await fireEvent.click(
    screen.getByRole('button', {
      name: 'Start a new draft from current values',
    }),
  );
  expect(callbacks.onStartNewDraft).toHaveBeenCalledOnce();
});

it('movie stale review requires acknowledgement and resets it when the current version changes', async () => {
  const state = movieState({
    mode: 'edit',
    phase: 'stale',
    saved: movie,
    current: movie,
    staleReviewOpen: true,
  });
  const { callbacks, rerender } = renderMovie(state);
  const button = screen.getByRole('button', {
    name: 'Use reviewed version for my edits',
  });
  expect(button).toHaveProperty('disabled', true);
  expect(screen.getByText('Current saved synopsis')).toBeTruthy();
  await fireEvent.click(
    screen.getByRole('checkbox', {
      name: 'I reviewed the current values and my retained edits.',
    }),
  );
  await fireEvent.click(button);
  expect(callbacks.onRebase).toHaveBeenCalledWith(true);
  await rerender({
    state: {
      ...state,
      current: { ...movie, version: '4' },
      draftBaseKnown: false,
    },
  });
  expect(button).toHaveProperty('disabled', true);
  expect(screen.getByText(/older base values are unavailable/)).toBeTruthy();
  await fireEvent.click(
    screen.getByRole('button', { name: 'Hide current version' }),
  );
  expect(callbacks.onReviewStale).toHaveBeenCalledOnce();
});

it('a saved create offers links to the same movie identity and prevents creating it again', () => {
  renderMovie(movieState({ phase: 'saved', saved: movie }));
  expect(
    screen.getByRole('link', { name: 'Edit saved movie' }).getAttribute('href'),
  ).toBe(`/manage/movies/${id}`);
  expect(
    screen
      .getByRole('link', { name: 'Review public record' })
      .getAttribute('href'),
  ).toBe(`/titles/${id}`);
  expect(screen.getByRole('button', { name: 'Create movie' })).toHaveProperty(
    'disabled',
    true,
  );
});

it.each([
  ['failed', 'Changes were not saved'],
  ['uncertain', 'Save result not confirmed'],
  ['resolved', 'Original save confirmed'],
  ['target-changed', 'This movie is now archived'],
] as const)(
  'movie %s feedback reports the outcome without losing edits',
  async (phase, heading) => {
    const { callbacks } = renderMovie(
      movieState({ phase, message: 'Retain these edits.' }),
    );
    expect(screen.getByText(heading)).toBeTruthy();
    expect(screen.getByLabelText('Title · Required')).toHaveProperty(
      'value',
      'Film',
    );
    if (phase === 'uncertain') {
      await fireEvent.click(
        screen.getByRole('button', { name: 'Check original result' }),
      );
      expect(callbacks.onCheckOriginal).toHaveBeenCalledOnce();
    }
  },
);

it('artwork outage leaves movie metadata editable and describes retained artwork', () => {
  renderMovie(movieState({ mediaOutage: true, mode: 'edit' }));
  expect(screen.getByText('Artwork is temporarily unavailable')).toBeTruthy();
  expect(screen.getByLabelText('Title · Required')).toHaveProperty(
    'disabled',
    false,
  );
});

it('person fields submit their entered values and offer an explicit discard action', async () => {
  const { callbacks } = renderPerson();
  await fireEvent.input(screen.getByLabelText('Professional name · Required'), {
    target: { value: '山田' },
  });
  await fireEvent.input(screen.getByLabelText('Biography · Optional'), {
    target: { value: '' },
  });
  expect(callbacks.onDraftChange).toHaveBeenCalledWith('name', '山田');
  expect(callbacks.onDraftChange).toHaveBeenCalledWith('biography', '');
  await fireEvent.submit(screen.getByRole('form', { name: 'Create person' }));
  expect(callbacks.onSubmit).toHaveBeenCalledOnce();
  await fireEvent.click(screen.getByRole('button', { name: 'Leave draft' }));
  expect(callbacks.onDiscardOpen).toHaveBeenCalledOnce();
});

it('person stale review shows the shared identity and requires explicit acknowledgement', async () => {
  const { callbacks } = renderPerson(
    personState({
      mode: 'edit',
      phase: 'stale',
      current: person,
      staleReviewOpen: true,
      draftBaseKnown: false,
    }),
  );
  expect(screen.getAllByText('Current name').length).toBeGreaterThan(0);
  const rebase = screen.getByRole('button', {
    name: 'Use reviewed version for my edits',
  });
  expect(rebase).toHaveProperty('disabled', true);
  await fireEvent.click(
    screen.getByRole('checkbox', {
      name: 'I reviewed the current values and my retained edits.',
    }),
  );
  await fireEvent.click(rebase);
  expect(callbacks.onRebase).toHaveBeenCalledWith(true);
  await fireEvent.click(
    screen.getByRole('button', { name: 'Hide current record' }),
  );
  expect(callbacks.onReviewStale).toHaveBeenCalledOnce();
});

it('a saved person keeps its public identity and cannot be created twice', () => {
  renderPerson(personState({ phase: 'saved', saved: person }));
  expect(
    screen
      .getByRole('link', { name: 'Edit saved person' })
      .getAttribute('href'),
  ).toBe(`/manage/people/${id}`);
  expect(
    screen
      .getByRole('link', { name: 'Review public record' })
      .getAttribute('href'),
  ).toBe(`/people/${id}`);
  expect(screen.getByRole('button', { name: 'Save person' })).toHaveProperty(
    'disabled',
    true,
  );
});

it('person retained drafts disable editing until an explicit selection', async () => {
  const { callbacks } = renderPerson(
    personState({
      draftChoices: [
        {
          id: 'person-draft',
          label: 'Shared name',
          expiresAt: '2099-01-01T00:00:00Z',
        },
      ],
    }),
  );
  expect(screen.getByLabelText('Professional name · Required')).toHaveProperty(
    'disabled',
    true,
  );
  await fireEvent.click(
    screen.getByRole('button', { name: 'Resume Shared name' }),
  );
  expect(callbacks.onSelectDraft).toHaveBeenCalledWith('person-draft');
  await fireEvent.click(
    screen.getByRole('button', {
      name: 'Start a new draft from current values',
    }),
  );
  expect(callbacks.onStartNewDraft).toHaveBeenCalledOnce();
});

it.each([
  ['failed', 'Changes were not saved'],
  ['uncertain', 'Save result not confirmed'],
  ['target-changed', 'Editing unavailable'],
] as const)(
  'person %s feedback preserves input and recovery',
  async (phase, heading) => {
    const { callbacks } = renderPerson(
      personState({
        phase,
        message: 'Keep these edits.',
        fieldErrors: [{ id: 'person-name', message: 'Name needs review.' }],
      }),
    );
    expect(screen.getByText(heading)).toBeTruthy();
    expect(
      screen
        .getByRole('link', { name: 'Name needs review.' })
        .getAttribute('href'),
    ).toBe('#person-name');
    if (phase === 'uncertain') {
      await fireEvent.click(
        screen.getByRole('button', { name: 'Check original result' }),
      );
      expect(callbacks.onCheckOriginal).toHaveBeenCalledOnce();
    }
  },
);
