-- Create a new schema named "CAZ_VEHICLE_ENTRANT" and transfer existing VCCS core tables
CREATE SCHEMA IF NOT EXISTS CAZ_VEHICLE_ENTRANT;
REVOKE CREATE ON schema CAZ_VEHICLE_ENTRANT FROM public;

ALTER TABLE public.t_charge_validity
SET SCHEMA CAZ_VEHICLE_ENTRANT;

ALTER TABLE public.t_clean_air_zone_entrant
SET SCHEMA CAZ_VEHICLE_ENTRANT;

ALTER TABLE public.t_failed_identification_logs
SET SCHEMA CAZ_VEHICLE_ENTRANT;