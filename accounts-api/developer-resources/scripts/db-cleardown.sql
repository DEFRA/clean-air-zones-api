-- This script is to be run into the Accounts database to delete records spanning.
-- It is recommended that these scripts be run sequentially after one another. 
DELETE FROM caz_account.t_account_direct_debit_mandate CASCADE;
DELETE FROM caz_account.t_account_job_register CASCADE;
DELETE FROM caz_account.t_account_user_code CASCADE;
DELETE FROM caz_account.t_account_user_permission CASCADE;
DELETE FROM caz_account.t_vehicle_chargeability CASCADE;
DELETE FROM caz_account.t_account_user CASCADE;
DELETE FROM caz_account.t_account_vehicle CASCADE;
DELETE FROM caz_account.t_account CASCADE;
DELETE FROM caz_account_audit.t_logged_actions CASCADE;

-- Truncate the logged actions to remove prior indexed values
TRUNCATE caz_account_audit.t_logged_actions;

-- Validation scripts (each should return a count of 0)
SELECT COUNT(1) FROM caz_account.t_account_direct_debit_mandate;
SELECT COUNT(1) FROM caz_account.t_account_job_register;
SELECT COUNT(1) FROM caz_account.t_account_user_code;
SELECT COUNT(1) FROM caz_account.t_account_user_permission;
SELECT COUNT(1) FROM caz_account.t_vehicle_chargeability;
SELECT COUNT(1) FROM caz_account.t_account_user;
SELECT COUNT(1) FROM caz_account.t_account_vehicle;
SELECT COUNT(1) FROM caz_account.t_account;
SELECT COUNT(1) FROM caz_account_audit.t_logged_actions;