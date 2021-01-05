-- create views for accounts reporting

-- number of accounts
CREATE OR REPLACE VIEW caz_account.number_of_accounts AS
  SELECT COUNT(*) AS "Number of Accounts" 
  FROM caz_account.t_account;

-- Number of vehicles per account
CREATE OR REPLACE VIEW caz_account.number_of_vehicles_per_account AS
  SELECT account_name, COUNT(account_vehicle_id) FROM
  caz_account.t_account acc 
  INNER JOIN
  caz_account.t_account_vehicle acc_v 
  ON acc.account_id = acc_v.account_id
 GROUP BY account_name;

-- Profile of vehicle types - numbers of each vehicle type, overall and per account
CREATE OR REPLACE VIEW caz_account.profile_of_vehicle_types AS
  SELECT caz_vehicle_type, COUNT(*) from 
  caz_account.t_account acc 
  INNER JOIN
  caz_account.t_account_vehicle acc_v 
  ON acc.account_id = acc_v.account_id
  GROUP BY caz_vehicle_type;

CREATE OR REPLACE VIEW caz_account.profile_of_vehicle_types_per_account AS
  SELECT account_name, caz_vehicle_type, count(*) FROM
  caz_account.t_account acc 
  INNER JOIN
  caz_account.t_account_vehicle acc_v 
  ON acc.account_id = acc_v.account_id
  GROUP BY account_name, caz_vehicle_type
  ORDER BY account_name;

-- Upload patterns and frequencies
CREATE OR REPLACE VIEW caz_account.last_upload AS
  SELECT account_name, max(insert_timestmp) AS last_upload_date, 
  COUNT(account_vehicle_id) AS n_vehicles
  FROM caz_account.t_account_job_register acc_job INNER JOIN
  caz_account.t_account acc
  ON acc_job.uploader_id = acc.account_id
  INNER JOIN caz_account.t_account_vehicle acc_v
  ON acc_v.account_id = acc.account_id
  WHERE status = 'FINISHED_SUCCESS'
  GROUP BY account_name;

CREATE OR REPLACE VIEW caz_account.weeks_since_last_upload AS
  SELECT account_name, 
  MAX(insert_timestmp) AS last_upload_date, 
  (date_part('days'::text, now() - max(insert_timestmp)::timestamp with time zone) / 7::double precision)::integer AS weeks_since_last_upload
  FROM caz_account.t_account_job_register acc_job INNER JOIN
  caz_account.t_account acc
  ON acc_job.uploader_id = acc.account_id
  WHERE status = 'FINISHED_SUCCESS'
  GROUP BY account_name;

