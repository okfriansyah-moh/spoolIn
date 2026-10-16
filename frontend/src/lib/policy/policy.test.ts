import { describe, expect, it } from 'vitest';
import { render } from 'svelte/server';
import {
  specimenPolicy,
  retainedDraft,
  plainTextExample,
} from '../../../../build-tools/verification-tests/frontend/src/routes/policy/fixture';
import { parsePolicy } from './parse';
import {
  boundFor,
  changedText,
  checkText,
  count,
  optionalIntent,
  trimOrdinary,
} from './rules';
import PolicyGuidance from './PolicyGuidance.svelte';
import {
  registrationFeedback,
  optionalFeedback,
  draftFeedback,
} from './feedback';

const policy = specimenPolicy();
function textGuidanceMarkup() {
  return render(PolicyGuidance, {
    props: {
      view: 'text-boundaries',
      policy,
      initialName: 'V',
      plainTextExample,
    },
  }).body;
}
describe('T009 feedback accessibility', () => {
  it('keeps a polite status region present before the first outcome', () => {
    for (const view of [
      'text-boundaries',
      'optional-clearing',
      'policy-changed',
    ] as const) {
      const body = render(PolicyGuidance, { props: { view, policy } }).body;
      expect(body).toMatch(
        /role="status"[^>]*aria-live="polite"[^>]*aria-atomic="true"/,
      );
    }
  });
});

describe('T009 shared policy rules', () => {
  it('ATDD-276 preserves exact Unicode and counts supplementary characters', () => {
    expect(count('😀'.repeat(100))).toBe(100);
    expect(
      checkText('😀'.repeat(100), boundFor(policy, 'personName')).error,
    ).toBe('');
    expect(
      checkText('😀'.repeat(101), boundFor(policy, 'personName')).value,
    ).toBe('😀'.repeat(101));
    expect(
      checkText('😀'.repeat(101), boundFor(policy, 'personName')).error,
    ).not.toBe('');
    expect(trimOrdinary('\u0085\u00a0V\u0085')).toBe('V');
    expect(trimOrdinary('\ufeffV\ufeff')).toBe('\ufeffV\ufeff');
    expect(count('é')).toBe(1);
    expect(count('e\u0301')).toBe(2);
    expect(checkText('e\u0301', boundFor(policy, 'personName')).value).toBe(
      'e\u0301',
    );
    expect(checkText('\ud800', boundFor(policy, 'personName')).error).not.toBe(
      '',
    );
  });
  it('T009-A3 omitted and clearing requests differ', () => {
    expect(optionalIntent(false, null)).toEqual({ kind: 'omitted' });
    for (const value of [null, '', ' \u0085 '])
      expect(optionalIntent(true, value)).toEqual({ kind: 'clear' });
    expect(optionalIntent(true, ' Maya / Echo ')).toEqual({
      kind: 'value',
      value: 'Maya / Echo',
    });
  });
});

describe('T009 retained content', () => {
  it('T009-A2 tightening preserves complete stored content and rejected draft', () => {
    const bound = { ...boundFor(policy, 'synopsis'), maxCodePoints: 100 };
    expect(changedText(retainedDraft, retainedDraft, bound)).toEqual({
      value: retainedDraft,
      error: '',
    });
    expect(checkText(retainedDraft, bound).value).toBe(retainedDraft);
    expect(checkText(retainedDraft, bound).error).toBe(
      'Use 1 to 100 Unicode code points.',
    );
    const body = render(PolicyGuidance, {
      props: { view: 'policy-changed', policy, initialDraft: retainedDraft },
    }).body;
    expect(body).toContain(retainedDraft);
    expect(body).toContain('The synopsis limit has changed');
    expect(body).not.toContain('Synopsis now exceeds the effective maximum');
    expect(body).toContain('Current maximum: 100 Unicode code points.');
    expect(body.match(/<button\b[^>]*>/g)).toEqual([
      expect.not.stringContaining('tabindex='),
    ]);
  });
  it('current password bytes stay exact', () => {
    const exact = { ...boundFor(policy, 'password'), minCodePoints: 1 };
    expect(checkText(' a\u00a0 ', exact).value).toBe(' a\u00a0 ');
  });
});

describe('T009 policy projection boundary', () => {
  it('accepts the complete safe policy and rejects private or missing keys', () => {
    expect(parsePolicy(policy)).toEqual(policy);
    expect(() =>
      parsePolicy({ ...policy, privateKey: 'synthetic-private' }),
    ).toThrow('could not be loaded safely');
    expect(() => parsePolicy({ ...policy, revision: 'invalid' })).toThrow();
    expect(() => parsePolicy({ ...policy, textBounds: {} })).toThrow();
    expect(() =>
      parsePolicy({ ...policy, artwork: { ...policy.artwork, formats: [] } }),
    ).toThrow();
    expect(() =>
      parsePolicy({
        ...policy,
        requestLimits: { ...policy.requestLimits, maxPage: 0 },
      }),
    ).toThrow();
  });
  it('renders four distinct controlled components with safe text and no enabled unavailable submission', () => {
    const text = textGuidanceMarkup();
    expect(text).toContain('&lt;script>');
    expect(text).not.toContain('<script>alert');
    expect(text.match(/<button\b[^>]*>/g)).toEqual([
      expect.stringContaining('tabindex="0"'),
    ]);
    const optional = render(PolicyGuidance, {
      props: {
        view: 'optional-clearing',
        policy,
        initialOriginalTitle: 'Cahaya Terakhir',
      },
    }).body;
    expect(optional).toContain('Cahaya Terakhir');
    expect(optional).not.toMatch(/<button\b[^>]*\btabindex=/);
    const unavailable = render(PolicyGuidance, {
      props: {
        view: 'policy-unavailable',
        policy,
        initialDraft: retainedDraft,
      },
    }).body;
    expect(unavailable).toContain('Editing is temporarily unavailable');
    expect(unavailable).toContain('disabled');
    expect(unavailable).not.toMatch(/<button\b[^>]*\btabindex=/);
    expect(unavailable).toContain(retainedDraft);
  });
});

describe('T009 visible policy feedback rules', () => {
  it('links registration corrections and emits distinct success', () => {
    const bound = boundFor(policy, 'registrationName');
    expect(registrationFeedback('V', bound)).toEqual({
      errors: [
        {
          id: 'policy-name',
          message: 'Registration name: Use 2 to 100 Unicode code points.',
        },
      ],
      outcome: '',
    });
    expect(registrationFeedback('李 · O’Connor', bound)).toEqual({
      errors: [],
      outcome: 'The name meets the current registration rule.',
    });
  });
  it('preserves omission even when an old unchanged value exceeds current bounds', () => {
    const bound = { ...boundFor(policy, 'originalTitle'), maxCodePoints: 3 };
    expect(optionalFeedback(false, 'Cahaya Terakhir', bound)).toEqual({
      errors: [],
      outcome: 'Original title stays unchanged.',
    });
    expect(optionalFeedback(true, '\u0085 ', bound)).toEqual({
      errors: [],
      outcome: 'Original title will be cleared.',
    });
    expect(optionalFeedback(true, 'Cahaya Terakhir', bound).errors).toEqual([
      { id: 'policy-original', message: 'Use 1 to 3 Unicode code points.' },
    ]);
    expect(optionalFeedback(true, ' A ', bound)).toEqual({
      errors: [],
      outcome: 'Original title will become A.',
    });
  });
});

describe('T009 draft feedback', () => {
  it('links changed draft correction without truncation or a successful outcome', () => {
    const bound = { ...boundFor(policy, 'synopsis'), maxCodePoints: 100 };
    expect(draftFeedback(retainedDraft, bound)).toEqual({
      errors: [
        {
          id: 'policy-draft',
          message: 'Synopsis: Use 1 to 100 Unicode code points.',
        },
      ],
      outcome: '',
    });
    expect(draftFeedback('First line.\nSecond line.', bound)).toEqual({
      errors: [],
      outcome: 'The revised draft meets the current rule.',
    });
  });
});

describe('T009 configured guidance minima', () => {
  it('C03 one-code-point examples reflect raised person and title minima', () => {
    const raised = parsePolicy({
      ...policy,
      textBounds: {
        ...policy.textBounds,
        personName: { ...policy.textBounds.personName, minCodePoints: 2 },
        title: { ...policy.textBounds.title, minCodePoints: 2 },
      },
    });
    const body = render(PolicyGuidance, {
      props: { view: 'text-boundaries', policy: raised },
    }).body;
    expect(body).toContain('person 2–100');
    expect(body).toContain('Use 2 to 100 Unicode code points.');
    expect(body).toContain('Use 2 to 255 Unicode code points.');
    expect(body).not.toContain('Valid professional name');
    expect(body).not.toContain('Accepted under the current title rule');
    expect(body).toContain('O’Connor · 李 · Jean-Luc');
    expect(body).toContain('é / é');
  });
});
