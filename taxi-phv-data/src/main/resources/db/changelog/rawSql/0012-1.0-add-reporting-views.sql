-- Author: Informed
-- View for daily and weekly taxi reports

CREATE OR REPLACE VIEW public.daily_taxi_report AS
	SELECT la.licence_authority_name,  COUNT(DISTINCT(taxi.vrm, taxi.description, taxi.licence_plate_number, taxi.licence_start_date, taxi.licence_end_date)) n_taxis_uploaded, MAX(rj.insert_timestmp) last_upload
	FROM public.t_md_licensing_authority la
  	JOIN public.t_md_register_jobs rj
  	ON la.licence_authority_id = ANY (rj.impacted_local_authority)
    JOIN public.t_md_taxi_phv taxi
    ON la.licence_authority_id = taxi.licence_authority_id
  	WHERE rj.status = 'FINISHED_SUCCESS'
  	GROUP BY la.licence_authority_name
	ORDER BY la.licence_authority_name ASC;
	
CREATE OR REPLACE VIEW public.weekly_taxi_report AS
	SELECT la.licence_authority_name, MAX(rj.insert_timestmp) last_upload, (EXTRACT(days FROM (now() - MAX(rj.insert_timestmp))) / 7)::int weeks_since_last_update
	FROM public.t_md_licensing_authority la
  	JOIN public.t_md_register_jobs rj
  	ON la.licence_authority_id = ANY (rj.impacted_local_authority)
  	WHERE rj.status = 'FINISHED_SUCCESS' AND la.licence_authority_name in (select * from public.authorities_that_have_not_uploaded_licences_in_last_days(7))
  	GROUP BY la.licence_authority_name
	ORDER BY la.licence_authority_name ASC;
