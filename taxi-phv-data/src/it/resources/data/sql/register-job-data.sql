INSERT INTO T_MD_REGISTER_JOBS(REGISTER_JOB_ID,
                               TRIGGER,
                               JOB_NAME,
                               UPLOADER_ID,
                               STATUS,
                               CORRELATION_ID)
VALUES (123,
        'CSV_FROM_S3',
        '20190809_154821_CSV_FROM_S3_FILENAME',
        '11111111-2222-3333-4444-555555555555',
        'RUNNING',
        'CorrelationId');

INSERT INTO T_MD_REGISTER_JOBS(REGISTER_JOB_ID,
                               TRIGGER,
                               JOB_NAME,
                               UPLOADER_ID,
                               STATUS,
                               CORRELATION_ID)
VALUES (456,
        'API_CALL',
        '20190809_154821_API_CALL_CAZ-2018-12-12',
        '11111111-2222-3333-4444-555555555555',
        'RUNNING',
        'CorrelationId');

INSERT INTO T_MD_REGISTER_JOBS(REGISTER_JOB_ID,
                       TRIGGER,
                       JOB_NAME,
                       UPLOADER_ID,
                       STATUS,
                       CORRELATION_ID)
VALUES (457,
        'API_CALL',
        '20190809_154821_API_CALL_CAZ-2018-12-13',
        '0d7ab5c4-5fff-4935-8c4e-56267c0c9493',
        'RUNNING',
        'CorrelationId');

INSERT INTO T_MD_REGISTER_JOBS(REGISTER_JOB_ID,
                       TRIGGER,
                       JOB_NAME,
                       UPLOADER_ID,
                       STATUS,
                       CORRELATION_ID)
VALUES (458,
        'API_CALL',
        '20190809_154821_API_CALL_CAZ-2018-12-14',
        '0d7ab5c4-5fff-4935-8c4e-56267c0c9493',
        'STARTING',
        'CorrelationId');

INSERT INTO T_MD_REGISTER_JOBS(REGISTER_JOB_ID,
                       TRIGGER,
                       JOB_NAME,
                       UPLOADER_ID,
                       STATUS,
                       CORRELATION_ID)
VALUES (460,
        'API_CALL',
        '20190809_154821_API_CALL_CAZ-2014-12-14',
        '0d7ab5c4-5fff-4935-8c4e-56267c0c9491',
        'STARTING',
        'CorrelationId');

INSERT INTO T_MD_REGISTER_JOBS(REGISTER_JOB_ID,
                       TRIGGER,
                       JOB_NAME,
                       UPLOADER_ID,
                       STATUS,
                       CORRELATION_ID)
VALUES (461,
        'API_CALL',
        '20190809_154821_API_CALL_CAZ-2015-12-14',
        '0d7ab5c4-5fff-4935-8c4e-56267c0c9492',
        'FINISHED_SUCCESS',
        'CorrelationId');

INSERT INTO T_MD_REGISTER_JOBS(REGISTER_JOB_ID,
                       TRIGGER,
                       JOB_NAME,
                       UPLOADER_ID,
                       STATUS,
                       CORRELATION_ID)
VALUES (462,
        'API_CALL',
        '20190809_154821_API_CALL_CAZ-2013-12-14',
        '0d7ab5c4-5fff-4935-8c4e-56267c0c9494',
        'STARTING',
        'CorrelationId');

INSERT INTO T_MD_REGISTER_JOBS(REGISTER_JOB_ID,
                       TRIGGER,
                       JOB_NAME,
                       UPLOADER_ID,
                       STATUS,
                       CORRELATION_ID)
VALUES (463,
        'API_CALL',
        '20190809_154812_API_CALL_CAZ-2014-12-14',
        '0d7ab5c4-5fff-4935-8c4e-56267c0c9471',
        'STARTING',
        'CorrelationId');

INSERT INTO T_MD_REGISTER_JOBS(REGISTER_JOB_ID,
                       TRIGGER,
                       JOB_NAME,
                       UPLOADER_ID,
                       STATUS,
                       CORRELATION_ID)
VALUES (464,
        'API_CALL',
        '20190809_154822_API_CALL_CAZ-2015-12-14',
        '0d7ab5c4-5fff-4935-8c4e-56267c0c9472',
        'STARTING',
        'CorrelationId');

INSERT INTO T_MD_REGISTER_JOBS(REGISTER_JOB_ID,
                       TRIGGER,
                       JOB_NAME,
                       UPLOADER_ID,
                       STATUS,
                       CORRELATION_ID)
VALUES (465,
        'API_CALL',
        '20190809_154823_API_CALL_CAZ-2013-12-14',
        '0d7ab5c4-5fff-4935-8c4e-56267c0c9474',
        'STARTING',
        'CorrelationId');

INSERT INTO T_MD_REGISTER_JOBS(REGISTER_JOB_ID,
                       TRIGGER,
                       JOB_NAME,
                       UPLOADER_ID,
                       STATUS,
                       CORRELATION_ID)
VALUES (466,
        'API_CALL',
        '20190809_154824_API_CALL_CAZ-2015-12-14',
        '0d7ab5c4-5fff-4935-8c4e-56267c0c9481',
        'STARTING',
        'CorrelationId');

INSERT INTO T_MD_REGISTER_JOBS(REGISTER_JOB_ID,
                       TRIGGER,
                       JOB_NAME,
                       UPLOADER_ID,
                       STATUS,
                       CORRELATION_ID)
VALUES (467,
        'API_CALL',
        '20190809_154825_API_CALL_CAZ-2013-12-14',
        '0d7ab5c4-5fff-4935-8c4e-56267c0c9484',
        'STARTING',
        'CorrelationId');

INSERT INTO T_MD_REGISTER_JOBS(REGISTER_JOB_ID,
                               TRIGGER,
                               JOB_NAME,
                               UPLOADER_ID,
                               STATUS,
                               CORRELATION_ID,
  			       IMPACTED_LOCAL_AUTHORITY)
VALUES (468,
        'CSV_FROM_S3',
        'CAZ-2020-01-08-Leeds-10000',
        '11111111-2222-3333-4444-555555555555',
        'RUNNING',
        'CorrelationId',
	(Select ARRAY(select licence_authority_id from t_md_licensing_authority where licence_authority_name = 'Leeds')));

INSERT INTO T_MD_REGISTER_JOBS(REGISTER_JOB_ID,
                               TRIGGER,
                               JOB_NAME,
                               UPLOADER_ID,
                               STATUS,
                               CORRELATION_ID,
  			       IMPACTED_LOCAL_AUTHORITY)
VALUES (469,
        'CSV_FROM_S3',
        'CAZ-2020-01-08-Birmingham-10000',
        '11111111-2222-3333-4444-555555555555',
        'RUNNING',
        'CorrelationId',
	(Select ARRAY(select licence_authority_id from t_md_licensing_authority where licence_authority_name = 'Birmingham')));
