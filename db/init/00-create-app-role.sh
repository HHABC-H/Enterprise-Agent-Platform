#!/bin/sh
set -eu
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" \
  --set=app_user="$APP_DB_USER" --set=app_password="$APP_DB_PASSWORD" <<-'EOSQL'
CREATE ROLE :"app_user" LOGIN PASSWORD :'app_password';
GRANT CONNECT ON DATABASE agent_platform TO :"app_user";
GRANT USAGE, CREATE ON SCHEMA public TO :"app_user";
EOSQL
