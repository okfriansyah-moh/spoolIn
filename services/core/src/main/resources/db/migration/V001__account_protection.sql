CREATE TABLE crypto_key_version (
  id varchar(64) PRIMARY KEY,
  purpose varchar(16) NOT NULL CHECK (purpose IN ('PII','DRAFT','EMAIL_LOOKUP')),
  state varchar(16) NOT NULL CHECK (state IN ('WRITE','READ','RETIRED')),
  next_nonce bigint NOT NULL DEFAULT 1 CHECK (next_nonce > 0),
  created_at timestamptz(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  retire_after timestamptz(6)
);
CREATE UNIQUE INDEX key_write_idx ON crypto_key_version (purpose) WHERE state = 'WRITE';

CREATE TABLE user_account (
  id uuid PRIMARY KEY,
  pii_key_id varchar(64) NOT NULL REFERENCES crypto_key_version(id) ON DELETE RESTRICT,
  pii_nonce bytea NOT NULL CHECK (octet_length(pii_nonce) = 12),
  pii_cipher bytea NOT NULL CHECK (octet_length(pii_cipher) BETWEEN 16 AND 65552),
  password_hash varchar(512) NOT NULL,
  role varchar(16) NOT NULL CHECK (role IN ('VIEWER','MAINTAINER')),
  auth_epoch bigint NOT NULL DEFAULT 0 CHECK (auth_epoch >= 0),
  version bigint NOT NULL DEFAULT 0 CHECK (version >= 0),
  created_at timestamptz(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at timestamptz(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE (pii_key_id, pii_nonce)
);

CREATE TABLE account_email_lookup (
  key_id varchar(64) NOT NULL REFERENCES crypto_key_version(id) ON DELETE RESTRICT,
  digest bytea NOT NULL CHECK (octet_length(digest) = 32),
  account_id uuid NOT NULL REFERENCES user_account(id) ON DELETE RESTRICT,
  created_at timestamptz(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (key_id, digest),
  UNIQUE (account_id, key_id)
);
CREATE INDEX email_account_idx ON account_email_lookup (account_id);

-- FK checks run as the table owner after the migrator's temporary membership is revoked.
ALTER TABLE crypto_key_version OWNER TO spoolin_core_owner;
ALTER TABLE user_account OWNER TO spoolin_core_owner;
ALTER TABLE account_email_lookup OWNER TO spoolin_core_owner;
