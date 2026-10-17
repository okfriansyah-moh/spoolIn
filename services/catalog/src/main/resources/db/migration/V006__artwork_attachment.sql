CREATE TABLE artwork_attachment_request (
  id uuid PRIMARY KEY REFERENCES operation_receipt(id) ON DELETE RESTRICT,
  title_id uuid NOT NULL,
  actor_id uuid NOT NULL,
  expected_version bigint NOT NULL CHECK (expected_version >= 0),
  asset_id uuid NOT NULL,
  old_hold_id uuid,
  state varchar(16) NOT NULL CHECK (state IN ('PREPARING','ATTACHED','ABORTED')),
  policy_revision varchar(64) NOT NULL,
  created_at timestamptz(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at timestamptz(6) NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX artwork_attachment_title_idx ON artwork_attachment_request (title_id, state, id);

ALTER TABLE artwork_attachment_request OWNER TO spoolin_catalog_owner;
