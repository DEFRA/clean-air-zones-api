INSERT INTO caz_whitelist_vehicles.t_whitelist_job_register(REGISTER_JOB_ID,
                               TRIGGER,
                               JOB_NAME,
                               UPLOADER_ID,
                               STATUS,
                               CORRELATION_ID)
VALUES (123,
        'WHITELIST_CSV_FROM_S3',
        '20190809_154821_WHITELIST_CSV_FROM_S3_FILENAME',
        '11111111-2222-3333-4444-555555555555',
        'RUNNING',
        'CorrelationId');

INSERT INTO caz_whitelist_vehicles.t_whitelist_job_register(REGISTER_JOB_ID,
                               TRIGGER,
                               JOB_NAME,
                               UPLOADER_ID,
                               STATUS,
                               CORRELATION_ID)
VALUES (456,
        'GREEN_MOD_CSV_FROM_S3',
        '20190809_154821_GREEN_MOD_CSV_FROM_S3_CAZ-2018-12-12',
        '11111111-2222-3333-4444-555555555555',
        'RUNNING',
        'CorrelationId');

INSERT INTO caz_whitelist_vehicles.t_whitelist_job_register(REGISTER_JOB_ID,
                               TRIGGER,
                               JOB_NAME,
                               UPLOADER_ID,
                               STATUS,
                               CORRELATION_ID)
VALUES (457,
        'WHITELIST_CSV_FROM_S3',
        '20190809_154821_WHITELIST_CSV_FROM_S3_CAZ-2018-12-13',
        '0d7ab5c4-5fff-4935-8c4e-56267c0c9493',
        'RUNNING',
        'CorrelationId');

INSERT INTO caz_whitelist_vehicles.t_whitelist_job_register(REGISTER_JOB_ID,
                               TRIGGER,
                               JOB_NAME,
                               UPLOADER_ID,
                               STATUS,
                               CORRELATION_ID)
VALUES (458,
        'WHITE_MOD_CSV_FROM_S3',
        '20190809_154821_RETROFIT_CSV_FROM_S3_CAZ-2018-12-14',
        '0d7ab5c4-5fff-4935-8c4e-56267c0c9493',
        'STARTING',
        'CorrelationId');