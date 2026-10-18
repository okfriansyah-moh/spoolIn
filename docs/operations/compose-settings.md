# Owner policy configuration forwarding

Core, Catalog, Media and the Catalog seed command inherit an explicit allowlist of nonsecret policy, retry, quota and log-level variables through the shared Compose environment mapping. MCP receives the same nonsecret mapping and retains its explicit read-quota defaults. An unresolved key stays absent, allowing protected service configuration and then application defaults to apply. A present empty string or zero reaches the owning validator and cannot silently select a default.

This uses Docker's documented [single-key environment resolution](https://docs.docker.com/reference/compose-file/services/#environment), including values supplied by the shell or Compose environment files. The shell wins over the environment file. Do not add secret contents to this mapping; the existing protected file mounts remain the credential boundary.

The hosted `verify-compose-settings.py` check inspects actual Compose configuration for absent, file, host override, empty and zero cases across all four JVM owner/seed entrypoints. It emits only case results, never the complete resolved configuration. This configuration check does not replace owner validation, startup or runtime acceptance.
