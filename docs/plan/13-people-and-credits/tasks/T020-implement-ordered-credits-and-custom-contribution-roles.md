---
kind: task
id: T020
group: G13
depends_on:
- T009
- T006
- T007
- T012
- T016
- T018
readiness: Ready
status: Implementation complete
risk: high
test_strategy: test-first
visual_review: not-applicable
---

# T020 — Implement ordered credits and custom contribution roles

[Feature group](../group.md) · [Overall plan](../../../06-PLAN.md)

## Purpose and scope

Roles: one person can be Actor and Director with multiple whole character strings, including embedded slash; no cloned person or duplicated identical credit. This task owns the change surface and local evidence below. Risk is high because permission, retained state, irreversible effects or a real service boundary can fail without an obvious UI error. Review the task-specific negative and recovery cases before coding. Unrelated features and source-document changes are outside this task.

## Traceability

| Obligation | Role |
|---|---|
| ATDD-026 | implements |
| ATDD-027 | implements |
| ATDD-079 | implements |
| ATDD-081 | implements |
| ATDD-240 | implements |
| ATDD-240-T1-R01 | implements |
| ATDD-240-T1-R02 | implements |
| ATDD-240-T1-R03 | implements |
| ATDD-240-T1-R04 | implements |
| ATDD-240-T1-R05 | implements |
| ATDD-240-T1-R06 | implements |
| ATDD-241 | implements |
| ATDD-241-T1-R01 | implements |
| ATDD-241-T1-R02 | implements |
| ATDD-241-T1-R03 | implements |
| ATDD-241-T1-R04 | implements |
| ATDD-241-T1-R05 | implements |
| ATDD-241-T1-R06 | implements |
| ATDD-241-T1-R07 | implements |
| ATDD-241-T1-R08 | implements |
| ATDD-251-T1-R13 | implements |
| ATDD-251-T1-R29 | implements |
| ATDD-251-T1-R30 | implements |
| ATDD-251-T1-R31 | implements |
| ATDD-251-T1-R32 | implements |
| ATDD-253-T1-R09 | implements |
| ATDD-253-T1-R14 | implements |
| ATDD-253-T1-R18 | implements |
| ATDD-253-T1-R19 | implements |
| ATDD-262 | implements |
| FR-006 | implements |
| NFR-014 | implements |
| A09 | implements |
| A10 | implements |
| D09 | implements |
| D25 | implements |
| AC-026 | implements |
| AC-026-C01 | implements |
| AC-027 | implements |
| AC-027-C01 | implements |
| AC-027-C02 | implements |
| AC-104 | implements |
| AC-104-C01 | implements |
| AC-107 | implements |
| AC-107-C01 | implements |
| G-03 | implements |
| OD-009 | implements |
| PRD-PERM-01 | implements |
| PRD-PERM-05 | implements |
| PRD-POLICY-05 | implements |
| PRD-BOUND-05 | implements |
| TD-ROW-L2736 | both |
| TD-ROW-L2752 | both |
| TD-ROW-L2753 | both |
| TD-ROW-L2754 | both |
| TD-ROW-L2755 | both |
| TD-ROW-L4614 | both |
| TD-ROW-L4615 | both |
| TD-ROW-L4616 | both |
| TD-ROW-L4617 | both |
| TD-ROW-L4618 | both |
| TD-ROW-L4619 | both |
| TD-ROW-L4658 | both |
| TD-ROW-L4659 | both |
| TD-ROW-L4660 | both |
| TD-ROW-L4661 | both |
| TD-ROW-L4662 | both |
| TD-ROW-L4663 | both |
| TD-ROW-L4664 | both |
| TD-ROW-L4665 | both |

## Prerequisites

- [T009 — Enforce scoped service identity and shared admission](../../07-guarded-access/tasks/T009-enforce-scoped-service-identity-and-shared-admission.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Identity: forged/wrong-audience/stale-role/revoked assertions fail before business work; valid mTLS alone never grants actor authority.
- [T006 — Implement effective policy and precise input semantics](../../04-effective-input-policy/tasks/T006-implement-effective-policy-and-precise-input-semantics.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Boundaries: each configured minimum/maximum and adjacent value gives the source-prescribed result for each field; code-point length distinguishes precomposed and combining sequences.
- [T007 — Implement owner receipts and atomic mutation evidence](../../05-reliable-work-mutation-receipts/tasks/T007-implement-owner-receipts-and-atomic-mutation-evidence.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Replay: identical concurrent requests produce one effect/receipt/audit/work set; changed content under the key fails without another mutation.
- [T012 — Serve precise public catalog and people reads](../../10-catalog-discovery/tasks/T012-serve-precise-public-catalog-and-people-reads.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Selection: 2024 year-only, 2024-10 and 2024-10-09 match year; October excludes year-only; unknown year appears only without a selected year.
- [T016 — Implement authoritative movie creation and edits](../../12-movie-management/tasks/T016-implement-authoritative-movie-creation-and-edits.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Create: valid A input commits one active movie with stable identity, required genres/year, safe receipt and matching audit; invalid/Viewer input commits none.
- [T018 — Implement shared person writes and identity](T018-implement-shared-person-writes-and-identity.md): consume its accepted implementation, declared contracts/change surface and local evidence. Specifically, Names: one and 100 code points pass the configured rule, zero and101 reject with no partial change; mononyms and punctuation remain intact.

All executable commands below are proposed interfaces produced by T001; none exists at planning time. Use its isolated fixture/runtime and protected synthetic access. Normal unfinished dependencies are execution prerequisites, not evidence of a failed implementation. Keep any newly discovered material contract incompatibility visible with affected source IDs and next action.

## Change surface

All listed implementation paths are proposed. Existing source documents remain read-only. Paths resolve from repository `app`.

| Repository | Action | Path | Responsibility |
|---|---|---|---|
| app | create | services/catalog/src/main/kotlin/com/spoolin/catalog/features/credit/ | T020 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | services/catalog/src/main/kotlin/com/spoolin/catalog/features/taxonomy/roles/ | T020 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | services/core/src/main/kotlin/com/spoolin/core/features/catalogview/credit/ | T020 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | services/catalog/src/test/kotlin/com/spoolin/catalog/features/credit/ | T020 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | tests/integration/src/test/kotlin/com/spoolin/integration/catalog/credits/ | T020 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | services/catalog/src/test/kotlin/com/spoolin/catalog/features/taxonomy/roles/ | T020 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |
| app | create | services/core/src/test/kotlin/com/spoolin/core/features/catalogview/credit/ | T020 owns the task-specific code/configuration/tests within this target; preserve the declared feature boundary |

Tests follow the owner paths above and the source test map. Frontend unit/component tests are colocated within the declared feature/component paths; JVM units use the declared src/test paths. The cross-service tests/integration project belongs only to the integration selector. If a source path has moved, record the actual producer/consumer mapping before continuing; do not silently broaden the task.

## Knowledge

Read overall shared rules → feature group → this task → the selected source sections/cases. Expand adjacent definitions only when needed to resolve a dependency.

- [5.3 Operation, permission and deadline inventory](../../../04-spoolin-techdoc.md#53-operation-permission-and-deadline-inventory): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.
- [5.4 Projection and presence crosswalk](../../../04-spoolin-techdoc.md#54-projection-and-presence-crosswalk): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.
- [6.2.5 Catalog tables](../../../04-spoolin-techdoc.md#625-catalog-tables): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.
- [7.1 Transaction inventory](../../../04-spoolin-techdoc.md#71-transaction-inventory): adopt the complete applicable signatures, types, field presence, storage rules, bounds, state transitions and failure semantics for the owned surface.

The following assigned source clauses are a checklist, not new contracts. Read their exact linked context; source examples retain their stated status.

| Source knowledge | Rule or exact owned surface |
|---|---|
| [ATDD-240-T1-R01 · L9758](../../../05-atdd.md#case-atdd-240-enforce-contribution-role-storage) | `id` (`uuid`); F-DB lowercase, hyphenated UUID; malformed UUID fails conversion, not a CHECK; Omission fails23502; explicit NULL fails23502; `PRIMARY KEY; no SQL default; owner supplies value` |
| [ATDD-240-T1-R02 · L9759](../../../05-atdd.md#case-atdd-240-enforce-contribution-role-storage) | `category` (`varchar(8)`); Source-valid technical token; 8 versus 9 PostgreSQL characters tests physical capacity independently of any declared allowlist; no configured user-text rule inferred; Omission fails23502; explicit NULL fails23502; `NOT NULL CHECK (category IN ('CAST','CREATOR')); no SQL default; owner supplies value` |
| [ATDD-240-T1-R03 · L9760](../../../05-atdd.md#case-atdd-240-enforce-contribution-role-storage) | `label` (`text`); Exact Unicode/plain text including blank/space/long content; reject only declared btrim/length/shape predicates, not an invented configured SQL maximum; Omission fails23502; explicit NULL fails23502; `NOT NULL; no SQL default; owner supplies value` |
| [ATDD-240-T1-R04 · L9761](../../../05-atdd.md#case-atdd-240-enforce-contribution-role-storage) | `normalized_label` (`text`); Exact Unicode/plain text including blank/space/long content; reject only declared btrim/length/shape predicates, not an invented configured SQL maximum; Omission fails23502; explicit NULL fails23502; `NOT NULL; no SQL default; owner supplies value` |
| [ATDD-240-T1-R05 · L9762](../../../05-atdd.md#case-atdd-240-enforce-contribution-role-storage) | `allows_characters` (`boolean`); true/false; no invented enum rejection; Omission supplies FALSE; explicit NULL fails23502; `NOT NULL DEFAULT false; NOT NULL (explicit or PK)` |
| [ATDD-240-T1-R06 · L9763](../../../05-atdd.md#case-atdd-240-enforce-contribution-role-storage) | `created_at` (`timestamptz(6)`); F-CLOCK UTC instant with six fractional digits; roundtrip microseconds. Finite-wire/expiry policy is owner validation unless DDL declares it; Omission supplies CURRENT_TIMESTAMP; explicit NULL fails23502; `NOT NULL DEFAULT CURRENT_TIMESTAMP; NOT NULL (explicit or PK)` |
| [ATDD-241-T1-R01 · L9809](../../../05-atdd.md#case-atdd-241-enforce-credit-relationship-and-ordered-character-storage) | `id` (`uuid`); F-DB lowercase, hyphenated UUID; malformed UUID fails conversion, not a CHECK; Omission fails23502; explicit NULL fails23502; `PRIMARY KEY; no SQL default; owner supplies value` |
| [ATDD-241-T1-R02 · L9810](../../../05-atdd.md#case-atdd-241-enforce-credit-relationship-and-ordered-character-storage) | `title_id` (`uuid`); F-DB lowercase, hyphenated UUID; malformed UUID fails conversion, not a CHECK; Omission fails23502; explicit NULL fails23502; `NOT NULL REFERENCES title(id) ON DELETE CASCADE; no SQL default; owner supplies value` |
| [ATDD-241-T1-R03 · L9811](../../../05-atdd.md#case-atdd-241-enforce-credit-relationship-and-ordered-character-storage) | `person_id` (`uuid`); F-DB lowercase, hyphenated UUID; malformed UUID fails conversion, not a CHECK; Omission fails23502; explicit NULL fails23502; `NOT NULL REFERENCES person(id) ON DELETE CASCADE; no SQL default; owner supplies value` |
| [ATDD-241-T1-R04 · L9812](../../../05-atdd.md#case-atdd-241-enforce-credit-relationship-and-ordered-character-storage) | `role_id` (`uuid`); F-DB lowercase, hyphenated UUID; malformed UUID fails conversion, not a CHECK; Omission fails23502; explicit NULL fails23502; `NOT NULL REFERENCES credit_role(id) ON DELETE RESTRICT; no SQL default; owner supplies value` |
| [ATDD-241-T1-R05 · L9813](../../../05-atdd.md#case-atdd-241-enforce-credit-relationship-and-ordered-character-storage) | `characters` (`text[]`); Ordered complete strings, empty/NULL-element/two-dimensional arrays only against declared array predicates; no invented per-entry SQL limit; Omission supplies CAST('{}' AS text[]); explicit NULL fails23502; `NOT NULL DEFAULT '{}'::text[] CHECK (array_position(characters,NULL) IS NULL); NOT NULL (explicit or PK)` |
| [ATDD-241-T1-R06 · L9814](../../../05-atdd.md#case-atdd-241-enforce-credit-relationship-and-ordered-character-storage) | `version` (`bigint`); Source-valid integer; physical upper 9223372036854775807 versus 9223372036854775808 tests range separately from row acceptance; declared CHECK probes -1, 0, 1; Omission supplies 0; explicit NULL fails23502; `NOT NULL DEFAULT 0 CHECK (version >= 0); NOT NULL (explicit or PK)` |
| [ATDD-241-T1-R07 · L9815](../../../05-atdd.md#case-atdd-241-enforce-credit-relationship-and-ordered-character-storage) | `created_at` (`timestamptz(6)`); F-CLOCK UTC instant with six fractional digits; roundtrip microseconds. Finite-wire/expiry policy is owner validation unless DDL declares it; Omission supplies CURRENT_TIMESTAMP; explicit NULL fails23502; `NOT NULL DEFAULT CURRENT_TIMESTAMP; NOT NULL (explicit or PK)` |
| [ATDD-241-T1-R08 · L9816](../../../05-atdd.md#case-atdd-241-enforce-credit-relationship-and-ordered-character-storage) | `updated_at` (`timestamptz(6)`); F-CLOCK UTC instant with six fractional digits; roundtrip microseconds. Finite-wire/expiry policy is owner validation unless DDL declares it; Omission supplies CURRENT_TIMESTAMP; explicit NULL fails23502; `NOT NULL DEFAULT CURRENT_TIMESTAMP; NOT NULL (explicit or PK)` |
| [ATDD-251-T1-R13 · L10359](../../../05-atdd.md#case-atdd-251-exercise-every-graphql-operation-at-its-authoritative-boundary) | `managedCredits`; Maintainer / Catalog \| Exactly one titleId/personId selector; retained surviving relationships; F-PERMISSIONS applies; omission rejects only for arguments actually declared non-null without a default in this operation’s SDL; no secrets or internal state escape the declared projection. |
| [ATDD-251-T1-R29 · L10375](../../../05-atdd.md#case-atdd-251-exercise-every-graphql-operation-at-its-authoritative-boundary) | `createRole`; Maintainer / Catalog \| Normalized category/label uniqueness; Starter Actor permits characters; custom roles default to allowsCharacters=false and CREATOR forbids characters; F-PERMISSIONS applies; omission rejects only for arguments actually declared non-null without a default in this operation’s SDL; no secrets or internal state escape the declared projection. |
| [ATDD-251-T1-R30 · L10376](../../../05-atdd.md#case-atdd-251-exercise-every-graphql-operation-at-its-authoritative-boundary) | `addCredit`; Maintainer / Catalog \| Active movie/person, title expectedVersion, known role, normalized unique association; F-PERMISSIONS applies; omission rejects only for arguments actually declared non-null without a default in this operation’s SDL; no secrets or internal state escape the declared projection. |
| [ATDD-251-T1-R31 · L10377](../../../05-atdd.md#case-atdd-251-exercise-every-graphql-operation-at-its-authoritative-boundary) | `updateCredit`; Maintainer / Catalog \| Credit expectedVersion; active endpoints; role/characters mask; identity endpoints immutable; F-PERMISSIONS applies; omission rejects only for arguments actually declared non-null without a default in this operation’s SDL; no secrets or internal state escape the declared projection. |
| [ATDD-251-T1-R32 · L10378](../../../05-atdd.md#case-atdd-251-exercise-every-graphql-operation-at-its-authoritative-boundary) | `removeCredit`; Maintainer / Catalog \| Credit expectedVersion; active movie; archived-person relationship may be removed; parent/impact versions advance; F-PERMISSIONS applies; omission rejects only for arguments actually declared non-null without a default in this operation’s SDL; no secrets or internal state escape the declared projection. |
| [ATDD-253-T1-R09 · L10473](../../../05-atdd.md#case-atdd-253-verify-catalogservice-typed-service-and-actor-boundary) | `CreateRole`; CreateRole(RoleWrite) → Outcome |
| [ATDD-253-T1-R14 · L10478](../../../05-atdd.md#case-atdd-253-verify-catalogservice-typed-service-and-actor-boundary) | `ManagedCredits`; ManagedCredits(PageRequest) → CreditPage |
| [ATDD-253-T1-R18 · L10482](../../../05-atdd.md#case-atdd-253-verify-catalogservice-typed-service-and-actor-boundary) | `WriteCredit`; WriteCredit(CreditWrite) → Outcome |
| [ATDD-253-T1-R19 · L10483](../../../05-atdd.md#case-atdd-253-verify-catalogservice-typed-service-and-actor-boundary) | `RemoveCredit`; RemoveCredit(VersionedId) → Outcome |
| [FR-006 · L240](../../../02-PRD.md#61-functional-requirements) | Maintain cast/creator credits, starter/custom roles, and multiple roles/character names; respect person lifecycle \| A09/A10 support; D25 Committed; product policy Committed (section 13.1) \| AC-026 through AC-028, AC-132, AC-135, AC-136, AC-143/AC-144 |
| [NFR-014 · L272](../../../02-PRD.md#62-quality-and-delivery-requirements) | Preserve stale drafts and prevent unintended duplicate effects without merging distinct works/people \| Approved product policy Committed; D09 mechanism Recommended; D21 safe replay Committed \| AC-104 through AC-107 |
| [A09 · L288](../../../02-PRD.md#63-project-requirements-coverage) | List cast and creators \| FR-001/FR-005/FR-006 / AC-004, AC-022, AC-026, AC-134 |
| [A10 · L289](../../../02-PRD.md#63-project-requirements-coverage) | Add, update, delete cast and creators \| FR-005/FR-006 / AC-023 through AC-028, AC-131 through AC-133 |
| [D09 · L310](../../../02-PRD.md#64-project-decision-register) | Optimistic concurrency; separate duplicate requests/data \| Stale-save rejection selected as product behavior; mechanism stays with techdoc. NFR-014 / AC-104 through AC-107 |
| [D25 · L326](../../../02-PRD.md#64-project-decision-register) | Metadata, type/genres, stable identities, provenance \| FR-006/FR-007 / AC-026 through AC-032 |
| [AC-026 · L390](../../../02-PRD.md#us-004-maintain-people-and-credits) | Multiple roles \| An active shared person and active movie exist \| A Maintainer assigns credits \| Actor/Director/Writer/Producer are available; custom roles fit Cast or Creator; one person may have multiple roles and multiple acting character names without cloning the person. |
| [AC-026-C01 · L390](../../../02-PRD.md#us-004-maintain-people-and-credits) | Actor/Director/Writer/Producer are available; custom roles fit Cast or Creator; one person may have multiple roles and multiple acting character names without cloning the person. |
| [AC-027 · L391](../../../02-PRD.md#us-004-maintain-people-and-credits) | Invalid/duplicate credit \| A name/category/reference is missing, the selected person is archived, or an identical credit exists \| The Maintainer saves a new credit \| Request correction for invalid input; reject assignment to an archived person until restored, and do not create duplicate identical display entries. Custom labels use consistent category/identity treatment. |
| [AC-027-C01 · L391](../../../02-PRD.md#us-004-maintain-people-and-credits) | Request correction for invalid input; reject assignment to an archived person until restored, and do not create duplicate identical display entries. |
| [AC-027-C02 · L391](../../../02-PRD.md#us-004-maintain-people-and-credits) | Custom labels use consistent category/identity treatment. |
| [AC-104 · L491](../../../02-PRD.md#us-018-save-edits-without-losing-or-duplicating-work) | Stale save \| Two Maintainers edit the same movie/person \| One saves before the other \| Reject the later stale save, retain its draft, and require review before resubmission; no force-overwrite option is offered. |
| [AC-104-C01 · L491](../../../02-PRD.md#us-018-save-edits-without-losing-or-duplicating-work) | Reject the later stale save, retain its draft, and require review before resubmission; no force-overwrite option is offered. |
| [AC-107 · L493](../../../02-PRD.md#us-018-save-edits-without-losing-or-duplicating-work) | Similar real-world records \| Independent works/people share names \| Records are created or compared \| Preserve distinct identities and do not merge by text alone; explain genuine identity conflicts without silently changing another record. |
| [AC-107-C01 · L493](../../../02-PRD.md#us-018-save-edits-without-losing-or-duplicating-work) | Preserve distinct identities and do not merge by text alone; explain genuine identity conflicts without silently changing another record. |
| [G-03 · L924](../../../02-PRD.md#122-release-gates-remain-planned) | Required management/discovery, movie/person lifecycle, metadata/seed/precision, and preservation rules work \| FR-001 through FR-008, FR-017; AC-001 through AC-037, AC-108 through AC-110, AC-129 through AC-144, AC-148, AC-155/AC-158/AC-159 \| Planned |
| [OD-009 · L1022](../../../02-PRD.md#131-resolved-product-decisions) | Reject stale writes, retain draft, require review; no force overwrite \| NFR-014; AC-020/AC-025/AC-104; concurrency mechanism transfers to techdoc |
| [PRD-PERM-01 · L68](../../../02-PRD.md#31-permission-matrix) | Browse/search/filter active movies and TV series; view metadata and public people/credits \| Yes \| Yes \| Yes |
| [PRD-PERM-05 · L72](../../../02-PRD.md#31-permission-matrix) | Manage shared people and custom credit roles \| No \| No \| Yes |
| [PRD-POLICY-05 · L122](../../../02-PRD.md#51-included-behavior) | Cast and creators share reusable Person records. Start with Actor in Cast and Director, Writer, and Producer in Creator. Maintainers may add custom labels within either category. A person may have several roles on a movie; an acting credit may list several character names. Repeated identical credits do not create duplicated display entries. Names and titles are not globally unique identities; similarly named people and remakes remain distinct. |
| [PRD-BOUND-05 · L181](../../../02-PRD.md#recommended-defaults-for-other-inputs) | Custom credit role \| 1 character \| 100 characters \| Must belong to Cast or Creator |
| [TD-ROW-L2736 · L2736](../../../04-spoolin-techdoc.md#59-browser-operation-inventory) | `managedCredits` \| Maintainer / Catalog \| Exactly one titleId/personId selector; retained surviving relationships |
| [TD-ROW-L2752 · L2752](../../../04-spoolin-techdoc.md#59-browser-operation-inventory) | `createRole` \| Maintainer / Catalog \| Normalized category/label uniqueness; Starter Actor permits characters; custom roles default to allowsCharacters=false and CREATOR forbids characters |
| [TD-ROW-L2753 · L2753](../../../04-spoolin-techdoc.md#59-browser-operation-inventory) | `addCredit` \| Maintainer / Catalog \| Active movie/person, title expectedVersion, known role, normalized unique association |
| [TD-ROW-L2754 · L2754](../../../04-spoolin-techdoc.md#59-browser-operation-inventory) | `updateCredit` \| Maintainer / Catalog \| Credit expectedVersion; active endpoints; role/characters mask; identity endpoints immutable |
| [TD-ROW-L2755 · L2755](../../../04-spoolin-techdoc.md#59-browser-operation-inventory) | `removeCredit` \| Maintainer / Catalog \| Credit expectedVersion; active movie; archived-person relationship may be removed; parent/impact versions advance |
| [TD-ROW-L4614 · L4614](../../../04-spoolin-techdoc.md#credit_role) | `id` \| `uuid` \| `PRIMARY KEY; no SQL default; owner supplies value` \| Application-generated immutable identity. API; presence/clearing §5 \| `UUID` → UUID / string |
| [TD-ROW-L4615 · L4615](../../../04-spoolin-techdoc.md#credit_role) | `category` \| `varchar(8)` \| `NOT NULL CHECK (category IN ('CAST','CREATOR')); no SQL default; owner supplies value` \| Contribution category, not access role. API; presence/clearing §5 \| `String` → String/declared enum / string |
| [TD-ROW-L4616 · L4616](../../../04-spoolin-techdoc.md#credit_role) | `label` \| `text` \| `NOT NULL; no SQL default; owner supplies value` \| Default 1–100 code points. API; presence/clearing §5 \| `String` → String/declared enum / string |
| [TD-ROW-L4617 · L4617](../../../04-spoolin-techdoc.md#credit_role) | `normalized_label` \| `text` \| `NOT NULL; no SQL default; owner supplies value` \| Trim + PostgreSQL casefold(pg_unicode_fast) + NFC identity within category. internal; presence/clearing §5 \| `String` → not exposed; no GraphQL/proto field |
| [TD-ROW-L4618 · L4618](../../../04-spoolin-techdoc.md#credit_role) | `allows_characters` \| `boolean` \| `NOT NULL DEFAULT false; NOT NULL (explicit or PK)` \| True only starter Cast/Actor in release. API; presence/clearing §5 \| `Boolean` → Boolean / bool |
| [TD-ROW-L4619 · L4619](../../../04-spoolin-techdoc.md#credit_role) | `created_at` \| `timestamptz(6)` \| `NOT NULL DEFAULT CURRENT_TIMESTAMP; NOT NULL (explicit or PK)` \| Creation instant. API; presence/clearing §5 \| `Instant` → DateTime / Timestamp |
| [TD-ROW-L4658 · L4658](../../../04-spoolin-techdoc.md#credit) | `id` \| `uuid` \| `PRIMARY KEY; no SQL default; owner supplies value` \| Application-generated immutable identity. API; presence/clearing §5 \| `UUID` → UUID / string |
| [TD-ROW-L4659 · L4659](../../../04-spoolin-techdoc.md#credit) | `title_id` \| `uuid` \| `NOT NULL REFERENCES title(id) ON DELETE CASCADE; no SQL default; owner supplies value` \| Local title. API; presence/clearing §5 \| `UUID` → UUID / string |
| [TD-ROW-L4660 · L4660](../../../04-spoolin-techdoc.md#credit) | `person_id` \| `uuid` \| `NOT NULL REFERENCES person(id) ON DELETE CASCADE; no SQL default; owner supplies value` \| Shared person, may become archived. API; presence/clearing §5 \| `UUID` → UUID / string |
| [TD-ROW-L4661 · L4661](../../../04-spoolin-techdoc.md#credit) | `role_id` \| `uuid` \| `NOT NULL REFERENCES credit_role(id) ON DELETE RESTRICT; no SQL default; owner supplies value` \| Role identity. API; presence/clearing §5 \| `UUID` → UUID / string |
| [TD-ROW-L4662 · L4662](../../../04-spoolin-techdoc.md#credit) | `characters` \| `text[]` \| `NOT NULL DEFAULT '{}'::text[] CHECK (array_position(characters,NULL) IS NULL); NOT NULL (explicit or PK)` \| Each supplied nonempty name 1–200; reject duplicate trimmed names with field feedback; array order preserved. API; presence/clearing §5 \| `List<String>` → [String!] / repeated string |
| [TD-ROW-L4663 · L4663](../../../04-spoolin-techdoc.md#credit) | `version` \| `bigint` \| `NOT NULL DEFAULT 0 CHECK (version >= 0); NOT NULL (explicit or PK)` \| Optimistic row version; increment on every domain change. API; presence/clearing §5 \| `Long` → Long(decimal string) / int64 |
| [TD-ROW-L4664 · L4664](../../../04-spoolin-techdoc.md#credit) | `created_at` \| `timestamptz(6)` \| `NOT NULL DEFAULT CURRENT_TIMESTAMP; NOT NULL (explicit or PK)` \| Creation instant. API; presence/clearing §5 \| `Instant` → DateTime / Timestamp |
| [TD-ROW-L4665 · L4665](../../../04-spoolin-techdoc.md#credit) | `updated_at` \| `timestamptz(6)` \| `NOT NULL DEFAULT CURRENT_TIMESTAMP; NOT NULL (explicit or PK)` \| Owner sets to transaction timestamp on update; not a DB automatic update. API; presence/clearing §5 \| `Instant` → DateTime / Timestamp |

## Implementation

1. Implement starter roles plus createRole with consistent normalized label/category identity and uniqueness; contribution role never grants application permission.
2. Implement add/update/remove credit under current movie/person/role/version/policy checks. Require active movie and person for new assignment, while removal of a retained archived-person credit on active movie remains permitted.
3. Preserve ordered arrays of complete character names, each bounded separately; apply selected collection envelopes without slash splitting or artificial identity merging.
4. Enforce identical-credit uniqueness and serialize with person/lifecycle changes. Increment affected impact/version state as the source requires and commit relationship/receipt/audit together.
5. Deny direct TV credit writes and dangling/missing/changed references; real constraints and owner policy checks are distinct proof rows.

## Development guidance

Follow [shared execution rules](../../../06-PLAN.md#shared-execution-rules). Keep feature command/query/validation/projection/persistence close, constructor dependencies explicit and DTOs immutable. Generated contracts are not entities or authorization. Do not import another service's repository, copy a schema, invent a fourth domain service, or bypass the owner with fixture SQL.

Use parameter binding, fixed identifiers/allowlisted projections and safe text sinks at the relevant boundary. Keep account secrets, raw files, CSRF, approval challenges and private payloads out of retained evidence. New source policy requires a recorded owner decision; an unavailable client/runtime is not permission to weaken acceptance. Do not alter expected values, delete negative rows or replace visual references merely to make checks pass.

## Acceptance

- **T020-A1:** Roles: one person can be Actor and Director with multiple whole character strings, including embedded slash; no cloned person or duplicated identical credit.
- **T020-A2:** Concurrency: duplicate role/credit races yield one allowed identity/effect; changed references require review with no dangling relation.
- **T020-A3:** Lifecycle: retained credit removal from an active movie survives person restoration; direct TV management remains denied.

Verification ownership is scoped: the exact cases and case rows in Tests require their full stated boundary. Shared requirement/AC/contract clauses require this task’s owned contribution and local assertions; their combined outcome is reconciled by the case, feature and release owners. An implementation-only contribution stops at its owned change and local tests; a downstream full-case result is not a condition for this task to finish.

## Tests

Write a meaningful failing rule/state regression test first; confirm the intended assertion fails rather than an import/setup error, implement the change, then rerun. Derive expected values independently from source fixtures, not implementation output.

Use [ATDD fixtures and approach](../../../05-atdd.md#approach-and-fixtures) and [C-RESET](../../../05-atdd.md#c-reset-shared-cleanup). Each case/table/recipe subrow resets its own F-STACK/F-ACTORS state; pin time/randomness and use observable barriers for races rather than sleep-only synchronization. Preserve sanitized failure and fresh-reader state, then clean only that UUID namespace.

### Assigned case assertions

These are full verification assignments at the source level. Each linked case's complete Test data, Preconditions, Given/When/Then, Verification and Cleanup remains binding; recipe variants need separate results even when they have no printed row ID.

| Case / level | Concrete input or fixture | Required assertion and observation boundary |
|---|---|---|
| Local contract contribution | Owned source clauses and selected section fixtures | Verify generated/owner behavior locally; full assigned case proof remains with the downstream owner identified in the root register |

Partial implementation contributions: [ATDD-026](../../../05-atdd.md#case-atdd-026-multiple-roles), [ATDD-027](../../../05-atdd.md#case-atdd-027-invalidduplicate-credit), [ATDD-079](../../../05-atdd.md#case-atdd-079-stale-save), [ATDD-081](../../../05-atdd.md#case-atdd-081-similar-real-world-records), [ATDD-240](../../../05-atdd.md#case-atdd-240-enforce-contribution-role-storage), [ATDD-241](../../../05-atdd.md#case-atdd-241-enforce-credit-relationship-and-ordered-character-storage), [ATDD-262](../../../05-atdd.md#case-atdd-262-inspect-credits-state-transitions-and-feedback). These require local unit/owner tests here; their real end-to-end/system pass remains with the root verification owner. Do not wait for downstream completion to finish this local task.

Unit/component tests prove only isolated logic or the controlled component. PostgreSQL constraints/locks/commit/crash tests use PostgreSQL18.6 and fresh independent reads; transport tests use actual TCP mTLS/gRPC; browser/client and provider claims need their named real boundary. Direct DDL probes prove only declared SQL constraints, not permission, policy or cryptographic validity.

## Validation

Workdir is `/Users/mekari/Developer/personal-project/movie-app` for every command/procedure. T001 creates documented `--task` selectors and maps this task to its listed test paths/levels. Required empty/unavailable selections fail with setup exit2. Aggregate release commands do not infer passes for unselected cases.

| Check | Method or command | Working directory | Expected result | Required |
|---|---|---|---|---|
| local-tests | `bash ops/bin/test-unit.sh --task T020` — planned, produced by T001; generated contracts/build and this task's isolated tests required | `/Users/mekari/Developer/personal-project/movie-app` | Owned build/validation rules/components pass with independently fixed outcomes; no normal DB access | yes |
| boundary-tests | `bash ops/bin/test-integration.sh --task T020` — planned, produced by T001; disposable owner runtime and assigned real boundary required | `/Users/mekari/Developer/personal-project/movie-app` | Every assigned row/recipe at its source level passes; prohibited effects absent; no false skipped pass | yes |
| self-review | Implementer compares baseline/change, exact source clauses, tests and negative effects; records findings and corrections | `/Users/mekari/Developer/personal-project/movie-app` | All task criteria addressed; required findings fixed and affected checks rerun | yes |
| independent-review | Fresh reviewer reads task/group/root and precise sources, inspects baseline/change and actual evidence without trusting implementer verdict | `/Users/mekari/Developer/personal-project/movie-app` | Source compliance/code quality/risk checks complete; required findings resolved for reviewed revision | yes |

## Review and evidence

The implementer checks the diff and runs affected tests. One independent reviewer then inspects the complete requested selection, including this task, relevant source contracts, coherent connections, actual results, security and accessibility. Reference the same selection review from each task's independent-review row; no separate task or intermediate-group review is required. Record current inputs, distinct implementer/reviewer identities, required findings and fixes in one ledger. Correct in the same branch and rerun affected checks; self-review cannot replace final independent review.

Visual review is not applicable: this owned result has no new graphical presentation. Any consumer UI/host proof remains with its explicitly assigned task; service/CLI evidence observes responses, state, files and diagnostics.

## Evidence

| Check | Result | Artifact | Revision | Environment | Reviewer |
|---|---|---|---|---|---|
| local-tests | Not run | — | — | — | — |
| boundary-tests | Not run | — | — | — | — |
| self-review | Not run | — | — | — | — |
| independent-review | Not run | — | — | — | — |

Create evidence files only after an actual authorized run. A Passed row needs a nonempty local linked artifact, implementation revision/content fingerprint and real environment; review rows identify different people/agent identities. Retain prior runs and mark affected evidence Stale after code/source changes.


## Implementation delivery (17 October 2026)

Implementation for this task is on branch `codex/g04-g16-delivery` ([PR #8](https://github.com/okfriansyah-moh/spoolIn/pull/8)). Local task and browser runners were **not** executed on the authoring machine (host resource limit). Use CI or a better-resourced environment to run `ops/bin/test-unit.sh` / `ops/bin/test-integration.sh` for this task; set `SPOOLIN_TASK_BROWSER=1` for lighter Playwright when running browser integrations. Formal acceptance, combined review, and release proof remain with **G26**.

## Completion and handoff

- Finish the owned changes and all verification-role acceptance/row assertions; record actual changed files and source-contract dispositions.
- Reach Ready instructions and Passed evidence for every required check, including implementation diff review, the shared final selection review and applicable behavior/visual checks. Any unresolved required oracle prevents the affected task from Done.
- Record exact revision, generated/artifact locations, commands/results, findings/rechecks, fixture cleanup and any unverified downstream proof.
- Hand the owned outputs to [T021 — Connect credit editing and verify the people outcome](T021-connect-credit-editing-and-verify-the-people-outcome.md) for feature integration. This task may finish its local obligations while that downstream evidence remains Not run; feature completion waits for all group tasks plus integrated proof.
- If unfinished, retain status In progress/In review or a genuine external Blocked prerequisite; list the exact unresolved finding, last verified state, attempted fixes and next file/command/action. Budget exhaustion alone is not an external blocker.
- Initial next action: read the prerequisite outputs, verify source fingerprints and task eligibility, then perform Implementation step1 under the separately authorized implementation request.
