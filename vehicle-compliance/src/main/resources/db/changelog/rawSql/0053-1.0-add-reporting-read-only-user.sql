DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'reporting_readonly_role') THEN
        CREATE ROLE reporting_readonly_role WITH
		NOLOGIN
		NOSUPERUSER
		NOINHERIT
		NOCREATEDB
		NOCREATEROLE
		NOREPLICATION;
    END IF;
END
$$;

GRANT CONNECT ON DATABASE vehicle_compliance TO reporting_readonly_role;
GRANT USAGE ON SCHEMA caz_reporting TO reporting_readonly_role;
GRANT SELECT ON ALL TABLES IN SCHEMA caz_reporting TO reporting_readonly_role;
ALTER DEFAULT PRIVILEGES IN SCHEMA caz_reporting GRANT SELECT ON TABLES TO reporting_readonly_role;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'reporting_user') THEN
		CREATE ROLE reporting_user WITH
		LOGIN
		PASSWORD 'md54fd95d0d5f0ba5bb758940383c8d5a9c'
		NOSUPERUSER
		INHERIT
		NOCREATEDB
		NOCREATEROLE
		NOREPLICATION
		IN ROLE reporting_readonly_role;
    END IF;
END
$$;