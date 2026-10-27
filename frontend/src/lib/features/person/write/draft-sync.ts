import type { PersonDraft } from '../../../generated/json-types';
import type { DraftSource } from '../../draft/types';
import { DraftSync } from '../../editing/draft-sync';
import type { WriteContext } from './types';

export class PersonDraftSync extends DraftSync<PersonDraft> {
  constructor(source: DraftSource, targetId: string | null) {
    super(source, targetId, { kind: 'PERSON', matches: (draft) => draft.targetKind === 'PERSON' && draft.targetId === targetId });
  }

  async completeSubmission(values: PersonDraft, context: WriteContext, baseVersion: string): Promise<void> {
    this.dispose();
    if (this.targetId === null) { await this.discard(context); return; }
    this.catalogOperationId = null;
    await this.save(values, context, baseVersion);
  }
}
