# PRD coverage reconciliation

PRD v2.4 · SHA-256 `55eb812ec2e51f09b88d1eacde1763b65833f55d6a9124feab906a03ed86be39`. 17 FRs, 16 NFRs and 159 ACs are accounted for. Visual mapping describes an inspectable contract, not passing production acceptance.

## Functional requirements

| ID | Disposition | Visual groups / reason |
|---|---|---|
| FR-001 | visual | [discovery](mockup/discovery.html) · [details](mockup/details.html) · [people](mockup/people.html) |
| FR-002 | visual | [discovery](mockup/discovery.html) · [people](mockup/people.html) |
| FR-003 | visual | [discovery](mockup/discovery.html) |
| FR-004 | visual | [management](mockup/management.html) · [archives](mockup/archives.html) · [policies](mockup/policies.html) |
| FR-005 | visual | [people](mockup/people.html) · [credits](mockup/credits.html) · [archives](mockup/archives.html) |
| FR-006 | visual | [people](mockup/people.html) · [credits](mockup/credits.html) · [archives](mockup/archives.html) |
| FR-007 | visual | [discovery](mockup/discovery.html) · [details](mockup/details.html) · [people](mockup/people.html) · [policies](mockup/policies.html) |
| FR-008 | technical | Initial seed count, snapshot, provenance validation and non-destructive reruns are data/engineering work; illustrative fixtures do not claim the 100/100 seed. |
| FR-009 | visual | [artwork](mockup/artwork.html) |
| FR-010 | visual | [management](mockup/management.html) · [artwork](mockup/artwork.html) |
| FR-011 | visual | [account](mockup/account.html) · [session](mockup/session.html) |
| FR-012 | visual | [details](mockup/details.html) · [account](mockup/account.html) · [session](mockup/session.html) · [people](mockup/people.html) · [archives](mockup/archives.html) · [audit](mockup/audit.html) · [ai](mockup/ai.html) |
| FR-013 | visual | [archives](mockup/archives.html) · [audit](mockup/audit.html) |
| FR-014 | technical | Durable follow-up, attempts and operations recovery are restricted operational tooling, not another application role or management screen (PRD 6.1). |
| FR-015 | technical | Notification delivery and storm prevention are operations responsibilities outside the catalog UI; no alert-management product screen is added. |
| FR-016 | visual | [ai](mockup/ai.html) |
| FR-017 | visual | [discovery](mockup/discovery.html) · [account](mockup/account.html) · [management](mockup/management.html) · [people](mockup/people.html) · [credits](mockup/credits.html) · [artwork](mockup/artwork.html) · [ai](mockup/ai.html) · [policies](mockup/policies.html) |

## Quality requirements

| ID | Disposition | Visual groups / reason |
|---|---|---|
| NFR-001 | technical | Visual contract only; backend, operational, delivery and integration evidence remains required by the PRD. No additional product screen is appropriate for this engineering/delivery obligation. |
| NFR-002 | cross-cutting | [management](mockup/management.html) · [artwork](mockup/artwork.html) |
| NFR-003 | technical | Visual contract only; backend, operational, delivery and integration evidence remains required by the PRD. No additional product screen is appropriate for this engineering/delivery obligation. |
| NFR-004 | cross-cutting | [management](mockup/management.html) · [artwork](mockup/artwork.html) · [archives](mockup/archives.html) |
| NFR-005 | cross-cutting | [policies](mockup/policies.html) |
| NFR-006 | technical | Visual contract only; backend, operational, delivery and integration evidence remains required by the PRD. No additional product screen is appropriate for this engineering/delivery obligation. |
| NFR-007 | cross-cutting | [discovery](mockup/discovery.html) · [details](mockup/details.html) · [account](mockup/account.html) · [session](mockup/session.html) · [management](mockup/management.html) · [people](mockup/people.html) · [credits](mockup/credits.html) · [artwork](mockup/artwork.html) · [archives](mockup/archives.html) · [audit](mockup/audit.html) · [ai](mockup/ai.html) · [policies](mockup/policies.html) |
| NFR-008 | technical | Visual contract only; backend, operational, delivery and integration evidence remains required by the PRD. No additional product screen is appropriate for this engineering/delivery obligation. |
| NFR-009 | technical | Visual contract only; backend, operational, delivery and integration evidence remains required by the PRD. No additional product screen is appropriate for this engineering/delivery obligation. |
| NFR-010 | technical | Visual contract only; backend, operational, delivery and integration evidence remains required by the PRD. No additional product screen is appropriate for this engineering/delivery obligation. |
| NFR-011 | technical | Visual contract only; backend, operational, delivery and integration evidence remains required by the PRD. No additional product screen is appropriate for this engineering/delivery obligation. |
| NFR-012 | technical | Visual contract only; backend, operational, delivery and integration evidence remains required by the PRD. No additional product screen is appropriate for this engineering/delivery obligation. |
| NFR-013 | cross-cutting | [policies](mockup/policies.html) · [management](mockup/management.html) · [account](mockup/account.html) |
| NFR-014 | cross-cutting | [management](mockup/management.html) · [people](mockup/people.html) · [archives](mockup/archives.html) · [ai](mockup/ai.html) |
| NFR-015 | cross-cutting | [account](mockup/account.html) · [session](mockup/session.html) · [audit](mockup/audit.html) · [ai](mockup/ai.html) |
| NFR-016 | cross-cutting | [policies](mockup/policies.html) · [account](mockup/account.html) · [artwork](mockup/artwork.html) · [ai](mockup/ai.html) |

## Acceptance criteria

| ID | Disposition | Inspectable states or technical disposition |
|---|---|---|
| AC-001 | visual | [catalog](mockup/discovery.html#catalog) · [movie](mockup/details.html#movie) |
| AC-002 | visual | [catalog-loading](mockup/discovery.html#catalog-loading) · [catalog-empty](mockup/discovery.html#catalog-empty) · [catalog-failure](mockup/discovery.html#catalog-failure) |
| AC-003 | visual | [catalog](mockup/discovery.html#catalog) · [movie](mockup/details.html#movie) |
| AC-004 | visual | [movie](mockup/details.html#movie) · [person-unavailable](mockup/people.html#person-unavailable) · [person](mockup/people.html#person) |
| AC-005 | visual | [unavailable](mockup/details.html#unavailable) |
| AC-006 | visual | [catalog](mockup/discovery.html#catalog) · [people](mockup/people.html#people) |
| AC-007 | visual | [catalog](mockup/discovery.html#catalog) · [catalog-no-match](mockup/discovery.html#catalog-no-match) |
| AC-008 | visual | [catalog](mockup/discovery.html#catalog) · [catalog-invalid](mockup/discovery.html#catalog-invalid) |
| AC-009 | visual | [catalog-loading](mockup/discovery.html#catalog-loading) · [catalog-failure](mockup/discovery.html#catalog-failure) · [catalog-outdated](mockup/discovery.html#catalog-outdated) |
| AC-010 | visual | [catalog-failure](mockup/discovery.html#catalog-failure) · [catalog-outdated](mockup/discovery.html#catalog-outdated) · [unavailable](mockup/details.html#unavailable) |
| AC-011 | visual | [catalog](mockup/discovery.html#catalog) |
| AC-012 | visual | [catalog](mockup/discovery.html#catalog) |
| AC-013 | visual | [catalog](mockup/discovery.html#catalog) |
| AC-014 | visual | [catalog](mockup/discovery.html#catalog) |
| AC-015 | visual | [catalog](mockup/discovery.html#catalog) · [catalog-no-match](mockup/discovery.html#catalog-no-match) |
| AC-016 | visual | [movies](mockup/management.html#movies) · [create](mockup/management.html#create) · [saved](mockup/management.html#saved) |
| AC-017 | visual | [movies](mockup/management.html#movies) · [editor](mockup/management.html#editor) · [saved](mockup/management.html#saved) · [optional-clearing](mockup/policies.html#optional-clearing) |
| AC-018 | visual | [editor](mockup/management.html#editor) · [create](mockup/management.html#create) · [save-failed](mockup/management.html#save-failed) |
| AC-019 | visual | [movie-delete](mockup/archives.html#movie-delete) · [deleted](mockup/archives.html#deleted) |
| AC-020 | visual | [editor](mockup/management.html#editor) · [target-changed](mockup/management.html#target-changed) |
| AC-021 | visual | [save-uncertain](mockup/management.html#save-uncertain) · [save-resolved](mockup/management.html#save-resolved) · [media-outage](mockup/management.html#media-outage) |
| AC-022 | visual | [people](mockup/people.html#people) · [people-loading](mockup/people.html#people-loading) · [people-empty](mockup/people.html#people-empty) · [people-failure](mockup/people.html#people-failure) · [person](mockup/people.html#person) |
| AC-023 | visual | [person-edit](mockup/people.html#person-edit) · [person-archived](mockup/people.html#person-archived) |
| AC-024 | visual | [person-archive](mockup/archives.html#person-archive) · [person-delete](mockup/archives.html#person-delete) · [deleted](mockup/archives.html#deleted) |
| AC-025 | visual | [person-conflict](mockup/people.html#person-conflict) |
| AC-026 | visual | [credits](mockup/credits.html#credits) |
| AC-027 | visual | [person-archived](mockup/people.html#person-archived) · [credits](mockup/credits.html#credits) · [credit-duplicate](mockup/credits.html#credit-duplicate) |
| AC-028 | visual | [credits](mockup/credits.html#credits) · [credit-retained](mockup/credits.html#credit-retained) |
| AC-029 | visual | [movie](mockup/details.html#movie) · [tv](mockup/details.html#tv) |
| AC-030 | visual | [movie](mockup/details.html#movie) · [tv](mockup/details.html#tv) · [optional-clearing](mockup/policies.html#optional-clearing) |
| AC-031 | visual | [movie](mockup/details.html#movie) · [tv](mockup/details.html#tv) · [identities](mockup/details.html#identities) · [people](mockup/people.html#people) · [person](mockup/people.html#person) |
| AC-032 | visual | [tv](mockup/details.html#tv) |
| AC-033 | technical | Seed/snapshot verification and preservation after reruns need real data and persistence; no seed-management UI is requested. Verify exact initial 100/100 counts without truncation or resurrecting records. |
| AC-034 | technical | Seed/snapshot verification and preservation after reruns need real data and persistence; no seed-management UI is requested. Verify exact initial 100/100 counts without truncation or resurrecting records. |
| AC-035 | technical | Seed/snapshot verification and preservation after reruns need real data and persistence; no seed-management UI is requested. Verify exact initial 100/100 counts without truncation or resurrecting records. |
| AC-036 | technical | Seed/snapshot verification and preservation after reruns need real data and persistence; no seed-management UI is requested. Verify exact initial 100/100 counts without truncation or resurrecting records. |
| AC-037 | technical | Seed/snapshot verification and preservation after reruns need real data and persistence; no seed-management UI is requested. Verify exact initial 100/100 counts without truncation or resurrecting records. |
| AC-038 | visual | [artwork](mockup/artwork.html#artwork) · [artwork-transfer](mockup/artwork.html#artwork-transfer) · [artwork-processing](mockup/artwork.html#artwork-processing) · [artwork-attached](mockup/artwork.html#artwork-attached) |
| AC-039 | visual | [artwork](mockup/artwork.html#artwork) · [artwork-invalid](mockup/artwork.html#artwork-invalid) |
| AC-040 | visual | [artwork-target](mockup/artwork.html#artwork-target) |
| AC-041 | visual | [artwork-transfer](mockup/artwork.html#artwork-transfer) · [artwork-cancelled](mockup/artwork.html#artwork-cancelled) · [artwork-failed](mockup/artwork.html#artwork-failed) |
| AC-042 | visual | [artwork-attached](mockup/artwork.html#artwork-attached) · [artwork-uncertain](mockup/artwork.html#artwork-uncertain) |
| AC-043 | visual | [artwork-failed](mockup/artwork.html#artwork-failed) |
| AC-044 | visual | [artwork-attached](mockup/artwork.html#artwork-attached) · [artwork-failed](mockup/artwork.html#artwork-failed) |
| AC-045 | visual | [artwork-uncertain](mockup/artwork.html#artwork-uncertain) |
| AC-046 | visual | [media-outage](mockup/management.html#media-outage) · [artwork-attached](mockup/artwork.html#artwork-attached) |
| AC-047 | visual | [login](mockup/account.html#login) · [viewer](mockup/account.html#viewer) · [maintainer](mockup/account.html#maintainer) · [auth-unavailable](mockup/session.html#auth-unavailable) |
| AC-048 | visual | [maintainer](mockup/account.html#maintainer) · [session-check](mockup/session.html#session-check) · [expired](mockup/session.html#expired) · [recheck](mockup/session.html#recheck) · [other-account](mockup/session.html#other-account) · [signout](mockup/session.html#signout) · [signed-out](mockup/session.html#signed-out) · [signout-failed](mockup/session.html#signout-failed) · [signout-uncertain](mockup/session.html#signout-uncertain) |
| AC-049 | visual | [login](mockup/account.html#login) · [viewer](mockup/account.html#viewer) · [maintainer](mockup/account.html#maintainer) |
| AC-050 | visual | [viewer](mockup/account.html#viewer) · [maintainer](mockup/account.html#maintainer) · [session-check](mockup/session.html#session-check) · [denied](mockup/session.html#denied) |
| AC-051 | visual | [viewer](mockup/account.html#viewer) · [denied](mockup/session.html#denied) |
| AC-052 | visual | [viewer](mockup/account.html#viewer) · [denied](mockup/session.html#denied) |
| AC-053 | visual | [saved](mockup/management.html#saved) · [audit](mockup/audit.html#audit) · [audit-detail](mockup/audit.html#audit-detail) |
| AC-054 | visual | [save-failed](mockup/management.html#save-failed) · [audit](mockup/audit.html#audit) · [audit-detail](mockup/audit.html#audit-detail) |
| AC-055 | visual | [audit](mockup/audit.html#audit) |
| AC-056 | visual | [audit](mockup/audit.html#audit) · [audit-detail](mockup/audit.html#audit-detail) · [audit-loading](mockup/audit.html#audit-loading) · [audit-empty](mockup/audit.html#audit-empty) · [audit-failed](mockup/audit.html#audit-failed) · [audit-denied](mockup/audit.html#audit-denied) |
| AC-057 | technical | Restricted operational follow-up/recovery/notification outcome. PRD 6.1 excludes new application roles/screens for this tooling; verify through the future operational design. |
| AC-058 | technical | Restricted operational follow-up/recovery/notification outcome. PRD 6.1 excludes new application roles/screens for this tooling; verify through the future operational design. |
| AC-059 | technical | Restricted operational follow-up/recovery/notification outcome. PRD 6.1 excludes new application roles/screens for this tooling; verify through the future operational design. |
| AC-060 | technical | Restricted operational follow-up/recovery/notification outcome. PRD 6.1 excludes new application roles/screens for this tooling; verify through the future operational design. |
| AC-061 | technical | Restricted operational follow-up/recovery/notification outcome. PRD 6.1 excludes new application roles/screens for this tooling; verify through the future operational design. |
| AC-062 | technical | Restricted operational follow-up/recovery/notification outcome. PRD 6.1 excludes new application roles/screens for this tooling; verify through the future operational design. |
| AC-063 | technical | Restricted operational follow-up/recovery/notification outcome. PRD 6.1 excludes new application roles/screens for this tooling; verify through the future operational design. |
| AC-064 | technical | Engineering/delivery obligation: Build and inspect the mandated frontend/backend technologies and selected compatible toolchain. Verify in technical design and implementation, not a static UI claim. |
| AC-065 | technical | Engineering/delivery obligation: Exercise meaningful real gRPC features across at least two running Spring Boot applications. Verify in technical design and implementation, not a static UI claim. |
| AC-066 | technical | Engineering/delivery obligation: Verify frontend GraphQL and documented upload/retrieval/auth exceptions against A05; reconcile the MCP boundary. Verify in technical design and implementation, not a static UI claim. |
| AC-067 | technical | Engineering/delivery obligation: Validate the preferred App/Catalog/Media responsibilities and absence of cross-service database access. Verify in technical design and implementation, not a static UI claim. |
| AC-068 | visual | [media-outage](mockup/management.html#media-outage) |
| AC-069 | technical | Engineering/delivery obligation: Verify JPA/Hibernate mappings, transactions, rollback, audit, and required-work consistency. Verify in technical design and implementation, not a static UI claim. |
| AC-070 | technical | Engineering/delivery obligation: Verify applicable native SQL parameterization, joins/order/pagination, mapping, plans, and write safeguards against the chosen engine. Verify in technical design and implementation, not a static UI claim. |
| AC-071 | technical | Restart persistence of records, archives, artwork, audit and pending work requires the real stack; static fixtures reset by design. |
| AC-072 | technical | Engineering/delivery obligation: Prove bounded safe exponential/jitter retry with one owner and deadline/library-attempt accounting. Verify in technical design and implementation, not a static UI claim. |
| AC-073 | visual | [save-uncertain](mockup/management.html#save-uncertain) |
| AC-074 | technical | Engineering/delivery obligation: Validate retry overrides/units/ranges; zero disables retries; D22 numerical defaults remain Recommended. Verify in technical design and implementation, not a static UI claim. |
| AC-075 | visual | [draft-cancel](mockup/management.html#draft-cancel) · [artwork-cancelled](mockup/artwork.html#artwork-cancelled) |
| AC-076 | technical | Engineering/delivery obligation: Verify required configuration and redacted startup/not-ready failure without unsafe fallback. Verify in technical design and implementation, not a static UI claim. |
| AC-077 | technical | Engineering/delivery obligation: Verify documented precedence and explicit missing/empty/false/zero semantics. Verify in technical design and implementation, not a static UI claim. |
| AC-078 | technical | Engineering/delivery obligation: Secrets. Verify in technical design and implementation, not a static UI claim. |
| AC-079 | technical | Engineering/delivery obligation: Distinguish process health, readiness, and actual operation success. Verify in technical design and implementation, not a static UI claim. |
| AC-080 | technical | Engineering/delivery obligation: Preserve sanitized correlation across frontend requests, internal calls, and durable work. Verify in technical design and implementation, not a static UI claim. |
| AC-081 | technical | Engineering/delivery obligation: Verify outcome/latency/retry/pending-age/dependency metrics and dashboards, including application errors under HTTP 200. Verify in technical design and implementation, not a static UI claim. |
| AC-082 | technical | Engineering/delivery obligation: Exercise actionable alerts, routing, deduplication, and channel failure under selected thresholds. Verify in technical design and implementation, not a static UI claim. |
| AC-083 | visual | [catalog](mockup/discovery.html#catalog) · [login](mockup/account.html#login) · [register](mockup/account.html#register) · [editor](mockup/management.html#editor) · [person-edit](mockup/people.html#person-edit) · [credits](mockup/credits.html#credits) · [artwork](mockup/artwork.html#artwork) |
| AC-084 | visual | [catalog](mockup/discovery.html#catalog) · [login](mockup/account.html#login) · [register](mockup/account.html#register) · [editor](mockup/management.html#editor) · [person-edit](mockup/people.html#person-edit) · [credits](mockup/credits.html#credits) · [artwork](mockup/artwork.html#artwork) |
| AC-085 | visual | [draft-cancel](mockup/management.html#draft-cancel) |
| AC-086 | technical | Engineering/delivery obligation: Trace unit and edge cases to included requirements without inventing a coverage percentage. Verify in technical design and implementation, not a static UI claim. |
| AC-087 | technical | Engineering/delivery obligation: Exercise GraphQL contracts, real gRPC, selected database, security/audit, native SQL where used, seed, artwork, recovery, and notification integration. Verify in technical design and implementation, not a static UI claim. |
| AC-088 | technical | Engineering/delivery obligation: Report failures/blocked/unexecuted checks honestly; test commands fail correctly and isolate normal data. Verify in technical design and implementation, not a static UI claim. |
| AC-089 | technical | Engineering/delivery obligation: Review cohesive tested vertical slices, shared rules, constructor dependencies, and justified abstractions. Verify in technical design and implementation, not a static UI claim. |
| AC-090 | technical | Engineering/delivery obligation: Verify readability, ownership, and explanation of generated/handwritten code and design tradeoffs. Verify in technical design and implementation, not a static UI claim. |
| AC-091 | technical | Engineering/delivery obligation: Fresh setup. Verify in technical design and implementation, not a static UI claim. |
| AC-092 | technical | Engineering/delivery obligation: Normal operations. Verify in technical design and implementation, not a static UI claim. |
| AC-093 | technical | Engineering/delivery obligation: Verification commands. Verify in technical design and implementation, not a static UI claim. |
| AC-094 | technical | Engineering/delivery obligation: Repeatability. Verify in technical design and implementation, not a static UI claim. |
| AC-095 | technical | Engineering/delivery obligation: Repository release access. Verify in technical design and implementation, not a static UI claim. |
| AC-096 | technical | Engineering/delivery obligation: README/AI disclosure. Verify in technical design and implementation, not a static UI claim. |
| AC-097 | technical | Engineering/delivery obligation: Walkthrough. Verify in technical design and implementation, not a static UI claim. |
| AC-098 | technical | Engineering/delivery obligation: Record relevant risks, selected controls, owners, evidence, and limitations. Verify in technical design and implementation, not a static UI claim. |
| AC-099 | technical | Engineering/delivery obligation: Distinguish intended controls from tested/deployed proof; do not imply ISO/regulatory certification, TLS enforcement, or successful restore without evidence. Verify in technical design and implementation, not a static UI claim. |
| AC-100 | technical | Engineering/delivery obligation: Define/verify GraphQL validation against section 5.5, authorization, omission versus clearing, and allowed partial results without partial invalid saves. Verify in technical design and implementation, not a static UI claim. |
| AC-101 | technical | Engineering/delivery obligation: Define/verify internal failure/status mapping, correlation, deadlines, cancellation, and safe recovery. Verify in technical design and implementation, not a static UI claim. |
| AC-102 | technical | Engineering/delivery obligation: Verify identity, units, presence, precision, Unicode-code-point counting, and boundary compatibility against section 5.5. Verify in technical design and implementation, not a static UI claim. |
| AC-103 | technical | Engineering/delivery obligation: Verify effective input/file policies from section 5.5; establish remaining request/query/page and decoded-image resource bounds with understandable outcomes. Verify in technical design and implementation, not a static UI claim. |
| AC-104 | visual | [recheck](mockup/session.html#recheck) · [editor](mockup/management.html#editor) · [target-changed](mockup/management.html#target-changed) · [person-conflict](mockup/people.html#person-conflict) |
| AC-105 | visual | [recheck](mockup/session.html#recheck) · [save-uncertain](mockup/management.html#save-uncertain) · [save-resolved](mockup/management.html#save-resolved) · [credit-duplicate](mockup/credits.html#credit-duplicate) |
| AC-106 | technical | Engineering/delivery obligation: Define/verify operation identity scope, incompatible reuse, in-progress/expiry/retention behavior, and safe replay. Verify in technical design and implementation, not a static UI claim. |
| AC-107 | visual | [movie](mockup/details.html#movie) · [identities](mockup/details.html#identities) · [people](mockup/people.html#people) |
| AC-108 | visual | [movie-archive](mockup/archives.html#movie-archive) · [movie-impact](mockup/archives.html#movie-impact) |
| AC-109 | visual | [movie-archive](mockup/archives.html#movie-archive) · [movie-restore](mockup/archives.html#movie-restore) |
| AC-110 | visual | [movies](mockup/management.html#movies) · [movie-archive](mockup/archives.html#movie-archive) · [movie-impact](mockup/archives.html#movie-impact) · [movie-delete](mockup/archives.html#movie-delete) |
| AC-111 | visual | [login](mockup/account.html#login) · [register](mockup/account.html#register) · [registered](mockup/account.html#registered) |
| AC-112 | visual | [register](mockup/account.html#register) · [registered](mockup/account.html#registered) · [registration-failure](mockup/account.html#registration-failure) |
| AC-113 | visual | [movie](mockup/details.html#movie) · [unavailable](mockup/details.html#unavailable) · [denied](mockup/session.html#denied) · [person-unavailable](mockup/people.html#person-unavailable) · [movie-archive](mockup/archives.html#movie-archive) · [archive-denied](mockup/archives.html#archive-denied) |
| AC-114 | visual | [audit](mockup/audit.html#audit) · [audit-detail](mockup/audit.html#audit-detail) |
| AC-115 | visual | [audit-loading](mockup/audit.html#audit-loading) · [audit-empty](mockup/audit.html#audit-empty) · [audit-failed](mockup/audit.html#audit-failed) · [audit-denied](mockup/audit.html#audit-denied) |
| AC-116 | visual | [chatgpt](mockup/ai.html#chatgpt) · [claude](mockup/ai.html#claude) |
| AC-117 | visual | [chatgpt](mockup/ai.html#chatgpt) · [claude](mockup/ai.html#claude) · [ai-approved](mockup/ai.html#ai-approved) · [ai-created](mockup/ai.html#ai-created) · [ai-approved-v2](mockup/ai.html#ai-approved-v2) · [ai-created-v2](mockup/ai.html#ai-created-v2) |
| AC-118 | visual | [chatgpt](mockup/ai.html#chatgpt) · [claude](mockup/ai.html#claude) · [ai-corrected](mockup/ai.html#ai-corrected) · [ai-rejected](mockup/ai.html#ai-rejected) · [ai-approved-v2](mockup/ai.html#ai-approved-v2) |
| AC-119 | visual | [ai-denied](mockup/ai.html#ai-denied) |
| AC-120 | visual | [chatgpt](mockup/ai.html#chatgpt) · [claude](mockup/ai.html#claude) · [ai-corrected](mockup/ai.html#ai-corrected) · [ai-unknown](mockup/ai.html#ai-unknown) |
| AC-121 | visual | [ai-created](mockup/ai.html#ai-created) · [ai-uncertain](mockup/ai.html#ai-uncertain) · [ai-created-v2](mockup/ai.html#ai-created-v2) |
| AC-122 | visual | [chatgpt](mockup/ai.html#chatgpt) · [claude](mockup/ai.html#claude) |
| AC-123 | visual | [movie](mockup/details.html#movie) · [other-account](mockup/session.html#other-account) · [people](mockup/people.html#people) · [person](mockup/people.html#person) · [ai-hostile](mockup/ai.html#ai-hostile) |
| AC-124 | technical | Encryption/key-rotation continuity requires real protected data and key management; normal login is shown, but a visual cannot verify rotation. |
| AC-125 | visual | [login](mockup/account.html#login) · [other-account](mockup/session.html#other-account) |
| AC-126 | visual | [chatgpt](mockup/ai.html#chatgpt) · [claude](mockup/ai.html#claude) · [ai-denied](mockup/ai.html#ai-denied) · [ai-hostile](mockup/ai.html#ai-hostile) |
| AC-127 | visual | [audit](mockup/audit.html#audit) · [audit-detail](mockup/audit.html#audit-detail) |
| AC-128 | visual | [auth-unavailable](mockup/session.html#auth-unavailable) · [policy-unavailable](mockup/policies.html#policy-unavailable) |
| AC-129 | visual | [person-archive](mockup/archives.html#person-archive) · [person-delete](mockup/archives.html#person-delete) · [impact-changed](mockup/archives.html#impact-changed) · [impact-current](mockup/archives.html#impact-current) |
| AC-130 | visual | [impact-changed](mockup/archives.html#impact-changed) · [archive-failed](mockup/archives.html#archive-failed) · [archive-uncertain](mockup/archives.html#archive-uncertain) |
| AC-131 | visual | [person-archive](mockup/archives.html#person-archive) · [person-impact](mockup/archives.html#person-impact) · [audit](mockup/audit.html#audit) |
| AC-132 | visual | [credit-retained](mockup/credits.html#credit-retained) · [person-archive](mockup/archives.html#person-archive) · [person-restore](mockup/archives.html#person-restore) · [audit](mockup/audit.html#audit) |
| AC-133 | visual | [person-archive](mockup/archives.html#person-archive) · [person-impact](mockup/archives.html#person-impact) · [person-delete](mockup/archives.html#person-delete) · [archive-denied](mockup/archives.html#archive-denied) · [audit](mockup/audit.html#audit) |
| AC-134 | visual | [unavailable](mockup/details.html#unavailable) · [denied](mockup/session.html#denied) · [people](mockup/people.html#people) · [person-unavailable](mockup/people.html#person-unavailable) · [person-archive](mockup/archives.html#person-archive) · [archive-loading](mockup/archives.html#archive-loading) · [archive-empty](mockup/archives.html#archive-empty) · [archive-failed](mockup/archives.html#archive-failed) · [archive-denied](mockup/archives.html#archive-denied) |
| AC-135 | visual | [person](mockup/people.html#person) · [credit-retained](mockup/credits.html#credit-retained) · [movie-restore](mockup/archives.html#movie-restore) · [person-archive](mockup/archives.html#person-archive) · [person-restore](mockup/archives.html#person-restore) |
| AC-136 | visual | [person-archived](mockup/people.html#person-archived) · [credit-retained](mockup/credits.html#credit-retained) · [person-archive](mockup/archives.html#person-archive) · [person-restore](mockup/archives.html#person-restore) |
| AC-137 | visual | [impact-changed](mockup/archives.html#impact-changed) · [deleted](mockup/archives.html#deleted) · [archive-failed](mockup/archives.html#archive-failed) · [archive-uncertain](mockup/archives.html#archive-uncertain) · [audit](mockup/audit.html#audit) · [impact-current](mockup/archives.html#impact-current) |
| AC-138 | technical | Seed/snapshot verification and preservation after reruns need real data and persistence; no seed-management UI is requested. Verify exact initial 100/100 counts without truncation or resurrecting records. |
| AC-139 | visual | [register](mockup/account.html#register) · [artwork](mockup/artwork.html#artwork) · [text-boundaries](mockup/policies.html#text-boundaries) · [catalog-invalid](mockup/discovery.html#catalog-invalid) |
| AC-140 | visual | [person-edit](mockup/people.html#person-edit) · [text-boundaries](mockup/policies.html#text-boundaries) |
| AC-141 | visual | [register](mockup/account.html#register) · [registration-failure](mockup/account.html#registration-failure) · [text-boundaries](mockup/policies.html#text-boundaries) |
| AC-142 | visual | [register](mockup/account.html#register) · [person-edit](mockup/people.html#person-edit) · [text-boundaries](mockup/policies.html#text-boundaries) |
| AC-143 | visual | [editor](mockup/management.html#editor) · [create](mockup/management.html#create) · [person-edit](mockup/people.html#person-edit) · [credits](mockup/credits.html#credits) · [optional-clearing](mockup/policies.html#optional-clearing) |
| AC-144 | visual | [editor](mockup/management.html#editor) · [create](mockup/management.html#create) · [credits](mockup/credits.html#credits) · [text-boundaries](mockup/policies.html#text-boundaries) |
| AC-145 | visual | [register](mockup/account.html#register) · [registration-failure](mockup/account.html#registration-failure) |
| AC-146 | visual | [login](mockup/account.html#login) · [register](mockup/account.html#register) · [registration-failure](mockup/account.html#registration-failure) |
| AC-147 | visual | [text-boundaries](mockup/policies.html#text-boundaries) |
| AC-148 | visual | [create](mockup/management.html#create) · [person-edit](mockup/people.html#person-edit) · [ai-unknown](mockup/ai.html#ai-unknown) |
| AC-149 | visual | [artwork](mockup/artwork.html#artwork) · [artwork-invalid](mockup/artwork.html#artwork-invalid) |
| AC-150 | visual | [artwork](mockup/artwork.html#artwork) · [artwork-invalid](mockup/artwork.html#artwork-invalid) |
| AC-151 | visual | [artwork](mockup/artwork.html#artwork) · [artwork-invalid](mockup/artwork.html#artwork-invalid) |
| AC-152 | visual | [artwork-failed](mockup/artwork.html#artwork-failed) |
| AC-153 | visual | [artwork-policy](mockup/artwork.html#artwork-policy) · [policy-changed](mockup/policies.html#policy-changed) |
| AC-154 | visual | [policy-unavailable](mockup/policies.html#policy-unavailable) |
| AC-155 | visual | [login](mockup/account.html#login) · [artwork-policy](mockup/artwork.html#artwork-policy) · [policy-changed](mockup/policies.html#policy-changed) |
| AC-156 | visual | [ai-corrected](mockup/ai.html#ai-corrected) · [ai-unknown](mockup/ai.html#ai-unknown) · [ai-approved-v2](mockup/ai.html#ai-approved-v2) · [ai-created-v2](mockup/ai.html#ai-created-v2) |
| AC-157 | visual | [artwork-policy](mockup/artwork.html#artwork-policy) · [policy-changed](mockup/policies.html#policy-changed) |
| AC-158 | technical | Seed/snapshot verification and preservation after reruns need real data and persistence; no seed-management UI is requested. Verify exact initial 100/100 counts without truncation or resurrecting records. |
| AC-159 | visual | [catalog](mockup/discovery.html#catalog) · [catalog-invalid](mockup/discovery.html#catalog-invalid) |


Catalog AC-008 now includes six sort directions, Previous/Next replacement pages, retained criteria/history and directional recovery. Its existing discovery mapping remains; new runtime and visual acceptance are pending under ATDD-008.
