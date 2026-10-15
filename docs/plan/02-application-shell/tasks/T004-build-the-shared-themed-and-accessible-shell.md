---
kind: task
id: T004
group: G02
depends_on:
- T001
- T002
- T003
readiness: Ready
status: Blocked
risk: medium
test_strategy: alongside
visual_review: required
---

# T004 — Build the shared themed and accessible shell

[Feature group](../group.md) · [Overall plan](../../../06-PLAN.md)

## Purpose and scope

Theme: switching preserves focus, input, filters and route state; unset storage follows system preference while dark remains the base token palette. This task owns the change surface and local evidence below. Risk is medium: incorrect composition, build assumptions or delivery claims can hide missing behavior. Verify the actual declared boundary. Unrelated features and source-document changes are outside this task.

## Traceability

| Obligation | Role |
|---|---|
| ATDD-069 | implements |
| ATDD-070 | implements |
| ATDD-071 | implements |
| ATDD-136 | implements |
| ATDD-260 | implements |
| ATDD-268 | implements |
| ATDD-269 | implements |
| ATDD-270 | implements |
| ATDD-271 | implements |
| ATDD-272 | implements |
| ATDD-273 | implements |
| ATDD-274 | implements |
| ATDD-321 | both |
| ATDD-335 | both |
| ATDD-335-T1-R01 | both |
| ATDD-335-T1-R02 | both |
| ATDD-335-T1-R03 | both |
| ATDD-335-T1-R04 | both |
| ATDD-335-T1-R05 | both |
| ATDD-335-T1-R06 | both |
| ATDD-335-T1-R07 | both |
| ATDD-335-T1-R08 | both |
| ATDD-335-T1-R09 | both |
| ATDD-335-T1-R10 | both |
| NFR-001 | implements |
| NFR-007 | implements |
| NFR-008 | both |
| NFR-013 | both |
| A05 | implements |
| A13 | implements |
| A14 | implements |
| D03 | implements |
| AC-083 | implements |
| AC-083-C01 | implements |
| AC-084 | implements |
| AC-084-C01 | implements |
| AC-085 | implements |
| AC-085-C01 | implements |
| AC-066 | implements |
| G-02 | implements |
| COMP-wordmark-and-frame-mark | implements |
| COMP-buttonlink | implements |
| COMP-field | implements |
| COMP-filters | implements |
| COMP-title-card | implements |
| COMP-navigation--metadata | implements |
| COMP-notice--error-summary | implements |
| COMP-progress--skeleton | implements |
| COMP-data-list--table | implements |
| COMP-confirmation-dialog | implements |
| COMP-conflict-review | implements |
| COMP-impact-preview | implements |
| COMP-session-boundary | implements |
| COMP-proposal-storyboard | implements |
| COMP-layout-primitives | implements |
| DS-POLICY-07 | implements |
| DS-POLICY-08 | implements |
| DS-TOKEN-primitive-dark-bg | implements |
| DS-TOKEN-primitive-dark-surface | implements |
| DS-TOKEN-primitive-dark-raised | implements |
| DS-TOKEN-primitive-dark-text | implements |
| DS-TOKEN-primitive-dark-muted | implements |
| DS-TOKEN-primitive-dark-line | implements |
| DS-TOKEN-primitive-dark-accent | implements |
| DS-TOKEN-primitive-dark-onaccent | implements |
| DS-TOKEN-primitive-dark-selected | implements |
| DS-TOKEN-primitive-dark-success | implements |
| DS-TOKEN-primitive-dark-warning | implements |
| DS-TOKEN-primitive-dark-danger | implements |
| DS-TOKEN-primitive-light-bg | implements |
| DS-TOKEN-primitive-light-surface | implements |
| DS-TOKEN-primitive-light-raised | implements |
| DS-TOKEN-primitive-light-text | implements |
| DS-TOKEN-primitive-light-muted | implements |
| DS-TOKEN-primitive-light-line | implements |
| DS-TOKEN-primitive-light-accent | implements |
| DS-TOKEN-primitive-light-onaccent | implements |
| DS-TOKEN-primitive-light-selected | implements |
| DS-TOKEN-primitive-light-success | implements |
| DS-TOKEN-primitive-light-warning | implements |
| DS-TOKEN-primitive-light-danger | implements |
| DS-TOKEN-primitive-font-body | implements |
| DS-TOKEN-primitive-font-display | implements |
| DS-TOKEN-primitive-font-editorial | implements |
| DS-TOKEN-primitive-space-1 | implements |
| DS-TOKEN-primitive-space-2 | implements |
| DS-TOKEN-primitive-space-3 | implements |
| DS-TOKEN-primitive-space-4 | implements |
| DS-TOKEN-primitive-space-6 | implements |
| DS-TOKEN-primitive-space-8 | implements |
| DS-TOKEN-primitive-space-12 | implements |
| DS-TOKEN-primitive-radius-control | implements |
| DS-TOKEN-primitive-radius-artwork | implements |
| DS-TOKEN-primitive-radius-pill | implements |
| DS-TOKEN-primitive-duration-short | implements |
| DS-TOKEN-primitive-type-small | implements |
| DS-TOKEN-primitive-type-body | implements |
| DS-TOKEN-primitive-type-section | implements |
| DS-TOKEN-primitive-type-title | implements |
| DS-TOKEN-primitive-container | implements |
| DS-TOKEN-primitive-shadow-overlay | implements |
| DS-TOKEN-primitive-image-ink | implements |
| DS-TOKEN-primitive-image-muted | implements |
| DS-TOKEN-primitive-image-scrim | implements |
| DS-TOKEN-primitive-image-caption | implements |
| DS-TOKEN-primitive-transparent | implements |
| DS-TOKEN-semantic-bg | implements |
| DS-TOKEN-semantic-surface | implements |
| DS-TOKEN-semantic-raised | implements |
| DS-TOKEN-semantic-text | implements |
| DS-TOKEN-semantic-muted | implements |
| DS-TOKEN-semantic-line | implements |
| DS-TOKEN-semantic-accent | implements |
| DS-TOKEN-semantic-onaccent | implements |
| DS-TOKEN-semantic-selected | implements |
| DS-TOKEN-semantic-success | implements |
| DS-TOKEN-semantic-warning | implements |
| DS-TOKEN-semantic-danger | implements |
| DS-TOKEN-semantic-font-body | implements |
| DS-TOKEN-semantic-font-display | implements |
| DS-TOKEN-semantic-radius | implements |
| DS-TOKEN-semantic-space-content | implements |
| DS-TOKEN-component-button-background | implements |
| DS-TOKEN-component-button-text | implements |
| DS-TOKEN-component-input-background | implements |
| DS-TOKEN-component-input-border | implements |
| DS-TOKEN-component-panel-background | implements |
| DS-TOKEN-component-artwork-radius | implements |
| DS-TOKEN-component-dialog-shadow | implements |
| DS-TOKEN-light-semantic-bg | implements |
| DS-TOKEN-light-semantic-surface | implements |
| DS-TOKEN-light-semantic-raised | implements |
| DS-TOKEN-light-semantic-text | implements |
| DS-TOKEN-light-semantic-muted | implements |
| DS-TOKEN-light-semantic-line | implements |
| DS-TOKEN-light-semantic-accent | implements |
| DS-TOKEN-light-semantic-onaccent | implements |
| DS-TOKEN-light-semantic-selected | implements |
| DS-TOKEN-light-semantic-success | implements |
| DS-TOKEN-light-semantic-warning | implements |
| DS-TOKEN-light-semantic-danger | implements |
| DS-TOKEN-light-semantic-font-display | implements |
| TD-C-012 | both |

## Prerequisites

- [T001 — Lock the build and isolated verification harness](../../01-reproducible-foundation/tasks/T001-lock-the-build-and-isolated-verification-harness.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Build: a clean checkout resolves the selected locks and compiles a minimal workspace twice with unchanged generated inputs; no floating dependency or image is accepted.
- [T002 — Generate and validate the shared wire contracts](../../01-reproducible-foundation/tasks/T002-generate-and-validate-the-shared-wire-contracts.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Schema: all 36 browser operations and all four RPC services generate against the single source; no duplicate schema or legacy identity alias appears.
- [T003 — Establish private service and database ownership](../../01-reproducible-foundation/tasks/T003-establish-private-service-and-database-ownership.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Ownership: each runtime can reach only its own DB with required grants; it cannot mutate audit/history, create roles/extensions or connect across owners.

All executable commands below are proposed interfaces produced by T001; none exists at planning time. Use its isolated fixture/runtime and protected synthetic access. Normal unfinished dependencies are execution prerequisites, not evidence of a failed implementation. Keep any newly discovered material contract incompatibility visible with affected source IDs and next action.

## Change surface

All listed implementation paths are proposed. Existing source documents remain read-only. Paths resolve from repository `app`.

| Repository | Action | Path | Responsibility |
|---|---|---|---|
| app | create | frontend/src/app.html | T004 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | frontend/src/routes/+layout.ts | T004 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | frontend/src/routes/+layout.svelte | T004 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | frontend/src/lib/components/ | T004 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | frontend/src/lib/design-system/ | T004 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | frontend/src/lib/state/ | T004 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | frontend/static/ | T004 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | tests/browser/flows/shell/ | T004 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |

Tests follow the owner paths above and the source test map. Frontend unit/component tests are colocated within the declared feature/component paths; JVM units use the declared src/test paths. The cross-service tests/integration project belongs only to the integration selector. If a source path has moved, record the actual producer/consumer mapping before continuing; do not silently broaden the task.

## Knowledge

Read overall shared rules → feature group → this task → the selected source sections/cases. Expand adjacent definitions only when needed to resolve a dependency.

- [3.1 Routes, screens and API dependencies](../../../04-spoolin-techdoc.md#31-routes-screens-and-api-dependencies): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.
- [3.4.1 Browser storage and restored-page state](../../../04-spoolin-techdoc.md#341-browser-storage-and-restored-page-state): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.
- [3.6 Components, accessibility and responsive behavior](../../../04-spoolin-techdoc.md#36-components-accessibility-and-responsive-behavior): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.
- [Design specification](../../../03-design-system.md), [component contracts](../../../design-system/components.md) and [token authority](../../../design-system/tokens.json): use adopted visual/interaction rules; mockup fixture code and values do not select production behavior.

The following assigned source clauses are a checklist, not new contracts. Read their exact linked context; source examples retain their stated status.

| Source knowledge | Rule or exact owned surface |
|---|---|
| [NFR-001 · L259](../../../02-PRD.md#62-quality-and-delivery-requirements) | Required project stack and real frontend/backend protocols \| A01 through A05, D03 Required \| AC-064 through AC-066; section 10 |
| [NFR-007 · L265](../../../02-PRD.md#62-quality-and-delivery-requirements) | Responsive, keyboard-usable journeys with accurate feedback and retained recoverable work \| A13/A14 Required; keyboard use Committed \| AC-083 through AC-085, AC-139, AC-147, AC-159 |
| [NFR-008 · L266](../../../02-PRD.md#62-quality-and-delivery-requirements) | Meaningful unit/edge and integration evidence proves release behavior \| A12 Required; D19 Committed \| AC-086 through AC-088 |
| [NFR-013 · L271](../../../02-PRD.md#62-quality-and-delivery-requirements) | Interfaces implement the product's validation, optional data, access, and failure behavior \| A04/A05/A14 Required support; detailed contracts Open \| AC-100 through AC-103, AC-142/AC-143, AC-148 |
| [A05 · L284](../../../02-PRD.md#63-project-requirements-coverage) | Frontend application communication through GraphQL \| NFR-001/NFR-013 / AC-066, AC-100 |
| [A13 · L292](../../../02-PRD.md#63-project-requirements-coverage) | Responsive application \| NFR-007 / AC-083 |
| [A14 · L293](../../../02-PRD.md#63-project-requirements-coverage) | Sufficient operation feedback \| NFR-007; FR-001/FR-002/FR-004/FR-009 / AC-002, AC-009, AC-018, AC-038, AC-085 |
| [D03 · L304](../../../02-PRD.md#64-project-decision-register) | GraphQL frontend and gRPC backend boundaries \| NFR-001/NFR-013 / AC-065, AC-066, AC-100 |
| [AC-083 · L481](../../../02-PRD.md#us-015-complete-tasks-across-devices) | Responsive use \| Desktop and mobile layouts are exercised \| The user completes an included application journey \| Content and primary actions remain readable/reachable without clipped controls; supported viewports are specified during design. |
| [AC-083-C01 · L481](../../../02-PRD.md#us-015-complete-tasks-across-devices) | Content and primary actions remain readable/reachable without clipped controls; supported viewports are specified during design. |
| [AC-084 · L482](../../../02-PRD.md#us-015-complete-tasks-across-devices) | Keyboard/validation \| The user uses a keyboard and submits invalid data \| Navigation and feedback are exercised \| Controls have clear labels and visible focus; feedback identifies correction, preserves permitted input, and does not rely only on color. |
| [AC-084-C01 · L482](../../../02-PRD.md#us-015-complete-tasks-across-devices) | Controls have clear labels and visible focus; feedback identifies correction, preserves permitted input, and does not rely only on color. |
| [AC-085 · L483](../../../02-PRD.md#us-015-complete-tasks-across-devices) | Pending/partial outcome \| Save, search, upload, registration, or approval is pending or partly complete \| Feedback changes \| Show the correct stage, discourage repeated submit, and offer safe correction/retry/review; never equate approval or transfer completion with publication. |
| [AC-085-C01 · L483](../../../02-PRD.md#us-015-complete-tasks-across-devices) | Show the correct stage, discourage repeated submit, and offer safe correction/retry/review; never equate approval or transfer completion with publication. |
| [AC-066 · L857](../../../02-PRD.md#102-retained-engineering-acceptance-references) | Verify frontend GraphQL and documented upload/retrieval/auth exceptions against A05; reconcile the MCP boundary \| NFR-001/NFR-013; technical design |
| [G-02 · L923](../../../02-PRD.md#122-release-gates-remain-planned) | Mandated stack/protocols and chosen integration approach work \| NFR-001/NFR-002/NFR-013; AC-064 through AC-067, AC-100 through AC-103 \| Planned in techdoc |
| [COMP-wordmark-and-frame-mark · L7](../../../design-system/components.md#shared-component-contracts) | Wordmark and frame mark \| Full wordmark; 16/24/48px compact icon \| Decorative frame hidden when adjacent name labels it \| Identity |
| [COMP-buttonlink · L8](../../../design-system/components.md#shared-component-contracts) | Button/link \| Primary, secondary, danger; action versus navigation \| Default, hover, visible focus, selected, disabled, pending; 44px target and explicit reason for disabled \| Buttons |
| [COMP-field · L9](../../../design-system/components.md#shared-component-contracts) | Field \| Label, input/textarea, helper, error \| Required/optional, invalid, disabled; associated help/errors; no password recovery persistence \| Fields |
| [COMP-filters · L10](../../../design-system/components.md#shared-component-contracts) | Filters \| Title type, multi-genre, year/month, search \| Native checked state, OR genres and AND categories; clear text preserves others; month depends on year \| Filters |
| [COMP-title-card · L11](../../../design-system/components.md#shared-component-contracts) | Title card \| Artwork/fallback, full title, type, date precision, metadata \| Long/missing/unknown; active record identity independent of text \| Artwork |
| [COMP-navigation--metadata · L12](../../../design-system/components.md#shared-component-contracts) | Navigation / metadata \| Role-aware links, breadcrumb, badge, definition list, provenance disclosure \| Visible current context; unknown distinct from zero; no direct private archive disclosure \| Navigation |
| [COMP-notice--error-summary · L13](../../../design-system/components.md#shared-component-contracts) | Notice / error summary \| Status title, explanation, safe next action; field-linked summary \| Pending/confirmed/failed/uncertain/denied; persistent, live when changed, no color-only state \| Feedback |
| [COMP-progress--skeleton · L14](../../../design-system/components.md#shared-component-contracts) | Progress / skeleton \| Current criterion or operation context, measured progress, separate processing \| No 100%-equals-success; quiet reduced-motion behavior \| Progress |
| [COMP-data-list--table · L15](../../../design-system/components.md#shared-component-contracts) | Data list / table \| Headings, stable record ID/context, actions appropriate to role \| Wrap long values; no identity collapse; no arbitrary history restore \| History |
| [COMP-confirmation-dialog · L16](../../../design-system/components.md#shared-component-contracts) | Confirmation dialog \| Current title/impact/consequences, cancel, explicit final action \| Focus starts at cancel; Tab contained; Escape cancels; opener receives focus \| Dialog |
| [COMP-conflict-review · L17](../../../design-system/components.md#shared-component-contracts) | Conflict review \| Current saved version, retained draft, intentional review, save \| Stale/removed/denied/uncertain; no force overwrite or duplicate submission \| Editor |
| [COMP-impact-preview · L18](../../../design-system/components.md#shared-component-contracts) | Impact preview \| Target plus active/archived movie and TV effects \| Changed impact invalidates prior confirmation; archive before delete; restore before editing \| Updated impact |
| [COMP-session-boundary · L19](../../../design-system/components.md#shared-component-contracts) | Session boundary \| Identity context, permitted intended destination, protected draft lock \| Checking/expired/unavailable/denied; failed/uncertain sign-out; cross-account isolation \| Session flows · Draft fixture harness |
| [COMP-proposal-storyboard · L20](../../../design-system/components.md#shared-component-contracts) | Proposal storyboard \| Exact publishable values, source/unknowns, human correction/rejection/approval \| Revision-specific approval; denial/uncertain outcome; no autonomous creation \| ChatGPT · Claude |
| [COMP-layout-primitives · L21](../../../design-system/components.md#shared-component-contracts) | Layout primitives \| Container, stack/grid, form groups, spacing, art radius \| Shared in both themes; 1440/390/320 review contexts \| Spacing |
| [DS-POLICY-07 · L72](../../../03-design-system.md#typography-layout-and-shape) | Use 16px body/control text with 1.55 line height, 13px supporting text, 25px section headings, and 48px page titles scaling to 36px on narrow screens. The catalog hero may use 58px desktop/42px narrow. Light serif titles retain the same box and hierarchy, with wrapping allowed. Keep prose approximately 65–80 characters per line. Never truncate the only accessible title or a retained invalid input; full names, Unicode, and synopsis line breaks must remain readable. Typography specimens use actual fixture content. |
| [DS-POLICY-08 · L74](../../../03-design-system.md#typography-layout-and-shape) | Shared spacing primitives: 4, 8, 12, 16, 24, 32, 48px. Use 8/12px within controls, 16/24px within groups, 32/48px between sections. This consolidated rhythm is shared by both selected themes. Main content max-width is 1280px. Poster grids use six columns on wide desktop, three below 1000px, and two below 700px. Sidebars collapse to contextual headings; navigation wraps without hiding essential actions. Form columns become a single column. Detail art may float beside the title on narrow views, while synopsis returns to full width. |
| [DS-TOKEN-primitive-dark-bg · L1](../../../design-system/tokens.json) | #101820 |
| [DS-TOKEN-primitive-dark-surface · L1](../../../design-system/tokens.json) | #18232e |
| [DS-TOKEN-primitive-dark-raised · L1](../../../design-system/tokens.json) | #223140 |
| [DS-TOKEN-primitive-dark-text · L1](../../../design-system/tokens.json) | #f3f6f7 |
| [DS-TOKEN-primitive-dark-muted · L1](../../../design-system/tokens.json) | #b0becb |
| [DS-TOKEN-primitive-dark-line · L1](../../../design-system/tokens.json) | #657787 |
| [DS-TOKEN-primitive-dark-accent · L1](../../../design-system/tokens.json) | #8fd8ee |
| [DS-TOKEN-primitive-dark-onaccent · L1](../../../design-system/tokens.json) | #10202a |
| [DS-TOKEN-primitive-dark-selected · L1](../../../design-system/tokens.json) | #29434f |
| [DS-TOKEN-primitive-dark-success · L1](../../../design-system/tokens.json) | #ace2c4 |
| [DS-TOKEN-primitive-dark-warning · L1](../../../design-system/tokens.json) | #f0d29a |
| [DS-TOKEN-primitive-dark-danger · L1](../../../design-system/tokens.json) | #ffb5af |
| [DS-TOKEN-primitive-light-bg · L1](../../../design-system/tokens.json) | #f5f3ef |
| [DS-TOKEN-primitive-light-surface · L1](../../../design-system/tokens.json) | #ffffff |
| [DS-TOKEN-primitive-light-raised · L1](../../../design-system/tokens.json) | #e9e5df |
| [DS-TOKEN-primitive-light-text · L1](../../../design-system/tokens.json) | #26232b |
| [DS-TOKEN-primitive-light-muted · L1](../../../design-system/tokens.json) | #625c69 |
| [DS-TOKEN-primitive-light-line · L1](../../../design-system/tokens.json) | #817987 |
| [DS-TOKEN-primitive-light-accent · L1](../../../design-system/tokens.json) | #643f67 |
| [DS-TOKEN-primitive-light-onaccent · L1](../../../design-system/tokens.json) | #ffffff |
| [DS-TOKEN-primitive-light-selected · L1](../../../design-system/tokens.json) | #eee4ef |
| [DS-TOKEN-primitive-light-success · L1](../../../design-system/tokens.json) | #2e674a |
| [DS-TOKEN-primitive-light-warning · L1](../../../design-system/tokens.json) | #7c5411 |
| [DS-TOKEN-primitive-light-danger · L1](../../../design-system/tokens.json) | #a82c3a |
| [DS-TOKEN-primitive-font-body · L1](../../../design-system/tokens.json) | "Avenir Next", "Segoe UI", Arial, sans-serif |
| [DS-TOKEN-primitive-font-display · L1](../../../design-system/tokens.json) | "Avenir Next", "Segoe UI", Arial, sans-serif |
| [DS-TOKEN-primitive-font-editorial · L1](../../../design-system/tokens.json) | Didot, "Times New Roman", serif |
| [DS-TOKEN-primitive-space-1 · L1](../../../design-system/tokens.json) | 4px |
| [DS-TOKEN-primitive-space-2 · L1](../../../design-system/tokens.json) | 8px |
| [DS-TOKEN-primitive-space-3 · L1](../../../design-system/tokens.json) | 12px |
| [DS-TOKEN-primitive-space-4 · L1](../../../design-system/tokens.json) | 16px |
| [DS-TOKEN-primitive-space-6 · L1](../../../design-system/tokens.json) | 24px |
| [DS-TOKEN-primitive-space-8 · L1](../../../design-system/tokens.json) | 32px |
| [DS-TOKEN-primitive-space-12 · L1](../../../design-system/tokens.json) | 48px |
| [DS-TOKEN-primitive-radius-control · L1](../../../design-system/tokens.json) | 8px |
| [DS-TOKEN-primitive-radius-artwork · L1](../../../design-system/tokens.json) | 12px |
| [DS-TOKEN-primitive-radius-pill · L1](../../../design-system/tokens.json) | 999px |
| [DS-TOKEN-primitive-duration-short · L1](../../../design-system/tokens.json) | 140ms |
| [DS-TOKEN-primitive-type-small · L1](../../../design-system/tokens.json) | 13px |
| [DS-TOKEN-primitive-type-body · L1](../../../design-system/tokens.json) | 16px |
| [DS-TOKEN-primitive-type-section · L1](../../../design-system/tokens.json) | 25px |
| [DS-TOKEN-primitive-type-title · L1](../../../design-system/tokens.json) | 48px |
| [DS-TOKEN-primitive-container · L1](../../../design-system/tokens.json) | 1280px |
| [DS-TOKEN-primitive-shadow-overlay · L1](../../../design-system/tokens.json) | 0 20px 60px #0005 |
| [DS-TOKEN-primitive-image-ink · L1](../../../design-system/tokens.json) | #f8fafb |
| [DS-TOKEN-primitive-image-muted · L1](../../../design-system/tokens.json) | #e1e8ee |
| [DS-TOKEN-primitive-image-scrim · L1](../../../design-system/tokens.json) | #0a121b |
| [DS-TOKEN-primitive-image-caption · L1](../../../design-system/tokens.json) | #101820 |
| [DS-TOKEN-primitive-transparent · L1](../../../design-system/tokens.json) | transparent |
| [DS-TOKEN-semantic-bg · L1](../../../design-system/tokens.json) | {primitive.dark-bg} |
| [DS-TOKEN-semantic-surface · L1](../../../design-system/tokens.json) | {primitive.dark-surface} |
| [DS-TOKEN-semantic-raised · L1](../../../design-system/tokens.json) | {primitive.dark-raised} |
| [DS-TOKEN-semantic-text · L1](../../../design-system/tokens.json) | {primitive.dark-text} |
| [DS-TOKEN-semantic-muted · L1](../../../design-system/tokens.json) | {primitive.dark-muted} |
| [DS-TOKEN-semantic-line · L1](../../../design-system/tokens.json) | {primitive.dark-line} |
| [DS-TOKEN-semantic-accent · L1](../../../design-system/tokens.json) | {primitive.dark-accent} |
| [DS-TOKEN-semantic-onaccent · L1](../../../design-system/tokens.json) | {primitive.dark-onaccent} |
| [DS-TOKEN-semantic-selected · L1](../../../design-system/tokens.json) | {primitive.dark-selected} |
| [DS-TOKEN-semantic-success · L1](../../../design-system/tokens.json) | {primitive.dark-success} |
| [DS-TOKEN-semantic-warning · L1](../../../design-system/tokens.json) | {primitive.dark-warning} |
| [DS-TOKEN-semantic-danger · L1](../../../design-system/tokens.json) | {primitive.dark-danger} |
| [DS-TOKEN-semantic-font-body · L1](../../../design-system/tokens.json) | {primitive.font-body} |
| [DS-TOKEN-semantic-font-display · L1](../../../design-system/tokens.json) | {primitive.font-display} |
| [DS-TOKEN-semantic-radius · L1](../../../design-system/tokens.json) | {primitive.radius-control} |
| [DS-TOKEN-semantic-space-content · L1](../../../design-system/tokens.json) | {primitive.space-12} |
| [DS-TOKEN-component-button-background · L1](../../../design-system/tokens.json) | {semantic.accent} |
| [DS-TOKEN-component-button-text · L1](../../../design-system/tokens.json) | {semantic.onaccent} |
| [DS-TOKEN-component-input-background · L1](../../../design-system/tokens.json) | {semantic.surface} |
| [DS-TOKEN-component-input-border · L1](../../../design-system/tokens.json) | {semantic.line} |
| [DS-TOKEN-component-panel-background · L1](../../../design-system/tokens.json) | {semantic.surface} |
| [DS-TOKEN-component-artwork-radius · L1](../../../design-system/tokens.json) | {primitive.radius-artwork} |
| [DS-TOKEN-component-dialog-shadow · L1](../../../design-system/tokens.json) | {primitive.shadow-overlay} |
| [DS-TOKEN-light-semantic-bg · L1](../../../design-system/tokens.json) | {primitive.light-bg} |
| [DS-TOKEN-light-semantic-surface · L1](../../../design-system/tokens.json) | {primitive.light-surface} |
| [DS-TOKEN-light-semantic-raised · L1](../../../design-system/tokens.json) | {primitive.light-raised} |
| [DS-TOKEN-light-semantic-text · L1](../../../design-system/tokens.json) | {primitive.light-text} |
| [DS-TOKEN-light-semantic-muted · L1](../../../design-system/tokens.json) | {primitive.light-muted} |
| [DS-TOKEN-light-semantic-line · L1](../../../design-system/tokens.json) | {primitive.light-line} |
| [DS-TOKEN-light-semantic-accent · L1](../../../design-system/tokens.json) | {primitive.light-accent} |
| [DS-TOKEN-light-semantic-onaccent · L1](../../../design-system/tokens.json) | {primitive.light-onaccent} |
| [DS-TOKEN-light-semantic-selected · L1](../../../design-system/tokens.json) | {primitive.light-selected} |
| [DS-TOKEN-light-semantic-success · L1](../../../design-system/tokens.json) | {primitive.light-success} |
| [DS-TOKEN-light-semantic-warning · L1](../../../design-system/tokens.json) | {primitive.light-warning} |
| [DS-TOKEN-light-semantic-danger · L1](../../../design-system/tokens.json) | {primitive.light-danger} |
| [DS-TOKEN-light-semantic-font-display · L1](../../../design-system/tokens.json) | {primitive.font-editorial} |
| [TD-C-012 · L1017](../../../04-spoolin-techdoc.md#36-components-accessibility-and-responsive-behavior) | Shared semantic components, escaped text, error summaries and announcements; dialog Cancel-first trap/Escape/restore, route heading focus/title, 44px targets, 320/390/1440 widths, 200% zoom, reduced motion, three browsers and screen-reader smoke. |

## Implementation

1. Implement shared semantic token adapters, exact SpoolIn wordmark/paired-frame asset and dark/light theme selection. Generate CSS from tokens.json; keep component geometry shared and only the approved color/display-font override.
2. Create shared Field/ErrorSummary/Notice/OperationFeedback/Dialog/ArtworkSlot/layout components with typed props and escaped text. Dialog focus starts on Cancel, traps Tab, supports Escape and returns focus; route headings/title and live status announcements remain meaningful.
3. Create the client-only layout and capability-aware navigation interface. Public shell may render before identity; protected projections wait for T007 SessionBoundary integration. No demo role/state selector becomes a production feature.
4. Use production localStorage only for validated spoolin.theme dark/light. Follow OS preference when unset, remove invalid values, and survive storage exceptions in memory. Do not port prototype storage keys or fixture JavaScript.
5. Verify component states, both themes, 1440/390/320 widths, long Unicode names, missing artwork and safe markup-looking text before downstream feature composition.

## Development guidance

Follow [shared execution rules](../../../06-PLAN.md#shared-execution-rules). Keep feature command/query/validation/projection/persistence close, constructor dependencies explicit and DTOs immutable. Generated contracts are not entities or authorization. Do not import another service's repository, copy a schema, invent a fourth domain service, or bypass the owner with fixture SQL.

Use parameter binding, fixed identifiers/allowlisted projections and safe text sinks at the relevant boundary. Keep account secrets, raw files, CSRF, approval challenges and private payloads out of retained evidence. New source policy requires a recorded owner decision; an unavailable client/runtime is not permission to weaken acceptance. Reuse shared components and preserve complete source state semantics. No fixture role/state picker or in-app AI approval queue is introduced. Do not alter expected values, delete negative rows or replace visual references merely to make checks pass.

## Acceptance

- **T004-A1:** Theme: switching preserves focus, input, filters and route state; unset storage follows system preference while dark remains the base token palette.
- **T004-A2:** Controls: keyboard-only invalid submission points to associated correction without color-only signals or lost content; dialog cancellation changes no business state.
- **T004-A3:** Presentation: shared components show readable full identity at 320px with 44px targets and approved typography; generated preview imagery is a fixture reference only.

Verification ownership is scoped: the exact cases and case rows in Tests require their full stated boundary. Shared requirement/AC/contract clauses require this task’s owned contribution and local assertions; their combined outcome is reconciled by the case, feature and release owners. An implementation-only contribution stops at its owned change and local tests; a downstream full-case result is not a condition for this task to finish.

## Tests

Use checks alongside implementation because build/visual/runtime composition must be exercised as its interfaces appear; acceptance still governs before completion. Derive expected values independently from source fixtures, not implementation output.

Use [ATDD fixtures and approach](../../../05-atdd.md#approach-and-fixtures) and [C-RESET](../../../05-atdd.md#c-reset-shared-cleanup). Each case/table/recipe subrow resets its own F-STACK/F-ACTORS state; pin time/randomness and use observable barriers for races rather than sleep-only synchronization. Preserve sanitized failure and fresh-reader state, then clean only that UUID namespace.

### Assigned case assertions

These are full verification assignments at the source level. Each linked case's complete Test data, Preconditions, Given/When/Then, Verification and Cleanup remains binding; recipe variants need separate results even when they have no printed row ID.

| Case / level | Concrete input or fixture | Required assertion and observation boundary |
|---|---|---|
| [ATDD-321](../../../05-atdd.md#case-atdd-321-render-exact-untrusted-text-through-safe-component-sinks) / Component | F-INJECTION script/img-event/closing-tag/attribute text, plainUnicode; URLs validhttp(s), javascript/data and encoded/whitespace scheme variants. | Then permitted text appears exactly as text without injected elements/event handlers/evaluation And disallowed URL schemes are not actionable links or resource sources And CSP is additional protection rather than the only evidence of safe rendering Observation: Inspect real DOM textContent/element/attribute construction and attempted navigation with controlled responses. Retained payload bytes are not stripped to manufacture safety; actual stored/browser flows remain complementary proof. |
| [ATDD-335](../../../05-atdd.md#case-atdd-335-persist-only-validated-theme-preferences) / Unit | Use exact production key `spoolin.theme`, controlled system light/dark, and independent stored/input values: `dark`, `light`, absent, `system`, empty, ` DARK `, `DARK`, `0`, `false`, `{"theme":"dark"}`, `__proto__`, `<script>alert(1)</script>` and `dark; color:red`. Storage get/set/remove throw independently; no real secret is used. | Then stored invalid or missing values follow the system preference without unsafe interpretation And a rejected selection changes no theme or stored value And a valid selection updates memory even when persistence fails and no other key/store is used Observation: Independent row \| Initial input \| Expected theme / storage observation \| \| --- \| --- \| --- \| \| Stored dark \| system light, stored `dark` \| dark; read only spoolin.theme; no rewrite \| \| Stored light \| system dark, stored `light` \| light; no rewrite \| \| Missing \| absent key with system dark, then light in a reset row \| matching system theme; no new entry written without explicit valid selection \| \| Invalid stored \| each rejected exact value with system light/dark \| system theme only; remove invalid spoolin.theme when permitted, no executable/style/selector value \| \| Read unavailable \| get throws; system light \| light in memory; no new entry or unsafe storage fallback \| \| Removal unavailable \| invalid stored; remove throws \| system theme remains usable; no new entry, and failure cannot select invalid value \| \| Valid write \| choose dark, then light in independent rows \| requested value in memory; only setItem(spoolin.theme, exact allowed value) \| \| Write unavailable \| stored dark/current dark; choose light; set throws before change \| light in memory remains usable; no alternative store/key \| \| Invalid write \| current light/stored dark; each invalid request \| current light and stored dark unchanged; no write \| \| Prototype alias \| only movie-design-theme=dark; system light \| light; prototype key is never read as a production alias \| Compare exact rule outputs and storage-call arguments with this hand-defined table; use no trimming, case folding, JSON coercion or prototype alias. The preference carries no actor/credential/draft data and cannot grant permission. This unit witness does not establish browser storage, rendered focus/theme behavior or session isolation; real complementary cases supply those boundaries. |

### Assigned rows and shared proof conditions

| Exact source row | Required assertion / condition |
|---|---|
| [ATDD-335-T1-R01 · L13799](../../../05-atdd.md#case-atdd-335-persist-only-validated-theme-preferences) | Stored dark; system light, stored `dark`; dark; read only spoolin.theme; no rewrite |
| [ATDD-335-T1-R02 · L13800](../../../05-atdd.md#case-atdd-335-persist-only-validated-theme-preferences) | Stored light; system dark, stored `light`; light; no rewrite |
| [ATDD-335-T1-R03 · L13801](../../../05-atdd.md#case-atdd-335-persist-only-validated-theme-preferences) | Missing; absent key with system dark, then light in a reset row; matching system theme; no new entry written without explicit valid selection |
| [ATDD-335-T1-R04 · L13802](../../../05-atdd.md#case-atdd-335-persist-only-validated-theme-preferences) | Invalid stored; each rejected exact value with system light/dark; system theme only; remove invalid spoolin.theme when permitted, no executable/style/selector value |
| [ATDD-335-T1-R05 · L13803](../../../05-atdd.md#case-atdd-335-persist-only-validated-theme-preferences) | Read unavailable; get throws; system light; light in memory; no new entry or unsafe storage fallback |
| [ATDD-335-T1-R06 · L13804](../../../05-atdd.md#case-atdd-335-persist-only-validated-theme-preferences) | Removal unavailable; invalid stored; remove throws; system theme remains usable; no new entry, and failure cannot select invalid value |
| [ATDD-335-T1-R07 · L13805](../../../05-atdd.md#case-atdd-335-persist-only-validated-theme-preferences) | Valid write; choose dark, then light in independent rows; requested value in memory; only setItem(spoolin.theme, exact allowed value) |
| [ATDD-335-T1-R08 · L13806](../../../05-atdd.md#case-atdd-335-persist-only-validated-theme-preferences) | Write unavailable; stored dark/current dark; choose light; set throws before change; light in memory remains usable; no alternative store/key |
| [ATDD-335-T1-R09 · L13807](../../../05-atdd.md#case-atdd-335-persist-only-validated-theme-preferences) | Invalid write; current light/stored dark; each invalid request; current light and stored dark unchanged; no write |
| [ATDD-335-T1-R10 · L13808](../../../05-atdd.md#case-atdd-335-persist-only-validated-theme-preferences) | Prototype alias; only movie-design-theme=dark; system light; light; prototype key is never read as a production alias |

Partial implementation contributions: [ATDD-069](../../../05-atdd.md#case-atdd-069-responsive-use), [ATDD-070](../../../05-atdd.md#case-atdd-070-keyboardvalidation), [ATDD-071](../../../05-atdd.md#case-atdd-071-pendingpartial-outcome), [ATDD-136](../../../05-atdd.md#case-atdd-136-keep-browser-navigation-and-api-failures-distinct), [ATDD-260](../../../05-atdd.md#case-atdd-260-inspect-management-state-transitions-and-feedback), [ATDD-268](../../../05-atdd.md#case-atdd-268-one-theme-switch-preserves-the-same-interaction), [ATDD-269](../../../05-atdd.md#case-atdd-269-use-adopted-spoolin-identity-and-discovery-copy), [ATDD-270](../../../05-atdd.md#case-atdd-270-keep-typography-and-layout-readable-under-stress), [ATDD-271](../../../05-atdd.md#case-atdd-271-verify-contrast-on-actual-control-and-artwork-surfaces), [ATDD-272](../../../05-atdd.md#case-atdd-272-confirm-consequences-with-keyboard-and-accessible-errors), [ATDD-273](../../../05-atdd.md#case-atdd-273-respect-zoom-reduced-motion-and-browser-differences), [ATDD-274](../../../05-atdd.md#case-atdd-274-keep-missing-art-and-alternative-text-meaningful). These require local unit/owner tests here; their real end-to-end/system pass remains with the root verification owner. Do not wait for downstream completion to finish this local task.

Unit/component tests prove only isolated logic or the controlled component. PostgreSQL constraints/locks/commit/crash tests use PostgreSQL18.6 and fresh independent reads; transport tests use actual TCP mTLS/gRPC; browser/client and provider claims need their named real boundary. Direct DDL probes prove only declared SQL constraints, not permission, policy or cryptographic validity.

## Validation

Workdir is `/Users/mekari/Developer/personal-project/movie-app` for every command/procedure. T001 creates documented `--task` selectors and maps this task to its listed test paths/levels. Required empty/unavailable selections fail with setup exit2. Aggregate release commands do not infer passes for unselected cases.

| Check | Method or command | Working directory | Expected result | Required |
|---|---|---|---|---|
| local-tests | `bash ops/bin/test-unit.sh --task T004` — planned, produced by T001; generated contracts/build and this task's isolated tests required | `/Users/mekari/Developer/personal-project/movie-app` | Owned build/validation rules/components pass with independently fixed outcomes; no normal DB access | yes |
| visual-review | Capture every listed state under matched source/actual conditions and compare using the contract below | `/Users/mekari/Developer/personal-project/movie-app` | Required visual differences resolved at current revision; no overflow/missing identity | yes |
| interaction-accessibility | Exercise keyboard/focus/labels/errors/live regions/dialogs, zoom/reduced motion and source browser/host matrix independently of pixels | `/Users/mekari/Developer/personal-project/movie-app` | Meaningful accessible interaction and safe recovery, with unavailable contexts kept unverified | yes |
| self-review | Implementer compares baseline/change, exact source clauses, tests and negative effects; records findings and corrections | `/Users/mekari/Developer/personal-project/movie-app` | All task criteria addressed; required findings fixed and affected checks rerun | yes |
| independent-review | Fresh reviewer reads task/group/root and precise sources, inspects baseline/change and actual evidence without trusting implementer verdict | `/Users/mekari/Developer/personal-project/movie-app` | Source compliance/code quality/risk checks complete; required findings resolved for reviewed revision | yes |

Only isolated build/unit/component validation is required here. The source case/row owners named in Traceability retain complete real-service/client proof; no empty integration selection is required for local completion. Graphical captures below still inspect a rendered component/reference under the declared contexts.

## Review and evidence

The implementer self-reviews first. A fresh independent reviewer then receives this task, inherited constraints, exact source clauses, baseline and resulting change, actual tests and sanitized evidence. Record distinct reviewer identities, reviewed revision, each finding as required/advisory, correction and recheck; labels alone do not establish independence. Scrutinize completeness, source fidelity, regression risk, usable instructions and the actual proof boundary. Implement → check → self-review → independent review → correct → rerun affected checks. Broaden testing only for a new concern. If no fresh reviewer is available, leave In review with exact next action; self-review cannot replace it.

For each state below, load the linked adopted reference and the actual route/panel (technical §3.1.1); use its source actor/fixture/trigger from ATDD. Review dark and light at1440/390/320CSSpx, current browser version/build, controlled time/data and matched state. Retain expected reference plus actual capture, source hash/code revision/environment/trigger, side-by-side comparison and verdict for layout, hierarchy, typography, colors, spacing, complete identity/content, overflow, artwork treatment and feedback. Narrow documented masks cannot hide required behavior.

Check keyboard/focus/labels/errors/announcements,200%zoom,reduced motion,touch and Chromium/Firefox/WebKit plus screen-reader smoke separately. For AI states include actual Claude/ChatGPT host context at T032/T033; standalone resource evidence cannot prove host isolation. Existing mockups are adopted references, not automatic pixel baselines or production test passes.

| Flow/state reference | Actor and fixture condition | Trigger and expected comparison |
|---|---|---|
| [Shared component specimens](../../../design-system/mockup/components.html) | Visitor/Viewer/Maintainer contexts; long/unknown/invalid content; both themes/widths | Exercise every assigned component variant including focus, pending, disabled reason, conflict, dialog and fallback |

## Evidence

| Check | Result | Artifact | Revision | Environment | Reviewer |
|---|---|---|---|---|---|
| local-tests | Passed: 93/93 | [Final receipt](/private/tmp/spoolin-shell-t004-correction/.ai/evidence/run-422ebc0a832340619261d437b7bbec38/receipt.json) | `572f5a2411086f0248d27f0d90613cd21b022f36` | Pinned macOS toolchain | shell-implementer |
| quality-gates | Blocked: 41 passed, 8 inapplicable, 1 failed | [Final receipt](/private/tmp/spoolin-shell-t004-correction/.ai/evidence/run-422ebc0a832340619261d437b7bbec38/receipt.json) | `572f5a2411086f0248d27f0d90613cd21b022f36` | Chromium executable path incomplete; 24 launch failures, 8 Firefox/WebKit passed | shell-implementer |
| visual-review | Reviewed; minor consistency differences | [Independent review](/private/tmp/spoolin-shell-t004-correction/.ai/evidence/t004-independent-review.json) | `572f5a2411086f0248d27f0d90613cd21b022f36` | Original paired desktop/mobile captures and held recordings | /root/reviewer |
| interaction-accessibility | Blocked: native screen-reader and browser zoom proof unavailable | [Reader attempt](/private/tmp/spoolin-shell-t004-correction/.ai/evidence/t004-native-screen-reader-attempt.json), [Zoom attempt](/private/tmp/spoolin-shell-t004-correction/.ai/evidence/t004-native-zoom-attempt.json) | `572f5a2411086f0248d27f0d90613cd21b022f36` | Reader produced no observable output; Mac locked during native zoom attempt | /root/reviewer |
| self-review | Complete; required gaps retained | [Self-review](/private/tmp/spoolin-shell-t004-correction/.ai/evidence/t004-self-review.json) | `572f5a2411086f0248d27f0d90613cd21b022f36` | Isolated development fixtures | shell-implementer |
| independent-review | Blocked; earlier source findings resolved | [Review ledger](/private/tmp/spoolin-shell-t004-correction/.ai/evidence/t004-independent-review.json) | `572f5a2411086f0248d27f0d90613cd21b022f36` | Actual test reports, original images and decoded recording frames | /root/reviewer |

Execution update — 15 October 2026: Implementation is retained on branch `codex/application-shell-correction` in `/private/tmp/spoolin-shell-t004-correction` and is not integrated or published. Input fingerprint: `429010ed80cda5f9e0ea549c9131f5f5d81e3471100cd9d8dc8437b2726f8c46`. Earlier same-input browser proof passed 32/32; the final required receipt failed Chromium startup because its invocation supplied an incomplete executable path. The secret scan passed after independently verified duplicate source-hash outputs were archived with a manifest. No scanner policy was weakened.

G02 stopped after two failed correction rounds. Retain all branches, receipts and original failed/limited captures. Next action: inspect the retained final receipt and establish a revised work-group decision under the workflow correction limit; validate the full Chromium executable path before rerunning the unchanged required contract. Run actual 200% native browser zoom and screen-reader checks in an available assistive environment. These gaps prevent Done and draft delivery of shell changes. T005 may proceed separately from accepted T002/T003 without this unaccepted shell implementation.

## Completion and handoff

- Finish the owned changes and all verification-role acceptance/row assertions; record actual changed files and source-contract dispositions.
- Reach Ready instructions and Passed evidence for every required check, including self-review, fresh independent review and applicable visual/interaction checks. Any unresolved required oracle prevents the affected task from Done.
- Record exact revision, generated/artifact locations, commands/results, findings/rechecks, fixture cleanup and any unverified downstream proof.
- As integration owner, reconcile every group member's current local evidence with the group's combined real-boundary acceptance. Cross-feature/release checks remain owned by the release-assurance group; they are not extra prerequisites for these upstream local tasks.
- If unfinished, retain status In progress/In review or a genuine external Blocked prerequisite; list the exact unresolved finding, last verified state, attempted fixes and next file/command/action. Budget exhaustion alone is not an external blocker.
- Initial next action: read the prerequisite outputs, verify source fingerprints and task eligibility, then perform Implementation step1 under the separately authorized implementation request.
