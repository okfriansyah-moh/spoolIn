CREATE TABLE prepared_movie_poster (
 id uuid PRIMARY KEY,
 asset_id uuid NOT NULL UNIQUE,
 actor_id uuid NOT NULL,
 connector_id uuid NOT NULL,
 request_hash bytea NOT NULL,
 metadata jsonb NOT NULL,
 policy_revision varchar(64) NOT NULL,
 expires_at timestamptz(6) NOT NULL,
 created_at timestamptz(6) NOT NULL DEFAULT CURRENT_TIMESTAMP
);
ALTER TABLE prepared_movie_poster OWNER TO spoolin_catalog_owner;
