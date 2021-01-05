-- There are 2 CAZes:
-- CAZ1 with ID 53e03a28-0627-11ea-9511-ffaaee87e375 - Birmingham
-- CAZ2 with ID 39e54ed8-3ed2-441d-be3f-38fc9b70c8d3 - Leeds

-- Create 2 Accounts/Fleets/Companies: "Fleet 1" and "Fleet 2"
INSERT INTO caz_account.t_account(account_id, account_name)
VALUES ('1f30838f-69ee-4486-95b4-7dfcd5c6c67c', 'Fleet 1');

INSERT INTO caz_account.t_account(account_id, account_name)
VALUES ('3de21da7-86fc-4ccc-bab3-130f3a10e380', 'Fleet 2');

INSERT INTO caz_account.t_account(account_id, account_name)
VALUES ('06fa82ca-7ace-4e31-b1ad-402e878ef9a5', 'Fleet 3');

-- In Fleet 1 Create 5 Vehicles: VRN1 to VRN5
INSERT INTO caz_account.t_account_vehicle(account_vehicle_id, account_id, vrn, caz_vehicle_type)
VALUES ('ccbc6bea-4b0f-45ec-bbf2-021451823441', '1f30838f-69ee-4486-95b4-7dfcd5c6c67c', 'VRN1',
        'Van');
INSERT INTO caz_account.t_account_vehicle(account_vehicle_id, account_id, vrn, caz_vehicle_type)
VALUES ('13c52f66-fdc1-43e2-b6af-a67d04987776', '1f30838f-69ee-4486-95b4-7dfcd5c6c67c', 'VRN2',
        'Van');
INSERT INTO caz_account.t_account_vehicle(account_vehicle_id, account_id, vrn, caz_vehicle_type)
VALUES ('6d3c83de-2c89-443c-be17-662bdde3841a', '1f30838f-69ee-4486-95b4-7dfcd5c6c67c', 'VRN3',
        'Van');
INSERT INTO caz_account.t_account_vehicle(account_vehicle_id, account_id, vrn, caz_vehicle_type)
VALUES ('e8704ce7-5038-4c2c-a263-c5006ad9423f', '1f30838f-69ee-4486-95b4-7dfcd5c6c67c', 'VRN4',
        'Van');
INSERT INTO caz_account.t_account_vehicle(account_vehicle_id, account_id, vrn, caz_vehicle_type)
VALUES ('0b498d32-3fc6-4814-b8ac-41211a8a395d', '1f30838f-69ee-4486-95b4-7dfcd5c6c67c', 'VRN5',
        'Van');

-- In Fleet 2 Create 3 Vehicles: VRN1, VRN2 and VRN3
INSERT INTO caz_account.t_account_vehicle(account_vehicle_id, account_id, vrn, caz_vehicle_type)
VALUES ('2556476e-0d0d-49cf-892c-f4958c993564', '3de21da7-86fc-4ccc-bab3-130f3a10e380', 'VRN1',
        'Van');
INSERT INTO caz_account.t_account_vehicle(account_vehicle_id, account_id, vrn, caz_vehicle_type)
VALUES ('63a744b3-7ece-4246-849c-9267005a710a', '3de21da7-86fc-4ccc-bab3-130f3a10e380', 'VRN2',
        'Van');
INSERT INTO caz_account.t_account_vehicle(account_vehicle_id, account_id, vrn, caz_vehicle_type)
VALUES ('e93f2a52-54ae-4b79-8352-e20a87dcfcc7', '3de21da7-86fc-4ccc-bab3-130f3a10e380', 'VRN3',
        'Van');

-- In Fleet 3 Create 5 Vehicles: EST121 to EST125
INSERT INTO caz_account.t_account_vehicle(account_vehicle_id, account_id, vrn, caz_vehicle_type)
VALUES ('e0e8eea6-cd51-4b65-ae03-ec6cbd9ec9d1', '06fa82ca-7ace-4e31-b1ad-402e878ef9a5', 'EST121',
        'Van');
INSERT INTO caz_account.t_account_vehicle(account_vehicle_id, account_id, vrn, caz_vehicle_type)
VALUES ('2ac2ee5e-0fc6-429f-affc-eec7ec9ee51b', '06fa82ca-7ace-4e31-b1ad-402e878ef9a5', 'EST122',
        'Van');
INSERT INTO caz_account.t_account_vehicle(account_vehicle_id, account_id, vrn, caz_vehicle_type)
VALUES ('a2aa5422-8a9b-4f47-be8b-6fab6529d25a', '06fa82ca-7ace-4e31-b1ad-402e878ef9a5', 'EST123',
        'Van');
INSERT INTO caz_account.t_account_vehicle(account_vehicle_id, account_id, vrn, caz_vehicle_type)
VALUES ('7b896ddf-424e-4bdb-bbb3-67fce4c154a2', '06fa82ca-7ace-4e31-b1ad-402e878ef9a5', 'EST124',
        'Van');
INSERT INTO caz_account.t_account_vehicle(account_vehicle_id, account_id, vrn, caz_vehicle_type)
VALUES ('86076c35-8e1c-47e0-9def-881749528b00', '06fa82ca-7ace-4e31-b1ad-402e878ef9a5', 'EST125',
        'Van');
INSERT INTO caz_account.t_account_vehicle(account_vehicle_id, account_id, vrn, caz_vehicle_type)
VALUES ('5d670686-143e-4017-b2af-2ef45072678c', '06fa82ca-7ace-4e31-b1ad-402e878ef9a5', 'EST126',
        'Van');

-- In Chargeability Cache:
-- For Fleet 1:
-- VRN1 has cache in both CAZes
INSERT INTO caz_account.t_vehicle_chargeability(account_vehicle_id, caz_id, charge, is_exempt,
                                                is_retrofitted, tariff_code)
VALUES ('ccbc6bea-4b0f-45ec-bbf2-021451823441', '53e03a28-0627-11ea-9511-ffaaee87e375', '12',
        'false', 'false', 'Tariff 1');
INSERT INTO caz_account.t_vehicle_chargeability(account_vehicle_id, caz_id, charge, is_exempt,
                                                is_retrofitted, tariff_code)
VALUES ('ccbc6bea-4b0f-45ec-bbf2-021451823441', '39e54ed8-3ed2-441d-be3f-38fc9b70c8d3', '15',
        'false', 'false', 'Tariff 2');
-- VRN2 has cache in first CAZ
INSERT INTO caz_account.t_vehicle_chargeability(account_vehicle_id, caz_id, charge, is_exempt,
                                                is_retrofitted, tariff_code)
VALUES ('13c52f66-fdc1-43e2-b6af-a67d04987776', '53e03a28-0627-11ea-9511-ffaaee87e375', '18',
        'false', 'false', 'Tariff 3');
-- VRN3 has cache in second CAZ
INSERT INTO caz_account.t_vehicle_chargeability(account_vehicle_id, caz_id, charge, is_exempt,
                                                is_retrofitted, tariff_code)
VALUES ('6d3c83de-2c89-443c-be17-662bdde3841a', '39e54ed8-3ed2-441d-be3f-38fc9b70c8d3', '25',
        'false', 'false', 'Tariff 4');
-- VRN4 and VRN5 do not have chargeability cache calculated at all

-- For Fleet 2:
-- VRN1 and VRN3 do not have chargeabiltiy cache calculated at all
-- VRN2 has cache in both CAZes
INSERT INTO caz_account.t_vehicle_chargeability(account_vehicle_id, caz_id, charge, is_exempt,
                                                is_retrofitted, tariff_code)
VALUES ('63a744b3-7ece-4246-849c-9267005a710a', '53e03a28-0627-11ea-9511-ffaaee87e375', '12',
        'false', 'false', 'Tariff 1');
INSERT INTO caz_account.t_vehicle_chargeability(account_vehicle_id, caz_id, charge, is_exempt,
                                                is_retrofitted, tariff_code)
VALUES ('63a744b3-7ece-4246-849c-9267005a710a', '39e54ed8-3ed2-441d-be3f-38fc9b70c8d3', '15',
        'false', 'false', 'Tariff 2');

-- For Fleet 3:
-- EST121 has cache in both CAZes
INSERT INTO caz_account.t_vehicle_chargeability(account_vehicle_id, caz_id, charge, is_exempt,
                                                is_retrofitted, tariff_code)
VALUES ('e0e8eea6-cd51-4b65-ae03-ec6cbd9ec9d1', '53e03a28-0627-11ea-9511-ffaaee87e375', '12',
        'false', 'false', 'Tariff 1');
INSERT INTO caz_account.t_vehicle_chargeability(account_vehicle_id, caz_id, charge, is_exempt,
                                                is_retrofitted, tariff_code)
VALUES ('e0e8eea6-cd51-4b65-ae03-ec6cbd9ec9d1', '39e54ed8-3ed2-441d-be3f-38fc9b70c8d3', '15',
        'false', 'false', 'Tariff 2');
-- EST122 has cache in first CAZ
INSERT INTO caz_account.t_vehicle_chargeability(account_vehicle_id, caz_id, charge, is_exempt,
                                                is_retrofitted, tariff_code)
VALUES ('2ac2ee5e-0fc6-429f-affc-eec7ec9ee51b', '53e03a28-0627-11ea-9511-ffaaee87e375', '18',
        'false', 'false', 'Tariff 3');
-- EST123 has cache in second CAZ
INSERT INTO caz_account.t_vehicle_chargeability(account_vehicle_id, caz_id, charge, is_exempt,
                                                is_retrofitted, tariff_code)
VALUES ('a2aa5422-8a9b-4f47-be8b-6fab6529d25a', '39e54ed8-3ed2-441d-be3f-38fc9b70c8d3', '25',
        'false', 'false', 'Tariff 4');
-- EST124 has expired cache in both CAZes
INSERT INTO caz_account.t_vehicle_chargeability(account_vehicle_id, caz_id, charge, is_exempt,
                                                is_retrofitted, tariff_code, refresh_timestmp)
VALUES ('7b896ddf-424e-4bdb-bbb3-67fce4c154a2', '53e03a28-0627-11ea-9511-ffaaee87e375', '25',
        'false', 'false', 'Tariff 4', now() - INTERVAL '10 DAYS');
INSERT INTO caz_account.t_vehicle_chargeability(account_vehicle_id, caz_id, charge, is_exempt,
                                                is_retrofitted, tariff_code, refresh_timestmp)
VALUES ('7b896ddf-424e-4bdb-bbb3-67fce4c154a2', '39e54ed8-3ed2-441d-be3f-38fc9b70c8d3', '15',
        'false', 'false', 'Tariff 2', now() - INTERVAL '10 DAYS');
-- EST125 has expired cache in one CAZ
INSERT INTO caz_account.t_vehicle_chargeability(account_vehicle_id, caz_id, charge, is_exempt,
                                                is_retrofitted, tariff_code)
VALUES ('86076c35-8e1c-47e0-9def-881749528b00', '53e03a28-0627-11ea-9511-ffaaee87e375', '25',
        'false', 'false', 'Tariff 4');
INSERT INTO caz_account.t_vehicle_chargeability(account_vehicle_id, caz_id, charge, is_exempt,
                                                is_retrofitted, tariff_code, refresh_timestmp)
VALUES ('86076c35-8e1c-47e0-9def-881749528b00', '39e54ed8-3ed2-441d-be3f-38fc9b70c8d3', '15',
        'false', 'false', 'Tariff 2', now() - INTERVAL '10 DAYS');
-- EST126 does not have chargeability cache calculated at all
