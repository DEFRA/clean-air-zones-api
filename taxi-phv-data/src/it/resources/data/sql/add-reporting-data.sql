-- Create 4 Licensing Authorities:
-- Birmingham (from main Liquibase scripts)
-- Leeds (from main Liquibase scripts)
-- Liverpool
-- London
INSERT INTO T_MD_LICENSING_AUTHORITY(LICENCE_AUTHORITY_ID, LICENCE_AUTHORITY_NAME, AUTHORISED_UPLOADER_IDS)
VALUES (3, 'Liverpool', '{"6314d1d6-706a-40ce-b392-a0e618ab45b8"}');

INSERT INTO T_MD_LICENSING_AUTHORITY(LICENCE_AUTHORITY_ID, LICENCE_AUTHORITY_NAME, AUTHORISED_UPLOADER_IDS)
VALUES (4, 'London', '{"6314d1d6-706a-40ce-b392-a0e618ab45b8"}');

-- Create some Register Job required as Foreign Key for Register Jobs Info entries
INSERT INTO T_MD_REGISTER_JOBS(REGISTER_JOB_ID,
                               TRIGGER,
                               JOB_NAME,
                               UPLOADER_ID,
                               STATUS,
                               CORRELATION_ID)
VALUES (1,
        'CSV_FROM_S3',
        '20190810_154821_CSV_FROM_S3_FILENAME',
        '11111111-2222-3333-4444-555555555555',
        'RUNNING',
        '66666666-7777-8888-9999-222222222222');
