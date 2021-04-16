INSERT INTO caz_account.t_account(account_id, account_name)
VALUES ('1f30838f-69ee-4486-95b4-7dfcd5c6c67a', 'Fleet 1');

INSERT INTO caz_account.t_account(account_id, account_name)
VALUES ('1f30838f-69ee-4486-95b4-7dfcd5c6c67b', 'Fleet 2');

INSERT INTO caz_account.t_account_vehicle(account_vehicle_id, account_id, vrn, caz_vehicle_type)
VALUES ('ccbc6bea-4b0f-45ec-bbf2-021451823442', '1f30838f-69ee-4486-95b4-7dfcd5c6c67a', 'VRN1',
        'Van');
INSERT INTO caz_account.t_account_vehicle(account_vehicle_id, account_id, vrn, caz_vehicle_type)
VALUES ('13c52f66-fdc1-43e2-b6af-a67d04987777', '1f30838f-69ee-4486-95b4-7dfcd5c6c67a', 'VRN2',
        'Car');
INSERT INTO caz_account.t_account_vehicle(account_vehicle_id, account_id, vrn, caz_vehicle_type)
VALUES ('6d3c83de-2c89-443c-be17-662bdde3841b', '1f30838f-69ee-4486-95b4-7dfcd5c6c67a', 'VRN3',
        'Van');

-- Birmingham VRN1
INSERT INTO caz_account.t_vehicle_chargeability(account_vehicle_id, caz_id, charge, is_exempt,
                                                is_retrofitted, tariff_code)
VALUES ('ccbc6bea-4b0f-45ec-bbf2-021451823442', '53e03a28-0627-11ea-9511-ffaaee87e375', '12',
        'false', 'false', 'Tariff 1');

-- Bath VRN1
INSERT INTO caz_account.t_vehicle_chargeability(account_vehicle_id, caz_id, charge, is_exempt,
                                                is_retrofitted, tariff_code)
VALUES ('ccbc6bea-4b0f-45ec-bbf2-021451823442', '742b343f-6ce6-42d3-8324-df689ad4c515', null,
        'false', 'false', 'Tariff 2');

-- -- Birmingham VRN2
INSERT INTO caz_account.t_vehicle_chargeability(account_vehicle_id, caz_id, charge, is_exempt,
                                                is_retrofitted, tariff_code)
VALUES ('13c52f66-fdc1-43e2-b6af-a67d04987777', '53e03a28-0627-11ea-9511-ffaaee87e375', '18',
        'false', 'false', 'Tariff 3');

-- Bath VRN3
INSERT INTO caz_account.t_vehicle_chargeability(account_vehicle_id, caz_id, charge, is_exempt,
                                                is_retrofitted, tariff_code)
VALUES ('6d3c83de-2c89-443c-be17-662bdde3841b', '742b343f-6ce6-42d3-8324-df689ad4c515', '25',
        'false', 'false', 'Tariff 4');

-- Undetermined VRN4
INSERT INTO caz_account.t_account_vehicle(account_vehicle_id, account_id, vrn, caz_vehicle_type)
VALUES ('de083fd0-6588-49d7-92e5-4f71e4d87e66', '1f30838f-69ee-4486-95b4-7dfcd5c6c67a', 'VRN4',
        null);
-- Birmingham VRN4
INSERT INTO caz_account.t_vehicle_chargeability(account_vehicle_id, caz_id, charge, is_exempt,
                                                is_retrofitted, tariff_code)
VALUES ('de083fd0-6588-49d7-92e5-4f71e4d87e66', '53e03a28-0627-11ea-9511-ffaaee87e375', null,
        'false', 'false', 'Tariff 1');
-- Bath VRN4
INSERT INTO caz_account.t_vehicle_chargeability(account_vehicle_id, caz_id, charge, is_exempt,
                                                is_retrofitted, tariff_code)
VALUES ('de083fd0-6588-49d7-92e5-4f71e4d87e66', '742b343f-6ce6-42d3-8324-df689ad4c515', null,
        'false', 'false', 'Tariff 2');

-- No charge VRN5
INSERT INTO caz_account.t_account_vehicle(account_vehicle_id, account_id, vrn, caz_vehicle_type)
VALUES ('1f3d09ea-4225-45b6-a734-4a705c226af6', '1f30838f-69ee-4486-95b4-7dfcd5c6c67a', 'VRN5',
        'Heavy Goods Vehicle');
-- Birmingham VRN5
INSERT INTO caz_account.t_vehicle_chargeability(account_vehicle_id, caz_id, charge, is_exempt,
                                                is_retrofitted, tariff_code)
VALUES ('1f3d09ea-4225-45b6-a734-4a705c226af6', '53e03a28-0627-11ea-9511-ffaaee87e375', '0',
        'false', 'false', 'Tariff 1');
-- Bath VRN5
INSERT INTO caz_account.t_vehicle_chargeability(account_vehicle_id, caz_id, charge, is_exempt,
                                                is_retrofitted, tariff_code)
VALUES ('1f3d09ea-4225-45b6-a734-4a705c226af6', '742b343f-6ce6-42d3-8324-df689ad4c515', '0',
        'false', 'false', 'Tariff 2');

-- Exempt VRN6
INSERT INTO caz_account.t_account_vehicle(account_vehicle_id, account_id, vrn, caz_vehicle_type)
VALUES ('29848629-0548-40ed-8d0f-7179ba3bad79', '1f30838f-69ee-4486-95b4-7dfcd5c6c67a', 'VRN6',
        'Heavy Goods Vehicle');
-- Birmingham VRN6
INSERT INTO caz_account.t_vehicle_chargeability(account_vehicle_id, caz_id, charge, is_exempt,
                                                is_retrofitted, tariff_code)
VALUES ('29848629-0548-40ed-8d0f-7179ba3bad79', '53e03a28-0627-11ea-9511-ffaaee87e375', '10',
        'true', 'false', 'Tariff 1');
-- Bath VRN6
INSERT INTO caz_account.t_vehicle_chargeability(account_vehicle_id, caz_id, charge, is_exempt,
                                                is_retrofitted, tariff_code)
VALUES ('29848629-0548-40ed-8d0f-7179ba3bad79', '742b343f-6ce6-42d3-8324-df689ad4c515', '10',
        'true', 'false', 'Tariff 2');
