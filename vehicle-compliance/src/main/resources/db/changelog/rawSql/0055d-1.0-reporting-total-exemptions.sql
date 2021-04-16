/* JAQU-CAZ MVP Reporting - Requirement 3
Total number of Exemptions by Vehicle Type and Exemption Reason 
Author: Informed */

CREATE OR REPLACE VIEW caz_reporting.report_3_total_exemptions_per_caz_per_day AS
-- Total number of Exemptions by Vehicle Type and Exemption Reason by CCAZ per Day
	SELECT 
	DATE_TRUNC('day', hour) as Day,
	-- Determine clean air zone name 
	CASE 
	WHEN clean_air_zone_id = '39e54ed8-3ed2-441d-be3f-38fc9b70c8d3'
	THEN 'Leeds'
	WHEN clean_air_zone_id = '5cd7441d-766f-48ff-b8ad-1809586fea37'
	THEN 'Birmingham'
	ELSE 'Unrecognised'
	END AS clean_air_zone,
	ccaz_vehicle_type, 
	exemption_reason,
	COUNT(*)
	FROM caz_reporting.t_vehicle_entrant_reporting ver
	INNER JOIN caz_reporting.t_entrant_exemption ee
	ON ver.vehicle_entrant_reporting_id = ee.vehicle_entrant_reporting_id
	INNER JOIN caz_reporting.t_exemption_reason er
	ON ee.exemption_reason_id = er.exemption_reason_id
	INNER JOIN caz_reporting.t_ccaz_vehicle_type cvt
	ON ver.ccaz_vehicle_type_id = cvt.ccaz_vehicle_type_id
	GROUP BY day, clean_air_zone_id, ccaz_vehicle_type, exemption_reason
	ORDER BY day;

CREATE OR REPLACE VIEW caz_reporting.report_3_total_exemptions_nationally_per_day AS
-- Total number of Exemptions by Vehicle Type and Exemption Reason by Nationally per Day
	SELECT 
	DATE_TRUNC('day', hour) as Day,
	ccaz_vehicle_type, 
	exemption_reason,
	COUNT(*)
	FROM caz_reporting.t_vehicle_entrant_reporting ver
	INNER JOIN caz_reporting.t_entrant_exemption ee
	ON ver.vehicle_entrant_reporting_id = ee.vehicle_entrant_reporting_id
	INNER JOIN caz_reporting.t_exemption_reason er
	ON ee.exemption_reason_id = er.exemption_reason_id
	INNER JOIN caz_reporting.t_ccaz_vehicle_type cvt
	ON ver.ccaz_vehicle_type_id = cvt.ccaz_vehicle_type_id
	GROUP BY day, ccaz_vehicle_type, exemption_reason
	ORDER BY day;
