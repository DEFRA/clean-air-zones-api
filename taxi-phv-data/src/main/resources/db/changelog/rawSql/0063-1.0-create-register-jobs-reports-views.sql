-- Create view for all vehicles held in DB.
drop view if exists public.number_of_all_vehicles;

CREATE OR REPLACE VIEW public.number_of_all_vehicles AS
SELECT COUNT(*) as records_count FROM public.t_md_taxi_phv;

-- Create view for total number of upload errors (success vs. failure) monthly summary
drop view if exists public.number_of_errors_monthly_summary;

CREATE OR REPLACE VIEW public.number_of_errors_monthly_summary AS
SELECT
    date_trunc('month', insert_timestmp)::date as month_start_date,
    COUNT(case when errors is not null then jobs.register_job_id end) as failed_uploads,
    COUNT(case when errors is null then jobs.register_job_id end) as successful_upload
FROM public.t_md_register_jobs jobs
group by 1
order by month_start_date asc;

-- Create view for total number of upload errors (success vs. failure) weekly summary
drop view if exists public.number_of_errors_weekly_summary;

CREATE OR REPLACE VIEW public.number_of_errors_weekly_summary AS
SELECT
    date_trunc('week', insert_timestmp)::date as week_start_date,
    COUNT(case when errors is not null then jobs.register_job_id end) as failed_uploads,
    COUNT(case when errors is null then jobs.register_job_id end) as successful_upload
FROM public.t_md_register_jobs jobs
group by 1
order by week_start_date asc;

-- Create view for total number of upload errors (success vs. failure) daily summary
drop view if exists public.number_of_errors_daily_summary;

CREATE OR REPLACE VIEW public.number_of_errors_daily_summary AS
SELECT
    date_trunc('day', insert_timestmp)::date as date,
    COUNT(case when errors is not null then jobs.register_job_id end) as failed_uploads,
    COUNT(case when errors is null then jobs.register_job_id end) as successful_upload
FROM public.t_md_register_jobs jobs
group by 1
order by date asc;