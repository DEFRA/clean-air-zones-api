INSERT INTO caz_account.t_account(account_id, account_name)
VALUES ('1f30838f-69ee-4486-95b4-7dfcd5c6c67c', 'Fleet 1');

INSERT INTO caz_account.t_account_vehicle(account_vehicle_id, account_id, vrn, caz_vehicle_type)
VALUES ('ccbc6bea-4b0f-45ec-bbf2-021451823441', '1f30838f-69ee-4486-95b4-7dfcd5c6c67c', 'VRN1',
        'Van');

INSERT INTO caz_account.t_vehicle_chargeability(account_vehicle_id, caz_id, charge, is_exempt,
                                                is_retrofitted, tariff_code)
VALUES ('ccbc6bea-4b0f-45ec-bbf2-021451823441', '39e54ed8-3ed2-441d-be3f-38fc9b70c8d3', '12',
        'false', 'false', 'Tariff 1');
INSERT INTO caz_account.t_vehicle_chargeability(account_vehicle_id, caz_id, charge, is_exempt,
                                                is_retrofitted, tariff_code)
VALUES ('ccbc6bea-4b0f-45ec-bbf2-021451823441', '53e03a28-0627-11ea-9511-ffaaee87e375', '15',
        'false', 'false', 'Tariff 2');