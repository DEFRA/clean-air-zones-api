-- first account
INSERT INTO caz_account.t_account(account_id, account_name)
VALUES ('2b424742-686f-4e63-b810-771c30da940c', 'Fleet 1');

INSERT INTO caz_account.t_account_vehicle(account_vehicle_id, account_id, vrn, caz_vehicle_type)
VALUES ('ccbc6bea-4b0f-45ec-bbf2-021451823441', '2b424742-686f-4e63-b810-771c30da940c', 'VRN1',
        'Van');
INSERT INTO caz_account.t_account_vehicle(account_vehicle_id, account_id, vrn, caz_vehicle_type)
VALUES ('13c52f66-fdc1-43e2-b6af-a67d04987776', '2b424742-686f-4e63-b810-771c30da940c', 'VRN2',
        'Van');
INSERT INTO caz_account.t_account_vehicle(account_vehicle_id, account_id, vrn, caz_vehicle_type)
VALUES ('6d3c83de-2c89-443c-be17-662bdde3841a', '2b424742-686f-4e63-b810-771c30da940c', 'VRN3',
        'Van');

-- second account
INSERT INTO caz_account.t_account(account_id, account_name)
VALUES ('1f30838f-69ee-4486-95b4-7dfcd5c6c67a', 'Fleet 2');

INSERT INTO caz_account.t_account_vehicle(account_vehicle_id, account_id, vrn, caz_vehicle_type)
VALUES ('ccbc6bea-4b0f-45ec-bbf2-021451823442', '1f30838f-69ee-4486-95b4-7dfcd5c6c67a', 'VRN1',
        'Van');
INSERT INTO caz_account.t_account_vehicle(account_vehicle_id, account_id, vrn, caz_vehicle_type)
VALUES ('13c52f66-fdc1-43e2-b6af-a67d04987777', '1f30838f-69ee-4486-95b4-7dfcd5c6c67a', 'VRN2',
        'Van');
INSERT INTO caz_account.t_account_vehicle(account_vehicle_id, account_id, vrn, caz_vehicle_type)
VALUES ('9448f64f-3d4c-4911-a50a-734ebb758c41', '1f30838f-69ee-4486-95b4-7dfcd5c6c67a', 'VRN3',
        'Van');