INSERT INTO caz_payment.t_payment(
	payment_id, user_id, payment_method, payment_provider_id, total_paid, payment_provider_status, payment_submitted_timestamp, payment_authorised_timestamp, operator_id, central_reference_number)
	VALUES
	  ('b71b72a5-902f-4a16-a91d-1a4463b801db', 'ab3e9f4b-4076-4154-b6dd-97c5d4800b47', 'CREDIT_DEBIT_CARD', 'provider_1_test', 100, 'SUCCESS', '2020-07-01T10:00:00Z', now(), 'd47bcc60-dafc-11ea-87d0-0242ac130002', 1),
	  ('dabc1391-ff31-427a-8000-69037deb2d3a', '88732cca-a5c7-4ad6-a60d-7edede935915', 'CREDIT_DEBIT_CARD', 'provider_2_test', 100, 'SUCCESS', '2020-07-01T09:00:00Z', now(), 'd47bce5e-dafc-11ea-87d0-0242ac130003', 2),
	  ('dabc1391-ff31-427a-8000-69037deb2d3b', '88732cca-a5c7-4ad6-a60d-7edede935915', 'CREDIT_DEBIT_CARD', 'provider_3_test', 100, 'SUCCESS', '2020-07-01T11:00:00Z', now(), 'd47bce5e-dafc-11ea-87d0-0242ac130004', 3);


INSERT INTO caz_payment.t_clean_air_zone_entrant_payment(
	clean_air_zone_entrant_payment_id, vrn, clean_air_zone_id, travel_date, tariff_code, charge, payment_status, update_actor)
	VALUES
	  ('43ea77cc-93cb-4df3-b731-5244c0de9cc8', 'ND84VSX', 'b8e53786-c5ca-426a-a701-b14ee74857d4', '2019-11-01', 'tariff_code1', 50, 'PAID', 'USER'),
	  ('688f8278-2f0f-4710-bb7c-6b0cca04c1bc', 'ND84VSX', 'b8e53786-c5ca-426a-a701-b14ee74857d4', '2019-11-03', 'tariff_code2', 50, 'PAID', 'USER'),
	  ('9cc2dd1a-905e-4eaf-af85-0b14f95aab89', 'ND84VSX', 'b8e53786-c5ca-426a-a701-b14ee74857d4', '2019-11-02', 'tariff_code3', 100, 'PAID', 'USER'),
	  ('21b7049d-b978-482f-a882-4de6bb9d699c', 'ND84VSX', 'b8e53786-c5ca-426a-a701-b14ee74857d4', '2019-11-04', 'tariff_code4', 100, 'PAID', 'USER'),
	  ('21b7049d-b978-482f-a882-4de6bb9d699d', 'ND84VSX', '53e03a28-0627-11ea-9511-ffaaee87e375', '2019-11-04', 'tariff_code4', 100, 'PAID', 'USER'),
    ('21b7049d-b978-482f-a882-4de6bb9d699e', 'ND84VSY', '53e03a28-0627-11ea-9511-ffaaee87e370', '2019-11-04', 'tariff_code4', 100, 'PAID', 'USER');

INSERT INTO caz_payment.t_clean_air_zone_entrant_payment_match(
	id, clean_air_zone_entrant_payment_id, payment_id, latest)
	VALUES
  ('a277431b-4b42-4f61-af43-b262a0467807', '43ea77cc-93cb-4df3-b731-5244c0de9cc8', 'b71b72a5-902f-4a16-a91d-1a4463b801db', true),
  ('2d34ec3b-6ca3-4a05-9301-8462b46e1cc0', '688f8278-2f0f-4710-bb7c-6b0cca04c1bc', 'b71b72a5-902f-4a16-a91d-1a4463b801db', true),
  ('74af34d6-255d-495d-bbce-aa5bcea9736d', '9cc2dd1a-905e-4eaf-af85-0b14f95aab89', 'b71b72a5-902f-4a16-a91d-1a4463b801db', true),
  ('26991ad9-c5c1-4173-a31e-88f7a98de8c0', '21b7049d-b978-482f-a882-4de6bb9d699c', 'dabc1391-ff31-427a-8000-69037deb2d3b', true),
  ('26991ad9-c5c1-4173-a31e-88f7a98de8c1', '21b7049d-b978-482f-a882-4de6bb9d699d', 'dabc1391-ff31-427a-8000-69037deb2d3a', true),
  ('26991ad9-c5c1-4173-a31e-88f7a98de8c2', '21b7049d-b978-482f-a882-4de6bb9d699e', 'dabc1391-ff31-427a-8000-69037deb2d3b', true);