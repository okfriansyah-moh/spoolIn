CREATE TABLE seed_artwork_attribution (
  title_id uuid NOT NULL,
  asset_id uuid NOT NULL,
  attribution jsonb NOT NULL CHECK(jsonb_typeof(attribution)='object'),
  PRIMARY KEY(title_id,asset_id)
);
ALTER TABLE seed_artwork_attribution OWNER TO spoolin_catalog_owner;
