CREATE TABLE actionable_alert (
  id uuid PRIMARY KEY,
  kind varchar(64) NOT NULL,
  mode varchar(16) NOT NULL CHECK (mode IN ('database','smtp','slack','google_chat')),
  summary jsonb NOT NULL CHECK (jsonb_typeof(summary) = 'object' AND octet_length(summary::text) <= 8192),
  created_at timestamptz(6) NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX actionable_alert_kind_idx ON actionable_alert (kind, created_at DESC);

ALTER TABLE actionable_alert OWNER TO spoolin_core_owner;
