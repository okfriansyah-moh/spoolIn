# Production UI binding assessment

This is a source diagnosis, not runtime acceptance or the independent combined review. Assessed input: `0ea7193cbbba70aa17c17117c81ca079138ebcf7`; artwork/audit corrections: `ef2a3ff`. The coordinator retains task status, the single findings ledger, remote evidence and formal acceptance. Current remote execution and rendered observations remain pending. This assessment enumerates all 16 page route files, both layout components and route configuration; it does not count source files or component tests as production proof.

## Shared boundary

Every listed API call uses `frontend/src/lib/transport/graphql.ts` → same-origin `/graphql`, cookie credentials, no-store, `X-Spoolin-Client:web`, mutation CSRF, bounded body/deadline and transport error classification. Core registration, identity/authorization, RPC/service/storage behavior are separately traced in [API/RPC assessment](api-rpc-production-wiring-assessment.md). Those downstream source paths do not establish successful browser execution. Protected descendants render only after `routes/manage/+layout.svelte` verifies an actual current Maintainer session; concealment destroys descendants on identity changes. Root layout owns session rechecks on history restoration, route-heading focus and session/theme context. Query failures still need actual quota/session/race observations at each applicable route.

## Every route and visible control

Paths below are relative to `frontend/src/`. Each action listed was traced to the named handler/API; linked navigation causes that destination's load rather than a simulated effect.

| Route / source | Every visible action family | Production handler and actual operation | Response / error / session / reload boundary |
|---|---|---|---|
| Root `routes/+layout.svelte`, `lib/components/AppShell.svelte` | Brand/home; Catalog; People; Movies; Manage people; Archives; History; skip link; switch theme | `navigation(capabilities)` supplies only permitted routes; `toggleTheme` → validated local theme preference; skip/afterNavigate → actual main/heading focus | Theme is the only persisted UI preference. Session controller bootstrap fetches `session,policy`; pagehide conceals, persisted pageshow rechecks. Real focus/theme/browser observations pending. |
| Root session menu | Sign in; Register; Sign out; Cancel/Escape; Check sign-out; Sign in again | `SessionMenu.signOut` → `SessionController.signOut` → `signOut`; `reconcileSignOut` → `operation(owner:CORE)` then authoritative session bootstrap | Original logout identity retained in memory; protected identity concealed while uncertain. Required expired-body guard is a diagnosed source follow-up below. |
| `/register`, `routes/register/+page.svelte` | Name/email/password; submit; refresh/check again; original request check; sign-in/catalog links | `AccountForm.submit` → policy validation → `SessionController.submit(register)` → actual `register`; `reconcileIdentity` → exact CORE operation | No role picker; creates Viewer only, no automatic signed-in success. Password cleared through session changes/unmount. Duplicate/invalid/rate-limit/pending/current-session states distinct in controller. Actual registration and denial/recovery rows pending. |
| `/sign-in`, `routes/sign-in/+page.svelte` | Email/password; submit; Check session/original request/sign-out; register/catalog links | Same AccountForm/controller with `signIn`; safeDestination accepts permitted relative destination and `goto` routes it | Successful sign-in is followed by authoritative session read; errors retain correction fields, passwords remain private. Unknown original ID never becomes a new automatic mutation. Expired retained receipt guard needs correction. |
| `/`, `routes/+page.svelte` | Search; clear search; type; each genre; year; month; retry; browse again; every title card | `CatalogFilters` updates criteria → URL + CatalogController → `titles(filter,first,after)`; `fetchGenres` → `genres`; retry preserves criteria | Abort/generation suppress stale replies; month depends on year; separate loading/empty/no-match/failure/invalid. Source currently drops reachability after first page; year picker exposes only 10 recent years; genre failure is swallowed; text is debounced twice. These remain diagnosed, not accepted. |
| `/people`, `routes/people/+page.svelte` | Search; clear; retry; person links | `onInput/clearSearch/load` → `people(text,first,after)` | First-page loading/empty/no-match/failure distinct and stale loads suppressed. No next-page control, no URL Back/Forward resync, pending debounce is not cleared by Clear. Source follow-up required. |
| `/titles/[id]`, `routes/titles/[id]/+page.svelte` | Catalog breadcrumb/return; person credit links; provenance links; artwork image error fallback | onMount → `title(id)` with metadata/artwork/credits; credits → actual `/people/[id]` | Null/archived → unavailable, read errors distinct. No edit/TV/episode affordance. Parameter reused-route changes are not observed; embedded credits' pageInfo has no next action; no same-record retry control. |
| `/people/[id]`, `routes/people/[id]/+page.svelte` | Catalog/People breadcrumb/return; title credit links | onMount → `person(id)` + `personCredits(id,first,after)` | Unknown/archived unavailable; errors distinct; duplicate names retain IDs. Parameter changes are not observed; credit pagination is discarded; no same-record retry control. |
| `/manage`, `routes/manage/+page.svelte` | No page-local action; shared management navigation remains | Protected layout checks actual session/role, then renders landing text | This is an access landing, not a CRUD/proposal operation. No runtime acceptance inferred. |
| `/manage/[...section]`, catch-all page | No page-local action; arbitrary unknown sections display Management text | Protected layout only; no section API/handler exists | Generic success-like landing at unknown management URLs is a diagnosed placeholder. It must present unavailable/404, not imply an implemented management section. No connector approval screen is enabled by this page. |
| `/manage/movies`, list page | Create movie; each Movie Edit/Review; TV read-only notice | loadWriteContext → MovieListController.load → `managedTitles(state:ACTIVE)`; links → actual editor/public route | Only active MOVIE gets Edit; TV remains read-only. First 24 results only; no load-more/retry, context rejection can leave indefinite loading. Root owns movie source follow-up. |
| `/manage/movies/new`, movie form | Title/synopsis/year/genres/original title; submit; draft picker/start new; leave/cancel/confirm discard; original result check; stale review checkbox/rebase; continue edit/public links after create | `applyMovieDraftField/updateDraft/toggleGenre` → MovieDraftSync `saveDraft`; `submit` → backed separate Catalog ID then `createMovie`; exact CORE/CATALOG operation lookup; `discardDraft` | Form only offers a subset of MovieInput at assessed SHA. Successful create refetches result ID and prevents duplicate create. Unknown/expired original remains constrained by controller, but direct-write expired body propagation needs correction. Root owns metadata/projection/patch/validation/rebase fixes. |
| `/manage/movies/[id]`, movie editor | Same movie controls plus archive; artwork; credit editor/custom roles (expanded below) | Current ID → `managedTitle`, `genres`, drafts/current operation; update → `updateMovie` with expected movie version; archive → actual lifecycle | TV/archived/removed targets disable editing; stale values/current snapshot remain separate for deliberate rebase. Root owns movie full metadata and route/version refresh work. |
| `/manage/people`, list page | Create person; each Edit/Review | loadWriteContext → PersonListController.load → `managedPeople(state:ACTIVE)`; links to actual person routes | First 24 only; no retry/load-more, bootstrap rejection can leave loading forever. Follow-up required. |
| `/manage/people/new`, person form | Name/biography; create; chosen retained draft/start new; leave/cancel/confirm; current result check; review/rebase; continue/public links | `updateDraft` → PersonDraftSync `saveDraft`; `submit` → backed separate Catalog ID then `createPerson`; explicit `discardDraft` | Current saved identity is refetched before success; no repeated create while saved. Picker preserves original draft ID. Pending discard retries original Core ID; Cancel/Escape retains values. Direct-write resultExpired needs follow-up. |
| `/manage/people/[id]`, person form | Name/biography; save; retained draft/rebase/discard controls; Archive person | `managedPerson`; update → `updatePerson` expected person version; archive → actual lifecycle | Preserves shared identity and complete biography; changed/archived target guarded. Edit success retains backed draft at new base without Catalog pending binding; create cleanup differs. Direct parameter reuse and expired body require correction. |
| `/manage/archives`, archive page | Movie/People tabs; Restore; Delete permanently; Cancel/Escape; confirm; Check original result; Retry original request | `fetchArchived` → archived managedTitles/managedPeople; restore → movieLifecycle/personLifecycle; person delete first actual deletionImpact; confirm retains original ID/row version/token | Current impact is required; IMPACT_CHANGED clears stale intent for fresh assessment; unknown/expired operation remains original. Confirmed outcome reloads actual owner state. First 48 only; tab race and read-error retry/pagination need follow-up. |
| `/manage/audit`, history page | Inspect changes details; Load more history; Retry history | `loadPage` → current Maintainer context → `fetchAuditPage` → actual `audit(first,after,subjectId)` | No write/control mutates audit. `ef2a3ff` fixes invalid String→Cursor request type, absent rows and repeated cursor handling. UI/table/omitted-diff formatting and actual complete outcomes require remote assertions. Initial bootstrap failure before permitted=true offers no retry in assessed route. |

## Editor subcontrols and original-intent paths

| Surface | Every action / input | Actual handler → GraphQL | Source disposition and required behavioral proof |
|---|---|---|---|
| Movie draft/rebase | Every field, each genre, retained choice, start new, review current, use reviewed version, leave/Cancel/Escape/confirm | Route field adapter → MovieEditorController → MovieDraftSync → saveDraft/discardDraft; separate CORE receipt before current drafts; separate CATALOG operation for submitted write | Root completes all MovieInput fields/presence-aware patching. Backed state, same-ID create, incomplete draft, stale base/current/draft, expiry/account-switch, conflict and same-ID recovery each need actual browser/owner proof. |
| Person draft/rebase | Name/biography, chosen draft, start new, review checkbox/rebase, Leave/Cancel/Escape/confirm, original result | PersonEditorController / PersonDraftSync → saveDraft/discardDraft, createPerson/updatePerson, owner operation | Real remote source journeys now cover retained original draft ID, biography reload, deliberate competing-write review and cancel/discard, but their execution remains pending. Native reader/account-switch/response-loss rows are not implied by those journeys. |
| Credit form | Person selection; Load more people; role selection; complete ordered newline character entries; Add/Save; each Edit/Remove; Cancel edit; removal Cancel/Escape/confirm | CreditPanel → `fetchActivePeoplePage/creditRoles/managedCredits`; addCredit/updateCredit/removeCredit with immutable captured ID/version/role/characters/policy | Current-person choices page explicitly; credit list itself is first 48 only. Roles stay domain contributions, never account permissions. Full ordered names preserve slash as data. Original request is captured once in memory; pending never increments guessed title version. |
| Credit draft | Retained picker; original receipt check/retry; stale current values review checkbox and reviewed version; discard Cancel/Escape/confirm | CreditDraftSync → saveDraft/discardDraft; exact CATALOG operation lookup before values; current managedPerson/managedTitle/role checks | TD3248: new credit targetId=null/body titleId; edit targetId is exact credit UUID, baseVersion its version. No tuple matching. Current Draft SDL exposes no original policyRevision; after reload unknown submitted intent is looked up and locked, never resent with guessed policy. Original Core save/discard identity and payload preserved across uncertainty. |
| Custom role | Label; CAST/CREATOR; Create role; original role result check; original role retry | RoleCreator → createRole; recoverCreditRole → exact CATALOG operation; confirmed result ID → refetch current creditRoles/select exact role | Requires actual role ID/version-safe receipt; pending/expired are not success. Normalized duplicate correction retains no duplicate. Custom roles do not accept character entries. Production browser custom-role journeys remain unexecuted at this input. |
| Artwork | File chooser; measured transfer progress; Cancel upload; Check original result | ArtworkUploadController → XHR uploadArtwork with real progress; cancelArtwork; exact CATALOG operation plus managedTitle read | `ef2a3ff` removes invented cancellation success, preserves original cancel ID through uncertain retry, validates original receipt/expiry/title/version and distinguishes terminal failure. Effective client validation before FileReader, label semantics and parent version/artwork refresh remain follow-ups; server validation does not replace them. |
| Archive action on active editor | Open archive confirmation; Cancel/Escape; Archive; Check original archive result | ArchiveAction → current write context; movieLifecycle/personLifecycle ARCHIVE; exact CATALOG receipt | Version/permission owner enforcement real. Original ID retained until confirmed/failed. No blind new write on pending; need actual public disappearance/restore and parent version coherence proof. |
| Shared dialogs/errors/artwork | Cancel-first focus, Tab trap, Escape, confirm pending, restored opener; linked field errors; missing-image fallback | ConfirmDialog close/confirm callbacks; ErrorSummary field anchors; ArtworkSlot safe local URL and onerror | Concrete semantic source exists; actual focus/announcement/contrast/reflow/reader observations still required. A DOM attribute is not proof that an assistive reader announced it. |

## Task-by-task UI responsibility

Every requested task below was explicitly assessed. The case lists are copied from each task's complete Traceability table (including subrows), not selected samples. UI ownership does not replace backend/protocol/database/security proof assigned to another task. All runtime/formal acceptance results in this document are **Pending**; source corrections are recorded independently.

### T004

[T004-build-the-shared-themed-and-accessible-shell](../plan/02-application-shell/tasks/T004-build-the-shared-themed-and-accessible-shell.md)

Shared shell/nav/theme/focus and protected menu; frontend plus real browser/reader.

Assigned ATDD obligations (24): `ATDD-069`, `ATDD-070`, `ATDD-071`, `ATDD-136`, `ATDD-260`, `ATDD-268`, `ATDD-269`, `ATDD-270`, `ATDD-271`, `ATDD-272`, `ATDD-273`, `ATDD-274`, `ATDD-321`, `ATDD-335`, `ATDD-335-T1-R01`, `ATDD-335-T1-R02`, `ATDD-335-T1-R03`, `ATDD-335-T1-R04`, `ATDD-335-T1-R05`, `ATDD-335-T1-R06`, `ATDD-335-T1-R07`, `ATDD-335-T1-R08`, `ATDD-335-T1-R09`, `ATDD-335-T1-R10`. Each remains individually mapped through the coordinator coverage matrix; none is accepted by this source inventory.

### T005

[T005-protect-account-identities-and-credential-storage](../plan/03-account-access-credential-storage/tasks/T005-protect-account-identities-and-credential-storage.md)

Identity/credential storage is Core-owned; account forms are its UI consumer. Password storage, hashes and privacy need actual Core/database/security proof.

Assigned ATDD obligations (39): `ATDD-047`, `ATDD-085`, `ATDD-086`, `ATDD-099`, `ATDD-115`, `ATDD-120`, `ATDD-171`, `ATDD-172`, `ATDD-175`, `ATDD-176`, `ATDD-180`, `ATDD-181`, `ATDD-228`, `ATDD-228-T1-R01`, `ATDD-228-T1-R02`, `ATDD-228-T1-R03`, `ATDD-228-T1-R04`, `ATDD-228-T1-R05`, `ATDD-228-T1-R06`, `ATDD-229`, `ATDD-229-T1-R01`, `ATDD-229-T1-R02`, `ATDD-229-T1-R03`, `ATDD-229-T1-R04`, `ATDD-229-T1-R05`, `ATDD-229-T1-R06`, `ATDD-229-T1-R07`, `ATDD-229-T1-R08`, `ATDD-229-T1-R09`, `ATDD-229-T1-R10`, `ATDD-230`, `ATDD-230-T1-R01`, `ATDD-230-T1-R02`, `ATDD-230-T1-R03`, `ATDD-230-T1-R04`, `ATDD-258`, `ATDD-277`, `ATDD-296`, `ATDD-297`. Each remains individually mapped through the coordinator coverage matrix; none is accepted by this source inventory.

### T006

[T006-implement-effective-policy-and-precise-input-semantics](../plan/04-effective-input-policy/tasks/T006-implement-effective-policy-and-precise-input-semantics.md)

Policy bootstrap/parser, form guidance and changed-field validation; required actual effective configuration and owner enforcement.

Assigned ATDD obligations (38): `ATDD-050`, `ATDD-051`, `ATDD-052`, `ATDD-097`, `ATDD-113`, `ATDD-116`, `ATDD-117`, `ATDD-118`, `ATDD-119`, `ATDD-121`, `ATDD-122`, `ATDD-127`, `ATDD-128`, `ATDD-129`, `ATDD-131`, `ATDD-140`, `ATDD-177`, `ATDD-184`, `ATDD-212`, `ATDD-251-T1-R02`, `ATDD-255-T1-R01`, `ATDD-267`, `ATDD-276`, `ATDD-278`, `ATDD-279`, `ATDD-289`, `ATDD-290`, `ATDD-291`, `ATDD-292`, `ATDD-323`, `ATDD-324`, `ATDD-325`, `ATDD-326`, `ATDD-328`, `ATDD-334`, `ATDD-336`, `ATDD-337`, `ATDD-338`. Each remains individually mapped through the coordinator coverage matrix; none is accepted by this source inventory.

### T007

[T007-implement-owner-receipts-and-atomic-mutation-evidence](../plan/05-reliable-work-mutation-receipts/tasks/T007-implement-owner-receipts-and-atomic-mutation-evidence.md)

All editor/account/archive/artwork write outcomes consume owner receipts; atomic DB/audit/receipt proof stays backend-owned.

Assigned ATDD obligations (41): `ATDD-151`, `ATDD-152`, `ATDD-153`, `ATDD-223`, `ATDD-223-T1-R01`, `ATDD-223-T1-R02`, `ATDD-223-T1-R03`, `ATDD-223-T1-R04`, `ATDD-223-T1-R05`, `ATDD-223-T1-R06`, `ATDD-223-T1-R07`, `ATDD-223-T1-R08`, `ATDD-223-T1-R09`, `ATDD-223-T1-R10`, `ATDD-223-T1-R11`, `ATDD-223-T1-R12`, `ATDD-226`, `ATDD-226-T1-R01`, `ATDD-226-T1-R02`, `ATDD-226-T1-R03`, `ATDD-226-T1-R04`, `ATDD-226-T1-R05`, `ATDD-226-T1-R06`, `ATDD-226-T1-R07`, `ATDD-226-T1-R08`, `ATDD-226-T1-R09`, `ATDD-226-T1-R10`, `ATDD-226-T1-R11`, `ATDD-226-T1-R12`, `ATDD-226-T1-R13`, `ATDD-226-T1-R14`, `ATDD-226-T1-R15`, `ATDD-226-T1-R16`, `ATDD-226-T1-R17`, `ATDD-226-T1-R18`, `ATDD-226-T1-R19`, `ATDD-251-T1-R17`, `ATDD-253-T1-R23`, `ATDD-254-T1-R06`, `ATDD-284`, `ATDD-288`. Each remains individually mapped through the coordinator coverage matrix; none is accepted by this source inventory.

### T008

[T008-implement-registration-and-authoritative-sessions](../plan/06-account-access-registration-and-sessions/tasks/T008-implement-registration-and-authoritative-sessions.md)

Register/signIn/signOut/session via AccountForm/SessionMenu/controller; authoritative session+CORE receipt.

Assigned ATDD obligations (35): `ATDD-047`, `ATDD-085`, `ATDD-086`, `ATDD-099`, `ATDD-115`, `ATDD-120`, `ATDD-153`, `ATDD-171`, `ATDD-172`, `ATDD-175`, `ATDD-176`, `ATDD-231`, `ATDD-231-T1-R01`, `ATDD-231-T1-R02`, `ATDD-231-T1-R03`, `ATDD-231-T1-R04`, `ATDD-231-T1-R05`, `ATDD-231-T1-R06`, `ATDD-231-T1-R07`, `ATDD-231-T1-R08`, `ATDD-231-T1-R09`, `ATDD-251-T1-R01`, `ATDD-251-T1-R20`, `ATDD-251-T1-R21`, `ATDD-251-T1-R22`, `ATDD-258`, `ATDD-337`, `ATDD-337-T1-R01`, `ATDD-337-T1-R02`, `ATDD-337-T1-R03`, `ATDD-337-T1-R04`, `ATDD-337-T1-R05`, `ATDD-337-T1-R06`, `ATDD-337-T1-R07`, `ATDD-337-T1-R08`. Each remains individually mapped through the coordinator coverage matrix; none is accepted by this source inventory.

### T009

[T009-enforce-scoped-service-identity-and-shared-admission](../plan/07-guarded-access/tasks/T009-enforce-scoped-service-identity-and-shared-admission.md)

Transport CSRF/current session is UI consumer; mTLS/scoped actor/admission proof is actual Core/Catalog boundary, not browser appearance.

Assigned ATDD obligations (51): `ATDD-047`, `ATDD-050`, `ATDD-051`, `ATDD-052`, `ATDD-085`, `ATDD-086`, `ATDD-097`, `ATDD-099`, `ATDD-113`, `ATDD-115`, `ATDD-116`, `ATDD-117`, `ATDD-118`, `ATDD-119`, `ATDD-120`, `ATDD-121`, `ATDD-122`, `ATDD-127`, `ATDD-128`, `ATDD-129`, `ATDD-131`, `ATDD-140`, `ATDD-171`, `ATDD-172`, `ATDD-173`, `ATDD-174`, `ATDD-177`, `ATDD-184`, `ATDD-212`, `ATDD-234`, `ATDD-234-T1-R01`, `ATDD-234-T1-R02`, `ATDD-234-T1-R03`, `ATDD-234-T1-R04`, `ATDD-252-T1-R01`, `ATDD-252-T1-R02`, `ATDD-258`, `ATDD-267`, `ATDD-280`, `ATDD-295`, `ATDD-318`, `ATDD-323`, `ATDD-324`, `ATDD-325`, `ATDD-326`, `ATDD-327`, `ATDD-328`, `ATDD-334`, `ATDD-336`, `ATDD-337`, `ATDD-338`. Each remains individually mapped through the coordinator coverage matrix; none is accepted by this source inventory.

### T010

[T010-connect-account-and-session-browser-journeys](../plan/08-account-access-browser-journeys/tasks/T010-connect-account-and-session-browser-journeys.md)

Account route/form/menu/returnTo and protected layout; all role/history/expiry/retry rows require actual browser.

Assigned ATDD obligations (29): `ATDD-047`, `ATDD-048`, `ATDD-085`, `ATDD-086`, `ATDD-099`, `ATDD-115`, `ATDD-120`, `ATDD-171`, `ATDD-172`, `ATDD-258`, `ATDD-258-T1-R01`, `ATDD-258-T1-R02`, `ATDD-258-T1-R03`, `ATDD-258-T1-R04`, `ATDD-258-T1-R05`, `ATDD-258-T1-R06`, `ATDD-259`, `ATDD-259-T1-R01`, `ATDD-259-T1-R02`, `ATDD-259-T1-R03`, `ATDD-259-T1-R04`, `ATDD-259-T1-R05`, `ATDD-259-T1-R06`, `ATDD-259-T1-R07`, `ATDD-259-T1-R08`, `ATDD-259-T1-R09`, `ATDD-259-T1-R10`, `ATDD-336`, `ATDD-338`. Each remains individually mapped through the coordinator coverage matrix; none is accepted by this source inventory.

### T011

[T011-run-fenced-recovery-and-bounded-retries](../plan/09-reliable-work-recovery-and-retries/tasks/T011-run-fenced-recovery-and-bounded-retries.md)

Recovery is operator/backend work; UI uncertainty keeps original operation identity, no force-success action.

Assigned ATDD obligations (36): `ATDD-057`, `ATDD-058`, `ATDD-059`, `ATDD-060`, `ATDD-066`, `ATDD-067`, `ATDD-154`, `ATDD-155`, `ATDD-156`, `ATDD-157`, `ATDD-158`, `ATDD-225`, `ATDD-225-T1-R01`, `ATDD-225-T1-R02`, `ATDD-225-T1-R03`, `ATDD-225-T1-R04`, `ATDD-225-T1-R05`, `ATDD-225-T1-R06`, `ATDD-225-T1-R07`, `ATDD-225-T1-R08`, `ATDD-225-T1-R09`, `ATDD-225-T1-R10`, `ATDD-225-T1-R11`, `ATDD-225-T1-R12`, `ATDD-225-T1-R13`, `ATDD-225-T1-R14`, `ATDD-225-T1-R15`, `ATDD-225-T1-R16`, `ATDD-225-T1-R17`, `ATDD-225-T1-R18`, `ATDD-225-T1-R19`, `ATDD-225-T1-R20`, `ATDD-285`, `ATDD-286`, `ATDD-287`, `ATDD-309`. Each remains individually mapped through the coordinator coverage matrix; none is accepted by this source inventory.

### T012

[T012-serve-precise-public-catalog-and-people-reads](../plan/10-catalog-discovery/tasks/T012-serve-precise-public-catalog-and-people-reads.md)

Catalog/people/title/credit query consumers; actual read projection/authorization/order/pagination owner proof.

Assigned ATDD obligations (88): `ATDD-001`, `ATDD-002`, `ATDD-003`, `ATDD-004`, `ATDD-005`, `ATDD-006`, `ATDD-007`, `ATDD-008`, `ATDD-009`, `ATDD-010`, `ATDD-011`, `ATDD-012`, `ATDD-013`, `ATDD-014`, `ATDD-015`, `ATDD-030`, `ATDD-031`, `ATDD-032`, `ATDD-133`, `ATDD-022`, `ATDD-137`, `ATDD-138`, `ATDD-139`, `ATDD-236`, `ATDD-236-T1-R01`, `ATDD-236-T1-R02`, `ATDD-236-T1-R03`, `ATDD-236-T1-R04`, `ATDD-236-T1-R05`, `ATDD-236-T1-R06`, `ATDD-236-T1-R07`, `ATDD-236-T1-R08`, `ATDD-236-T1-R09`, `ATDD-236-T1-R10`, `ATDD-236-T1-R11`, `ATDD-236-T1-R12`, `ATDD-236-T1-R13`, `ATDD-236-T1-R14`, `ATDD-236-T1-R15`, `ATDD-236-T1-R16`, `ATDD-236-T1-R17`, `ATDD-236-T1-R18`, `ATDD-236-T1-R19`, `ATDD-236-T1-R20`, `ATDD-236-T1-R21`, `ATDD-236-T1-R22`, `ATDD-236-T1-R23`, `ATDD-238`, `ATDD-238-T1-R01`, `ATDD-238-T1-R02`, `ATDD-238-T1-R03`, `ATDD-238-T1-R04`, `ATDD-239`, `ATDD-239-T1-R01`, `ATDD-239-T1-R02`, `ATDD-242`, `ATDD-242-T1-R01`, `ATDD-242-T1-R02`, `ATDD-242-T1-R03`, `ATDD-242-T1-R04`, `ATDD-242-T1-R05`, `ATDD-242-T1-R06`, `ATDD-242-T1-R07`, `ATDD-242-T1-R08`, `ATDD-242-T1-R09`, `ATDD-242-T1-R10`, `ATDD-251-T1-R03`, `ATDD-251-T1-R04`, `ATDD-251-T1-R05`, `ATDD-251-T1-R06`, `ATDD-251-T1-R07`, `ATDD-251-T1-R08`, `ATDD-251-T1-R09`, `ATDD-251-T1-R10`, `ATDD-253-T1-R01`, `ATDD-253-T1-R02`, `ATDD-253-T1-R03`, `ATDD-253-T1-R04`, `ATDD-253-T1-R05`, `ATDD-253-T1-R06`, `ATDD-253-T1-R07`, `ATDD-253-T1-R08`, `ATDD-256`, `ATDD-257`, `ATDD-281`, `ATDD-282`, `ATDD-303`, `ATDD-317`. Each remains individually mapped through the coordinator coverage matrix; none is accepted by this source inventory.

### T013

[T013-connect-discovery-details-and-public-people-views](../plan/10-catalog-discovery/tasks/T013-connect-discovery-details-and-public-people-views.md)

Discovery, title/person detail, people search, links/URL/history/paging; diagnosed route/paging gaps above.

Assigned ATDD obligations (36): `ATDD-001`, `ATDD-002`, `ATDD-003`, `ATDD-004`, `ATDD-005`, `ATDD-006`, `ATDD-007`, `ATDD-008`, `ATDD-009`, `ATDD-010`, `ATDD-011`, `ATDD-012`, `ATDD-013`, `ATDD-014`, `ATDD-015`, `ATDD-029`, `ATDD-030`, `ATDD-031`, `ATDD-032`, `ATDD-133`, `ATDD-022`, `ATDD-139`, `ATDD-256`, `ATDD-256-T1-R01`, `ATDD-256-T1-R02`, `ATDD-256-T1-R03`, `ATDD-256-T1-R04`, `ATDD-256-T1-R05`, `ATDD-256-T1-R06`, `ATDD-256-T1-R07`, `ATDD-257`, `ATDD-257-T1-R01`, `ATDD-257-T1-R02`, `ATDD-257-T1-R03`, `ATDD-257-T1-R04`, `ATDD-303`. Each remains individually mapped through the coordinator coverage matrix; none is accepted by this source inventory.

### T014

[T014-persist-encrypted-drafts-with-durable-save-and-discard-receipts](../plan/11-draft-recovery-storage-and-controllers/tasks/T014-persist-encrypted-drafts-with-durable-save-and-discard-receipts.md)

Draft API consumer; encrypted owned/versioned save/discard and receipt atomicity require Core/database proof.

Assigned ATDD obligations (30): `ATDD-048`, `ATDD-178`, `ATDD-179`, `ATDD-180`, `ATDD-233`, `ATDD-233-T1-R01`, `ATDD-233-T1-R02`, `ATDD-233-T1-R03`, `ATDD-233-T1-R04`, `ATDD-233-T1-R05`, `ATDD-233-T1-R06`, `ATDD-233-T1-R07`, `ATDD-233-T1-R08`, `ATDD-233-T1-R09`, `ATDD-233-T1-R10`, `ATDD-233-T1-R11`, `ATDD-233-T1-R12`, `ATDD-233-T1-R13`, `ATDD-251-T1-R18`, `ATDD-251-T1-R35`, `ATDD-251-T1-R36`, `ATDD-259`, `ATDD-293`, `ATDD-319`, `ATDD-320`, `ATDD-329`, `ATDD-330`, `ATDD-331`, `ATDD-332`, `ATDD-333`. Each remains individually mapped through the coordinator coverage matrix; none is accepted by this source inventory.

### T015

[T015-build-account-isolated-browser-draft-controllers](../plan/11-draft-recovery-storage-and-controllers/tasks/T015-build-account-isolated-browser-draft-controllers.md)

Draft controllers consumed by real movie/person/credit forms; same-owner/target/policy/reconciliation/concealment required.

Assigned ATDD obligations (6): `ATDD-048`, `ATDD-179`, `ATDD-259`, `ATDD-304`, `ATDD-322`, `ATDD-333`. Each remains individually mapped through the coordinator coverage matrix; none is accepted by this source inventory.

### T016

[T016-implement-authoritative-movie-creation-and-edits](../plan/12-movie-management/tasks/T016-implement-authoritative-movie-creation-and-edits.md)

Movie create/update consumer; root completes full metadata and valid patches; actual Catalog persistence/audit remains separate.

Assigned ATDD obligations (38): `ATDD-016`, `ATDD-017`, `ATDD-018`, `ATDD-021`, `ATDD-080`, `ATDD-053`, `ATDD-054`, `ATDD-055`, `ATDD-056`, `ATDD-088`, `ATDD-089`, `ATDD-101`, `ATDD-147`, `ATDD-148`, `ATDD-151`, `ATDD-185`, `ATDD-224`, `ATDD-224-T1-R01`, `ATDD-224-T1-R02`, `ATDD-224-T1-R03`, `ATDD-224-T1-R04`, `ATDD-224-T1-R05`, `ATDD-224-T1-R06`, `ATDD-224-T1-R07`, `ATDD-224-T1-R08`, `ATDD-224-T1-R09`, `ATDD-224-T1-R10`, `ATDD-224-T1-R11`, `ATDD-251-T1-R11`, `ATDD-251-T1-R14`, `ATDD-251-T1-R23`, `ATDD-251-T1-R24`, `ATDD-253-T1-R10`, `ATDD-253-T1-R12`, `ATDD-253-T1-R16`, `ATDD-265`, `ATDD-298`, `ATDD-307`. Each remains individually mapped through the coordinator coverage matrix; none is accepted by this source inventory.

### T017

[T017-connect-movie-editors-and-confirmed-outcomes](../plan/12-movie-management/tasks/T017-connect-movie-editors-and-confirmed-outcomes.md)

Movie list/create/edit/recovery/feedback/optional input and outcomes; root owns source corrections.

Assigned ATDD obligations (20): `ATDD-016`, `ATDD-017`, `ATDD-018`, `ATDD-020`, `ATDD-021`, `ATDD-079`, `ATDD-080`, `ATDD-081`, `ATDD-064`, `ATDD-260`, `ATDD-260-T1-R01`, `ATDD-260-T1-R02`, `ATDD-260-T1-R03`, `ATDD-260-T1-R04`, `ATDD-260-T1-R05`, `ATDD-260-T1-R06`, `ATDD-260-T1-R07`, `ATDD-260-T1-R08`, `ATDD-260-T1-R09`, `ATDD-260-T1-R10`. Each remains individually mapped through the coordinator coverage matrix; none is accepted by this source inventory.

### T018

[T018-implement-shared-person-writes-and-identity](../plan/13-people-and-credits/tasks/T018-implement-shared-person-writes-and-identity.md)

Person create/update consumer; shared identity/backend validation and atomic records need actual Catalog proof.

Assigned ATDD obligations (24): `ATDD-023`, `ATDD-026`, `ATDD-027`, `ATDD-079`, `ATDD-081`, `ATDD-114`, `ATDD-237`, `ATDD-237-T1-R01`, `ATDD-237-T1-R02`, `ATDD-237-T1-R03`, `ATDD-237-T1-R04`, `ATDD-237-T1-R05`, `ATDD-237-T1-R06`, `ATDD-237-T1-R07`, `ATDD-237-T1-R08`, `ATDD-251-T1-R12`, `ATDD-251-T1-R15`, `ATDD-251-T1-R26`, `ATDD-251-T1-R27`, `ATDD-253-T1-R11`, `ATDD-253-T1-R13`, `ATDD-253-T1-R17`, `ATDD-261`, `ATDD-262`. Each remains individually mapped through the coordinator coverage matrix; none is accepted by this source inventory.

### T019

[T019-connect-people-management-and-retained-edits](../plan/13-people-and-credits/tasks/T019-connect-people-management-and-retained-edits.md)

Person list/create/edit/current-value review/discard and public identity links; source paging/expiry/router gaps remain.

Assigned ATDD obligations (18): `ATDD-023`, `ATDD-025`, `ATDD-026`, `ATDD-027`, `ATDD-079`, `ATDD-081`, `ATDD-114`, `ATDD-261`, `ATDD-261-T1-R01`, `ATDD-261-T1-R02`, `ATDD-261-T1-R03`, `ATDD-261-T1-R04`, `ATDD-261-T1-R05`, `ATDD-261-T1-R06`, `ATDD-261-T1-R07`, `ATDD-261-T1-R08`, `ATDD-261-T1-R09`, `ATDD-262`. Each remains individually mapped through the coordinator coverage matrix; none is accepted by this source inventory.

### T020

[T020-implement-ordered-credits-and-custom-contribution-roles](../plan/13-people-and-credits/tasks/T020-implement-ordered-credits-and-custom-contribution-roles.md)

Custom CAST/CREATOR roles and ordered credit create/update/remove; actual backend authorization/constraint/version proof.

Assigned ATDD obligations (30): `ATDD-026`, `ATDD-027`, `ATDD-079`, `ATDD-081`, `ATDD-240`, `ATDD-240-T1-R01`, `ATDD-240-T1-R02`, `ATDD-240-T1-R03`, `ATDD-240-T1-R04`, `ATDD-240-T1-R05`, `ATDD-240-T1-R06`, `ATDD-241`, `ATDD-241-T1-R01`, `ATDD-241-T1-R02`, `ATDD-241-T1-R03`, `ATDD-241-T1-R04`, `ATDD-241-T1-R05`, `ATDD-241-T1-R06`, `ATDD-241-T1-R07`, `ATDD-241-T1-R08`, `ATDD-251-T1-R13`, `ATDD-251-T1-R29`, `ATDD-251-T1-R30`, `ATDD-251-T1-R31`, `ATDD-251-T1-R32`, `ATDD-253-T1-R09`, `ATDD-253-T1-R14`, `ATDD-253-T1-R18`, `ATDD-253-T1-R19`, `ATDD-262`. Each remains individually mapped through the coordinator coverage matrix; none is accepted by this source inventory.

### T021

[T021-connect-credit-editing-and-verify-the-people-outcome](../plan/13-people-and-credits/tasks/T021-connect-credit-editing-and-verify-the-people-outcome.md)

CreditPanel/RoleCreator/people picker/draft recovery; source API paths traced for every control, credit-list paging remains.

Assigned ATDD obligations (9): `ATDD-026`, `ATDD-027`, `ATDD-028`, `ATDD-079`, `ATDD-081`, `ATDD-262`, `ATDD-262-T1-R01`, `ATDD-262-T1-R02`, `ATDD-262-T1-R03`. Each remains individually mapped through the coordinator coverage matrix; none is accepted by this source inventory.

### T022

[T022-accept-and-process-bounded-artwork-streams](../plan/14-artwork-management-processing-and-attachment/tasks/T022-accept-and-process-bounded-artwork-streams.md)

Artwork chooser/progress is stream consumer; byte limits/codecs/scratch/mTLS/crash proof require actual Media boundary.

Assigned ATDD obligations (49): `ATDD-064`, `ATDD-038`, `ATDD-039`, `ATDD-040`, `ATDD-041`, `ATDD-042`, `ATDD-043`, `ATDD-044`, `ATDD-045`, `ATDD-046`, `ATDD-123`, `ATDD-124`, `ATDD-125`, `ATDD-126`, `ATDD-159`, `ATDD-160`, `ATDD-161`, `ATDD-162`, `ATDD-163`, `ATDD-168`, `ATDD-169`, `ATDD-170`, `ATDD-248`, `ATDD-248-T1-R01`, `ATDD-248-T1-R02`, `ATDD-248-T1-R03`, `ATDD-248-T1-R04`, `ATDD-248-T1-R05`, `ATDD-248-T1-R06`, `ATDD-248-T1-R07`, `ATDD-248-T1-R08`, `ATDD-248-T1-R09`, `ATDD-248-T1-R10`, `ATDD-248-T1-R11`, `ATDD-248-T1-R12`, `ATDD-248-T1-R13`, `ATDD-248-T1-R14`, `ATDD-248-T1-R15`, `ATDD-248-T1-R16`, `ATDD-250`, `ATDD-250-T1-R01`, `ATDD-250-T1-R02`, `ATDD-250-T1-R03`, `ATDD-250-T1-R04`, `ATDD-254-T1-R01`, `ATDD-263`, `ATDD-315`, `ATDD-305`, `ATDD-325`. Each remains individually mapped through the coordinator coverage matrix; none is accepted by this source inventory.

### T023

[T023-coordinate-protective-holds-attachment-and-cleanup](../plan/14-artwork-management-processing-and-attachment/tasks/T023-coordinate-protective-holds-attachment-and-cleanup.md)

Artwork cancel/receipt/current attachment consumer; holds/release/atomic references/cleanup require actual Media/Catalog boundaries.

Assigned ATDD obligations (58): `ATDD-064`, `ATDD-038`, `ATDD-039`, `ATDD-040`, `ATDD-041`, `ATDD-042`, `ATDD-043`, `ATDD-044`, `ATDD-045`, `ATDD-046`, `ATDD-123`, `ATDD-124`, `ATDD-125`, `ATDD-126`, `ATDD-159`, `ATDD-160`, `ATDD-161`, `ATDD-162`, `ATDD-163`, `ATDD-164`, `ATDD-165`, `ATDD-166`, `ATDD-167`, `ATDD-169`, `ATDD-170`, `ATDD-245`, `ATDD-245-T1-R01`, `ATDD-245-T1-R02`, `ATDD-245-T1-R03`, `ATDD-245-T1-R04`, `ATDD-245-T1-R05`, `ATDD-245-T1-R06`, `ATDD-245-T1-R07`, `ATDD-245-T1-R08`, `ATDD-245-T1-R09`, `ATDD-245-T1-R10`, `ATDD-249`, `ATDD-249-T1-R01`, `ATDD-249-T1-R02`, `ATDD-249-T1-R03`, `ATDD-249-T1-R04`, `ATDD-249-T1-R05`, `ATDD-249-T1-R06`, `ATDD-249-T1-R07`, `ATDD-251-T1-R33`, `ATDD-251-T1-R34`, `ATDD-253-T1-R24`, `ATDD-253-T1-R25`, `ATDD-253-T1-R26`, `ATDD-253-T1-R27`, `ATDD-254-T1-R02`, `ATDD-254-T1-R03`, `ATDD-254-T1-R04`, `ATDD-254-T1-R05`, `ATDD-254-T1-R07`, `ATDD-263`, `ATDD-315`, `ATDD-305`. Each remains individually mapped through the coordinator coverage matrix; none is accepted by this source inventory.

### T024

[T024-implement-current-impact-archive-restore-and-deletion](../plan/15-archive-management/tasks/T024-implement-current-impact-archive-restore-and-deletion.md)

ArchiveAction/Archives lifecycle consumer; current impact/concurrency/deletion backend contracts remain owner-enforced.

Assigned ATDD obligations (28): `ATDD-019`, `ATDD-020`, `ATDD-024`, `ATDD-025`, `ATDD-028`, `ATDD-082`, `ATDD-083`, `ATDD-084`, `ATDD-103`, `ATDD-104`, `ATDD-105`, `ATDD-106`, `ATDD-107`, `ATDD-108`, `ATDD-109`, `ATDD-110`, `ATDD-111`, `ATDD-087`, `ATDD-149`, `ATDD-150`, `ATDD-251-T1-R16`, `ATDD-251-T1-R25`, `ATDD-251-T1-R28`, `ATDD-253-T1-R20`, `ATDD-253-T1-R21`, `ATDD-253-T1-R22`, `ATDD-264`, `ATDD-311`. Each remains individually mapped through the coordinator coverage matrix; none is accepted by this source inventory.

### T025

[T025-connect-private-archives-and-irreversible-confirmations](../plan/15-archive-management/tasks/T025-connect-private-archives-and-irreversible-confirmations.md)

Archive tabs/restore/delete/current impact/original intent; actual browser and persisted irreversible effects required.

Assigned ATDD obligations (35): `ATDD-019`, `ATDD-020`, `ATDD-024`, `ATDD-025`, `ATDD-028`, `ATDD-082`, `ATDD-083`, `ATDD-084`, `ATDD-103`, `ATDD-104`, `ATDD-105`, `ATDD-106`, `ATDD-107`, `ATDD-108`, `ATDD-109`, `ATDD-110`, `ATDD-111`, `ATDD-087`, `ATDD-264`, `ATDD-264-T1-R01`, `ATDD-264-T1-R02`, `ATDD-264-T1-R03`, `ATDD-264-T1-R04`, `ATDD-264-T1-R05`, `ATDD-264-T1-R06`, `ATDD-264-T1-R07`, `ATDD-264-T1-R08`, `ATDD-264-T1-R09`, `ATDD-264-T1-R10`, `ATDD-264-T1-R11`, `ATDD-264-T1-R12`, `ATDD-264-T1-R13`, `ATDD-264-T1-R14`, `ATDD-264-T1-R15`, `ATDD-264-T1-R16`. Each remains individually mapped through the coordinator coverage matrix; none is accepted by this source inventory.

### T026

[T026-connect-measured-progress-and-recover-attachment-outcomes](../plan/16-artwork-management-browser-journeys/tasks/T026-connect-measured-progress-and-recover-attachment-outcomes.md)

Artwork measured XHR progress/cancellation/processing/attachment/result check; corrections ef2a3ff, remaining client pre-read/refresh gaps.

Assigned ATDD obligations (34): `ATDD-064`, `ATDD-038`, `ATDD-039`, `ATDD-040`, `ATDD-041`, `ATDD-042`, `ATDD-043`, `ATDD-044`, `ATDD-045`, `ATDD-046`, `ATDD-123`, `ATDD-124`, `ATDD-125`, `ATDD-126`, `ATDD-159`, `ATDD-160`, `ATDD-161`, `ATDD-162`, `ATDD-163`, `ATDD-169`, `ATDD-170`, `ATDD-263`, `ATDD-263-T1-R01`, `ATDD-263-T1-R02`, `ATDD-263-T1-R03`, `ATDD-263-T1-R04`, `ATDD-263-T1-R05`, `ATDD-263-T1-R06`, `ATDD-263-T1-R07`, `ATDD-263-T1-R08`, `ATDD-263-T1-R09`, `ATDD-263-T1-R10`, `ATDD-315`, `ATDD-305`. Each remains individually mapped through the coordinator coverage matrix; none is accepted by this source inventory.

### T027

[T027-prove-recovery-through-real-movie-person-and-credit-editors](../plan/17-draft-recovery-editor-integration/tasks/T027-prove-recovery-through-real-movie-person-and-credit-editors.md)

Real movie/person/credit editor recovery journeys and original saved IDs; no component-only substitution. Full fault/account/expiry matrix pending.

Assigned ATDD obligations (14): `ATDD-048`, `ATDD-179`, `ATDD-259`, `ATDD-259-T1-R01`, `ATDD-259-T1-R02`, `ATDD-259-T1-R03`, `ATDD-259-T1-R04`, `ATDD-259-T1-R05`, `ATDD-259-T1-R06`, `ATDD-259-T1-R07`, `ATDD-259-T1-R08`, `ATDD-259-T1-R09`, `ATDD-259-T1-R10`, `ATDD-333`. Each remains individually mapped through the coordinator coverage matrix; none is accepted by this source inventory.

### T031

[T031-expose-complete-read-only-audit-outcomes](../plan/19-accountable-history/tasks/T031-expose-complete-read-only-audit-outcomes.md)

Read-only audit UI and Cursor API corrected ef2a3ff; complete outcomes/diffs/role denial/paging require actual browser/API/storage.

Assigned ATDD obligations (18): `ATDD-053`, `ATDD-054`, `ATDD-055`, `ATDD-056`, `ATDD-088`, `ATDD-089`, `ATDD-101`, `ATDD-185`, `ATDD-251-T1-R19`, `ATDD-253-T1-R15`, `ATDD-265`, `ATDD-265-T1-R01`, `ATDD-265-T1-R02`, `ATDD-265-T1-R03`, `ATDD-265-T1-R04`, `ATDD-265-T1-R05`, `ATDD-265-T1-R06`, `ATDD-307`. Each remains individually mapped through the coordinator coverage matrix; none is accepted by this source inventory.

## Source follow-ups and proof limits

The coordinator's findings ledger is the only findings ledger; the observations here supply concrete source inputs for it. Audit/artwork changes are committed source fixes, not accepted behavioral proof. The next owned implementation lane will address public/list pagination, same-route parameter/history revalidation, session/person expiry guards and client artwork pre-read/label corrections. Root owns all movie metadata/editor/route corrections and artwork parent refresh. Retain initial source observations alongside corrected revisions rather than overwriting their history.

No remote workflow was dispatched by this authoring lane. No local build, browser, formatter, test or scanner was run. Actual checks must use the pinned remote runner and final composed SHA. Source regression support is colocated in audit/api.test.ts and artwork/{api,upload-controller,upload-poll}.test.ts; real journeys are in tests/browser/flows/remote, against make start after actual readiness. Bounded screenshots need case/route/step/theme/width/browser/SHA manifests and assertions; no implementation screenshot becomes an approved visual reference.

Both required assisted clients and native assistive-technology readers remain separate actual-host obligations. G-03 remains disabled until the approved trusted host assertion boundary is supplied. UI/browser/DOM/SDK harness observations cannot claim those host or speech cases passed. Neither this document nor passing unit tests grants formal selection acceptance.

## Follow-up source correction milestone

The initial observations above remain historical at their recorded SHA. Follow-up source `a31ea28` adds direct session/person resultExpired guards and effective artwork file-size/declared-format checks before FileReader; the artwork input has a separate explicit label and retains the original-upload lock. Root wires the effective artworkPolicy prop at `77898b6` and owns movie metadata, expiry and parent attachment/version refresh.

The next composed source adds actual server-cursor paging to catalog, public people, both public credit-detail lists, managed people and archived records. Public catalog/people cursor state is stored as an opaque history marker, filters reset it, reload/history restores it, and no credentials or draft values enter history. Detail-route parameter changes abort old reads, clear the previous identity and reload the actual new ID. Managed person parameter changes dispose the previous editor and its subscriptions before current session/bootstrap/drafts. Read failures gain explicit retries; a later page failure preserves current rows and cursor. Repeated cursors and duplicate identities fail visibly. Catalog text uses one 250 ms debounce and invalid/new criteria invalidate earlier generations immediately. Unknown management catch-all pages now state that the section is unavailable instead of presenting a generic implemented section.

Remaining source follow-ups outside this lane: the ten-year discovery select cannot choose older approved seed years; genre-load errors remain silent; managed movie and credit-list pagination are outside this author's exact paths and need their supported owners. Root owns managed movie paths. Archive keyboard tab semantics and missing actual native-reader observations remain separately assessed; paging source does not establish them. These are concrete coordinator-led follow-ups, not Not applicable rows. The new real-browser catalog cursor/reload/Back journey is partial ATDD-001 support and must execute remotely; it does not cover all search, fault, role, dataset, artwork, viewport, reader or host obligations.
