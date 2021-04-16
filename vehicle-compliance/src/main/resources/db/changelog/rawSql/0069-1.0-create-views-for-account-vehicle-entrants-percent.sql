CREATE OR REPLACE VIEW caz_reporting.all_vehicle_entrants AS
	SELECT vrn_hash, charge_validity_code, caz_name, hour, cvt.ccaz_vehicle_type
	FROM caz_reporting.t_vehicle_entrant_reporting ver 
	INNER JOIN caz_reporting.t_clean_air_zone caz 
	ON ver.clean_air_zone_id = caz.clean_air_zone_id
	INNER JOIN caz_reporting.t_ccaz_vehicle_type cvt
	ON ver.ccaz_vehicle_type_id = cvt.ccaz_vehicle_type_id
