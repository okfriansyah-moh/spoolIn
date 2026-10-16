\set ON_ERROR_STOP on
-- Run after each owner migration with privileged preparation credentials.
DO $grants$
DECLARE
  db text := current_database();
  runtime_role text := db || '_runtime';
  maintenance_role text := db || '_maintenance';
  backup_role text := db || '_backup';
  names text[] := ARRAY['operation_receipt', 'durable_work'];
  audit_names text[] := ARRAY['operational_event'];
  table_name text;
BEGIN
  IF db NOT IN ('spoolin_core','spoolin_catalog','spoolin_media') THEN
    RAISE EXCEPTION 'grants: an owned database is required';
  END IF;
  IF db = 'spoolin_core' THEN
    names := names || ARRAY['user_account','account_email_lookup','browser_session','mcp_credential','form_draft','request_rate_window'];
  ELSIF db = 'spoolin_catalog' THEN
    audit_names := audit_names || ARRAY['catalog_audit_event'];
    names := names || ARRAY['title','title_genre','person','credit','genre','credit_role','provenance_record','seed_member','artwork_attachment_request','movie_proposal'];
  ELSE
    names := names || ARRAY['asset','asset_reference'];
  END IF;
  EXECUTE format('REVOKE ALL ON ALL TABLES IN SCHEMA appdata FROM %I', runtime_role);
  FOREACH table_name IN ARRAY names LOOP
    IF to_regclass(format('appdata.%I',table_name)) IS NOT NULL THEN
      EXECUTE format('GRANT SELECT,INSERT,UPDATE,DELETE ON appdata.%I TO %I',table_name,runtime_role);
    END IF;
  END LOOP;
  IF to_regclass('appdata.operation_receipt') IS NOT NULL THEN
    EXECUTE format('REVOKE DELETE ON appdata.operation_receipt FROM %I',runtime_role);
    EXECUTE format('GRANT SELECT,UPDATE ON appdata.operation_receipt TO %I',maintenance_role);
  END IF;
  IF db = 'spoolin_core' AND to_regclass('appdata.crypto_key_version') IS NOT NULL THEN
    EXECUTE format('GRANT SELECT,INSERT,UPDATE ON appdata.crypto_key_version TO %I',runtime_role);
  END IF;
  FOREACH table_name IN ARRAY audit_names LOOP
    IF to_regclass(format('appdata.%I',table_name)) IS NOT NULL THEN
      EXECUTE format('GRANT SELECT,INSERT ON appdata.%I TO %I',table_name,runtime_role);
      EXECUTE format('GRANT SELECT ON appdata.%I TO %I',table_name,maintenance_role);
      IF table_name = 'operational_event' THEN
        EXECUTE format('GRANT DELETE ON appdata.%I TO %I',table_name,maintenance_role);
      END IF;
    END IF;
  END LOOP;
  IF to_regclass('appdata.flyway_schema_history') IS NOT NULL THEN
    EXECUTE format('GRANT SELECT ON appdata.flyway_schema_history TO %I',runtime_role);
  END IF;
  EXECUTE format('GRANT SELECT ON ALL TABLES IN SCHEMA appdata TO %I',backup_role);
END
$grants$;
