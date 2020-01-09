--
-- Low Level HELPERS
--

--
-- Takes timestamp and truncates hours, minutes, seconds, milliseconds to 00:00:00.00.
--
-- Input: datetime - timestamp value to truncate to whole day.
-- Output: Timestamp truncated to whole day (with hours, minutes, seconds and milliseconds
--         fixed to 00).
--
CREATE OR REPLACE FUNCTION truncated_to_whole_day(datetime timestamp) RETURNS timestamp
AS $$
BEGIN
    RETURN date_trunc('day', datetime);
END;$$
LANGUAGE PLPGSQL;

--
-- Calculates interval between two timestamps truncated to whole days. See truncated_to_whole_day
-- for more details about truncation to the whole day.
--
-- Input: start_date starting timestamp; end_date ending timestamp.
-- Output: Interval between two timestamps truncated to whole day (so with hour, minute, second,
--         millisecond set to 00).
--
CREATE OR REPLACE FUNCTION interval_between(start_date timestamp, end_date timestamp) RETURNS interval
AS $$
BEGIN
    RETURN truncated_to_whole_day(start_date) - truncated_to_whole_day(end_date);
END;$$
LANGUAGE PLPGSQL;

--
-- Calculates number of whole days in specified time interval.
--
-- Input: interval_timestamp interval between two timestamps.
-- Output: integer with number of whole days between two timestamps.
--
CREATE OR REPLACE FUNCTION in_days(interval_timestamp interval) RETURNS integer
AS $$
BEGIN
    RETURN cast(extract(day from interval_timestamp) as integer);
END;$$
LANGUAGE PLPGSQL;


--
-- High Level HELPERS
--

--
-- Calculates number of whole days in specified time interval.
--
-- Output: New Table that in the first column has Array of Licensing Authority IDs that were updated
-- at some date in the past and in the second column has number of days that have passed since this
-- update until today.
--
CREATE OR REPLACE FUNCTION licensing_authorities_with_days_since_last_update()
    RETURNS TABLE(licence_authority_id integer[], days_since_last_update integer)
AS $$
BEGIN
    RETURN QUERY SELECT jobs_info.licence_authority_id,
                        in_days(interval_between(now()::timestamp, jobs_info.insert_timestmp))
                 FROM t_md_register_jobs_info AS jobs_info
                 WHERE jobs_info.licence_authority_id IS NOT NULL;
END;$$
LANGUAGE PLPGSQL;

--
-- Gives distinct Licensing Authority IDs of Licensing Authorities which have uploaded licences in at
-- least specified number of days.
--
-- Input: number_of_days - maximum number of days from today for which function should look when selecting
--        Licensing Authorities based on their date of last update
-- Output: New Table that has only one column with distinct Licensing Authority IDs which have uploaded
--         licences in at least number_of_days.
--
CREATE OR REPLACE FUNCTION get_ids_of_authorities_that_have_been_updated_in_last_days(number_of_days integer)
    RETURNS TABLE(license_authority_id integer)
AS $$
BEGIN
    ASSERT number_of_days >= 0, 'number_of_days must be zero or positive number';

    RETURN QUERY
        SELECT DISTINCT unnest(las_with_days.licence_authority_id)
        FROM licensing_authorities_with_days_since_last_update() as las_with_days
        WHERE las_with_days.days_since_last_update <= number_of_days;
END;$$
LANGUAGE PLPGSQL;


--
-- Main Reporting Function
--

--
-- Returns table of names of Licensing Authorities which have not uploaded licences in specified
-- number of days.
--
-- Input: number_of_days - maximum number of days the function should go back from today
--        to look for Licensing Authorities that have not uploaded data.
-- Output: Table of names of Licensing Authorities which have not uploaded licences in specified number of days.
--
CREATE OR REPLACE FUNCTION authorities_that_have_not_uploaded_licences_in_last_days(number_of_days integer)
    RETURNS TABLE(licensing_authority_name t_md_licensing_authority.licence_authority_name%TYPE)
AS $$
BEGIN
    ASSERT number_of_days >= 0, 'number_of_days must be zero or positive number';

    -- Select all IDs of LAs that are in t_md_licensing_authority
    -- but are not in IDs that *Uploaded* data within number_of_days.
    CREATE TEMP TABLE authorities_that_have_not_uploaded ON COMMIT DROP AS
    SELECT la.licence_authority_name
    FROM t_md_licensing_authority la
    WHERE la.licence_authority_id NOT IN
          -- Select IDs of LAs that *Uploaded* data within number_of_days
          (SELECT la_ids_that_were_updated_in_last_days.license_authority_id
           FROM get_ids_of_authorities_that_have_been_updated_in_last_days(number_of_days)
           AS la_ids_that_were_updated_in_last_days);

    -- Check if every Licensing Authority uploaded within number of days. If so
    -- put some helpful message, if not put info that there are some perpetrators.
    IF (SELECT EXISTS (SELECT * FROM authorities_that_have_not_uploaded)) THEN
        RAISE NOTICE 'Listing Licensing Authorities that have not uploaded within % days', number_of_days;
    ELSE
        RAISE NOTICE 'Every Licensing Authority uploaded within % days', number_of_days;
    END IF;

    RETURN QUERY SELECT * FROM authorities_that_have_not_uploaded;
END;$$
LANGUAGE PLPGSQL;
