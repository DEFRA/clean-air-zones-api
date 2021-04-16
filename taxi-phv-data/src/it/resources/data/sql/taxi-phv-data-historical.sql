UPDATE t_md_taxi_phv
SET description = 'new description'
WHERE vrm = 'EB12QMD';

DELETE
FROM t_md_taxi_phv
WHERE vrm = 'EB12QMD';

UPDATE audit.logged_actions
SET action_tstamp = TO_TIMESTAMP('2017-03-01 9:30:20', 'YYYY-MM-DD HH:MI:SS')
WHERE action = 'I'
  AND new_data ->> 'vrm' = 'EB12QMD';

UPDATE audit.logged_actions
SET action_tstamp = TO_TIMESTAMP('2017-03-15 9:30:20', 'YYYY-MM-DD HH:MI:SS')
WHERE action = 'U'
  AND new_data ->> 'vrm' = 'EB12QMD';

UPDATE audit.logged_actions
set action_tstamp = TO_TIMESTAMP('2017-03-31 9:30:20', 'YYYY-MM-DD HH:MI:SS')
WHERE action = 'D'
  AND original_data ->> 'vrm' = 'EB12QMD';

-- Test data for timezone verification
INSERT INTO audit.logged_actions (schema_name, table_name, user_name, action_tstamp, action, original_data, new_data, query, modifier_id)
VALUES ('public', 't_md_taxi_phv', 'postgres', '2020-07-15 23:36:25.175645Z', 'U', null, '{"vrm": "BST123", "description": "TAXI", "uploader_id": "9023312c-a9e7-11e9-a2a3-2a2ae2dbcce4", "insert_timestmp": "2020-11-19T17:02:39.794648", "licence_end_date": "2020-11-26", "licence_start_date": "2019-05-22", "licence_authority_id": 4, "licence_plate_number": "plate-no-91", "taxi_phv_register_id": 18, "wheelchair_access_flag": null}',
'UPDATE t_md_taxi_phv SET description = $1 WHERE vrm = $2', '6314d1d6-706a-40ce-b392-a0e618ab45b8');

INSERT INTO audit.logged_actions (schema_name, table_name, user_name, action_tstamp, action, original_data, new_data, query, modifier_id)
VALUES ('public', 't_md_taxi_phv', 'postgres', '2020-12-15 23:36:25.175645Z', 'U', null, '{"vrm": "WNTR123", "description": "TAXI", "uploader_id": "9023312c-a9e7-11e9-a2a3-2a2ae2dbcce4", "insert_timestmp": "2020-11-19T17:02:39.794648", "licence_end_date": "2020-11-26", "licence_start_date": "2019-05-22", "licence_authority_id": 4, "licence_plate_number": "plate-no-91", "taxi_phv_register_id": 18, "wheelchair_access_flag": null}',
'UPDATE t_md_taxi_phv SET description = $1 WHERE vrm = $2', '6314d1d6-706a-40ce-b392-a0e618ab45b8');