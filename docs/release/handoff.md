# Release handoff (G26 / T046)

Runtime acceptance for selection G17–G26 remains **pending** under lane resource limits.

## Source closure

See **[g01-g26-closure-matrix.md](./g01-g26-closure-matrix.md)** for one row per **T001–T046**: required outcomes, source wiring, integration status, evidence boundaries, open source gaps, review (pending Grok), and runtime (pending).

## Open findings (source)

| Area | Gap |
|------|-----|
| MCP adapter | Full Streamable MCP session, stdio bridge, and live `McpGatewayService` gRPC client not wired; POST composer covers propose result shaping only |
| MCP UI assertion | `MCP_UI_ASSERTION_*` signing/verification not implemented in services |
| Trusted review | Approve/reject HTTP exists on Core; review UI needs host `postMessage` `connectorToken` for bearer |
| Proof | Gradle integration tests, Compose startup, PostgreSQL seed load, Playwright, and client hosts (T038–T039) not executed in this lane |

## Prior delivered source (unchanged summary)

- Catalog proposal approve/reject + exact binding; Core MCP gateway and connector identity; review resource + challenge binding endpoint; seed loader + manifest validator; ops lifecycle scripts; audit GraphQL path; owner receipts with in-transaction audit on catalog mutations.
