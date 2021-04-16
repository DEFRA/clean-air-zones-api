/* JAQU-CAZ MVP Reporting - Requirement 1.2
Total number of unique vehicle numbers by Vehicle Type Approval and DVLA Fuel Type entering a CCAZ
Author: Informed */

CREATE OR REPLACE VIEW caz_reporting.report_1_total_unique_per_caz_per_day AS
-- Total number of unique vehicle numbers by Vehicle Type Approval and Fuel Type entering a CCAZ by CCAZ per day
	SELECT 
	DATE_TRUNC('day', hour) as Day,
	-- determine clean air zone name
	CASE
	WHEN clean_air_zone_id = '39e54ed8-3ed2-441d-be3f-38fc9b70c8d3'
	THEN 'Leeds'
	WHEN clean_air_zone_id = '5cd7441d-766f-48ff-b8ad-1809586fea37'
	THEN 'Birmingham'
	ELSE 'Unrecognised'
	END AS clean_air_zone, 
	type_approval, 
	fuel_type, 
	COUNT(DISTINCT vrn_hash) 
	FROM caz_reporting.t_vehicle_entrant_reporting ver 
	INNER JOIN caz_reporting.t_type_approval ta 
	ON ver.type_approval_id = ta.type_approval_id
	INNER JOIN caz_reporting.t_fuel_type ft 
	ON ver.fuel_type_id = ft.fuel_type_id
	GROUP BY day, clean_air_zone_id, type_approval, fuel_type
	ORDER BY day;

CREATE OR REPLACE VIEW caz_reporting.report_1_total_unique_nationally_per_day AS
-- Total number of unique vehicle numbers by Vehicle Type Approval and Fuel Type entering a CCAZ Nationally per day
	SELECT 
	DATE_TRUNC('day', hour) as Day,
	type_approval, 
	fuel_type, 
	COUNT(DISTINCT vrn_hash) 
	FROM caz_reporting.t_vehicle_entrant_reporting ver 
	INNER JOIN caz_reporting.t_type_approval ta 
	ON ver.type_approval_id = ta.type_approval_id
	INNER JOIN caz_reporting.t_fuel_type ft 
	ON ver.fuel_type_id = ft.fuel_type_id
	GROUP BY day, type_approval, fuel_type
	ORDER BY day;

CREATE OR REPLACE VIEW caz_reporting.report_1_total_unique_per_caz_per_hour AS
-- Total number of unique vehicle numbers by Vehicle Type Approval and Fuel Type entering a CCAZ by CCAZ per hour
	SELECT hour, 
	-- determine clean air zone name
	CASE 
	WHEN clean_air_zone_id = '39e54ed8-3ed2-441d-be3f-38fc9b70c8d3'
	THEN 'Leeds'
	WHEN clean_air_zone_id = '5cd7441d-766f-48ff-b8ad-1809586fea37'
	THEN 'Birmingham'
	ELSE 'Unrecognised'
	END as clean_air_zone,
	type_approval, 
	fuel_type, 
	COUNT(DISTINCT vrn_hash) 
	FROM caz_reporting.t_vehicle_entrant_reporting ver
	INNER JOIN caz_reporting.t_type_approval ta 
	ON ver.type_approval_id = ta.type_approval_id
	INNER JOIN caz_reporting.t_fuel_type ft 
	ON ver.fuel_type_id = ft.fuel_type_id
	GROUP BY hour, clean_air_zone_id, type_approval, fuel_type
	ORDER BY hour;

CREATE OR REPLACE VIEW caz_reporting.report_1_total_unique_nationally_per_hour AS
-- Total number of unique vehicle numbers by Vehicle Type Approval and Fuel Type entering a CCAZ Nationally per hour
	SELECT hour,
	type_approval, 
	fuel_type, 
	COUNT(DISTINCT vrn_hash) 
	FROM caz_reporting.t_vehicle_entrant_reporting ver
	INNER JOIN caz_reporting.t_type_approval ta 
	ON ver.type_approval_id = ta.type_approval_id
	INNER JOIN caz_reporting.t_fuel_type ft 
	ON ver.fuel_type_id = ft.fuel_type_id
	GROUP BY hour, type_approval, fuel_type
	ORDER BY hour;