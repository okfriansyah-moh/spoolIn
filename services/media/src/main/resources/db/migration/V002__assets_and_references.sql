CREATE TABLE asset (
  id uuid PRIMARY KEY,
  upload_operation_id uuid NOT NULL UNIQUE REFERENCES operation_receipt(id) ON DELETE RESTRICT,
  actor_id uuid NOT NULL,
  state varchar(16) NOT NULL CHECK (state IN ('RECEIVING', 'PROCESSING', 'READY', 'REJECTED',
    'DELETING', 'DELETED')),
  storage_key varchar(160) UNIQUE,
  content_hash bytea CHECK (octet_length(content_hash) = 32),
  format varchar(8) CHECK (format IN ('JPEG','PNG','WEBP')),
  input_bytes bigint NOT NULL CHECK (input_bytes > 0),
  output_bytes bigint CHECK (output_bytes > 0),
  width integer CHECK (width > 0),
  height integer CHECK (height > 0),
  policy_revision varchar(64) NOT NULL,
  provenance jsonb NOT NULL DEFAULT '{}'::jsonb CHECK (jsonb_typeof(provenance) = 'object' AND
    octet_length(provenance::text) <= 8192),
  error_code varchar(64),
  created_at timestamptz(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at timestamptz(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  processing_work_id uuid UNIQUE REFERENCES durable_work(id) ON DELETE RESTRICT,
  processing_staging_key varchar(160),
  processing_input_hash bytea CHECK (octet_length(processing_input_hash) = 32),
  processing_created_at timestamptz(6),
  CHECK (
    (processing_work_id IS NULL AND processing_staging_key IS NULL
     AND processing_input_hash IS NULL AND processing_created_at IS NULL)
    OR
    (processing_work_id IS NOT NULL AND processing_staging_key IS NOT NULL
     AND processing_input_hash IS NOT NULL AND processing_created_at IS NOT NULL)
  ),
  CHECK (state <> 'PROCESSING' OR processing_work_id IS NOT NULL),
  CHECK (state <> 'READY' OR (storage_key IS NOT NULL AND content_hash IS NOT NULL AND format IS NOT
    NULL AND output_bytes IS NOT NULL AND width IS NOT NULL AND height IS NOT NULL))
);
CREATE INDEX asset_cleanup_idx ON asset (state, updated_at, id);

CREATE TABLE asset_reference (
  id uuid PRIMARY KEY,
  asset_id uuid NOT NULL REFERENCES asset(id) ON DELETE RESTRICT,
  title_id uuid NOT NULL,
  state varchar(16) NOT NULL CHECK (state IN ('RESERVED','ACTIVE','RELEASED')),
  catalog_version bigint CHECK (catalog_version >= 0),
  created_at timestamptz(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at timestamptz(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CHECK (state <> 'ACTIVE' OR catalog_version IS NOT NULL)
);
CREATE INDEX hold_asset_idx ON asset_reference (asset_id, id) WHERE state IN ('RESERVED','ACTIVE');
CREATE INDEX hold_title_idx ON asset_reference (title_id, id);

ALTER TABLE asset OWNER TO spoolin_media_owner;
ALTER TABLE asset_reference OWNER TO spoolin_media_owner;
