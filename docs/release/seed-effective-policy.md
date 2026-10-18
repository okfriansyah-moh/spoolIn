# Seed input policy and repeat-safe initialization

TD 10.1 requires the complete frozen snapshot to pass current input and provenance rules before initial application. Catalog now resolves the effective policy and validates every person name, biography, title, original title, synopsis, role label, character name, source URL and reuse basis before creating a pending initialization receipt or any domain rows. Supplied optional values require field provenance. Metadata stays unchanged; malformed Unicode, missing provenance and text requiring normalization fail validation. Physical provenance storage limits also apply. The legacy snapshot remains available as an upgrade baseline. The current `public-pages-2026-10-v2` package preserves stable identities and adds attributed public-page metadata and frozen posters under `public-pages-v1`. Supplied rights information is recorded without claiming an open license.

The CLI loads Spring ConfigData imports before resolving policy, using the same protected configuration file and explicit environment precedence as Catalog. This loads configuration without starting Catalog's HTTP/gRPC servers. The `catalog-seed` container must mount the protected Catalog JSON and pass its `SPRING_CONFIG_IMPORT`, together with the policy environment allowlist.

The Python validator is a structural preflight for complete counts, identities, relationships, dates, checksums, source evidence and poster file inventories, hashes and dimensions. Its report explicitly states `effectivePolicyVerified: false`. It does not guess configured text bounds. Authoritative preparation is available remotely after building the actual image:

```sh
bash ops/bin/seed.sh --validate-only
```

That command runs structural preflight and the Catalog CLI with `--seed-manifest=/app/seed/manifest.json --validate-only` in the configured seed container. The CLI validates against current policy and returns before loading database settings or opening a database connection. Calling the script without `--apply`, `SPOOLIN_SEED_APPLY=1` or `--validate-only` retains its exit code 2 and reports that application/preparation was skipped.

For fresh initialization or a retained PENDING/FAILED initialization, the loader locks initialization and Catalog writes, checks the existing snapshot identity, then validates the complete snapshot against current policy before writing. The fresh successful receipt records the assessed policy revision. Person biographies, original titles/languages, countries, runtimes and character arrays are persisted when present and valid instead of being silently discarded.

TD 10.1's successful-rerun contract differs from fresh validation: the same version/hash verifies the permanent receipt, expected counts, retained member hashes and provenance. It does not reinterpret the old success under a tightened policy, rewrite existing summaries, normalize retained text, upsert edits or resurrect tombstoned members. A changed snapshot under the same version fails before consulting the current policy. Preparation always evaluates current policy, even when a database elsewhere already contains an older successful initialization.

The added rule, structural and PostgreSQL boundary tests are remote-executable verification support. Source changes do not establish runtime acceptance. Their actual run revisions, conclusions and artifacts belong in the selection evidence ledger.
