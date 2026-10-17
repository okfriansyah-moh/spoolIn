# Release handoff (G26 / T046)

Runtime acceptance for selection G17–G26 remains **pending** under lane resource limits.

## Source delivered (not proof)

- **T029** — Catalog `ProposalWrites.approve` / `reject` with exact binding, movie create via `MovieWrites.publishFromProposal`, deterministic proposal content maps.
- **T028** — Core connector bearer interceptor, `IdentityRpc` connector `checkActor`, MCP gateway method-scoped actor tokens.
- **T030** — Review resource loads query-bound revision metadata; approval posts only via explicit challenge + gateway paths (no auto-approve).
- **T027** — Credit panel draft autosave/recovery wiring.
- **T032–T033** — Manifest validator, `CatalogSeedLoader`, documented `seed.sh` steps.
- **T034–T041** — Actionable alert table, logback skeletons, ops script wrappers, key rotation plan source.
- **T042–T046** — Release matrix stub rows, policy/security test skeletons, root README.

## Proof still required

| Boundary | Gap |
|----------|-----|
| PostgreSQL integration | Proposal approve/reject ATDD rows, alert persistence, seed load |
| Gradle test execution | Changed-module unit/integration suites |
| Compose / Docker | `start.sh` wrappers not executed in lane |
| MCP real hosts | Claude Desktop / ChatGPT tunnel, human review evidence |
| Browser / Playwright | Manage audit UI, credit draft recovery UX |
