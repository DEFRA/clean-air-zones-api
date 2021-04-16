/* JAQU-CAZ MVP Reporting - Requirement 5	
Number of vehicles not recorded/not identified
Author: Informed */

CREATE OR REPLACE VIEW caz_reporting.report_5_total_unidentified_per_caz_per_day AS
-- Number of vehicle not identified by CCAZ per day
	SELECT DATE_TRUNC('day', hour) as Day,
	-- determine clean air zone name 
	CASE
	WHEN clean_air_zone_id = '39e54ed8-3ed2-441d-be3f-38fc9b70c8d3'
	THEN 'Leeds'
	WHEN clean_air_zone_id = '5cd7441d-766f-48ff-b8ad-1809586fea37'
	THEN 'Birmingham'
	ELSE 'Unrecognised'
	END AS clean_air_zone, 
	COUNT(*)
	FROM caz_reporting.t_vehicle_entrant_reporting
	WHERE charge_validity_code = 'CVC04'
	GROUP BY Day, clean_air_zone_id
	ORDER BY Day;

CREATE OR REPLACE VIEW caz_reporting.report_5_total_unidentified_nationally_per_day AS
-- Number of vehicle not identified nationally per day
	SELECT DATE_TRUNC('day', hour) as Day, 
	COUNT(*)
	FROM caz_reporting.t_vehicle_entrant_reporting
	WHERE charge_validity_code = 'CVC04'
	GROUP BY Day
	ORDER BY Day;