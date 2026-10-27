-- Existing IDs stay unchanged. Legacy owners require a verified offline registry.
CREATE TABLE appdata.identity_hash_migration (
  singleton boolean PRIMARY KEY DEFAULT true CHECK (singleton),
  legacy_required boolean NOT NULL,
  registry_sha256 varchar(64) CHECK (registry_sha256 ~ '^[0-9a-f]{64}$')
);

INSERT INTO appdata.identity_hash_migration(singleton, legacy_required)
SELECT true, EXISTS (SELECT 1 FROM appdata.operation_receipt)
  OR EXISTS (SELECT 1 FROM appdata.durable_work);

ALTER TABLE appdata.identity_hash_migration OWNER TO spoolin_catalog_owner;
REVOKE ALL ON appdata.identity_hash_migration FROM PUBLIC;
GRANT SELECT ON appdata.identity_hash_migration TO spoolin_catalog_runtime, spoolin_catalog_maintenance;
GRANT SELECT, UPDATE (registry_sha256) ON appdata.identity_hash_migration TO spoolin_catalog_migrator;
