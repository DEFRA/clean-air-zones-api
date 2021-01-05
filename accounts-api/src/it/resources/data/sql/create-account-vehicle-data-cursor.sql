INSERT INTO caz_account.t_account(
	account_id, account_name)
	VALUES ('1f30838f-69ee-4486-95b4-7dfcd5c6c67c', 'test');

INSERT INTO caz_account.t_account(
	account_id, account_name)
	VALUES ('3de21da7-86fc-4ccc-bab3-130f3a10e380', 'test account without vehicles');

INSERT INTO caz_account.t_account(
    account_id, account_name)
VALUES ('e6cf9e24-31b1-45c9-9149-5fc866424386', 'account containing all chargeability data');

INSERT INTO caz_account.t_account(
    account_id, account_name)
VALUES ('1dfe3e6d-363e-4b21-acca-0968ba764d46', 'account containing some non chargeable vehicles');

-- vehicles for `1f30838f-69ee-4486-95b4-7dfcd5c6c67c` account
INSERT INTO caz_account.t_account_vehicle(
	account_vehicle_id, account_id, vrn, caz_vehicle_type)
	VALUES ('db821a16-30ff-4f74-b5a1-bb88652a6903', '1f30838f-69ee-4486-95b4-7dfcd5c6c67c', 'ABC456', 'Van');
INSERT INTO caz_account.t_account_vehicle(
	account_vehicle_id, account_id, vrn, caz_vehicle_type)
	VALUES ('01ed9577-69a1-4c58-bb3d-3b979fae9011', '1f30838f-69ee-4486-95b4-7dfcd5c6c67c', 'BX92 CNE', 'Coach');
INSERT INTO caz_account.t_account_vehicle(
	account_vehicle_id, account_id, vrn, caz_vehicle_type)
	VALUES ('033929c7-e39f-468c-8274-394cb59147f0', '1f30838f-69ee-4486-95b4-7dfcd5c6c67c', 'CAS123', 'HGV');
INSERT INTO caz_account.t_account_vehicle(
	account_vehicle_id, account_id, vrn, caz_vehicle_type)
	VALUES ('4d3bd0c9-d29d-4a15-a4ec-0fe43d101a2a', '1f30838f-69ee-4486-95b4-7dfcd5c6c67c', 'DKC789', 'Van');
INSERT INTO caz_account.t_account_vehicle(
	account_vehicle_id, account_id, vrn, caz_vehicle_type)
	VALUES ('d4d45d52-b1e2-4e2c-b845-378f20878998', '1f30838f-69ee-4486-95b4-7dfcd5c6c67c', 'KQ93NFL', 'Bus');
INSERT INTO caz_account.t_account_vehicle(
	account_vehicle_id, account_id, vrn, caz_vehicle_type)
	VALUES ('7b4ef0b6-0b70-4a9a-986a-594a844ef510', '1f30838f-69ee-4486-95b4-7dfcd5c6c67c', 'PO44 BCN', 'Van');

-- single vehicle for `e6cf9e24-31b1-45c9-9149-5fc866424386` account
INSERT INTO caz_account.t_account_vehicle(
    account_vehicle_id, account_id, vrn, caz_vehicle_type)
VALUES ('60ed96fa-da04-41f6-a1cc-d37c553d8fd3', 'e6cf9e24-31b1-45c9-9149-5fc866424386', 'DEF456', 'Van');

-- vehicles for `1dfe3e6d-363e-4b21-acca-0968ba764d46` account
INSERT INTO caz_account.t_account_vehicle(
	account_vehicle_id, account_id, vrn, caz_vehicle_type)
	VALUES ('e4b7d109-0323-4e1e-a27e-7140151ab2f5', '1dfe3e6d-363e-4b21-acca-0968ba764d46', 'EST121', 'Van');
INSERT INTO caz_account.t_account_vehicle(
	account_vehicle_id, account_id, vrn, caz_vehicle_type)
	VALUES ('480eaaf2-1bc8-4742-8c87-f37fb793e765', '1dfe3e6d-363e-4b21-acca-0968ba764d46', 'EST122', 'Coach');
INSERT INTO caz_account.t_account_vehicle(
	account_vehicle_id, account_id, vrn, caz_vehicle_type)
	VALUES ('64bd66bc-50ef-4385-ac45-2db713785685', '1dfe3e6d-363e-4b21-acca-0968ba764d46', 'EST123', 'HGV');
INSERT INTO caz_account.t_account_vehicle(
	account_vehicle_id, account_id, vrn, caz_vehicle_type)
	VALUES ('76c0d6ff-c04a-49e2-8568-0d1f50a78340', '1dfe3e6d-363e-4b21-acca-0968ba764d46', 'EST124', 'Van');
INSERT INTO caz_account.t_account_vehicle(
	account_vehicle_id, account_id, vrn, caz_vehicle_type)
	VALUES ('7cf1bed0-1375-11eb-adc1-0242ac120002', '1dfe3e6d-363e-4b21-acca-0968ba764d46', 'EST125', 'Van');
INSERT INTO caz_account.t_account_vehicle(
	account_vehicle_id, account_id, vrn, caz_vehicle_type)
	VALUES ('7ee38d68-1375-11eb-adc1-0242ac120002', '1dfe3e6d-363e-4b21-acca-0968ba764d46', 'EST126', 'Van');

-- chargeability data for some vehicles : BEGIN
-- two CAZes: 4a09097a-2175-4146-b7df-90dd9e58ac5c and 4b384b63-0c0e-4979-90bf-aee3b7b51c3a

-- charges for vehicles in `e6cf9e24-31b1-45c9-9149-5fc866424386` account
insert into caz_account.t_vehicle_chargeability
(account_vehicle_id, caz_id, charge, is_exempt, is_retrofitted, tariff_code)
values ('60ed96fa-da04-41f6-a1cc-d37c553d8fd3', '4a09097a-2175-4146-b7df-90dd9e58ac5c', 2.56, false,
        false, 't-1');
insert into caz_account.t_vehicle_chargeability
(account_vehicle_id, caz_id, charge, is_exempt, is_retrofitted, tariff_code)
values ('60ed96fa-da04-41f6-a1cc-d37c553d8fd3', '4b384b63-0c0e-4979-90bf-aee3b7b51c3a', 4.89, false,
        false, 't-2');

-- charges for vehicles in `1f30838f-69ee-4486-95b4-7dfcd5c6c67c` account
insert into caz_account.t_vehicle_chargeability
(account_vehicle_id, caz_id, charge, is_exempt, is_retrofitted, tariff_code)
values ('db821a16-30ff-4f74-b5a1-bb88652a6903', '4a09097a-2175-4146-b7df-90dd9e58ac5c', 2.56, false,
        false, 't-1');
insert into caz_account.t_vehicle_chargeability
(account_vehicle_id, caz_id, charge, is_exempt, is_retrofitted, tariff_code)
values ('db821a16-30ff-4f74-b5a1-bb88652a6903', '4b384b63-0c0e-4979-90bf-aee3b7b51c3a', 4.89, false,
        false, 't-2');

-------
insert into caz_account.t_vehicle_chargeability
(account_vehicle_id, caz_id, charge, is_exempt, is_retrofitted, tariff_code)
values ('01ed9577-69a1-4c58-bb3d-3b979fae9011', '4a09097a-2175-4146-b7df-90dd9e58ac5c', 9.65, false,
        false, 't-10');
insert into caz_account.t_vehicle_chargeability
(account_vehicle_id, caz_id, charge, is_exempt, is_retrofitted, tariff_code)
values ('01ed9577-69a1-4c58-bb3d-3b979fae9011', '4b384b63-0c0e-4979-90bf-aee3b7b51c3a', 12.94, false,
        false, 't-20');

-------
insert into caz_account.t_vehicle_chargeability
(account_vehicle_id, caz_id, charge, is_exempt, is_retrofitted)
values ('033929c7-e39f-468c-8274-394cb59147f0', '4a09097a-2175-4146-b7df-90dd9e58ac5c', 1.15, true,
        false);
insert into caz_account.t_vehicle_chargeability
(account_vehicle_id, caz_id, charge, is_exempt, is_retrofitted)
values ('033929c7-e39f-468c-8274-394cb59147f0', '4b384b63-0c0e-4979-90bf-aee3b7b51c3a', 1.15, false,
        true);

-------
insert into caz_account.t_vehicle_chargeability
(account_vehicle_id, caz_id, charge, is_exempt, is_retrofitted)
values ('4d3bd0c9-d29d-4a15-a4ec-0fe43d101a2a', '4a09097a-2175-4146-b7df-90dd9e58ac5c', 5.55, true,
        false);
insert into caz_account.t_vehicle_chargeability
(account_vehicle_id, caz_id, charge, is_exempt, is_retrofitted)
values ('4d3bd0c9-d29d-4a15-a4ec-0fe43d101a2a', '4b384b63-0c0e-4979-90bf-aee3b7b51c3a', 5.55, false,
        true);

-------
insert into caz_account.t_vehicle_chargeability
(account_vehicle_id, caz_id, charge, is_exempt, is_retrofitted)
values ('d4d45d52-b1e2-4e2c-b845-378f20878998', '4a09097a-2175-4146-b7df-90dd9e58ac5c', 3.33, false,
        false);
insert into caz_account.t_vehicle_chargeability
(account_vehicle_id, caz_id, charge, is_exempt, is_retrofitted)
values ('d4d45d52-b1e2-4e2c-b845-378f20878998', '4b384b63-0c0e-4979-90bf-aee3b7b51c3a', 3.33, false,
        false);

-------
insert into caz_account.t_vehicle_chargeability
(account_vehicle_id, caz_id, charge, is_exempt, is_retrofitted)
values ('7b4ef0b6-0b70-4a9a-986a-594a844ef510', '4a09097a-2175-4146-b7df-90dd9e58ac5c', null, false,
        false);
insert into caz_account.t_vehicle_chargeability
(account_vehicle_id, caz_id, charge, is_exempt, is_retrofitted)
values ('7b4ef0b6-0b70-4a9a-986a-594a844ef510', '4b384b63-0c0e-4979-90bf-aee3b7b51c3a', null, false,
        false);

-- charges for vehicles in `1dfe3e6d-363e-4b21-acca-0968ba764d46` account
-- EST121 --
insert into caz_account.t_vehicle_chargeability
(account_vehicle_id, caz_id, charge, is_exempt, is_retrofitted, tariff_code)
values ('e4b7d109-0323-4e1e-a27e-7140151ab2f5', '4a09097a-2175-4146-b7df-90dd9e58ac5c', 2.56, false,
        false, 't-1');
insert into caz_account.t_vehicle_chargeability
(account_vehicle_id, caz_id, charge, is_exempt, is_retrofitted, tariff_code)
values ('e4b7d109-0323-4e1e-a27e-7140151ab2f5', '4b384b63-0c0e-4979-90bf-aee3b7b51c3a', 4.89, false,
        false, 't-2');

-- EST122 --
insert into caz_account.t_vehicle_chargeability
(account_vehicle_id, caz_id, charge, is_exempt, is_retrofitted)
values ('480eaaf2-1bc8-4742-8c87-f37fb793e765', '4a09097a-2175-4146-b7df-90dd9e58ac5c', 0, true,
        false);
insert into caz_account.t_vehicle_chargeability
(account_vehicle_id, caz_id, charge, is_exempt, is_retrofitted)
values ('480eaaf2-1bc8-4742-8c87-f37fb793e765', '4b384b63-0c0e-4979-90bf-aee3b7b51c3a', 0, false,
        true);

-- EST123 --
insert into caz_account.t_vehicle_chargeability
(account_vehicle_id, caz_id, charge, is_exempt, is_retrofitted)
values ('64bd66bc-50ef-4385-ac45-2db713785685', '4a09097a-2175-4146-b7df-90dd9e58ac5c', null, false,
        false);
insert into caz_account.t_vehicle_chargeability
(account_vehicle_id, caz_id, charge, is_exempt, is_retrofitted)
values ('64bd66bc-50ef-4385-ac45-2db713785685', '4b384b63-0c0e-4979-90bf-aee3b7b51c3a', null, false,
        false);

-- EST125 --
insert into caz_account.t_vehicle_chargeability
(account_vehicle_id, caz_id, charge, is_exempt, is_retrofitted, tariff_code)
values ('7cf1bed0-1375-11eb-adc1-0242ac120002', '4a09097a-2175-4146-b7df-90dd9e58ac5c', 2.56, false,
        false, 't-1');
insert into caz_account.t_vehicle_chargeability
(account_vehicle_id, caz_id, charge, is_exempt, is_retrofitted)
values ('7cf1bed0-1375-11eb-adc1-0242ac120002', '4b384b63-0c0e-4979-90bf-aee3b7b51c3a', null, false,
        false);

-- EST126 --
insert into caz_account.t_vehicle_chargeability
(account_vehicle_id, caz_id, charge, is_exempt, is_retrofitted, tariff_code)
values ('7ee38d68-1375-11eb-adc1-0242ac120002', '4a09097a-2175-4146-b7df-90dd9e58ac5c', 2.56, false,
        false, 't-1');
insert into caz_account.t_vehicle_chargeability
(account_vehicle_id, caz_id, charge, is_exempt, is_retrofitted)
values ('7ee38d68-1375-11eb-adc1-0242ac120002', '4b384b63-0c0e-4979-90bf-aee3b7b51c3a', 0, false,
        true);

-- chargeability data : END