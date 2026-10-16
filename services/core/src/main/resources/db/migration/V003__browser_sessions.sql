CREATE TABLE browser_session (
  token_hash bytea PRIMARY KEY CHECK (octet_length(token_hash) = 32),
  id uuid NOT NULL UNIQUE,
  account_id uuid NOT NULL REFERENCES user_account(id) ON DELETE RESTRICT,
  auth_epoch bigint NOT NULL CHECK (auth_epoch >= 0),
  csrf_hash bytea NOT NULL CHECK (octet_length(csrf_hash) = 32),
  created_at timestamptz(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  last_seen_at timestamptz(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  expires_at timestamptz(6) NOT NULL,
  revoked_at timestamptz(6)
);
CREATE INDEX session_account_idx ON browser_session (account_id, created_at DESC);
CREATE INDEX session_expiry_idx ON browser_session (expires_at);
ALTER TABLE browser_session OWNER TO spoolin_core_owner;
