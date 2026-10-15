\set ON_ERROR_STOP on
-- Privileged preparation only. Values arrive from protected mounted files.
SELECT 'CREATE ROLE spoolin_core_owner NOLOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION' WHERE NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'spoolin_core_owner') \gexec
SELECT format('CREATE ROLE spoolin_core_runtime LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION PASSWORD %L', :'core_runtime_password') WHERE NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'spoolin_core_runtime') \gexec
ALTER ROLE spoolin_core_runtime SET search_path = appdata;
ALTER ROLE spoolin_core_runtime SET statement_timeout = '3s';
ALTER ROLE spoolin_core_runtime SET lock_timeout = '2s';
ALTER ROLE spoolin_core_runtime SET idle_in_transaction_session_timeout = '5s';
SELECT format('CREATE ROLE spoolin_core_migrator LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION PASSWORD %L', :'core_migrator_password') WHERE NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'spoolin_core_migrator') \gexec
ALTER ROLE spoolin_core_migrator SET search_path = appdata;
ALTER ROLE spoolin_core_migrator SET statement_timeout = '3s';
ALTER ROLE spoolin_core_migrator SET lock_timeout = '2s';
ALTER ROLE spoolin_core_migrator SET idle_in_transaction_session_timeout = '5s';
SELECT format('CREATE ROLE spoolin_core_maintenance LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION PASSWORD %L', :'core_maintenance_password') WHERE NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'spoolin_core_maintenance') \gexec
ALTER ROLE spoolin_core_maintenance SET search_path = appdata;
ALTER ROLE spoolin_core_maintenance SET statement_timeout = '3s';
ALTER ROLE spoolin_core_maintenance SET lock_timeout = '2s';
ALTER ROLE spoolin_core_maintenance SET idle_in_transaction_session_timeout = '5s';
SELECT format('CREATE ROLE spoolin_core_backup LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION PASSWORD %L', :'core_backup_password') WHERE NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'spoolin_core_backup') \gexec
ALTER ROLE spoolin_core_backup SET search_path = appdata;
ALTER ROLE spoolin_core_backup SET statement_timeout = '3s';
ALTER ROLE spoolin_core_backup SET lock_timeout = '2s';
ALTER ROLE spoolin_core_backup SET idle_in_transaction_session_timeout = '5s';
SELECT 'CREATE DATABASE spoolin_core OWNER spoolin_core_owner ENCODING ''UTF8'' LC_COLLATE ''C'' LC_CTYPE ''C'' TEMPLATE template0' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'spoolin_core') \gexec
REVOKE ALL ON DATABASE spoolin_core FROM PUBLIC;
GRANT CONNECT ON DATABASE spoolin_core TO spoolin_core_runtime, spoolin_core_migrator, spoolin_core_maintenance, spoolin_core_backup;
SELECT 'CREATE ROLE spoolin_catalog_owner NOLOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION' WHERE NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'spoolin_catalog_owner') \gexec
SELECT format('CREATE ROLE spoolin_catalog_runtime LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION PASSWORD %L', :'catalog_runtime_password') WHERE NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'spoolin_catalog_runtime') \gexec
ALTER ROLE spoolin_catalog_runtime SET search_path = appdata;
ALTER ROLE spoolin_catalog_runtime SET statement_timeout = '3s';
ALTER ROLE spoolin_catalog_runtime SET lock_timeout = '2s';
ALTER ROLE spoolin_catalog_runtime SET idle_in_transaction_session_timeout = '5s';
SELECT format('CREATE ROLE spoolin_catalog_migrator LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION PASSWORD %L', :'catalog_migrator_password') WHERE NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'spoolin_catalog_migrator') \gexec
ALTER ROLE spoolin_catalog_migrator SET search_path = appdata;
ALTER ROLE spoolin_catalog_migrator SET statement_timeout = '3s';
ALTER ROLE spoolin_catalog_migrator SET lock_timeout = '2s';
ALTER ROLE spoolin_catalog_migrator SET idle_in_transaction_session_timeout = '5s';
SELECT format('CREATE ROLE spoolin_catalog_maintenance LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION PASSWORD %L', :'catalog_maintenance_password') WHERE NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'spoolin_catalog_maintenance') \gexec
ALTER ROLE spoolin_catalog_maintenance SET search_path = appdata;
ALTER ROLE spoolin_catalog_maintenance SET statement_timeout = '3s';
ALTER ROLE spoolin_catalog_maintenance SET lock_timeout = '2s';
ALTER ROLE spoolin_catalog_maintenance SET idle_in_transaction_session_timeout = '5s';
SELECT format('CREATE ROLE spoolin_catalog_backup LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION PASSWORD %L', :'catalog_backup_password') WHERE NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'spoolin_catalog_backup') \gexec
ALTER ROLE spoolin_catalog_backup SET search_path = appdata;
ALTER ROLE spoolin_catalog_backup SET statement_timeout = '3s';
ALTER ROLE spoolin_catalog_backup SET lock_timeout = '2s';
ALTER ROLE spoolin_catalog_backup SET idle_in_transaction_session_timeout = '5s';
SELECT 'CREATE DATABASE spoolin_catalog OWNER spoolin_catalog_owner ENCODING ''UTF8'' LC_COLLATE ''C'' LC_CTYPE ''C'' TEMPLATE template0' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'spoolin_catalog') \gexec
REVOKE ALL ON DATABASE spoolin_catalog FROM PUBLIC;
GRANT CONNECT ON DATABASE spoolin_catalog TO spoolin_catalog_runtime, spoolin_catalog_migrator, spoolin_catalog_maintenance, spoolin_catalog_backup;
SELECT 'CREATE ROLE spoolin_media_owner NOLOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION' WHERE NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'spoolin_media_owner') \gexec
SELECT format('CREATE ROLE spoolin_media_runtime LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION PASSWORD %L', :'media_runtime_password') WHERE NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'spoolin_media_runtime') \gexec
ALTER ROLE spoolin_media_runtime SET search_path = appdata;
ALTER ROLE spoolin_media_runtime SET statement_timeout = '3s';
ALTER ROLE spoolin_media_runtime SET lock_timeout = '2s';
ALTER ROLE spoolin_media_runtime SET idle_in_transaction_session_timeout = '5s';
SELECT format('CREATE ROLE spoolin_media_migrator LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION PASSWORD %L', :'media_migrator_password') WHERE NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'spoolin_media_migrator') \gexec
ALTER ROLE spoolin_media_migrator SET search_path = appdata;
ALTER ROLE spoolin_media_migrator SET statement_timeout = '3s';
ALTER ROLE spoolin_media_migrator SET lock_timeout = '2s';
ALTER ROLE spoolin_media_migrator SET idle_in_transaction_session_timeout = '5s';
SELECT format('CREATE ROLE spoolin_media_maintenance LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION PASSWORD %L', :'media_maintenance_password') WHERE NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'spoolin_media_maintenance') \gexec
ALTER ROLE spoolin_media_maintenance SET search_path = appdata;
ALTER ROLE spoolin_media_maintenance SET statement_timeout = '3s';
ALTER ROLE spoolin_media_maintenance SET lock_timeout = '2s';
ALTER ROLE spoolin_media_maintenance SET idle_in_transaction_session_timeout = '5s';
SELECT format('CREATE ROLE spoolin_media_backup LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION PASSWORD %L', :'media_backup_password') WHERE NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'spoolin_media_backup') \gexec
ALTER ROLE spoolin_media_backup SET search_path = appdata;
ALTER ROLE spoolin_media_backup SET statement_timeout = '3s';
ALTER ROLE spoolin_media_backup SET lock_timeout = '2s';
ALTER ROLE spoolin_media_backup SET idle_in_transaction_session_timeout = '5s';
SELECT 'CREATE DATABASE spoolin_media OWNER spoolin_media_owner ENCODING ''UTF8'' LC_COLLATE ''C'' LC_CTYPE ''C'' TEMPLATE template0' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'spoolin_media') \gexec
REVOKE ALL ON DATABASE spoolin_media FROM PUBLIC;
GRANT CONNECT ON DATABASE spoolin_media TO spoolin_media_runtime, spoolin_media_migrator, spoolin_media_maintenance, spoolin_media_backup;
REVOKE CONNECT ON DATABASE postgres, template1 FROM PUBLIC;
\connect spoolin_core
REVOKE ALL ON SCHEMA public FROM PUBLIC;
CREATE SCHEMA IF NOT EXISTS appdata AUTHORIZATION spoolin_core_owner;
REVOKE ALL ON SCHEMA appdata FROM PUBLIC;
GRANT USAGE ON SCHEMA appdata TO spoolin_core_runtime, spoolin_core_maintenance, spoolin_core_backup;
\connect spoolin_catalog
REVOKE ALL ON SCHEMA public FROM PUBLIC;
CREATE SCHEMA IF NOT EXISTS appdata AUTHORIZATION spoolin_catalog_owner;
REVOKE ALL ON SCHEMA appdata FROM PUBLIC;
GRANT USAGE ON SCHEMA appdata TO spoolin_catalog_runtime, spoolin_catalog_maintenance, spoolin_catalog_backup;
CREATE EXTENSION IF NOT EXISTS pg_trgm WITH SCHEMA appdata;
\connect spoolin_media
REVOKE ALL ON SCHEMA public FROM PUBLIC;
CREATE SCHEMA IF NOT EXISTS appdata AUTHORIZATION spoolin_media_owner;
REVOKE ALL ON SCHEMA appdata FROM PUBLIC;
GRANT USAGE ON SCHEMA appdata TO spoolin_media_runtime, spoolin_media_maintenance, spoolin_media_backup;
