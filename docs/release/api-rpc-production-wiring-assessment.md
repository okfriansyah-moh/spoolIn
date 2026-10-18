# API and RPC production wiring assessment

Initial assessed source: `1b4402699b2eacf9d03717327a2a06d42e8b5fbd`, 2026-10-18. Scope: all 19 Query roots, 17 Mutation roots and 45 RPC methods in the approved source contracts. This source diagnosis records registrations, owner paths and response boundaries. It is not an independent review, remote execution result or formal acceptance. Every selected row still requires its assigned current remote proof. The coordinator owns finding IDs, status and evidence links.

## Registration and shared boundaries

The source contracts are `contracts/graphql/schema.graphql` and `contracts/grpc/src/main/proto/spoolin/v1/spoolin.proto`. `BrowserGraphql.execute` receives `/graphql`, checks `BrowserBoundary`, parses/bounds the envelope, applies `BrowserIdentity`/`BrowserAdmission`, then invokes `PolicyGraphql`. `PolicyGraphqlConfiguration` loads the packaged schema; `PolicyGraphqlProviders.resolved` supplies the production feature beans and `PolicyGraphql` binds their fetchers. Scalar, shape, depth, size and alias handling use `PolicyScalars` and `GraphqlEnvelope`; variable DTO conversion occurs in each resolver.

Browser mutations require the exact configured HTTPS origin and session/anonymous CSRF authority. Identity and mutation/read rate admission precede dispatch. Protected Catalog resolvers issue exact audience/method/operation/correlation actor assertions; the downstream Catalog interceptor verifies signature and live Core identity. A non-null authenticated browser principal alone does not grant Maintainer permission. Catalog owner handlers independently require the management role and enforce version/policy/relationship conditions.

Core `Application` registers `IdentityRpc` and global service identity/connector interceptors; `McpGatewayConfiguration.mcpGatewayRpc` is the MCP BindableService bean. Catalog `Application.catalogService` constructs the single `CatalogServiceRpc` from configured owner handlers. Media `Application.mediaService` constructs the single `MediaServiceImpl` delegating to upload, holds, assets and receipt reads. Spring gRPC registers these BindableService beans. Global service interceptors check exact certificate SAN and method before business dispatch; Catalog additionally uses `CatalogActors`, Media uses `MediaActors` and persisted asset/hold grants. A method merely being overridden is not evidence that these production beans started.

Relevant feature configuration is explicit: Catalog write/lifecycle/artwork/proposal providers, Core account/draft/operation providers, actor signing/verification and catalog-view configuration must resolve in the intended deployment. Nullable provider fallbacks return unavailable/denied rather than successful no-ops; the configured startup and actual remote journeys must prove these branches are not silently used in the intended application.

## GraphQL root inventory

Resolver paths below are relative to `services/core/src/main/kotlin/com/spoolin/core/`. `CV` means `features/catalogview/`. Every row is a source trace with remote proof pending, including rows corrected in this change.

| Root | Production resolver and DTO/downstream | Owner storage/result and failure boundary |
| --- | --- | --- |
| Query.session | `boundary/graphql/AccountGraphql.session`; BrowserContext | SessionStore live credential/expiry/epoch; ProtectedAccounts identity decrypt; anonymous CSRF or authenticated identity projection. Unavailable protected data fails. |
| Query.policy | `boundary/graphql/PolicyGraphql`; PolicyProvider | Effective validated PublicPolicy projection, not a database write; no credential fields. |
| Query.titles | `CV/read/CatalogViewGraphql.titles`; SearchInput → SearchTitles | Catalog TitleSearchSql ACTIVE SQL/keyset/filters, title_genre and ProvenanceReads; TitlePage projection. Invalid search/cursor fails. Genre labels/provenance corrected here. |
| Query.title | `CV/read/CatalogViewGraphql.title`; UUID → GetTitle | ACTIVE title SQL; absent/archived returns null from empty owner title, never same-name replacement. Metadata corrections apply. |
| Query.people | `CV/read/CatalogViewGraphql.people`; text/page → SearchPeople | PersonSearch ACTIVE SQL and scoped cursor; PersonConnection. Invalid input/cursor fails. |
| Query.person | `CV/read/CatalogViewGraphql.person`; UUID → GetPerson | PersonSearch ACTIVE row; empty owner response maps null. |
| Query.titleCredits | `CV/read/CatalogViewGraphql.titleCredits`; UUID/page → TitleCredits | PersonSearch credit/title/person/role join with ACTIVE visibility; CreditConnection and cursor. |
| Query.personCredits | `CV/read/CatalogViewGraphql.personCredits`; UUID/page → PersonCredits | Same public visibility join, person selector; CreditConnection and cursor. |
| Query.genres | `CV/read/CatalogViewGraphql.genres`; ListGenres | TaxonomyReads genre SQL; ID/label list. Taxonomy failures propagate, no seed-label constants. |
| Query.creditRoles | `CV/read/CatalogViewGraphql.creditRoles`; ListRoles | TaxonomyReads credit_role SQL; declared role category/characters fields. |
| Query.managedTitle | `CV/movie/ManagedCatalogGraphql.managedTitle`; actor + UUID → ManagedTitle | ManagedMovieReads exact active/archived row, genres/provenance; null if absent; management enforced at Catalog. Metadata corrections apply. |
| Query.managedPerson | `CV/person/ManagedPersonGraphql.managedPerson`; actor + UUID → ManagedPerson | ManagedPersonReads exact private row; absent null, permission enforced. |
| Query.managedCredits | `CV/credit/ManagedCreditGraphql.managedCredits`; exactly one title/person selector → ManagedCredits | ManagedCreditReads private join/page; rejects both/neither selectors. |
| Query.managedTitles | `CV/movie/ManagedCatalogGraphql.managedTitles`; state/page + actor → ManagedTitles | ManagedMovieReads explicit ACTIVE/ARCHIVED SQL/keyset; metadata corrections apply. |
| Query.managedPeople | `CV/person/ManagedPersonGraphql.managedPeople`; state/page + actor → ManagedPeople | ManagedPersonReads explicit state SQL/keyset, management predicate. |
| Query.deletionImpact | `CV/lifecycle/LifecycleGraphql.deletionImpact`; person UUID + actor → DeletionImpact | Catalog DeletionImpact/ImpactSnapshot credit relationship snapshot and signed short-lived token; not a deletion or success substitute. |
| Query.operation | `boundary/graphql/PolicyGraphql`, BrowserContext/OperationQueries; UUID/optional owner | OperationQueries/OperationRouter actor-scoped Core/Catalog/Media receipt lookup; anonymous/retained logout scope where allowed; ambiguity/hidden/expiry handling. Lookup is not a new mutation. |
| Query.drafts | `features/draft/DraftGraphql.drafts`; principal account | ProtectedDrafts account-bound eligible encrypted draft rows; decrypt/expiry/version projection. No other owner's draft. |
| Query.audit | `CV/movie/ManagedCatalogGraphql.audit`; optional subject/page + actor → Audit | ManagedMovieReads/AuditReads audit_event SQL; safe actor reference and changes, scoped cursor; management required. |
| Mutation.register | `boundary/graphql/AccountGraphql.register`; RegistrationInput + anonymous submission | RegisterAccount validates identity/password, ProtectedAccounts encrypted insert, account alias/hash and OwnerReceipts transaction; rejects authenticated registration/duplicate/conflict. |
| Mutation.signIn | `boundary/graphql/AccountGraphql.signIn`; LoginInput + anonymous/current credential | SignInAccount protected lookup/password verification, rate checks, SessionStore issue/rotation and Core receipt; response sets secure session cookie only when issued. |
| Mutation.signOut | `boundary/graphql/AccountGraphql.signOut`; operation/credential/CSRF | SignOutAccount and SessionStore revoke/retained receipt; deletes cookie after successful receipt; replay retains owner outcome. |
| Mutation.createMovie | `CV/movie/ManagedCatalogGraphql.createMovie`; MovieInput → MovieWrite without id | MovieWrites owner validation/transaction/title and genres, audit/work/receipt; typed Outcome → MutationResult, remembered owner CATALOG. |
| Mutation.updateMovie | `CV/movie/ManagedCatalogGraphql.updateMovie`; MoviePatch/update mask + version → MovieWrite | MovieWrites locked current active movie, changed fields/policy/version; same transaction and original receipt conflict semantics. |
| Mutation.movieLifecycle | `CV/lifecycle/LifecycleGraphql.movieLifecycle`; LifecycleAction/version/confirmed → MovieLifecycle | LifecycleStore archive/restore/delete, credits/attachments/seed tombstones, audit/work/receipt; confirmed destructive branch only. |
| Mutation.createPerson | `CV/person/ManagedPersonGraphql.createPerson`; PersonInput → PersonWrite without id | PersonWrites validated insert plus audit/work/receipt, distinct identity. |
| Mutation.updatePerson | `CV/person/ManagedPersonGraphql.updatePerson`; PersonPatch/mask/version → PersonWrite | PersonWrites lock/current active/version and changed-field checks, atomic audit/receipt. |
| Mutation.personLifecycle | `CV/lifecycle/LifecycleGraphql.personLifecycle`; action/version/confirmation/impact token → PersonLifecycle | PersonLifecycle/LifecycleStore verifies current impact and person/credit relationships; tombstone/delete/audit/work/receipt atomic. |
| Mutation.createRole | `CV/credit/ManagedCreditGraphql.createRole`; category/label/policy → RoleWrite | RoleWrites normalized-label uniqueness and declared category/characters rules; credit_role plus receipt/audit. |
| Mutation.addCredit | `CV/credit/ManagedCreditGraphql.addCredit`; CreditInput/title/version → CreditWrite | CreditWrites active movie/person/role/characters and duplicate checks; credit insert, parent versions, audit/receipt. |
| Mutation.updateCredit | `CV/credit/ManagedCreditGraphql.updateCredit`; CreditPatch/mask/version → CreditWrite | CreditWrites locked current relationships, role/characters compatibility and conflict; parent versions/audit/receipt. |
| Mutation.removeCredit | `CV/credit/ManagedCreditGraphql.removeCredit`; VersionedId | CreditWrites delete/current version/parent bumps and seed-member tombstone with receipt/audit. |
| Mutation.uploadArtwork | `features/artworkproxy/ArtworkGraphql.uploadArtwork`; bounded Base64 spool → BeginArtwork + streamed Media.Upload | Catalog intent/PENDING receipt before Media upload; owner Media validation/quarantine/processing work, Catalog attachment reconciliation. Returns actual pending outcome, not unconditional success. |
| Mutation.cancelArtwork | `features/artworkproxy/ArtworkGraphql.cancelArtwork`; upload operation → CancelArtwork | Catalog actor-scoped PREPARING cancellation and release work; original upload receipt preserved in ArtworkCancelResult. |
| Mutation.saveDraft | `features/draft/DraftGraphql.saveDraft`; DraftInput/id/optional version | SaveDraft validation/policy, account-bound ProtectedDrafts encryption/version and Core receipt; explicit-null create version rejected. |
| Mutation.discardDraft | `features/draft/DraftGraphql.discardDraft`; id/required version | DiscardDraft owner/version checks, deletion and receipt; boolean discarded reflects result rather than assumed success. |

Nested fields also matter: `CatalogViewGraphql` binds `Title.credits` to the public TitleCredits RPC. Management callers use the separate managedCredits query for private credits. `ArtworkGraphql` binds `Title.artwork` through ArtworkReadProxy to Catalog ResolveArtworkAccess and Media metadata; binary delivery uses its authorized HTTP/read proxy. This correction binds `Genre.label` to actual ListGenres, cached once within the GraphQL execution, and projects only typed FieldSource properties from provenance. General GraphQL errors are sanitized by PolicyGraphql, while typed owner Outcome problems remain business results. More specific transport-error-to-GraphQL classification requires its own current negative assertions; this assessment does not claim every error envelope already conforms merely because a resolver exists.

## RPC method inventory

For Catalog rows, registration is `services/catalog/src/main/kotlin/com/spoolin/catalog/Application.kt:catalogService` → `boundary/grpc/CatalogServiceRpc.kt`; paths following it are relative to the Catalog package. For Media rows, registration is Media `Application.mediaService` → `features/upload/MediaServiceImpl.kt`; handler paths are relative to the Media package. Core rows use the Core package. Every selected RPC row has runtime proof pending in this assessment.

| Service.method | Caller/authentication and DTO | Actual owner path, response/error disposition |
| --- | --- | --- |
| Identity.ResolveActor | No permitted SAN; retained ResolveActorRequest → Actor contract | **No selected network flow** under TD lines2645–2650. Core resolves credentials locally. Global Core ServiceIdentityInterceptor must deny core/catalog/media/mcp before default handler. Do not add a network identity resolver to fill a manifest count. New real mTLS denial test covers all four callers. |
| Identity.CheckActor | catalog SAN; CheckActorRequest account/credential/epoch/audience | Core `boundary/grpc/IdentityRpc.checkActor` → SessionStore.checkActor or ConnectorStore credential/live/account-role lookup; Actor exact identity/role/client/expiry. Wrong request denied; backing failure unavailable. |
| Catalog.SearchTitles | core SAN, public Search | `features/search/TitleSearchSql.search`, scoped signed CatalogCursor; title/genre/provenance queries → TitlePage. Active rows only; input/cursor errors fail. |
| Catalog.GetTitle | core SAN, public Id | TitleSearchSql.findById → Title or empty absent/hidden; same provenance/genre projection. |
| Catalog.SearchPeople | core SAN, public Search | `features/person/read/PersonSearch.searchPeople`, scoped PeopleCursor → PersonPage. |
| Catalog.GetPerson | core SAN, public Id | PersonSearch.findPerson → active Person or empty absent/hidden. |
| Catalog.PersonCredits | core SAN, public PageRequest | PersonSearch.personCredits, visible person/title join and cursor → CreditPage. |
| Catalog.TitleCredits | core SAN, public PageRequest | PersonSearch.titleCredits, same visibility and cursor → CreditPage. |
| Catalog.ListGenres | core SAN, Empty | `features/taxonomy/read/TaxonomyReads.listGenres` → Genres from actual SQL. |
| Catalog.ListRoles | core SAN, Empty | TaxonomyReads.listRoles → Roles from actual SQL. |
| Catalog.CreateRole | core SAN + live management actor; RoleWrite | `features/taxonomy/roles/RoleWrites.createRole` → normalized credit_role mutation and owner receipt/audit → Outcome. |
| Catalog.ManagedTitles | core SAN + live management actor; PageRequest state | `features/movie/ManagedMovieReads.managedTitles` → private SQL/keyset → TitlePage. |
| Catalog.ManagedPeople | core SAN + live management actor; PageRequest state | `features/person/read/ManagedPersonReads.managedPeople` → private SQL/keyset → PersonPage. |
| Catalog.ManagedTitle | core SAN + live management actor; Id | ManagedMovieReads.managedTitle → private title/provenance/genre SQL → Title/absent. |
| Catalog.ManagedPerson | core SAN + live management actor; Id | ManagedPersonReads.managedPerson → private person SQL → Person/absent. |
| Catalog.ManagedCredits | core SAN + live management actor; PageRequest target kind/id | `features/credit/ManagedCreditReads.managedCredits` → private relationship SQL → CreditPage. |
| Catalog.Audit | core SAN + live management actor; PageRequest subject/page | ManagedMovieReads.audit → `operations/audit/AuditReads` → AuditPage; no mutation. |
| Catalog.WriteMovie | core SAN + live management actor; MovieWrite context/values/mask/version | `features/movie/MovieWrites.writeMovie` → locked validated title/genre write and owner audit/work/receipt transaction → Outcome. |
| Catalog.WritePerson | core SAN + live management actor; PersonWrite context/values/mask/version | `features/person/write/PersonWrites.writePerson` → person write/current version/audit/receipt → Outcome. |
| Catalog.WriteCredit | core SAN + live management actor; CreditWrite | `features/credit/CreditWrites.writeCredit` → relationship validation and credit/parent versions/audit/receipt → Outcome. |
| Catalog.RemoveCredit | core SAN + live management actor; VersionedId | CreditWrites.removeCredit → delete/tombstone/parent versions/audit/receipt → Outcome. |
| Catalog.MovieLifecycle | core SAN + live management actor; Lifecycle | `features/lifecycle/MovieLifecycle.apply` → LifecycleStore current movie/action/version/confirmation and dependent SQL → Outcome. |
| Catalog.PersonLifecycle | core SAN + live management actor; Lifecycle | `features/lifecycle/PersonLifecycle.apply` → ImpactSnapshot/token and LifecycleStore locked person/credit effects → Outcome. |
| Catalog.DeletionImpact | core SAN + live management actor; Id | `features/lifecycle/DeletionImpact.preview` → current ImpactSnapshot and signed ImpactToken → Impact. |
| Catalog.GetOperation | core SAN + live actor; Id | `operations/receipts/OwnerReceiptReads.find` exact actor scope → Outcome; hidden/not-found denied or absent, no new effect. |
| Catalog.BeginArtwork | core SAN + live management actor; Hold | `features/artwork/ArtworkRpc.beginArtwork` → current active movie/version, artwork_attachment_request/PENDING receipt/durable_work → Outcome. |
| Catalog.GetAttachment | media SAN; Id/persisted tuple | ArtworkRpc.getAttachment → attachment projection helper and exact intent state; attachment evidence, no management permission. |
| Catalog.CancelArtwork | core SAN + live management actor; CancelArtworkRequest | ArtworkRpc.cancelArtwork → actor-scoped intent/current phase, cancellation/release work and original receipt projection → ArtworkCancelOutcome. |
| Catalog.ResolveArtworkAccess | core SAN; ArtworkAccessRequest and public/current or signed management scope | ArtworkRpc.resolveArtworkAccess → attachment authorization helper/current title/asset/hold grant → ArtworkGrant; no grant for hidden unauthorized row. |
| Catalog.ProposeMovie | core SAN + live client-bound Maintainer; Proposal | `features/proposal/ProposalWrites.propose` → normalized immutable ProposalRevisions/ProposalStore and deferred original receipt; owner digest/private transient challenge → ProposalRevision. |
| Catalog.ApproveAndCreate | core SAN + client-bound Maintainer, prior trusted UI assertion checked at Core; ApprovalInput | ProposalWrites.approve → exact current binding/challenge/expiry/one-use, MovieWrites.publishFromProposal in owner transaction → original Outcome. Production host entry remains blocked G-03. |
| Catalog.RejectProposal | same scoped authorization; ApprovalInput | ProposalWrites.reject → exact revision/binding/challenge, rejected original receipt without new movie; repeats/committed outcome retain original Outcome. Host entry remains blocked G-03. |
| Media.Upload | core SAN + actor bound to PREPARING Catalog intent; stream UploadChunk | `features/upload/UploadRpc.upload` → bounded header/chunks, quarantine, owner receipt/asset/processing work; actual PENDING/typed failure. Completion/cancellation cleans staging. |
| Media.GetAsset | core/catalog SAN; Id, actor or persisted Catalog reconciliation tuple | `features/asset/AssetRpc.getAsset` → authorized asset metadata/availability and CatalogAttachments check → Asset; no unrestricted SAN-only read. |
| Media.Reserve | catalog SAN; Hold persisted tuple | `features/hold/HoldRpc.reserve` → CatalogAttachments, locked asset/hold and owner receipt → Attachment; no delegated Core actor. |
| Media.Confirm | catalog SAN; Hold persisted tuple | HoldRpc.confirm → exact existing hold/state/version and receipt → Attachment; replay stable. |
| Media.Release | catalog SAN; Hold persisted tuple | HoldRpc.release → exact existing hold, release/replay and owner receipt → Attachment. |
| Media.GetOperation | core/catalog SAN; Id plus actor/persisted asset/hold scope | MediaServiceImpl.getOperation → AssetRpc.receiptScope + OwnerReceiptReads → Outcome; absent/unauthorized fails. |
| Media.ReadAsset | core SAN; AssetReadRequest current authorized grant | AssetRpc.readAsset → authorized actual processed file/quarantine store, streamed AssetReadChunk; authorization before bytes, file/transport failures terminate. |
| McpGateway.GetPolicy | mcp SAN + live bound management connector; Empty | Core `features/mcpgateway/McpGatewayRpc.getPolicy` → McpCatalogAccess.managementConnector/PolicyProvider → PolicySnapshot. No actor chosen by model. |
| McpGateway.GetMovieContext | mcp SAN + live management connector; MovieContextRequest | McpCatalogAccess exact per-method actor → Catalog SearchTitles/ListGenres/ListRoles → bounded active MovieContext/policy. |
| McpGateway.ProposeMovie | mcp SAN + live management connector; Proposal | McpCatalogAccess exact Catalog actor → ProposeMovie; transient challenge retained only in protected UI store/adapter metadata path → ProposalRevision. |
| McpGateway.ApproveMovie | mcp SAN + live connector + verified scoped UI assertion; ApprovalInput | UiAssertionVerifier then exact Catalog ApproveAndCreate actor → Outcome. Production signer/host handler missing; publication intentionally disabled, not source-complete. |
| McpGateway.RejectMovie | same bound trusted UI requirements; ApprovalInput | UiAssertionVerifier then Catalog RejectProposal → Outcome. Same explicit G-03 gap. |
| McpGateway.GetMovieOperation | mcp SAN + live connector; Id | McpCatalogAccess bound actor → Catalog GetOperation → original Outcome, no retry creating a new movie. |

## Corrections and evidence boundaries

This change fixes missing non-null genre labels in actual title GraphQL responses using a per-execution taxonomy lookup, without hardcoded labels or per-title RPC amplification. It also fixes two discarded provenance stages: Catalog ProtoHelpers now maps validated public field-source facts from the bounded owner ledger into the declared FieldSource DTO, and Core public/managed projections retain those typed facts rather than emitting empty objects. Unknown ledger properties are never copied into responses; malformed required evidence fails rather than being fabricated. Unknown public field names, invalid URL/time/precision and excessive input fail the projection.

New unit tests execute the actual metadata projection and GraphQL field resolver, including 100 title nodes with one taxonomy request per execution, missing taxonomy failure, preserved typed facts, rejected malformed/duplicate/oversized provenance and exclusion of private ledger extras. These are component proof only.

`tests/integration/src/test/kotlin/com/spoolin/integration/catalog/reads/PublicCatalogReadFixture.kt` explicitly creates synthetic disposable setup rows, then registers the production CatalogServiceRpc with the actual certificate interceptor and PostgreSQL-backed reads. New CatalogReadsBoundaryTest cases exercise all eight public RPCs over real mTLS; assert active/hidden visibility, actual genre and source data, denied wrong SAN; execute the production GraphQL schema/resolver against that real RPC; and deny ResolveActor for all four SAN callers. Synthetic SQL is setup for an API test, not proof of UI mutation, seed loading, Spring startup or actual browser behavior.

Required remote commands include `:services:core:test`, `:services:catalog:test`, `:tests:integration:catalogReadsTest` and affected source/coverage gates. Core catalog read unit selection also includes the new `com.spoolin.core.features.catalogview.read.CatalogMetadataProjectionRulesTest`. No command or new test was executed locally. Actual hosted-run SHA/job/result/artifact references must be added by the coordinator after inspection; no row here is runtime-accepted or formally accepted.

Outstanding boundaries include actual make-start bean/configuration readiness, every assigned API/RPC negative and concurrency obligation, persistence/restart proof, full browser integration and independent final combined review. The known G-03 signer/host channel gap remains open under `mcp-host-provenance-assessment.md`; no model-visible approval fallback was introduced. No selected RPC was found inheriting a default unimplemented handler except the deliberately uncallable retained ResolveActor contract. That finding is limited to registration tracing and does not certify every service branch complete.
