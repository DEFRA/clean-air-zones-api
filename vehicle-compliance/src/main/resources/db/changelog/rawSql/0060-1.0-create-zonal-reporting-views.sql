-- Update non-UK reporting view
CREATE OR REPLACE VIEW caz_reporting.number_of_non_uk_vehicles_by_month AS
	SELECT  hour, caz_name as clean_air_zone,
	COUNT(*) FROM CAZ_REPORTING.t_vehicle_entrant_reporting ver
	INNER JOIN caz_reporting.t_clean_air_zone caz on
	caz.clean_air_zone_id = ver.clean_air_zone_id
	WHERE non_uk_vehicle = true
	GROUP BY hour, caz_name;

--monthly all entrants by CAZ
CREATE OR REPLACE VIEW caz_reporting.monthly_entrants AS
	SELECT to_char(hour, 'MM/YYYY') AS month, caz_name, COUNT(*)
	FROM caz_reporting.t_vehicle_entrant_reporting ver 
	INNER JOIN caz_reporting.t_clean_air_zone caz 
	ON ver.clean_air_zone_id = caz.clean_air_zone_id
	GROUP BY month, caz_name;

-- monthly chargeable vehicles
CREATE OR REPLACE VIEW caz_reporting.monthly_chargeable_entrants AS
	SELECT to_char(hour, 'MM/YYYY') AS month, caz_name, count(*)
	FROM caz_reporting.t_vehicle_entrant_reporting ver 
	INNER JOIN caz_reporting.t_clean_air_zone caz 
	ON ver.clean_air_zone_id = caz.clean_air_zone_id
	WHERE charge_validity_code IN ('CVC01', 'CVC04')
	GROUP BY month, caz_name;

-- monthly non-chargeable vehicles
CREATE OR REPLACE VIEW caz_reporting.monthly_non_chargeable_entrants AS
	SELECT to_char(hour, 'MM/YYYY') AS month, caz_name, count(*)
	FROM caz_reporting.t_vehicle_entrant_reporting ver 
	INNER JOIN caz_reporting.t_clean_air_zone caz 
	ON ver.clean_air_zone_id = caz.clean_air_zone_id
	WHERE charge_validity_code IN ('CVC02', 'CVC03')
	GROUP BY month, caz_name;

-- monthly exempt vehicles
CREATE OR REPLACE VIEW caz_reporting.monthly_exempt_entrants AS
	SELECT to_char(hour, 'MM/YYYY') AS month, caz_name, count(*)
	FROM caz_reporting.t_vehicle_entrant_reporting ver 
	INNER JOIN caz_reporting.t_clean_air_zone caz 
	ON ver.clean_air_zone_id = caz.clean_air_zone_id
	WHERE charge_validity_code = 'CVC02'
	GROUP BY month, caz_name;
	
-- weekly all vehicles
CREATE OR REPLACE VIEW caz_reporting.weekly_entrants AS
	SELECT date_trunc('week', hour) AS week_commencing, caz_name, count(*)
	FROM caz_reporting.t_vehicle_entrant_reporting ver 
	INNER JOIN caz_reporting.t_clean_air_zone caz 
	ON ver.clean_air_zone_id = caz.clean_air_zone_id
	GROUP BY week_commencing, caz_name;

-- weekly chargeable vehicles
CREATE OR REPLACE VIEW caz_reporting.weekly_chargeable_entrants AS
	SELECT date_trunc('week', hour) as week_commencing, caz_name, count(*)
	FROM caz_reporting.t_vehicle_entrant_reporting ver 
	INNER JOIN caz_reporting.t_clean_air_zone caz 
	ON ver.clean_air_zone_id = caz.clean_air_zone_id
	WHERE charge_validity_code in ('CVC01', 'CVC04')
	GROUP BY week_commencing, caz_name;

-- weekly non-chargeable vehicles
CREATE OR REPLACE VIEW caz_reporting.weekly_non_chargeable_entrants AS
	SELECT date_trunc('week', hour) AS week_commencing, caz_name, count(*)
	FROM caz_reporting.t_vehicle_entrant_reporting ver 
	INNER JOIN caz_reporting.t_clean_air_zone caz 
	ON ver.clean_air_zone_id = caz.clean_air_zone_id
	WHERE charge_validity_code in ('CVC02', 'CVC03')
	GROUP BY week_commencing, caz_name;

-- weekly exempt vehicles
CREATE OR REPLACE VIEW caz_reporting.weekly_exempt_entrants AS
	SELECT date_trunc('week', hour) AS week_commencing, caz_name, count(*)
	FROM caz_reporting.t_vehicle_entrant_reporting ver 
	INNER JOIN caz_reporting.t_clean_air_zone caz 
	ON ver.clean_air_zone_id = caz.clean_air_zone_id
	WHERE charge_validity_code = 'CVC02'
	GROUP BY week_commencing, caz_name;
	
--  daily all vehicles
CREATE OR REPLACE VIEW caz_reporting.daily_entrants AS
	SELECT date_trunc('day', hour) AS day, caz_name, count(*)
	FROM caz_reporting.t_vehicle_entrant_reporting ver 
	INNER JOIN caz_reporting.t_clean_air_zone caz 
	ON ver.clean_air_zone_id = caz.clean_air_zone_id
	GROUP BY day, caz_name;

-- daily chargeable vehicles
CREATE OR REPLACE VIEW caz_reporting.daily_chargeable_entrants AS
	SELECT date_trunc('day', hour) AS day, caz_name, count(*)
	FROM caz_reporting.t_vehicle_entrant_reporting ver 
	INNER JOIN caz_reporting.t_clean_air_zone caz 
	ON ver.clean_air_zone_id = caz.clean_air_zone_id
	WHERE charge_validity_code IN ('CVC01', 'CVC04')
	GROUP BY day, caz_name;

-- daily non-chargeable vehicles
CREATE OR REPLACE VIEW caz_reporting.daily_non_chargeable_entrants AS
	SELECT date_trunc('day', hour) AS day, caz_name, count(*)
	FROM caz_reporting.t_vehicle_entrant_reporting ver 
	INNER JOIN caz_reporting.t_clean_air_zone caz 
	ON ver.clean_air_zone_id = caz.clean_air_zone_id
	WHERE charge_validity_code IN ('CVC02', 'CVC03')
	GROUP BY day, caz_name;

-- daily exempt vehicles
CREATE OR REPLACE VIEW caz_reporting.daily_exempt_entrants AS
	SELECT date_trunc('day', hour) AS day, caz_name, count(*)
	FROM caz_reporting.t_vehicle_entrant_reporting ver 
	INNER JOIN caz_reporting.t_clean_air_zone caz 
	ON ver.clean_air_zone_id = caz.clean_air_zone_id	
	WHERE charge_validity_code in ('CVC02')
	GROUP BY day, caz_name;


