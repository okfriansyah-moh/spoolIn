CREATE TABLE operation_receipt (
  id uuid PRIMARY KEY,
  actor_scope varchar(96) NOT NULL,
  kind varchar(64) NOT NULL,
  receipt_type varchar(24) NOT NULL DEFAULT 'REQUEST'
    CHECK (receipt_type IN ('REQUEST','ACCOUNT_INITIALIZATION','CATALOG_INITIALIZATION')),
  request_hash bytea NOT NULL CHECK (octet_length(request_hash) = 32),
  hash_key_id varchar(64),
  state varchar(16) NOT NULL CHECK (state IN ('PENDING','SUCCEEDED','FAILED','EXPIRED')),
  result jsonb CHECK (result IS NULL OR (jsonb_typeof(result) = 'object' AND
    octet_length(result::text) <= 16384)),
  error_code varchar(64),
  correlation_id uuid NOT NULL,
  created_at timestamptz(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  expires_at timestamptz(6),
  CHECK ((receipt_type = 'REQUEST' AND expires_at IS NOT NULL) OR
         (receipt_type <> 'REQUEST' AND expires_at IS NULL)),
  CHECK (receipt_type = 'REQUEST' OR
         (actor_scope = 'SERVICE_INITIALIZER' AND result IS NULL AND hash_key_id IS NULL
          AND state <> 'EXPIRED'))
);
CREATE INDEX operation_actor_idx ON operation_receipt (actor_scope, created_at DESC, id);

CREATE TABLE durable_work (
  id uuid PRIMARY KEY,
  operation_id uuid REFERENCES operation_receipt(id) ON DELETE RESTRICT,
  kind varchar(64) NOT NULL,
  payload jsonb NOT NULL CHECK (jsonb_typeof(payload) = 'object' AND octet_length(payload::text) <=
    16384),
  state varchar(16) NOT NULL DEFAULT 'READY' CHECK (state IN ('READY', 'LEASED', 'DONE',
    'EXHAUSTED', 'CANCELLED')),
  generation integer NOT NULL DEFAULT 0 CHECK (generation >= 0),
  attempt_count integer NOT NULL DEFAULT 0 CHECK (attempt_count >= 0),
  max_attempts integer NOT NULL CHECK (max_attempts BETWEEN 1 AND 10),
  next_attempt_at timestamptz(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deadline_at timestamptz(6) NOT NULL,
  lease_owner uuid,
  lease_until timestamptz(6),
  lease_epoch bigint NOT NULL DEFAULT 0 CHECK (lease_epoch >= 0),
  last_error varchar(64),
  correlation_id uuid NOT NULL,
  created_at timestamptz(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at timestamptz(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  base_delay_ms integer NOT NULL CHECK (base_delay_ms BETWEEN 1 AND 60000),
  cap_delay_ms integer NOT NULL CHECK (cap_delay_ms BETWEEN 1 AND 3600000),
  backoff_multiplier integer NOT NULL CHECK (backoff_multiplier BETWEEN 1 AND 10),
  notification_key varchar(160) UNIQUE,
  notification_mode varchar(16),
  notification_state varchar(24),
  notification_summary jsonb,
  provider_reference varchar(255),
  CHECK (
    (kind <> 'NOTIFICATION' AND notification_key IS NULL AND notification_mode IS NULL
     AND notification_state IS NULL AND notification_summary IS NULL
     AND provider_reference IS NULL)
    OR
    (kind = 'NOTIFICATION' AND notification_key IS NOT NULL
     AND notification_mode IS NOT NULL AND notification_state IS NOT NULL
     AND notification_summary IS NOT NULL
     AND notification_mode IN ('database','smtp','slack','google_chat')
     AND notification_state IN
       ('RECORDED','PENDING','ACCEPTED','DELIVERY_CONFIRMED','UNKNOWN','FAILED')
     AND jsonb_typeof(notification_summary) = 'object'
     AND octet_length(notification_summary::text) <= 8192
     AND (
       (notification_mode = 'database' AND notification_state = 'RECORDED'
        AND state = 'DONE' AND attempt_count = 0 AND generation = 0 AND lease_epoch = 0
        AND provider_reference IS NULL)
       OR (notification_mode <> 'database' AND notification_state <> 'RECORDED'
           AND (notification_state NOT IN ('ACCEPTED','DELIVERY_CONFIRMED') OR state = 'DONE'))
     ))
  ),
  CHECK ((state = 'LEASED' AND lease_owner IS NOT NULL AND lease_until IS NOT NULL) OR (state <>
    'LEASED' AND lease_owner IS NULL AND lease_until IS NULL)),
  CHECK (cap_delay_ms >= base_delay_ms),
  CHECK (attempt_count <= max_attempts)
);
CREATE INDEX work_due_idx ON durable_work (next_attempt_at, id) WHERE state = 'READY';
CREATE INDEX work_expired_idx ON durable_work (lease_until, id) WHERE state = 'LEASED';
CREATE INDEX work_failed_idx ON durable_work (updated_at, id) WHERE state = 'EXHAUSTED';
CREATE INDEX work_operation_idx ON durable_work (operation_id);
CREATE INDEX notification_state_idx ON durable_work
  (notification_state, created_at, id) WHERE kind = 'NOTIFICATION';

CREATE TABLE operational_event (
  id uuid PRIMARY KEY,
  operation_id uuid REFERENCES operation_receipt(id) ON DELETE RESTRICT,
  actor_id uuid,
  actor_kind varchar(16) NOT NULL CHECK (actor_kind IN ('HUMAN','SERVICE','ANONYMOUS')),
  action varchar(64) NOT NULL,
  subject_kind varchar(24) NOT NULL,
  subject_id uuid,
  outcome varchar(16) CHECK (outcome IN ('SUCCEEDED','DENIED','FAILED','APPROVED','REJECTED')),
  changes jsonb NOT NULL DEFAULT '{}'::jsonb CHECK (jsonb_typeof(changes) = 'object' AND
    octet_length(changes::text) <= 65536),
  correlation_id uuid NOT NULL,
  created_at timestamptz(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  event_type varchar(24) NOT NULL CHECK
    (event_type IN ('ACTIVITY','ATTEMPT_STARTED','ATTEMPT_FINISHED','RECOVERY')),
  work_id uuid REFERENCES durable_work(id) ON DELETE RESTRICT,
  generation integer CHECK (generation >= 0),
  attempt_no integer CHECK (attempt_no BETWEEN 1 AND 10),
  lease_epoch bigint CHECK (lease_epoch >= 0),
  attempt_outcome varchar(24) CHECK
    (attempt_outcome IN ('SUCCEEDED','TRANSIENT','PERMANENT','UNCERTAIN','CANCELLED')),
  error_code varchar(64),
  operator_id varchar(96),
  CHECK (
    (event_type = 'ACTIVITY' AND outcome IS NOT NULL AND work_id IS NULL
     AND generation IS NULL AND attempt_no IS NULL AND lease_epoch IS NULL
     AND attempt_outcome IS NULL)
    OR
    (event_type IN ('ATTEMPT_STARTED','ATTEMPT_FINISHED') AND work_id IS NOT NULL
     AND generation IS NOT NULL AND attempt_no IS NOT NULL
     AND lease_epoch IS NOT NULL AND lease_epoch > 0
     AND ((event_type = 'ATTEMPT_STARTED' AND outcome IS NULL AND attempt_outcome IS NULL
           AND error_code IS NULL)
       OR (event_type = 'ATTEMPT_FINISHED' AND attempt_outcome IS NOT NULL
           AND outcome IS NOT NULL
           AND ((attempt_outcome = 'SUCCEEDED' AND outcome = 'SUCCEEDED')
             OR (attempt_outcome <> 'SUCCEEDED' AND outcome = 'FAILED')))))
    OR
    (event_type = 'RECOVERY' AND outcome IS NOT NULL AND work_id IS NOT NULL
     AND generation IS NOT NULL AND attempt_no IS NULL AND lease_epoch IS NOT NULL
     AND attempt_outcome IS NULL AND operator_id IS NOT NULL)
  )
);
CREATE UNIQUE INDEX event_attempt_phase_uq ON operational_event
  (work_id, generation, attempt_no, event_type)
  WHERE event_type IN ('ATTEMPT_STARTED','ATTEMPT_FINISHED');
CREATE UNIQUE INDEX event_recovery_generation_uq ON operational_event (work_id, generation)
  WHERE event_type = 'RECOVERY';
CREATE INDEX event_work_idx ON operational_event (work_id, generation, attempt_no, created_at);
CREATE INDEX event_time_idx ON operational_event (created_at, id);
CREATE INDEX event_operation_idx ON operational_event (operation_id);

ALTER TABLE operation_receipt
  ADD COLUMN seed_version varchar(64) UNIQUE,
  ADD COLUMN seed_snapshot_hash bytea CHECK (octet_length(seed_snapshot_hash) = 32),
  ADD COLUMN seed_movie_count integer CHECK (seed_movie_count BETWEEN 0 AND 100),
  ADD COLUMN seed_tv_count integer CHECK (seed_tv_count BETWEEN 0 AND 100),
  ADD COLUMN seed_summary jsonb CHECK
    (jsonb_typeof(seed_summary) = 'object' AND octet_length(seed_summary::text) <= 16384),
  ADD CONSTRAINT catalog_receipt_shape CHECK (
    (receipt_type = 'REQUEST' AND seed_version IS NULL AND seed_snapshot_hash IS NULL
     AND seed_movie_count IS NULL AND seed_tv_count IS NULL AND seed_summary IS NULL)
    OR
    (receipt_type = 'CATALOG_INITIALIZATION' AND kind = 'INITIALIZE_CATALOG'
     AND seed_version IS NOT NULL AND seed_snapshot_hash IS NOT NULL
     AND request_hash = seed_snapshot_hash AND seed_movie_count IS NOT NULL
     AND seed_tv_count IS NOT NULL AND seed_summary IS NOT NULL
     AND ((state = 'SUCCEEDED' AND seed_movie_count = 100 AND seed_tv_count = 100)
       OR (state IN ('PENDING','FAILED') AND seed_movie_count = 0 AND seed_tv_count = 0)))
  );

ALTER TABLE operation_receipt OWNER TO spoolin_catalog_owner;
ALTER TABLE durable_work OWNER TO spoolin_catalog_owner;
ALTER TABLE operational_event OWNER TO spoolin_catalog_owner;
