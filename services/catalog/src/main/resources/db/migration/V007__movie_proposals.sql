CREATE TABLE movie_proposal (
  id uuid NOT NULL,
  revision integer NOT NULL CHECK (revision > 0),
  actor_id uuid NOT NULL,
  connector_id uuid NOT NULL,
  client_kind varchar(16) NOT NULL CHECK (client_kind IN ('CLAUDE','CHATGPT')),
  content jsonb NOT NULL CHECK (jsonb_typeof(content) = 'object' AND octet_length(content::text) <= 131072),
  content_hash bytea NOT NULL CHECK (octet_length(content_hash) = 32),
  challenge_hash bytea NOT NULL CHECK (octet_length(challenge_hash) = 32),
  state varchar(16) NOT NULL CHECK (state IN ('REVIEW', 'SUPERSEDED', 'REJECTED', 'APPROVED', 'CREATED', 'EXPIRED', 'FAILED')),
  operation_id uuid NOT NULL REFERENCES operation_receipt(id) ON DELETE RESTRICT,
  movie_id uuid,
  expires_at timestamptz(6) NOT NULL,
  created_at timestamptz(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  approval_actor_id uuid,
  approval_connector_id uuid,
  approval_content_hash bytea CHECK (octet_length(approval_content_hash) = 32),
  approved_at timestamptz(6),
  approval_expires_at timestamptz(6),
  approval_consumed_at timestamptz(6),
  CHECK (
    (approval_actor_id IS NULL AND approval_connector_id IS NULL
     AND approval_content_hash IS NULL AND approved_at IS NULL
     AND approval_expires_at IS NULL AND approval_consumed_at IS NULL)
    OR
    (approval_actor_id IS NOT NULL AND approval_connector_id IS NOT NULL
     AND approval_content_hash IS NOT NULL AND approved_at IS NOT NULL
     AND approval_expires_at IS NOT NULL AND approval_actor_id = actor_id
     AND approval_connector_id = connector_id AND approval_content_hash = content_hash
     AND approval_expires_at > approved_at AND approval_expires_at <= expires_at
     AND (approval_consumed_at IS NULL OR
          (approval_consumed_at >= approved_at AND approval_consumed_at <= approval_expires_at)))
  ),
  CHECK ((state = 'CREATED' AND movie_id IS NOT NULL AND approval_consumed_at IS NOT NULL)
      OR (state <> 'CREATED' AND approval_consumed_at IS NULL)),
  PRIMARY KEY (id, revision)
);
CREATE INDEX proposal_owner_idx ON movie_proposal (actor_id, created_at DESC);
CREATE INDEX proposal_operation_idx ON movie_proposal (operation_id, revision);
CREATE UNIQUE INDEX proposal_one_created_idx ON movie_proposal (id) WHERE state = 'CREATED';
CREATE UNIQUE INDEX proposal_one_review_idx ON movie_proposal (id) WHERE state = 'REVIEW';

ALTER TABLE movie_proposal OWNER TO spoolin_catalog_owner;
