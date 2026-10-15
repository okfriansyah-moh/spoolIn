# SpoolIn design system

**One design system, with dark and light themes.** Cinema at night (A) supplies the dark palette and modern display treatment; Film gallery (C) supplies the light palette and editorial display treatment. Both use the same component definitions, layouts, responsive rules, information hierarchy, and interaction contracts.

The owner selected this combination in chat on 10 October 2026: “option A as a dark theme, and option C as light theme” and “only 1 generated design system.” This supersedes the three-way selection checkpoint. Projection room (B) is not adopted. The earlier fixed porcelain/navy/cobalt palette, Georgia typography, and light-only prescription remain withdrawn. Spacing/corners below are routine consolidation of the selected pair, not reinstatement of that withdrawn identity.

[Open the gallery](design-system/mockup/index.html) · [Visual components](design-system/mockup/components.html) · [Component contracts](design-system/components.md) · [PRD reconciliation](design-system/coverage.md) · [UI preview](design-system/mockup/assets/design-preview.jpg).

## Use this package for technical design

Read this specification together with [the PRD](02-PRD.md). The PRD defines product scope and acceptance; this document defines the selected visual identity, visible behavior, responsive rules, and component responsibilities. Use [the UI gallery](design-system/mockup/index.html) to inspect all 97 states across 12 flows, [component contracts](design-system/components.md) for reusable UI behavior, and [PRD coverage](design-system/coverage.md) to trace requirements to examples or engineering responsibilities.

The design-system handoff is complete for technical-document creation. Authentication, APIs, data/storage models, concurrency, security enforcement, deployment, and production tests are decisions for that technical document and implementation. Their visible UI contracts are specified here. Illustrative settings must not silently become committed product policy. If an example conflicts with the PRD, the PRD takes precedence.

## Authority, scope, and targets

Product authority: [PRD v2.4, Ready for technical design](02-PRD.md), SHA-256 `d31018db43b0b2bc4ead0b7bbd3fd6a32d2e2c232fbe5720a6205d2b7676e895`. Product behavior outranks examples and design recommendations. [Coverage JSON](design-system/coverage.json) records all 17 FRs, 16 NFRs, and 159 ACs, including specific technical dispositions. A visual mapping is not a passing production test.

| Decision | Selected target / evidence |
|---|---|
| Product | Movie/TV discovery and movie/person management application; PRD 1, 3, 5 |
| Platform | Responsive web; narrow views are not a native mobile app |
| Input | Keyboard, pointer, touch; PRD NFR-007 and AC-083/084 |
| Frontend | Svelte + TypeScript, required by PRD A01/NFR-001/AC-064 |
| Backend | Kotlin/Spring Boot; GraphQL/gRPC constraints stay technical-design responsibilities |
| Library | Open. Optional shadcn-svelte mappings do not adopt or install a library |
| Review runtime | Static HTML/CSS with small vanilla JavaScript, entirely local |
| Theme targets | `dark` and `light` identify two rendering contexts of this same application |
| Review widths | 1440px desktop, 390px narrow, 320px stress; not a claim of universal device support |

Visitors and Viewers share public catalog-reading access. Maintainers also manage movies, shared people, credits, artwork, separate archives, and read-only history. Cast/Creator describe contributions, never application roles. Public credits require both active title and active person. TV is read-only except indirect shared-person effects. Archive controls are private; account PII is never a person profile or catalog search result.

Streaming, ratings/reviews, watchlists, social functions, episode tracking, direct TV management, role/account administration, password recovery, email verification, in-app AI approval queues, and a Maintainer configuration screen are excluded. The fictional fixtures are not the required 100-movie/100-series seed.

## Identity and voice

**SpoolIn** is named after the spool that holds a length of film. Its brand promise is **“Unspooling stories and discovering what’s behind them.”** The discovery homepage uses **“Every reel holds a story.”** as its headline and the brand promise as supporting copy. [Brand history](design-system/brand-history.md) records the origin, narrative, spelling, and adopted copy.

Use “SpoolIn” as the wordmark and an original paired-frame mark at small sizes. Preserve its silhouette; never substitute a play button that implies streaming. [Icon](design-system/mockup/assets/icon.svg) and [component identity specimen](design-system/mockup/components.html#identity) show browser-icon sizes of 16, 24, and 48px. The compact icon uses the stable dark brand tile across both themes; surrounding UI changes theme.

The product feels cinematic through artwork, restrained contrast, typography, and composition. Catalog discovery gives posters prominence; forms use calm reading surfaces and concise labels. Use sentence case, direct verbs, concrete record names, and honest unknowns. Say “Save result not confirmed” when uncertain, “Changes were not saved” only after confirmed failure, and “Changes saved” only after confirmation. Do not hide consequential feedback in disappearing toasts.

## One token authority, two themes

[tokens.json](design-system/tokens.json) is the sole value authority. [tokens.css](design-system/mockup/tokens.css) is its CSS representation: each `layer.key` becomes `--layer-key`, and `{layer.key}` becomes `var(--layer-key)`. Update both together when tokens change. Flat primitive → semantic → component aliases separate raw values, intent, and anatomy. Default values are dark. The light override replaces semantic aliases under `[data-design-target="light"]`; all components resolve those same semantic names. No component maintains separate dark/light markup.

All JSON dimensions use explicit CSS strings (`px`, `ms`) to avoid implicit-unit conversion. Theme switching changes color and the display-family alias, while preserving the selected state, focus, filters, and typed text. It neither submits nor resets a form. Body/control type, sizing, spacing and component geometry stay shared. The initial preference follows the system when no review preference exists; only the non-sensitive theme choice is persisted in the prototype. Theme persistence is unrelated to authentication or draft recovery.

| Semantic job | Dark | Light | Use |
|---|---|---|---|
| Canvas | `#101820` | `#F5F3EF` | Foundation behind content |
| Surface | `#18232E` | `#FFFFFF` | Fields, notices, reading panels |
| Raised | `#223140` | `#E9E5DF` | Secondary groups, missing artwork |
| Primary text | `#F3F6F7` | `#26232B` | Essential readable information |
| Supporting text | `#B0BECB` | `#625C69` | Metadata and help; never illegibly faint |
| Border | `#657787` | `#817987` | Input boundaries and important separation |
| Primary action | `#8FD8EE` | `#643F67` | Deliberate next action |
| Text on action | `#10202A` | `#FFFFFF` | Text inside primary buttons |
| Selection | `#29434F` | `#EEE4EF` | A chosen filter, with native checked state |
| Success | `#ACE2C4` | `#2E674A` | Confirmed completed outcome |
| Warning / uncertainty | `#F0D29A` | `#7C5411` | Attention; words distinguish warning from unresolved result |
| Danger | `#FFB5AF` | `#A82C3A` | Errors/destruction plus explicit explanation |

[Visible swatches and state examples](design-system/mockup/components.html#colors) and [measured contrast](design-system/contrast.json) make these values inspectable. Text targets ≥4.5:1, critical non-text control boundaries/focus ≥3:1. Measurements cover actual solid pairs; image text uses protective scrims and separate review. Disabled controls still explain why they are unavailable.

Do not use accent as a generic success signal, warning as a confirmed failure, or selection as a completed save. Never place low-contrast supporting text on the accent fill. Do not show a red button as the sole explanation of deletion. Do not put text directly on arbitrary user artwork without an opaque or verified protective surface. Do not lighten critical text with opacity. Status uses words and, when useful, symbols in addition to color.

## Typography, layout, and shape

Body and controls share Avenir Next → Segoe UI → Arial → sans-serif. Dark display headings use the same family; light display headings use Didot → Times New Roman → serif, retaining C’s editorial character. These are local system stacks, with no downloaded font or licensing claim. Font fallback may change wrapping; content must accommodate it.

Use 16px body/control text with 1.55 line height, 13px supporting text, 25px section headings, and 48px page titles scaling to 36px on narrow screens. The catalog hero may use 58px desktop/42px narrow. Light serif titles retain the same box and hierarchy, with wrapping allowed. Keep prose approximately 65–80 characters per line. Never truncate the only accessible title or a retained invalid input; full names, Unicode, and synopsis line breaks must remain readable. [Typography specimens](design-system/mockup/components.html#typography) use actual fixture content.

Shared spacing primitives: 4, 8, 12, 16, 24, 32, 48px. Use 8/12px within controls, 16/24px within groups, 32/48px between sections. This consolidated rhythm is shared by both selected themes. Main content max-width is 1280px. Poster grids use six columns on wide desktop, three below 1000px, and two below 700px. Sidebars collapse to contextual headings; navigation wraps without hiding essential actions. Form columns become a single column. Detail art may float beside the title on narrow views, while synopsis returns to full width.

Controls use shared 8px corners, artwork 12px, identity/status pills fully rounded. Cards derive structure from content and alignment; avoid outlining every metadata fragment. Overlay elevation uses one restrained shadow; forms and catalog cards do not need decorative shadows. Controls target at least 44px touch height. Table cells wrap and preserve headers; phone layouts prioritize identifiable records and explicit actions over compactness.

## Artwork, icons, and motion

Posters use a 2:3 slot with cover cropping; the hero is a wide landscape frame. Do not stretch images. Display title/type/context beside artwork, so a missing image never erases navigation or identity. Decorative repeated posters use empty alternative text; unique narrative/context imagery has descriptive text. Placeholder text says “Artwork unavailable,” distinct from a catalog failure.

The four local raster assets were generated for fictional films and TV fixtures using imagegen; [provenance and prompts](design-system/mockup/assets/artwork-provenance.json) record their origin. They do not claim rights to existing films, provider seed data, or recognizable performers. User artwork remains untrusted: format/byte checks are not a substitute for authoritative decoding and bounded processing.

Icons are simple local SVG/line shapes. Use labeled controls and avoid ambiguous icon-only deletion. Motion is brief feedback, 140ms by default; no autoplay cinema effects. `prefers-reduced-motion: reduce` disables smooth scrolling and transitions. Loading uses stable placeholders and text rather than motion as the only signal.

## Visible interaction contracts

Discovery combines case-insensitive partial display/original-title search, any selected genre, and all other categories. People search is a separate labeled mode. Year is required before month; clearing year also clears month, clearing text preserves other filters. Year-only records do not acquire a month/day. Unknown dates remain available without a date filter, ordered last. Invalid current queries retain text and suppress stale results. Newer requests own the visible results; outdated responses cannot replace them.

Forms pair labels, effective guidance, and associated field errors. Required ordinary text is trimmed for emptiness/counting; preserve legitimate internal spacing and synopsis line breaks. Count Unicode code points: `é` is one, `e` plus combining accent is two. Person defaults are 1–100; registration defaults 2–100. Other numerical examples remain Recommended: titles 1–255, synopsis 1–5,000, roles 1–100, each character name 1–200, search 0–200, email maximum 254, URL maximum 2,048, new passwords 15–128, artwork 1 byte–5 MiB. Exact HTTP/HTTPS URL syntax/credential policy and processing limits remain technical design. A source URL does not grant arbitrary fetch permission.

Optional omission preserves saved data; an explicit empty value clears it. Policy tightening must not truncate existing data, remove accepted artwork, or invalidate existing credentials. Validate newly changed values against the current policy, preserve unchanged fields, and explain newly invalid drafts. No configuration editing UI is added. Invalid policy blocks the affected operation safely.

Login uses `autocomplete="username"` and `current-password`; registration uses name/email/new-password. Enter submits and pending feedback prevents duplicate UI submission. Generic credential failure reveals no account details. Registration creates only Viewer, then directs to sign-in. Existing-password login has no registration-length rule. Passwords are never copied into recovery data, diagnostics, or audit. Cookies/tokens, CSRF, session expiry/storage, provisioning, verified MCP identity and security enforcement are technical-design responsibilities.

Session checking conceals protected work. Expiry or reauthentication preserves permitted draft ownership without implying committed work rolled back. Resume only after same-account, permission, target, and original-operation checks. A different account must not see or inherit the draft. A permitted intended destination returns after sign-in; a disallowed destination falls back to permitted content. Confirmed sign-out conceals drafts; failed sign-out retains the active session; uncertain sign-out conceals private content while checking, without claiming completion.

Stale saves retain draft and current version for review; no force overwrite. Uncertainty is reconciled against the original operation before another write. Metadata work remains usable during artwork outage. Upload transfer, processing, attachment, failed replacement, cancellation, changed access/target, and uncertainty are separate states. Transfer 100% is not saved artwork. Existing art remains after replacement failure; later cleanup cannot remove another title’s valid attachment.

Movies and people archive before permanent deletion. Public views disclose no archived data or retained credits. Restore first to edit. Person impact includes active movies, archived movies, and TV. Permanent deletion has a current impact preview followed by a separate irreversible confirmation; changed impact invalidates earlier confirmation. Restoring one side does not restore the other side or recreate deleted/removed relationships. Remove a retained person credit only from an active movie; never direct-edit TV credits.

History is read-only and shows actor/action/record/time/outcome/changes without account emails, credentials, raw files or unrelated profile data. Success, failure, approval and creation remain distinct. Restricted security/operations inspection is not a new app role or screen.

ChatGPT and Claude examples are explicitly external-client storyboards. All exact publishable values, sources and unknowns appear before human approval. Correction requires fresh approval of the revised exact content; rejection publishes nothing. Permission and validation still apply after approval. No assistant claim substitutes for authorized human approval. Unknown creation is reconciled without blind duplication. Client compatibility/enforcement requires real integration testing; neither a storyboard nor model text proves it.

## Svelte/TypeScript handoff

| Responsibility | Suggested owner / contract |
|---|---|
| Theme application | `ThemeRoot`: apply the shared semantic override; preserve focus/form state |
| App navigation | `AppShell`: consume verified role capabilities; never derive authorization from hidden links |
| Catalog criteria/results | `CatalogFilters` + `CatalogResults`: controlled criteria, latest-request identity, honest precision |
| Metadata/artwork | `TitleCard`, `TitleDetails`, `ArtworkSlot`: stable identity, explicit unknowns, fallback |
| Forms | `Field`, `ErrorSummary`, feature form: labels/policy/error associations; safe draft owned by feature |
| Save outcomes | `OperationFeedback`: discriminated confirmed/failed/uncertain states; expose safe next action |
| Stale draft | `ConflictReview`: compare base/current/draft; permission/target check before resubmission |
| Artwork | `ArtworkTransfer`: transfer versus processing versus confirmed attachment; no premature success |
| Lifecycle | `ImpactPreview` + `ConfirmDialog`: exact target/current impact; cancel and focus return |
| Account interruption | `SessionBoundary`: verified identity, permitted return, account-isolated recovery |
| History | `AuditList`, `AuditDetail`: read-only sanitized events |
| AI handoff | Client-independent proposal/revision/approval/outcome view models; not an in-app queue |

Use TypeScript discriminated unions such as `OperationState = { kind: 'pending' } | { kind: 'confirmed'; recordId: string } | { kind: 'failed'; correction: string } | { kind: 'uncertain'; originalOperationId: string }`. Keep operation identity distinct from record identity and display names. Validation/policy services supply effective rules; UI-only disabling never guarantees authorization/idempotency.

If shadcn-svelte is later adopted, Button/Input/Textarea/Checkbox/Select/Dialog/AlertDialog/Tabs/Badge/Table primitives can map to these contracts. Accessible multi-part composition and real installed APIs must be checked then. Domain-specific conflict, impact, provenance, policy and AI approval behavior remains application-owned. No library, frontend build, network API or backend mechanism is installed by this package.

## Validation and handoff boundaries

Branding updated on 11 October 2026: SpoolIn name, brand history, discovery headline and supporting copy, gallery links, and regenerated two-theme preview. [Branding verification](design-system/verification.md) records the current checks separately from the original design review below.

Reviewed on 10 October 2026. PRD coverage reconciles all 17 functional requirements, 16 nonfunctional requirements, and 159 acceptance criteria: 109 criteria have visual examples and 50 have documented technical dispositions. [Coverage](design-system/coverage.md) explains each mapping. Recheck the source fingerprint before revising this package.

After correcting nested sections in the details template, browser checks revalidated all 97 states in both themes at 1440, 390, and 320 CSS pixels (582 combinations). Each check required exactly one actually rendered state, matching the requested fragment, with a visible heading and nonzero layout size; checking only the hidden attribute had missed descendants inside a hidden parent. All combinations passed, with no theme mismatches, horizontal page overflow, or broken visible images. All 97 state-selector transitions and a browser-back transition also passed. Principal templates received manual screenshot review. The retained [two-theme preview](design-system/mockup/assets/design-preview.jpg) illustrates the selected system; the interactive HTML remains the complete UI reference.

Interaction checks covered keyboard login and generic errors, registration, existing-password compatibility, assigned-role navigation, session interruption and account-isolated draft recovery, sign-out, dialog focus/Escape/return, composable filters, stale editing, updated deletion impact, artwork stages, and revised AI approval. These were local fixture checks. Authentication, writes, uploads, and AI outcomes are simulated; they do not prove service behavior or production acceptance.

[Measured contrast](design-system/contrast.json) records 38 solid-color pairs meeting text ≥4.5:1 or control/focus ≥3:1 thresholds. Full assistive-technology testing, other browsers/font fallbacks, forced colors, zoom/text scaling, runtime reduced-motion testing, exhaustive interaction combinations, and arbitrary image-overlay contrast were not verified. This is not a WCAG conformance certification. Production artwork requires a verified protective text treatment.

Technical design must define authoritative validation, account/session security, permission and target rechecks, persistent account-isolated recovery, concurrent mutations, impact freshness, operation reconciliation, bounded artwork decoding/processing, audit atomicity, and restart persistence. Real ChatGPT/Claude integration must enforce exact-content approval; storyboards do not establish that capability. Production seed data, provider licenses, deployment settings, and a component library remain outside this design package.

Open `design-system/mockup/index.html` directly or serve `docs/design-system/` with a local static server and visit `mockup/index.html`. The package has no build step or remote runtime dependency. Only theme preference is persisted by the demonstrations; protected draft examples use page memory and reset on reload.
