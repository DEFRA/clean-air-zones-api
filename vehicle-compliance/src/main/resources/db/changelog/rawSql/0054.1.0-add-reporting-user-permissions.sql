ALTER USER reporting_user WITH PASSWORD 'md5c79a8b73146bc5ddc900d7574ae70915';
GRANT SELECT ON pg_stats TO reporting_readonly_role;
GRANT SELECT ON pg_class TO reporting_readonly_role;
GRANT SELECT ON pg_namespace TO reporting_readonly_role;