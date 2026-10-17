-- V003: Catalog business audit — immutable, attributable change history.
-- Separated from operational_event (work/activity tracking) per ATDD-224 / PRD-HANDOFF-05.
-- All tables owned by spoolin_catalog_owner.

CREATE TABLE catalog_audit_event (
  id             uuid           PRIMARY KEY,
  operation_id   uuid           REFERENCES operation_receipt(id) ON DELETE RESTRICT,
  actor_id       uuid,
  actor_kind     varchar(16)    NOT NULL
                                CHECK (actor_kind IN ('HUMAN','SERVICE','ANONYMOUS')),
  action         varchar(64)    NOT NULL,
  subject_kind   varchar(24)    NOT NULL,
  subject_id     uuid,
  outcome        varchar(16)    NOT NULL
                                CHECK (outcome IN ('SUCCEEDED','DENIED','FAILED','APPROVED','REJECTED')),
  changes        jsonb          NOT NULL DEFAULT '{}'::jsonb
                                CHECK (jsonb_typeof(changes) = 'object'
                                       AND octet_length(changes::text) <= 65536),
  correlation_id uuid           NOT NULL,
  created_at     timestamptz(6) NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Subject-scoped access — primary history lookup by affected entity.
CREATE INDEX catalog_audit_subject_idx
  ON catalog_audit_event (subject_id, created_at DESC, id ASC)
  WHERE subject_id IS NOT NULL;

-- Time-ordered global access — for full audit views and cursor pagination.
CREATE INDEX catalog_audit_time_idx
  ON catalog_audit_event (created_at DESC, id ASC);

ALTER TABLE catalog_audit_event OWNER TO spoolin_catalog_owner;
