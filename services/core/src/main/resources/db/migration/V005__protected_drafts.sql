CREATE TABLE form_draft (
  id uuid PRIMARY KEY,
  account_id uuid NOT NULL REFERENCES user_account(id) ON DELETE RESTRICT,
  target_kind varchar(16) NOT NULL CHECK (target_kind IN ('MOVIE','PERSON','CREDIT')),
  target_id uuid,
  base_version bigint CHECK (base_version >= 0),
  operation_id uuid,
  policy_revision varchar(64) NOT NULL,
  body_key_id varchar(64) NOT NULL REFERENCES crypto_key_version(id) ON DELETE RESTRICT,
  body_nonce bytea NOT NULL CHECK (octet_length(body_nonce) = 12),
  body_cipher bytea NOT NULL CHECK (octet_length(body_cipher) BETWEEN 16 AND 65552),
  version bigint NOT NULL DEFAULT 0 CHECK (version >= 0),
  updated_at timestamptz(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  expires_at timestamptz(6) NOT NULL,
  UNIQUE (body_key_id, body_nonce)
);
CREATE INDEX draft_owner_idx ON form_draft (account_id, updated_at DESC);
CREATE INDEX draft_expiry_idx ON form_draft (expires_at);

ALTER TABLE form_draft OWNER TO spoolin_core_owner;
