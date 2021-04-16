CREATE ROLE whitelist_readonly_role WITH
NOLOGIN
NOSUPERUSER
NOINHERIT
NOCREATEDB
NOCREATEROLE
NOREPLICATION;

GRANT CONNECT ON DATABASE vehicle_compliance TO whitelist_readonly_role;
GRANT USAGE ON SCHEMA caz_whitelist_vehicles TO whitelist_readonly_role;
GRANT SELECT ON ALL TABLES IN SCHEMA caz_whitelist_vehicles TO whitelist_readonly_role;
ALTER DEFAULT PRIVILEGES IN SCHEMA caz_whitelist_vehicles GRANT SELECT ON TABLES TO whitelist_readonly_role;
GRANT USAGE ON SCHEMA caz_whitelist_vehicles_audit TO whitelist_readonly_role;
GRANT SELECT ON ALL TABLES IN SCHEMA caz_whitelist_vehicles_audit TO whitelist_readonly_role;
ALTER DEFAULT PRIVILEGES IN SCHEMA caz_whitelist_vehicles_audit GRANT SELECT ON TABLES TO whitelist_readonly_role;


CREATE ROLE whitelist_readwrite_role WITH
NOLOGIN
NOSUPERUSER
NOINHERIT
NOCREATEDB
NOCREATEROLE
NOREPLICATION;

GRANT CONNECT ON DATABASE vehicle_compliance TO whitelist_readwrite_role;
GRANT USAGE ON SCHEMA caz_whitelist_vehicles TO whitelist_readwrite_role;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA caz_whitelist_vehicles TO whitelist_readwrite_role;
ALTER DEFAULT PRIVILEGES IN SCHEMA caz_whitelist_vehicles GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO whitelist_readwrite_role;
GRANT USAGE ON SCHEMA caz_whitelist_vehicles_audit TO whitelist_readwrite_role;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA caz_whitelist_vehicles_audit TO whitelist_readwrite_role;
ALTER DEFAULT PRIVILEGES IN SCHEMA caz_whitelist_vehicles_audit GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO whitelist_readwrite_role;