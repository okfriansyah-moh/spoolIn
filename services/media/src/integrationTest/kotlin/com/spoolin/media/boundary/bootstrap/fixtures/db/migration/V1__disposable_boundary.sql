-- Disposable boundary fixture only; this is not application schema DDL.
CREATE TABLE appdata.operation_receipt (id uuid PRIMARY KEY, result jsonb);
CREATE TABLE appdata.operational_event (id uuid PRIMARY KEY);
CREATE TABLE appdata.catalog_audit_event (id uuid PRIMARY KEY);
CREATE TABLE appdata.private_key_material (id uuid PRIMARY KEY);
