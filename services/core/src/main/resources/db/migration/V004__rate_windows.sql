CREATE TABLE request_rate_window (
  bucket_key bytea NOT NULL CHECK (octet_length(bucket_key) = 32),
  window_start timestamptz(6) NOT NULL,
  hits integer NOT NULL CHECK (hits > 0),
  expires_at timestamptz(6) NOT NULL,
  PRIMARY KEY (bucket_key, window_start)
);
CREATE INDEX rate_expiry_idx ON request_rate_window (expires_at);
ALTER TABLE request_rate_window OWNER TO spoolin_core_owner;
