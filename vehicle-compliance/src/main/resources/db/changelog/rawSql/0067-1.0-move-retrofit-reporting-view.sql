DROP VIEW IF EXISTS public.retrofit_view cascade;

CREATE OR REPLACE VIEW caz_reporting.total_retrofit_vehicles
 AS
 SELECT COUNT(retrofit_id) AS total_retrofit
   FROM public.t_vehicle_retrofit;