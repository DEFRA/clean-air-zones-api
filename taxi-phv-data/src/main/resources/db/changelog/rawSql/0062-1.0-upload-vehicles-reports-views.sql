-- Create view for all vehicles held in DB.
CREATE OR REPLACE VIEW public.number_of_all_vehicles AS
SELECT COUNT(*) as count FROM public.t_md_taxi_phv;

-- Create view for total number of upload errors (success vs. failure) monthly summary
CREATE OR REPLACE VIEW public.number_of_errors_monthly_summary AS
SELECT COUNT(*) as count, 'YES' as upload_error FROM public.t_md_register_jobs
where errors is not null and insert_timestmp > (now() - interval '30 days')
union
SELECT COUNT(*) as count, 'NO' as upload_error FROM public.t_md_register_jobs
where errors is NULL and insert_timestmp > (now() - interval '30 days');

-- Create view for total number of upload errors (success vs. failure) weekly summary
CREATE OR REPLACE VIEW public.number_of_errors_weekly_summary AS
SELECT COUNT(*) as count, 'YES' as upload_error FROM public.t_md_register_jobs
where errors is not null and insert_timestmp > (now() - interval '7 days')
union
SELECT COUNT(*) as count, 'NO' as upload_error FROM public.t_md_register_jobs
where errors is NULL and insert_timestmp > (now() - interval '7 days');

-- Create view for total number of upload errors (success vs. failure) daily summary
CREATE OR REPLACE VIEW public.number_of_errors_daily_summary AS
SELECT COUNT(*) as count, 'YES' as upload_error FROM public.t_md_register_jobs
where errors is not null and insert_timestmp > (now() - interval '1 days')
union
SELECT COUNT(*) as count, 'NO' as upload_error FROM public.t_md_register_jobs
where errors is NULL and insert_timestmp > (now() - interval '1 days');