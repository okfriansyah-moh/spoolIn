-- Native metadata/lifecycle writes cannot leave an existing deletion preview current.
CREATE FUNCTION invalidate_person_impact_metadata() RETURNS trigger
LANGUAGE plpgsql SET search_path = pg_catalog, appdata AS $impact$
BEGIN
  IF ROW(NEW.name,NEW.biography,NEW.status,NEW.version)
     IS DISTINCT FROM ROW(OLD.name,OLD.biography,OLD.status,OLD.version) THEN
    NEW.impact_version := GREATEST(NEW.impact_version,OLD.impact_version + 1);
  END IF;
  IF NEW.impact_version < OLD.impact_version THEN
    RAISE EXCEPTION 'Person impact version cannot regress' USING ERRCODE = '23514';
  END IF;
  RETURN NEW;
END
$impact$;
CREATE TRIGGER person_impact_metadata_guard BEFORE UPDATE ON person
FOR EACH ROW EXECUTE FUNCTION invalidate_person_impact_metadata();

CREATE FUNCTION invalidate_linked_title_impact() RETURNS trigger
LANGUAGE plpgsql SET search_path = pg_catalog, appdata AS $impact$
BEGIN
  IF ROW(NEW.display_title,NEW.kind,NEW.status) IS DISTINCT FROM ROW(OLD.display_title,OLD.kind,OLD.status) THEN
    UPDATE appdata.person SET impact_version=impact_version+1,updated_at=CURRENT_TIMESTAMP
    WHERE id IN (SELECT person_id FROM appdata.credit WHERE title_id=NEW.id);
  END IF;
  RETURN NEW;
END
$impact$;
CREATE TRIGGER title_impact_metadata_guard AFTER UPDATE ON title
FOR EACH ROW EXECUTE FUNCTION invalidate_linked_title_impact();

ALTER FUNCTION invalidate_person_impact_metadata() OWNER TO spoolin_catalog_owner;
ALTER FUNCTION invalidate_linked_title_impact() OWNER TO spoolin_catalog_owner;
REVOKE ALL ON FUNCTION invalidate_person_impact_metadata() FROM PUBLIC;
REVOKE ALL ON FUNCTION invalidate_linked_title_impact() FROM PUBLIC;
GRANT EXECUTE ON FUNCTION invalidate_person_impact_metadata() TO spoolin_catalog_runtime;
GRANT EXECUTE ON FUNCTION invalidate_linked_title_impact() TO spoolin_catalog_runtime;
