CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

ALTER TABLE t_clean_air_zone_entrant
    DROP CONSTRAINT entrant_id_pkey;
ALTER TABLE t_clean_air_zone_entrant
    ADD COLUMN tmp_column uuid;

update t_clean_air_zone_entrant
set tmp_column=public.uuid_generate_v4();

ALTER TABLE t_clean_air_zone_entrant
    DROP COLUMN entrant_id;
ALTER TABLE t_clean_air_zone_entrant
    RENAME COLUMN tmp_column to entrant_id;

ALTER TABLE t_clean_air_zone_entrant
    ADD PRIMARY KEY (entrant_id);

