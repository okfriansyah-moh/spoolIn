import { readFileSync } from 'node:fs';
import { createHash } from 'node:crypto';
import { resolve } from 'node:path';

type Screen = { id: string; file: string; states: string[]; state_locations: Record<string, unknown> };
const coveragePath = resolve('../../docs/design-system/coverage.json');
export const designCoverage = JSON.parse(readFileSync(coveragePath, 'utf8')) as { screens: Screen[] };
export const designSourceHash = createHash('sha256').update(readFileSync(coveragePath)).digest('hex');

const boundaries: Record<string, { route: string; source: string; case: string }> = {
  discovery: { route: '/', source: 'frontend/src/routes/+page.svelte', case: 'ATDD-256' },
  details: { route: '/titles/[id]', source: 'frontend/src/routes/titles/[id]/+page.svelte', case: 'ATDD-257' },
  account: { route: '/register, /sign-in', source: 'frontend/src/lib/features/session/AccountForm.svelte', case: 'ATDD-258' },
  session: { route: 'Shared protected session boundary', source: 'frontend/src/lib/components/SessionBoundary.svelte', case: 'ATDD-259' },
  management: { route: '/manage/movies', source: 'frontend/src/lib/features/movie/MovieEditorForm.svelte', case: 'ATDD-260' },
  people: { route: '/people, /manage/people', source: 'frontend/src/lib/features/person/write/PersonEditorForm.svelte', case: 'ATDD-261' },
  credits: { route: '/manage/movies/[id]', source: 'frontend/src/lib/features/credit/CreditPanel.svelte', case: 'ATDD-262' },
  artwork: { route: '/manage/movies/[id]', source: 'frontend/src/lib/features/artwork/ArtworkUploadField.svelte', case: 'ATDD-263' },
  archives: { route: '/manage/archives', source: 'frontend/src/routes/manage/archives/+page.svelte', case: 'ATDD-264' },
  audit: { route: '/manage/audit', source: 'frontend/src/routes/manage/audit/+page.svelte', case: 'ATDD-265' },
  ai: { route: 'Actual ChatGPT and Claude review frame', source: 'mcp/src/review/', case: 'ATDD-266' },
  policies: { route: 'Shared forms and effective policy', source: 'frontend/src/lib/policy/', case: 'ATDD-267' },
};

export const statesByStep: Record<string, string[]> = {
  catalog: ['catalog'], detail: [], 'no-match': ['catalog-no-match'],
  'signed-in-catalog': ['viewer'], 'signed-out': ['signed-out'],
  'person-persisted': ['person'], 'movie-persisted': ['movie'],
  'credit-persisted': ['credits'], 'credit-removed': ['credits'],
  'conflict-retained': ['editor'], 'conflict-rebased': [],
  'draft-picker': [], 'draft-recovered': [],
  'artwork-confirmed-partial': ['artwork-attached'], 'artwork-persisted-partial': ['artwork-attached'],
  'invalid-artwork': ['artwork-invalid'], 'audit-actual-mutation': ['audit-detail'],
  'movie-archived': ['movie-archive'], 'person-archived': ['person-archive'],
  'movie-restored': [], 'person-restored': [],
  'movie-deleted': [], 'person-deleted': [],
  'movie-delete-dialog': ['movie-delete'], 'person-delete-dialog': ['person-delete'],
};

export function fullStateInventory() {
  return designCoverage.screens.flatMap((screen) => screen.states.map((state) => ({
    state, screen: screen.id, reference: `${screen.file}#${state}`, ...boundaries[screen.id],
    sourceCompleteness: 'Source association only; production behavior requires its own assertions.',
    productionJourneys: Object.entries(statesByStep).filter(([, states]) => states.includes(state)).map(([step]) => step),
    requiredBoundary: screen.id === 'ai' ? 'Actual named assisted-client host and protocol transport'
      : 'Real make start application; controlled faults only for the assigned fault rows',
    runtimeResult: 'Pending remote production observation; reference render cannot satisfy this obligation.',
  })));
}
