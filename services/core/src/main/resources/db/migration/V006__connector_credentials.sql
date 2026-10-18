CREATE TABLE mcp_credential (
  id uuid PRIMARY KEY,
  token_hash bytea NOT NULL UNIQUE CHECK (octet_length(token_hash) = 32),
  account_id uuid NOT NULL REFERENCES user_account(id) ON DELETE RESTRICT,
  client_kind varchar(16) NOT NULL CHECK (client_kind IN ('CLAUDE','CHATGPT')),
  auth_epoch bigint NOT NULL CHECK (auth_epoch >= 0),
  scope varchar(64) NOT NULL CHECK (scope = 'movie:propose movie:approve'),
  created_at timestamptz(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  expires_at timestamptz(6) NOT NULL,
  revoked_at timestamptz(6)
);
CREATE INDEX connector_account_idx ON mcp_credential (account_id);

ALTER TABLE mcp_credential OWNER TO spoolin_core_owner;
