-- V004: person impact_version (ATDD-237) and starter contribution roles (PRD-POLICY-05)

ALTER TABLE person
  ADD COLUMN impact_version bigint NOT NULL DEFAULT 0 CHECK (impact_version >= 0);

INSERT INTO credit_role (id, category, label, normalized_label, allows_characters) VALUES
  ('b0000001-0000-4000-8000-000000000001', 'CAST', 'Actor', 'actor', true),
  ('b0000001-0000-4000-8000-000000000002', 'CREATOR', 'Director', 'director', false),
  ('b0000001-0000-4000-8000-000000000003', 'CREATOR', 'Writer', 'writer', false),
  ('b0000001-0000-4000-8000-000000000004', 'CREATOR', 'Producer', 'producer', false)
ON CONFLICT (normalized_label) DO NOTHING;
