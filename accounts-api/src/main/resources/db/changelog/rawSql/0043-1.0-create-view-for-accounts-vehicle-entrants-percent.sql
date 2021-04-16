CREATE OR REPLACE VIEW caz_reporting.all_account_vehicles AS
	SELECT encode(sha256(vrn::bytea), 'hex')::varchar(64) AS vrn_hash
	FROM caz_account.t_account_vehicle
	