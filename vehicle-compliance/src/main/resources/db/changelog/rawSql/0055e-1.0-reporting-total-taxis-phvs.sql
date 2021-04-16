/* JAQU-CAZ MVP Reporting - Requirement 4	
Number of taxis and number of PHVs entering a CCAZ by licensing_authority
Author: Informed */

CREATE OR REPLACE VIEW caz_reporting.report_4_total_taxis_phvs_per_caz_per_day AS
-- Number of taxis and number of PHVs entering a CCAZ by licensing_authority by CCAZ per day
	SELECT DATE_TRUNC('day', hour) as Day,
	-- determine clean air zone name
	CASE
	WHEN clean_air_zone_id = '39e54ed8-3ed2-441d-be3f-38fc9b70c8d3'
	THEN 'Leeds'
	WHEN clean_air_zone_id = '5cd7441d-766f-48ff-b8ad-1809586fea37'
	THEN 'Birmingham'
	ELSE 'Unrecognised'
	END AS clean_air_zone,
	lower(description) as description_lower, 
	licensing_authority, 
	COUNT(*)
	FROM caz_reporting.t_vehicle_entrant_reporting ver
	INNER JOIN caz_reporting.t_entrant_taxi_phv taxi
	ON ver.vehicle_entrant_reporting_id = taxi.vehicle_entrant_reporting_id
	GROUP BY day, clean_air_zone_id, description_lower, licensing_authority
	ORDER BY day;

CREATE OR REPLACE VIEW caz_reporting.report_4_total_taxis_phvs_nationally_per_day AS
-- Number of taxis and number of PHVs entering a CCAZ by licensing_authority nationally per day
	SELECT DATE_TRUNC('day', hour) as Day,
	lower(description) as description_lower, 
	licensing_authority, 
	COUNT(*)
	FROM caz_reporting.t_vehicle_entrant_reporting ver
	INNER JOIN caz_reporting.t_entrant_taxi_phv taxi
	ON ver.vehicle_entrant_reporting_id = taxi.vehicle_entrant_reporting_id
	GROUP BY day, description_lower, licensing_authority
	ORDER BY day;