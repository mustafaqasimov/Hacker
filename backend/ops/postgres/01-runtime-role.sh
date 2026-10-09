#!/bin/sh
set -eu
psql --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" --set=ON_ERROR_STOP=1 --set=runtime_password="$RUNTIME_DB_PASSWORD" <<'SQL'
CREATE ROLE hacktrain_runtime LOGIN PASSWORD :'runtime_password' NOSUPERUSER NOBYPASSRLS;
GRANT USAGE ON SCHEMA public TO hacktrain_runtime;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT,INSERT,UPDATE,DELETE ON TABLES TO hacktrain_runtime;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT USAGE,SELECT ON SEQUENCES TO hacktrain_runtime;
SQL
