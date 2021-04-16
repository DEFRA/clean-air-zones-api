-- Create reporting views for non-UK Vehicle reporting

-- Create view for non-UK Vehicles by CAZ
CREATE OR REPLACE VIEW caz_reporting.number_of_non_uk_vehicles_by_month AS
	SELECT  hour, caz_name as clean_air_zone,
	COUNT(*) FROM CAZ_REPORTING.t_vehicle_entrant_reporting ver
	INNER JOIN caz_reporting.t_clean_air_zone caz on
	caz.clean_air_zone_id = ver.clean_air_zone_id
	WHERE non_uk_vehicle = true and charge_validity_code = 'CVC03'
	GROUP BY hour, caz_name;
	
-- Create view for compliant non-UK Vehicles by CAZ
CREATE OR REPLACE VIEW caz_reporting.number_of_compliant_non_uk_vehicles_by_month AS
	SELECT hour, caz_name as clean_air_zone,
	COUNT(*) FROM CAZ_REPORTING.t_vehicle_entrant_reporting ver
	INNER JOIN caz_reporting.t_clean_air_zone caz on
	caz.clean_air_zone_id = ver.clean_air_zone_id
	WHERE non_uk_vehicle = true and charge_validity_code = 'CVC03'
	GROUP BY hour, caz_name;

-- Create view for not compliant non-UK Vehicles by CAZ
CREATE OR REPLACE VIEW caz_reporting.number_of_not_compliant_non_uk_vehicles_by_month AS
	SELECT hour, caz_name as clean_air_zone,
	COUNT(*) FROM CAZ_REPORTING.t_vehicle_entrant_reporting ver
	INNER JOIN caz_reporting.t_clean_air_zone caz on
	caz.clean_air_zone_id = ver.clean_air_zone_id
	WHERE non_uk_vehicle = true and charge_validity_code in ('CVC01', 'CVC04')
	GROUP BY hour, caz_name;