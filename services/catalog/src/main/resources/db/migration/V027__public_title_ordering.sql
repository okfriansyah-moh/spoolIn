-- Public keyset orders retain UUID ascending for ties in both directions.
-- V002 already supplies the ascending release-date order.
CREATE INDEX title_active_release_desc_idx
  ON title (release_year DESC NULLS LAST, release_month DESC NULLS LAST, release_day DESC NULLS LAST, id ASC)
  WHERE status = 'ACTIVE';
CREATE INDEX title_active_name_asc_idx
  ON title (casefold(display_title COLLATE pg_unicode_fast) ASC, id ASC)
  WHERE status = 'ACTIVE';
CREATE INDEX title_active_name_desc_idx
  ON title (casefold(display_title COLLATE pg_unicode_fast) DESC NULLS LAST, id ASC)
  WHERE status = 'ACTIVE';
CREATE INDEX title_active_added_asc_idx ON title (created_at ASC, id ASC) WHERE status = 'ACTIVE';
CREATE INDEX title_active_added_desc_idx ON title (created_at DESC NULLS LAST, id ASC) WHERE status = 'ACTIVE';
