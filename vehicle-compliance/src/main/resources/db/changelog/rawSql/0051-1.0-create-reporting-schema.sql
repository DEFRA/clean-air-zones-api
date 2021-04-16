CREATE SCHEMA IF NOT EXISTS CAZ_REPORTING;
REVOKE CREATE ON schema CAZ_REPORTING FROM public;

CREATE TABLE IF NOT EXISTS CAZ_REPORTING.t_type_approval
	(type_approval_id UUID NOT NULL,
	type_approval varchar(15) NOT NULL,
	CONSTRAINT t_type_approval_pkey PRIMARY KEY (type_approval_id));

CREATE TABLE IF NOT EXISTS CAZ_REPORTING.t_fuel_type
	(fuel_type_id UUID NOT NULL,
	fuel_type varchar(15) NOT NULL,
	CONSTRAINT t_fuel_type_pkey PRIMARY KEY (fuel_type_id));

CREATE TABLE IF NOT EXISTS CAZ_REPORTING.t_ccaz_vehicle_type
	(ccaz_vehicle_type_id UUID NOT NULL,
	ccaz_vehicle_type varchar(30) NOT NULL,
	CONSTRAINT t_ccaz_vehicle_type_pkey PRIMARY KEY (ccaz_vehicle_type_id));
	
CREATE TABLE IF NOT EXISTS CAZ_REPORTING.t_exemption_reason
	(exemption_reason_id UUID NOT NULL,
	exemption_reason varchar(50) NOT NULL,
	CONSTRAINT t_exemption_reason_pkey PRIMARY KEY (exemption_reason_id));

CREATE TABLE IF NOT EXISTS CAZ_REPORTING.t_vehicle_entrant_reporting 
	(vehicle_entrant_reporting_id uuid NOT NULL,
	vrn_hash varchar(64) NOT NULL,
	hour timestamp NOT NULL,
	clean_air_zone_id uuid NOT NULL,
	type_approval_id uuid,
	fuel_type_id uuid,
	charge_validity_code varchar(5),
	ccaz_vehicle_type_id uuid,
	make varchar(50),
	model varchar(50),
	colour varchar(50),
	CONSTRAINT t_vehicle_entrant_reporting_pkey PRIMARY KEY (vehicle_entrant_reporting_id),
	CONSTRAINT fk_type_approval_id FOREIGN KEY (type_approval_id)
        REFERENCES caz_reporting.t_type_approval (type_approval_id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION,
    CONSTRAINT fk_fuel_type_id FOREIGN KEY (fuel_type_id)
        REFERENCES caz_reporting.t_fuel_type (fuel_type_id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION,
    CONSTRAINT fk_charge_validity_code FOREIGN KEY (charge_validity_code)
        REFERENCES caz_vehicle_entrant.t_charge_validity (charge_validity_code) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION,
    CONSTRAINT fk_ccaz_vehicle_type_id FOREIGN KEY (ccaz_vehicle_type_id)
        REFERENCES caz_reporting.t_ccaz_vehicle_type (ccaz_vehicle_type_id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION);
 
CREATE TABLE IF NOT EXISTS CAZ_REPORTING.t_entrant_taxi_phv
	(entrant_taxi_phv_id uuid NOT NULL,
	vehicle_entrant_reporting_id uuid NOT NULL,
	description varchar(100) NOT NULL,
	licensing_authority varchar(50) NOT NULL,
	CONSTRAINT t_entrant_taxi_phv_pkey PRIMARY KEY (entrant_taxi_phv_id),
	CONSTRAINT fk_vehicle_entrant_reporting_id FOREIGN KEY (vehicle_entrant_reporting_id)
        REFERENCES caz_reporting.t_vehicle_entrant_reporting (vehicle_entrant_reporting_id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION);
 
CREATE TABLE IF NOT EXISTS CAZ_REPORTING.t_entrant_exemption
	(entrant_exemption_id uuid NOT NULL,
	vehicle_entrant_reporting_id uuid NOT NULL,
	exemption_reason_id uuid NOT NULL,
	CONSTRAINT t_entrant_exemption_pkey PRIMARY KEY (entrant_exemption_id),
	CONSTRAINT fk_vehicle_entrant_reporting_id FOREIGN KEY (vehicle_entrant_reporting_id)
        REFERENCES caz_reporting.t_vehicle_entrant_reporting (vehicle_entrant_reporting_id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION,
    CONSTRAINT fk_exemption_reason_id FOREIGN KEY (exemption_reason_id)
        REFERENCES caz_reporting.t_exemption_reason (exemption_reason_id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION);  
	