# Release handoff (G26 / T046)

Runtime acceptance for selection G17–G26 remains **pending** under lane resource limits.

## Source closure

See **[g01-g26-closure-matrix.md](./g01-g26-closure-matrix.md)** for one row per **T001–T046**. Runtime acceptance stays pending. Independent source re-review is Grok 4.7 at `1c0f196`.

## Finding dispositions (source)

| ID | Status | Reviewer |
|----|--------|----------|
| F-001 | resolved at `1c0f196` — challenge only in `_meta.ui`; binding GET returns 404 | Grok 4.7 |
| F-002, F-003, F-004, F-005, F-009, F-011 | resolved in earlier verification | Grok 4.7 |
| F-006, F-007, F-008, F-010 | resolved at `6e8a760` against the original defects | Grok 4.7 |

Ledger: `.ai/state/g17-g26-findings-ledger.json`. Prior correction round remains 2. Continuation was explicitly authorized and did not reset that counter.

## Remaining source gaps

| Area | Gap |
|------|-----|
| MCP adapter | Full Streamable session, stdio bridge, and live `McpGatewayService` client are still incomplete; propose result shaping is wired |
| MCP UI assertion | `MCP_UI_ASSERTION_*` signing is not implemented |
| Trusted review | Host must inject the UI challenge and connector bearer into the review frame; the model tool list does not include approve/reject |
| Proof | Gradle, Compose, PostgreSQL seed apply, Playwright, and client hosts were not executed in this lane |

## Prior delivered source (unchanged summary)

- Catalog proposal approve/reject + exact binding; Core MCP gateway and connector identity; review resource + challenge binding endpoint; seed loader + manifest validator; ops lifecycle scripts; audit GraphQL path; owner receipts with in-transaction audit on catalog mutations.
