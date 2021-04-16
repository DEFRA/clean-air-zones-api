UPDATE caz_whitelist_vehicles.t_whitelist_vehicles
SET manufacturer = 'new manu'
WHERE vrn = 'EB12QMD';

DELETE
FROM caz_whitelist_vehicles.t_whitelist_vehicles
WHERE vrn = 'EB12QMD';

UPDATE caz_whitelist_vehicles_audit.logged_actions
SET action_tstamp = TO_TIMESTAMP('2017-03-01 9:30:20', 'YYYY-MM-DD HH:MI:SS')
WHERE action = 'I'
  AND new_data ->> 'vrn' = 'EB12QMD';

UPDATE caz_whitelist_vehicles_audit.logged_actions
SET action_tstamp = TO_TIMESTAMP('2017-03-15 9:30:20', 'YYYY-MM-DD HH:MI:SS')
WHERE action = 'U'
  AND new_data ->> 'vrn' = 'EB12QMD';

UPDATE caz_whitelist_vehicles_audit.logged_actions
set action_tstamp = TO_TIMESTAMP('2017-03-31 9:30:20', 'YYYY-MM-DD HH:MI:SS')
WHERE action = 'D'
  AND original_data ->> 'vrn' = 'EB12QMD';


--  For UK summer time updates --
UPDATE caz_whitelist_vehicles.t_whitelist_vehicles
SET manufacturer = 'summer manufacturer'
WHERE vrn = 'SUM123';

DELETE
FROM caz_whitelist_vehicles.t_whitelist_vehicles
WHERE vrn = 'SUM123';

UPDATE caz_whitelist_vehicles_audit.logged_actions
SET action_tstamp = '2020-09-14 23:36:25.175645Z'
WHERE action = 'I'
  AND new_data ->> 'vrn' = 'SUM123';

UPDATE caz_whitelist_vehicles_audit.logged_actions
SET action_tstamp = '2020-09-15 10:36:25.175645Z'
WHERE action = 'U'
  AND new_data ->> 'vrn' = 'SUM123';

UPDATE caz_whitelist_vehicles_audit.logged_actions
set action_tstamp = '2020-07-15 23:36:25.175645Z'
WHERE action = 'D'
  AND original_data ->> 'vrn' = 'SUM123';

--  For UK winter time updates --
UPDATE caz_whitelist_vehicles.t_whitelist_vehicles
SET manufacturer = 'winter manufacturer'
WHERE vrn = 'WIN123';

DELETE
FROM caz_whitelist_vehicles.t_whitelist_vehicles
WHERE vrn = 'WIN123';

UPDATE caz_whitelist_vehicles_audit.logged_actions
SET action_tstamp = '2020-11-14 23:36:25.175645Z'
WHERE action = 'I'
  AND new_data ->> 'vrn' = 'WIN123';

UPDATE caz_whitelist_vehicles_audit.logged_actions
SET action_tstamp = '2020-11-15 10:36:25.175645Z'
WHERE action = 'U'
  AND new_data ->> 'vrn' = 'WIN123';

UPDATE caz_whitelist_vehicles_audit.logged_actions
set action_tstamp = '2020-11-15 23:36:25.175645Z'
WHERE action = 'D'
  AND original_data ->> 'vrn' = 'WIN123';