-- Create a new schema named "CAZ_TEST_HARNESS" and transfer vehicle table
CREATE SCHEMA IF NOT EXISTS CAZ_TEST_HARNESS;
REVOKE CREATE ON schema CAZ_TEST_HARNESS FROM public;

ALTER TABLE public.vehicle
RENAME TO t_vehicle;

ALTER TABLE public.t_vehicle
SET SCHEMA CAZ_TEST_HARNESS;