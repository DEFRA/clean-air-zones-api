--Create view for number of vehicle entrants by vehicle type
CREATE OR REPLACE VIEW CAZ_REPORTING.NO_ENTRANTS_BY_VEHICLE_TYPE AS
SELECT hour, caz_name, ccaz_vehicle_type, count(*) from caz_reporting.t_vehicle_entrant_reporting ver
inner join caz_reporting.t_ccaz_vehicle_type cvt
on ver.ccaz_vehicle_type_id = cvt.ccaz_vehicle_type_id
inner join caz_reporting.t_clean_air_zone caz
on ver.clean_air_zone_id = caz.clean_air_zone_id
group by hour, caz_name, ccaz_vehicle_type;

-- Create view for number of vehicles entrants by vehicle type and charge/no charge
CREATE OR REPLACE VIEW CAZ_REPORTING.NO_ENTRANTS_BY_CHARGE_AND_VEHICLE_TYPE AS
SELECT hour, caz_name, ccaz_vehicle_type, 
CASE 
	WHEN charge_validity_code in
	('CVC01', 'CVC04') THEN 'Charge'
	ELSE 'No charge'
	END AS charge,
	count(*) from caz_reporting.t_vehicle_entrant_reporting ver
inner join caz_reporting.t_ccaz_vehicle_type cvt
on ver.ccaz_vehicle_type_id = cvt.ccaz_vehicle_type_id
inner join caz_reporting.t_clean_air_zone caz
on ver.clean_air_zone_id = caz.clean_air_zone_id
group by hour, caz_name, ccaz_vehicle_type, charge;
