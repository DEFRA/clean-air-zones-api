GRANT USAGE ON SCHEMA caz_account_audit TO accounts_readonly_role;
GRANT SELECT ON ALL TABLES IN SCHEMA caz_account_audit TO accounts_readonly_role;
ALTER DEFAULT PRIVILEGES IN SCHEMA caz_account_audit GRANT SELECT ON TABLES TO accounts_readonly_role;

GRANT USAGE ON SCHEMA caz_account_audit TO accounts_readwrite_role;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA caz_account_audit TO accounts_readwrite_role;
ALTER DEFAULT PRIVILEGES IN SCHEMA caz_account_audit GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO accounts_readwrite_role;
