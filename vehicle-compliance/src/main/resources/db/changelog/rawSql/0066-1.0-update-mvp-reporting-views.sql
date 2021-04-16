

-- update mvp reports to use the t_clean_air_zone table
-- report 1
DROP VIEW IF EXISTS caz_reporting.report_1_total_per_caz_per_day CASCADE;
CREATE OR REPLACE VIEW caz_reporting.report_1_total_per_caz_per_day AS
 SELECT date_trunc('day'::text, ver.hour) AS day,
        caz_name AS clean_air_zone,
    ta.type_approval,
    ft.fuel_type,
    count(DISTINCT ver.vrn_hash) AS count
   FROM caz_reporting.t_vehicle_entrant_reporting ver
     JOIN caz_reporting.t_type_approval ta ON ver.type_approval_id = ta.type_approval_id
     JOIN caz_reporting.t_fuel_type ft ON ver.fuel_type_id = ft.fuel_type_id
	 JOIN caz_reporting.t_clean_air_zone caz on ver.clean_air_zone_id = caz.clean_air_zone_id
  GROUP BY (date_trunc('day'::text, ver.hour)), clean_air_zone, ta.type_approval, ft.fuel_type
  ORDER BY (date_trunc('day'::text, ver.hour));

DROP VIEW IF EXISTS caz_reporting.report_1_total_per_caz_per_hour CASCADE;
CREATE OR REPLACE VIEW caz_reporting.Report_1_total_per_caz_per_hour AS
	SELECT ver.hour,
        caz_name AS clean_air_zone,
    ta.type_approval,
    ft.fuel_type,
    count(*) AS count
   FROM caz_reporting.t_vehicle_entrant_reporting ver
     JOIN caz_reporting.t_type_approval ta ON ver.type_approval_id = ta.type_approval_id
     JOIN caz_reporting.t_fuel_type ft ON ver.fuel_type_id = ft.fuel_type_id
	 JOIN caz_reporting.t_clean_air_zone caz on ver.clean_air_zone_id = caz.clean_air_zone_id
  GROUP BY ver.hour, caz_name, ta.type_approval, ft.fuel_type
  ORDER BY ver.hour;

DROP VIEW IF EXISTS caz_reporting.report_1_total_unique_per_caz_per_day CASCADE;
CREATE OR REPLACE VIEW caz_reporting.report_1_total_unique_per_caz_per_day AS
 SELECT date_trunc('day'::text, ver.hour) AS day,
 	caz_name as clean_air_zone,
    ta.type_approval,
    ft.fuel_type,
    count(DISTINCT ver.vrn_hash) AS count
   FROM caz_reporting.t_vehicle_entrant_reporting ver
     JOIN caz_reporting.t_type_approval ta ON ver.type_approval_id = ta.type_approval_id
     JOIN caz_reporting.t_fuel_type ft ON ver.fuel_type_id = ft.fuel_type_id
	 JOIN caz_reporting.t_clean_air_zone caz on ver.clean_air_zone_id = caz.clean_air_zone_id
  GROUP BY (date_trunc('day'::text, ver.hour)), caz_name, ta.type_approval, ft.fuel_type
  ORDER BY (date_trunc('day'::text, ver.hour));

DROP VIEW IF EXISTS caz_reporting.report_1_total_unique_per_caz_per_hour CASCADE;
CREATE OR REPLACE VIEW caz_reporting.report_1_total_unique_per_caz_per_hour AS
	 SELECT ver.hour,
        caz_name AS clean_air_zone,
    ta.type_approval,
    ft.fuel_type,
    count(DISTINCT ver.vrn_hash) AS count
   FROM caz_reporting.t_vehicle_entrant_reporting ver
     JOIN caz_reporting.t_type_approval ta ON ver.type_approval_id = ta.type_approval_id
     JOIN caz_reporting.t_fuel_type ft ON ver.fuel_type_id = ft.fuel_type_id
	 JOIN caz_reporting.t_clean_air_zone caz ON ver.clean_air_zone_id = caz.clean_air_zone_id
  GROUP BY ver.hour, caz_name, ta.type_approval, ft.fuel_type
  ORDER BY ver.hour;
 
DROP VIEW IF EXISTS caz_reporting.report_2_total_per_caz_per_day CASCADE;
CREATE OR REPLACE VIEW caz_reporting.report_2_total_per_caz_per_day AS
 SELECT date_trunc('day'::text, ver.hour) AS day,
        caz_name AS clean_air_zone,
    ta.type_approval,
        CASE
            WHEN ver.charge_validity_code::text = ANY (ARRAY['CVC01'::character varying, 'CVC04'::character varying]::text[]) THEN 'Charge'::text
            ELSE 'No charge'::text
        END AS charge,
    count(*) AS count
   FROM caz_reporting.t_vehicle_entrant_reporting ver
     JOIN caz_reporting.t_type_approval ta ON ver.type_approval_id = ta.type_approval_id
	 JOIN caz_reporting.t_clean_air_zone caz on ver.clean_air_zone_id = caz.clean_air_zone_id
  GROUP BY (date_trunc('day'::text, ver.hour)), caz_name, ta.type_approval, (
        CASE
            WHEN ver.charge_validity_code::text = ANY (ARRAY['CVC01'::character varying, 'CVC04'::character varying]::text[]) THEN 'Charge'::text
            ELSE 'No charge'::text
        END)
  ORDER BY (date_trunc('day'::text, ver.hour));

DROP VIEW IF EXISTS caz_reporting.report_3_total_exemptions_per_caz_per_day CASCADE;
CREATE OR REPLACE VIEW caz_reporting.report_3_total_exemptions_per_caz_per_day AS
	SELECT date_trunc('day'::text, ver.hour) AS day,
        caz_name AS clean_air_zone,
    cvt.ccaz_vehicle_type,
    er.exemption_reason,
    count(*) AS count
   FROM caz_reporting.t_vehicle_entrant_reporting ver
     JOIN caz_reporting.t_entrant_exemption ee ON ver.vehicle_entrant_reporting_id = ee.vehicle_entrant_reporting_id
     JOIN caz_reporting.t_exemption_reason er ON ee.exemption_reason_id = er.exemption_reason_id
     JOIN caz_reporting.t_ccaz_vehicle_type cvt ON ver.ccaz_vehicle_type_id = cvt.ccaz_vehicle_type_id
	 JOIN caz_reporting.t_clean_air_zone caz on ver.clean_air_zone_id = caz.clean_air_zone_id
  GROUP BY (date_trunc('day'::text, ver.hour)), caz_name, cvt.ccaz_vehicle_type, er.exemption_reason
  ORDER BY (date_trunc('day'::text, ver.hour));

DROP VIEW IF EXISTS caz_reporting.report_4_total_taxis_phvs_per_caz_per_day CASCADE;
CREATE OR REPLACE VIEW caz_reporting.report_4_total_taxis_phvs_per_caz_per_day AS
 SELECT date_trunc('day'::text, ver.hour) AS day,
        caz_name AS clean_air_zone,
    lower(taxi.description::text) AS description_lower,
    taxi.licensing_authority,
    count(*) AS count
   FROM caz_reporting.t_vehicle_entrant_reporting ver
     JOIN caz_reporting.t_entrant_taxi_phv taxi ON ver.vehicle_entrant_reporting_id = taxi.vehicle_entrant_reporting_id
	 JOIN caz_reporting.t_clean_air_zone caz ON ver.clean_air_zone_id = caz.clean_air_zone_id
  GROUP BY (date_trunc('day'::text, ver.hour)), caz_name, (lower(taxi.description::text)), taxi.licensing_authority
  ORDER BY (date_trunc('day'::text, ver.hour));

DROP VIEW IF EXISTS caz_reporting.report_5_total_unidentified_per_caz_per_day CASCADE;
CREATE OR REPLACE VIEW caz_reporting.report_5_total_unidentified_per_caz_per_day AS
	SELECT date_trunc('day'::text, ver.hour) AS day,
        caz_name as clean_air_zone,
    count(*) AS count
   FROM caz_reporting.t_vehicle_entrant_reporting ver
   JOIN caz_reporting.t_clean_air_zone caz ON ver.clean_air_zone_id = caz.clean_air_zone_id
  WHERE ver.charge_validity_code::text = 'CVC04'::text
  GROUP BY day, caz_name
  ORDER BY day;
  
  
