CREATE OR REPLACE VIEW public.retrofit_view
 AS
 SELECT COUNT(retrofit_id) AS total_retrofit
   FROM public.t_vehicle_retrofit;

GRANT INSERT, SELECT, UPDATE, DELETE ON TABLE public.retrofit_view TO vccs_readwrite_role;
GRANT SELECT ON TABLE public.retrofit_view TO vccs_readonly_role;
GRANT SELECT ON TABLE public.retrofit_view TO reporting_user;
GRANT SELECT ON TABLE public.retrofit_view TO reporting_readonly_role;
