-- V002: Catalog read model — title, genre, person, credit, provenance, seed_member
-- pg_unicode_fast extension is installed by privileged bootstrap (not here).
-- All tables owned by spoolin_catalog_owner per T003 ownership rules.

-- ─── person ──────────────────────────────────────────────────────────────────
CREATE TABLE person (
  id           uuid         PRIMARY KEY,
  name         text         NOT NULL CHECK (length(btrim(name)) > 0),
  biography    text,
  status       varchar(8)   NOT NULL DEFAULT 'ACTIVE'
                            CHECK (status IN ('ACTIVE', 'ARCHIVED')),
  version      bigint       NOT NULL DEFAULT 0 CHECK (version >= 0),
  created_at   timestamptz(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at   timestamptz(6) NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX person_active_name_idx ON person (name, id) WHERE status = 'ACTIVE';

-- ─── credit_role ─────────────────────────────────────────────────────────────
CREATE TABLE credit_role (
  id               uuid         PRIMARY KEY,
  category         varchar(8)   NOT NULL CHECK (category IN ('CAST', 'CREATOR')),
  label            text         NOT NULL CHECK (length(btrim(label)) > 0),
  normalized_label text         NOT NULL UNIQUE,
  allows_characters boolean     NOT NULL,
  created_at       timestamptz(6) NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ─── title ───────────────────────────────────────────────────────────────────
CREATE TABLE title (
  id                uuid         PRIMARY KEY,
  kind              varchar(8)   NOT NULL CHECK (kind IN ('MOVIE', 'TV')),
  status            varchar(8)   NOT NULL DEFAULT 'ACTIVE'
                                 CHECK (status IN ('ACTIVE', 'ARCHIVED')),
  display_title     text         NOT NULL CHECK (length(btrim(display_title)) > 0),
  original_title    text,
  synopsis          text         NOT NULL CHECK (length(btrim(synopsis)) > 0),
  synopsis_language varchar(35)  NOT NULL,
  original_language varchar(35),
  countries         text[]       NOT NULL DEFAULT '{}'::text[]
                                 CHECK (array_position(countries, NULL) IS NULL),
  release_year      smallint     CHECK (release_year BETWEEN 1 AND 9999),
  release_month     smallint     CHECK (release_month BETWEEN 1 AND 12),
  release_day       smallint     CHECK (release_day BETWEEN 1 AND 31),
  release_context   text,
  runtime_minutes   integer      CHECK (runtime_minutes BETWEEN 1 AND 100000),
  tv_status         varchar(16)  CHECK (tv_status IN ('CONTINUING', 'ENDED', 'CANCELLED', 'UNKNOWN')),
  end_year          smallint     CHECK (end_year BETWEEN 1 AND 9999),
  end_month         smallint     CHECK (end_month BETWEEN 1 AND 12),
  end_day           smallint     CHECK (end_day BETWEEN 1 AND 31),
  asset_id          uuid,
  attachment_id     uuid,
  version           bigint       NOT NULL DEFAULT 0 CHECK (version >= 0),
  created_at        timestamptz(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at        timestamptz(6) NOT NULL DEFAULT CURRENT_TIMESTAMP
);
-- Keyset index: newest first, unknown year last, stable UUID tie-break.
CREATE INDEX title_active_date_uuid_idx
  ON title (release_year NULLS LAST, release_month NULLS LAST, release_day NULLS LAST, id)
  WHERE status = 'ACTIVE';

-- ─── genre ───────────────────────────────────────────────────────────────────
CREATE TABLE genre (
  id               uuid         PRIMARY KEY,
  label            text         NOT NULL,
  normalized_label text         NOT NULL UNIQUE,
  created_at       timestamptz(6) NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX genre_label_idx ON genre (normalized_label, id);

-- ─── title_genre ─────────────────────────────────────────────────────────────
CREATE TABLE title_genre (
  title_id uuid NOT NULL REFERENCES title(id) ON DELETE CASCADE,
  genre_id uuid NOT NULL REFERENCES genre(id) ON DELETE RESTRICT,
  PRIMARY KEY (title_id, genre_id)
);
CREATE INDEX title_genre_by_genre_idx ON title_genre (genre_id, title_id);

-- ─── credit ──────────────────────────────────────────────────────────────────
CREATE TABLE credit (
  id          uuid     PRIMARY KEY,
  title_id    uuid     NOT NULL REFERENCES title(id)       ON DELETE CASCADE,
  person_id   uuid     NOT NULL REFERENCES person(id)      ON DELETE RESTRICT,
  role_id     uuid     NOT NULL REFERENCES credit_role(id) ON DELETE RESTRICT,
  characters  text[]   NOT NULL DEFAULT '{}'::text[]
                       CHECK (array_position(characters, NULL) IS NULL),
  version     bigint   NOT NULL DEFAULT 0 CHECK (version >= 0),
  created_at  timestamptz(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at  timestamptz(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE (title_id, person_id, role_id)
);
CREATE INDEX credit_by_title_idx  ON credit (title_id, id);
CREATE INDEX credit_by_person_idx ON credit (person_id, id);

-- ─── provenance_record ───────────────────────────────────────────────────────
-- No FK to title/person: record can outlive soft-deleted subjects.
CREATE TABLE provenance_record (
  source           varchar(64)   NOT NULL,
  external_id      text          NOT NULL,
  subject_kind     varchar(8)    NOT NULL CHECK (subject_kind IN ('TITLE', 'PERSON')),
  subject_id       uuid          NOT NULL,
  record_url       text,
  retrieved_at     timestamptz(6) NOT NULL,
  reuse_note       text          NOT NULL,
  field_sources    jsonb         NOT NULL
                                 CHECK (jsonb_typeof(field_sources) = 'object'
                                        AND octet_length(field_sources::text) <= 16384),
  locally_changed  text[]        NOT NULL DEFAULT '{}'::text[]
                                 CHECK (array_position(locally_changed, NULL) IS NULL),
  deleted_at       timestamptz(6),
  PRIMARY KEY (source, external_id, subject_kind)
);
CREATE INDEX provenance_by_subject_idx ON provenance_record (subject_id, subject_kind, source);

-- ─── seed_member ─────────────────────────────────────────────────────────────
CREATE TABLE seed_member (
  seed_id      varchar(64)  NOT NULL REFERENCES operation_receipt(seed_version) ON DELETE RESTRICT,
  member_key   varchar(255) NOT NULL,
  subject_kind varchar(16)  NOT NULL CHECK (subject_kind IN ('TITLE', 'PERSON', 'GENRE', 'ROLE', 'CREDIT')),
  subject_id   uuid         NOT NULL,
  content_hash bytea        NOT NULL CHECK (octet_length(content_hash) = 32),
  applied_at   timestamptz(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  removed_at   timestamptz(6),
  PRIMARY KEY (seed_id, member_key)
);
CREATE INDEX seed_member_by_subject_idx ON seed_member (subject_kind, subject_id);

-- ─── ownership ───────────────────────────────────────────────────────────────
ALTER TABLE person          OWNER TO spoolin_catalog_owner;
ALTER TABLE credit_role     OWNER TO spoolin_catalog_owner;
ALTER TABLE title           OWNER TO spoolin_catalog_owner;
ALTER TABLE genre           OWNER TO spoolin_catalog_owner;
ALTER TABLE title_genre     OWNER TO spoolin_catalog_owner;
ALTER TABLE credit          OWNER TO spoolin_catalog_owner;
ALTER TABLE provenance_record OWNER TO spoolin_catalog_owner;
ALTER TABLE seed_member     OWNER TO spoolin_catalog_owner;
