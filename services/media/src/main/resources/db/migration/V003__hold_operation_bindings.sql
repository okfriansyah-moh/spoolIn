-- Permanent typed routing evidence survives the seven-day purge of receipt result bodies.
CREATE TABLE hold_operation_binding (
  operation_id uuid PRIMARY KEY REFERENCES operation_receipt(id) ON DELETE RESTRICT,
  hold_id uuid NOT NULL REFERENCES asset_reference(id) ON DELETE RESTRICT,
  actor_id uuid NOT NULL,
  verb varchar(8) NOT NULL CHECK (verb IN ('RESERVE','CONFIRM','RELEASE')),
  created_at timestamptz(6) NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX hold_operation_hold_idx ON hold_operation_binding(hold_id, operation_id);

CREATE FUNCTION check_hold_operation_binding() RETURNS trigger
LANGUAGE plpgsql SET search_path = pg_catalog, appdata AS $binding$
BEGIN
  IF TG_OP <> 'INSERT' THEN
    RAISE EXCEPTION 'Hold operation binding is immutable' USING ERRCODE = '23514';
  END IF;
  IF NOT EXISTS (
    SELECT 1 FROM appdata.operation_receipt r
    JOIN appdata.asset_reference h ON h.id = NEW.hold_id
    JOIN appdata.asset a ON a.id = h.asset_id
    WHERE r.id = NEW.operation_id AND r.kind = 'HOLD_' || NEW.verb
      AND r.actor_scope = 'account:' || NEW.actor_id::text
      AND r.state = 'SUCCEEDED' AND a.actor_id = NEW.actor_id
  ) THEN
    RAISE EXCEPTION 'Hold operation binding disagrees with its owner tuple' USING ERRCODE = '23514';
  END IF;
  RETURN NEW;
END
$binding$;
CREATE TRIGGER hold_operation_binding_guard BEFORE INSERT OR UPDATE OR DELETE ON hold_operation_binding
FOR EACH ROW EXECUTE FUNCTION check_hold_operation_binding();

INSERT INTO hold_operation_binding(operation_id, hold_id, actor_id, verb)
SELECT r.id, h.id, a.actor_id, substring(r.kind FROM 6)
FROM operation_receipt r
JOIN asset_reference h ON h.id::text = r.result->>'resultId'
JOIN asset a ON a.id = h.asset_id
WHERE r.kind IN ('HOLD_RESERVE','HOLD_CONFIRM','HOLD_RELEASE') AND r.state = 'SUCCEEDED'
  AND r.actor_scope = 'account:' || a.actor_id::text;

DO $retained$
BEGIN
  IF EXISTS (
    SELECT 1 FROM operation_receipt r LEFT JOIN hold_operation_binding b ON b.operation_id = r.id
    WHERE r.kind IN ('HOLD_RESERVE','HOLD_CONFIRM','HOLD_RELEASE') AND r.state = 'SUCCEEDED'
      AND b.operation_id IS NULL
  ) THEN
    RAISE EXCEPTION 'Retained hold receipt requires operator reconstruction before migration';
  END IF;
END
$retained$;

ALTER TABLE hold_operation_binding OWNER TO spoolin_media_owner;
ALTER FUNCTION check_hold_operation_binding() OWNER TO spoolin_media_owner;
REVOKE ALL ON hold_operation_binding FROM PUBLIC;
REVOKE ALL ON FUNCTION check_hold_operation_binding() FROM PUBLIC;
GRANT SELECT,INSERT ON hold_operation_binding TO spoolin_media_runtime;
GRANT SELECT ON hold_operation_binding TO spoolin_media_maintenance,spoolin_media_backup;
GRANT EXECUTE ON FUNCTION check_hold_operation_binding() TO spoolin_media_runtime;
