-- create zonal reports on unrecognised vehicles
-- number of unrecognised vehicles who have paid
CREATE OR REPLACE VIEW caz_reporting.zonal_unrecognised_vehicle_paid AS
	SELECT DATE_TRUNC('hour', entrant_timestmp) AS hour, 
	caz_name, 
	COUNT(*) AS n_entrants 
	FROM
	caz_payment.t_clean_air_zone_entrant_payment caz_entrant_payment
	INNER JOIN caz_vehicle_entrant.t_clean_air_zone_entrant caz_entrant
	ON caz_entrant_payment.vrn = caz_entrant.vrn 
	AND caz_entrant_payment.travel_date = DATE_TRUNC('day', caz_entrant.entrant_timestmp)
	AND caz_entrant_payment.clean_air_zone_id = caz_entrant.clean_air_zone_id
	INNER JOIN caz_reporting.t_clean_air_zone caz 
	ON caz.clean_air_zone_id = caz_entrant_payment.clean_air_zone_id
	WHERE charge_validity_code = 'CVC04' AND payment_status = 'PAID'
	GROUP BY hour, caz_name;


-- number of unrecognised vehicles who have not paid
CREATE OR REPLACE VIEW caz_reporting.zonal_unrecognised_vehicle_not_paid AS
	SELECT DATE_TRUNC('hour', entrant_timestmp) AS hour, 
	caz_name, 
	COUNT(*) AS n_entrants FROM
	caz_payment.t_clean_air_zone_entrant_payment caz_entrant_payment
	INNER JOIN caz_vehicle_entrant.t_clean_air_zone_entrant caz_entrant
	ON caz_entrant_payment.vrn = caz_entrant.vrn 
	AND caz_entrant_payment.travel_date = DATE_TRUNC('day', caz_entrant.entrant_timestmp)
	AND caz_entrant_payment.clean_air_zone_id = caz_entrant.clean_air_zone_id
	INNER JOIN caz_reporting.t_clean_air_zone caz on caz.clean_air_zone_id = caz_entrant_payment.clean_air_zone_id
	WHERE charge_validity_code = 'CVC04' 
	AND payment_status = 'NOT_PAID'
	GROUP BY hour, caz_name;
