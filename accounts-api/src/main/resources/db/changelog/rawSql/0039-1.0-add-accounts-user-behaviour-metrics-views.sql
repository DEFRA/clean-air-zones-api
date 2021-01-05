CREATE SCHEMA IF NOT EXISTS caz_reporting;
GRANT USAGE ON SCHEMA caz_reporting TO reporting_readonly_role;
GRANT SELECT ON ALL TABLES IN SCHEMA caz_reporting TO reporting_readonly_role;
ALTER DEFAULT PRIVILEGES IN SCHEMA caz_reporting GRANT SELECT ON TABLES TO reporting_readonly_role;

--% of created business accounts which are active
CREATE OR REPLACE VIEW caz_reporting.no_active_inactive_business_accounts AS
  SELECT CASE WHEN inactivation_tstamp IS null
         THEN 'active'
         ELSE 'inactive'
         END AS activation_status, 
    COUNT(account_id) AS no_accounts
	FROM caz_account.t_account
	GROUP BY activation_status;
	
	
--How many users are registered within business accounts	
CREATE OR REPLACE VIEW caz_reporting.no_business_account_users AS
SELECT COUNT(account_user_id) FROM caz_account.t_account_user;

--Number of users per business account
CREATE OR REPLACE VIEW caz_reporting.no_business_account_user_per_account AS
SELECT count(account_user_id) FROM caz_account.t_account_user
GROUP BY account_id;

-- Total number of users who can per permission
CREATE OR REPLACE VIEW caz_reporting.no_users_per_permission AS
SELECT description, count(user_perm.account_user_id) FROM caz_account.t_account_user acc_user
INNER JOIN caz_account.t_account_user_permission user_perm
ON acc_user.account_user_id = user_perm.account_user_id
INNER JOIN caz_account.t_account_permission perm
ON user_perm.account_permission_id = perm.account_permission_id
GROUP BY description;

--Number of permissions per user
CREATE OR REPLACE VIEW caz_reporting.no_permissions_per_user AS
SELECT count(user_perm.account_permission_id) FROM caz_account.t_account_user acc_user
INNER JOIN caz_account.t_account_user_permission user_perm
ON acc_user.account_user_id = user_perm.account_user_id
GROUP BY acc_user.account_user_id;

--Total number of active,pending, and failed or abandoned direct debits per CAZ
CREATE OR REPLACE VIEW caz_reporting.direct_debits_by_caz_and_status AS
SELECT CASE WHEN clean_air_zone_id = '5cd7441d-766f-48ff-b8ad-1809586fea37'
    THEN 'Birmingham'
	WHEN clean_air_zone_id = '131af03c-f7f4-4aef-81ee-aae4f56dbeb5'
	THEN 'Bath'
	ELSE clean_air_zone_id::text
	END AS clean_air_zone, status, count(direct_debit_mandate_id)
	FROM caz_account.t_account_direct_debit_mandate
	GROUP BY clean_air_zone, status
	ORDER BY clean_air_zone;

--Number of weeks between creation and creating direct debit per account
CREATE OR REPLACE VIEW caz_reporting.no_weeks_between_creation_and_dd_creation AS
Select (EXTRACT(days FROM (min(created) - action_tstamp)) / 7)::int as no_weeks_from_creation_to_dd
from caz_account.t_account_direct_debit_mandate dd
inner join (select new_data ->> 'account_id'::text as account_id, action_tstamp from 
caz_account_audit.t_logged_actions
where table_name = 't_account' and query like '%INSERT%') as log_acc
on  dd.account_id::text = log_acc.account_id
group by dd.account_id, action_tstamp;