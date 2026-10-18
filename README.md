# SpoolIn movie-app

Maintainer-facing catalog for movies, people, credits, artwork, and AI-assisted movie proposals with trusted in-chat approval.

## Components

| Area | Path |
|------|------|
| Core (identity, drafts, MCP gateway) | `services/core` |
| Catalog (writes, proposals, audit) | `services/catalog` |
| Media (artwork upload/processing) | `services/media` |
| Web UI (SvelteKit) | `frontend` |
| MCP adapter + review resource | `mcp` |
| Contracts (gRPC, JSON schemas) | `contracts` |
| Ops scripts & compose | `ops` |

## Local workflow (source)

1. Configure toolchain via `ops/bin/toolchain-env.sh`.
2. Validate seed manifest: `python3 build-tools/validate-seed/validate_manifest.py`.
3. On the intended Linux Docker host, provision the protected [Media sandbox policy](docs/release/media-sandbox-policy.md) before startup. Missing enforcement is a prerequisite failure.
4. Use documented `make start`, `make stop`, `make restart`, `make status`, `make logs`, `make seed`, `make test-unit` and `make test-integration` with the protected configuration and installation identifier.

The current remote continuation covers all 46 tasks; source corrections and actual run failures are recorded separately in [the assessment](docs/release/remote-acceptance-assessment.json) and the retained findings ledger. The [handoff](docs/release/handoff.md) preserves the earlier source checkpoint. Formal acceptance remains pending, including required installed-host review flows, reader observations and current remote behavioral proof. Trusted in-chat publication remains disabled until verified host provenance is available.
