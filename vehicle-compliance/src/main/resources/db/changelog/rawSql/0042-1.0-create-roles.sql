CREATE ROLE vccs_readonly_role WITH
NOLOGIN
NOSUPERUSER
NOINHERIT
NOCREATEDB
NOCREATEROLE
NOREPLICATION;
GRANT CONNECT ON DATABASE vehicle_compliance TO vccs_readonly_role;
GRANT USAGE ON SCHEMA public TO vccs_readonly_role;
GRANT SELECT ON ALL TABLES IN SCHEMA public TO vccs_readonly_role;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT ON TABLES TO vccs_readonly_role;

GRANT USAGE ON SCHEMA audit TO vccs_readonly_role;
GRANT SELECT ON ALL TABLES IN SCHEMA audit TO vccs_readonly_role;
ALTER DEFAULT PRIVILEGES IN SCHEMA audit GRANT SELECT ON TABLES TO vccs_readonly_role;

GRANT USAGE ON SCHEMA caz_test_harness TO vccs_readonly_role;
GRANT SELECT ON ALL TABLES IN SCHEMA caz_test_harness TO vccs_readonly_role;
ALTER DEFAULT PRIVILEGES IN SCHEMA caz_test_harness GRANT SELECT ON TABLES TO vccs_readonly_role;

GRANT USAGE ON SCHEMA caz_vehicle_entrant TO vccs_readonly_role;
GRANT SELECT ON ALL TABLES IN SCHEMA caz_vehicle_entrant TO vccs_readonly_role;
ALTER DEFAULT PRIVILEGES IN SCHEMA caz_vehicle_entrant GRANT SELECT ON TABLES TO vccs_readonly_role;

GRANT USAGE ON SCHEMA caz_vehicle_entrant_audit TO vccs_readonly_role;
GRANT SELECT ON ALL TABLES IN SCHEMA caz_vehicle_entrant_audit TO vccs_readonly_role;
ALTER DEFAULT PRIVILEGES IN SCHEMA caz_vehicle_entrant_audit GRANT SELECT ON TABLES TO vccs_readonly_role;


CREATE ROLE vccs_readwrite_role WITH
NOLOGIN
NOSUPERUSER
NOINHERIT
NOCREATEDB
NOCREATEROLE
NOREPLICATION;

GRANT CONNECT ON DATABASE vehicle_compliance TO vccs_readwrite_role;

GRANT USAGE ON SCHEMA public TO vccs_readwrite_role;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO vccs_readwrite_role;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO vccs_readwrite_role;

GRANT USAGE ON SCHEMA audit TO vccs_readwrite_role;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA audit TO vccs_readwrite_role;
ALTER DEFAULT PRIVILEGES IN SCHEMA audit GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO vccs_readwrite_role;

GRANT USAGE ON SCHEMA caz_test_harness TO vccs_readwrite_role;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA caz_test_harness TO vccs_readwrite_role;
ALTER DEFAULT PRIVILEGES IN SCHEMA caz_test_harness GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO vccs_readwrite_role;

GRANT USAGE ON SCHEMA caz_vehicle_entrant TO vccs_readwrite_role;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA caz_vehicle_entrant TO vccs_readwrite_role;
ALTER DEFAULT PRIVILEGES IN SCHEMA caz_vehicle_entrant GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO vccs_readwrite_role;

GRANT USAGE ON SCHEMA caz_vehicle_entrant_audit TO vccs_readwrite_role;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA caz_vehicle_entrant_audit TO vccs_readwrite_role;
ALTER DEFAULT PRIVILEGES IN SCHEMA caz_vehicle_entrant_audit GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO vccs_readwrite_role;ALTER DEFAULT PRIVILEGES IN SCHEMA audit GRANT SELECT ON TABLES TO vccs_readonly_role;

GRANT USAGE ON SCHEMA caz_test_harness TO vccs_readonly_role;
GRANT SELECT ON ALL TABLES IN SCHEMA caz_test_harness TO vccs_readonly_role;
ALTER DEFAULT PRIVILEGES IN SCHEMA caz_test_harness GRANT SELECT ON TABLES TO vccs_readonly_role;

GRANT USAGE ON SCHEMA caz_vehicle_entrant TO vccs_readonly_role;
GRANT SELECT ON ALL TABLES IN SCHEMA caz_vehicle_entrant TO vccs_readonly_role;
ALTER DEFAULT PRIVILEGES IN SCHEMA caz_vehicle_entrant GRANT SELECT ON TABLES TO vccs_readonly_role;

GRANT USAGE ON SCHEMA caz_vehicle_entrant_audit TO vccs_readonly_role;
GRANT SELECT ON ALL TABLES IN SCHEMA caz_vehicle_entrant_audit TO vccs_readonly_role;
ALTER DEFAULT PRIVILEGES IN SCHEMA caz_vehicle_entrant_audit GRANT SELECT ON TABLES TO vccs_readonly_role;