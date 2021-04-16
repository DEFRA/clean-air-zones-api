/* JAQU-CAZ MVP Reporting - Requirement 2
Total number of vehicles by Vehicle Type Approval by Charge/No Charge assessment
Author: Informed */

CREATE OR REPLACE VIEW caz_reporting.report_2_total_per_caz_per_day AS
-- Total number of vehicles by Type Approval and Charge/No charge assessment by CCAZ per day
	SELECT DATE_TRUNC('day', hour) as Day, 
	-- determine clean air zone name
	CASE WHEN
	clean_air_zone_id = '39e54ed8-3ed2-441d-be3f-38fc9b70c8d3'
	THEN 'Leeds'
	WHEN clean_air_zone_id = '5cd7441d-766f-48ff-b8ad-1809586fea37'
	THEN 'Birmingham'
	ELSE 'Unrecognised'
	END AS clean_air_zone,
	type_approval,
	-- determine charge/no charge assessment based on charge validity code 
	CASE 
	WHEN charge_validity_code in ('CVC01', 'CVC04') THEN 'Charge'
	ELSE 'No charge'
	END AS charge,
	COUNT(*)
	FROM caz_reporting.t_vehicle_entrant_reporting ver 
	INNER JOIN
	caz_reporting.t_type_approval ta 
	ON ver.type_approval_id = ta.type_approval_id
	GROUP BY day, clean_air_zone_id, type_approval, charge
	ORDER BY day;

CREATE OR REPLACE VIEW caz_reporting.report_2_total_nationally_per_day AS
-- Total number of vehicles by Type Approval and Charge/No charge assessment Nationally per day
	SELECT DATE_TRUNC('day', hour) as Day, 
	type_approval,
	-- determine charge/no charge assessment based on charge validity code 
	CASE 
	WHEN charge_validity_code in ('CVC01', 'CVC04') THEN 'Charge'
	ELSE 'No charge'
	END AS charge,
	COUNT(*)
	FROM caz_reporting.t_vehicle_entrant_reporting ver 
	INNER JOIN
	caz_reporting.t_type_approval ta 
	ON ver.type_approval_id = ta.type_approval_id
	GROUP BY day, type_approval, charge
	ORDER BY day;