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

GRANT CONNECT ON DATABASE accounts_caz TO reporting_readonly_role;
GRANT USAGE ON SCHEMA caz_account TO reporting_readonly_role;
GRANT SELECT ON ALL TABLES IN SCHEMA caz_account TO reporting_readonly_role;
GRANT SELECT ON pg_stats TO reporting_readonly_role;
GRANT SELECT ON pg_class TO reporting_readonly_role;
GRANT SELECT ON pg_namespace TO reporting_readonly_role;
ALTER DEFAULT PRIVILEGES IN SCHEMA caz_account GRANT SELECT ON TABLES TO reporting_readonly_role;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'reporting_user') THEN
		CREATE USER reporting_user WITH
		LOGIN
		PASSWORD 'md594881d6dd7827e9364802154474330ed'
		NOSUPERUSER
		INHERIT
		NOCREATEDB
		NOCREATEROLE
		NOREPLICATION
		IN ROLE reporting_readonly_role;
    END IF;
END
$$;  
