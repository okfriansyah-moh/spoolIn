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
3. Use `ops/bin/start.sh` / `stop.sh` / `status.sh` / `logs.sh` as compose wrappers (require `SPOOLIN_INSTALLATION`).

Runtime proof (integration, browser, real ChatGPT/Claude) remains release-owned; see `docs/release/handoff.md`.
