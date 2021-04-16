-- function which performs a date cast - as we store the dates in JSONB we cannot perform a proper
-- casting without a helper method, more info here: https://stackoverflow.com/a/16405935
CREATE OR REPLACE FUNCTION custom_date_cast(text)
RETURNS DATE
AS
$$
SELECT ($1)::date
$$
LANGUAGE SQL
IMMUTABLE;

-- Partial index for audit.logged_actions for INSERT data (action = 'I')
CREATE INDEX IF NOT EXISTS licence_authority_licence_dates_insert_data_idx ON audit.logged_actions
(((new_data ->> 'licence_authority_id' )::integer),
(custom_date_cast(new_data ->> 'licence_start_date')),
(custom_date_cast(new_data ->> 'licence_end_date')))
WHERE ((new_data ->> 'licence_authority_id')::integer) IS NOT NULL
AND table_name = 't_md_taxi_phv'
AND action = 'I';

-- Partial index for audit.logged_actions for DELETE data (action = 'D')
CREATE INDEX IF NOT EXISTS licence_authority_licence_dates_delete_data_idx ON audit.logged_actions
(((original_data ->> 'licence_authority_id' )::integer),
(custom_date_cast(original_data ->> 'licence_start_date')),
(custom_date_cast(original_data ->> 'licence_end_date')))
WHERE ((original_data ->> 'licence_authority_id')::integer) IS NOT NULL
AND table_name = 't_md_taxi_phv'
AND action = 'D';
