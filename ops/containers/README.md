# Service foundation

Build owner boot JARs before the container images. Every application reads its owner database password from a protected mounted file, uses TLS verification, and listens for authenticated gRPC and management traffic only on the private network. Core HTTPS is published on loopback. PostgreSQL uses SCRAM and TLS; no database port is published.

Real owner migrations are supplied by their feature tasks. This foundation contains no production application-table migration. A missing packaged migration or incompatible/invalid history prevents readiness. Representative HTML and disposable SQL are integration-test resources and are excluded from boot JARs. Packaging the source SDL does not implement its business resolvers; domain gRPC handlers return the generated unimplemented status until their feature owners supply behavior.

Provision the CA, server/client certificate and key files plus distinct bootstrap, runtime, migrator, maintenance and backup password files outside the checkout. Secret files require owner read/write permissions. PostgreSQL copies required bootstrap files only into a protected tmpfs directory; its persistent database volume contains no working plaintext credential or TLS-key copies. Container UIDs must be able to read the mounted files; provision those ownerships before startup. Never use normal installation paths for tests.

Run `bash ops/bin/migrate.sh core`, then catalog and media, after privileged database preparation. The wrapper grants the selected migrator owner membership only for that operation and always revokes it. A per-installation file lock and an owner PostgreSQL advisory lock bound acquisition to 120 seconds; Flyway still manages its own history lock and validates checksums. Migration failure does not trigger repair. Inspect retained failure state and use an owner-reviewed forward migration or restore procedure.

These files establish the foundation only. Full lifecycle orchestration, seed/accounts, Media image processing, browser shell, business endpoints and release capacity evidence belong to later tasks.
