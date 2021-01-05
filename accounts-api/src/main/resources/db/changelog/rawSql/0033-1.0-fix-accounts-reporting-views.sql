-- Fix last upload view
CREATE OR REPLACE VIEW caz_account.last_upload AS
	SELECT account_name, 
	max(insert_timestmp) AS last_upload_date, 
	n_vehicles
	FROM caz_account.t_account_job_register acc_job 
	INNER JOIN
	caz_account.t_account acc
	ON acc_job.uploader_id = acc.account_id
	INNER JOIN 
	  (SELECT account_id, 
	   count(*) AS n_vehicles 
	   FROM caz_account.t_account_vehicle 
	   GROUP BY account_id) AS no_vehicles
    ON no_vehicles.account_id = acc_job.uploader_id
    WHERE status = 'FINISHED_SUCCESS'
    GROUP BY account_name, n_vehicles
