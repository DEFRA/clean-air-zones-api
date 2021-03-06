INSERT INTO caz_payment.t_payment(
	payment_id, user_id, payment_method, payment_provider_id, total_paid, payment_provider_status, payment_submitted_timestamp, payment_authorised_timestamp, operator_id, central_reference_number)
	VALUES
	  ('b71b72a5-902f-4a16-a91d-1a4463b801db', 'ab3e9f4b-4076-4154-b6dd-97c5d4800b47', 'CREDIT_DEBIT_CARD', '12345test', 100, 'SUCCESS', now(), now(), 'd47bcc60-dafc-11ea-87d0-0242ac130003', 3000),
	  ('dabc1391-ff31-427a-8000-69037deb2d3a', '88732cca-a5c7-4ad6-a60d-7edede935915', 'CREDIT_DEBIT_CARD', '98765test', 100, 'SUCCESS', now(), now(), 'd47bce5e-dafc-11ea-87d0-0242ac130003', 3001),
	  ('1d378f50-3326-412a-b663-cd080744f1f1', '3f319922-71d2-432c-9757-8e5f060c2447', 'CREDIT_DEBIT_CARD', null, 100, null, null, null, 'd47bcf4e-dafc-11ea-87d0-0242ac130003', 3002);

INSERT INTO caz_payment.t_clean_air_zone_entrant_payment(
	clean_air_zone_entrant_payment_id, vrn, clean_air_zone_id, travel_date, tariff_code, charge, payment_status, update_actor)
	VALUES
	  ('43ea77cc-93cb-4df3-b731-5244c0de9cc8', 'ND84VSX', 'b8e53786-c5ca-426a-a701-b14ee74857d4', '2019-11-01', 'tariff_code', 50, 'PAID', 'USER'),
	  ('688f8278-2f0f-4710-bb7c-6b0cca04c1bc', 'ND84VSX', 'b8e53786-c5ca-426a-a701-b14ee74857d4', '2019-11-03', 'tariff_code', 50, 'PAID', 'USER'),
	  ('9cc2dd1a-905e-4eaf-af85-0b14f95aab89', 'ND84VSX', 'b8e53786-c5ca-426a-a701-b14ee74857d4', '2019-11-02', 'tariff_code', 100, 'PAID', 'USER'),
	  ('21b7049d-b978-482f-a882-4de6bb9d699c', 'ND84VSX', 'b8e53786-c5ca-426a-a701-b14ee74857d4', '2019-11-04', 'tariff_code', 100, 'PAID', 'USER');

INSERT INTO caz_payment.t_clean_air_zone_entrant_payment_match(
	id, clean_air_zone_entrant_payment_id, payment_id, latest)
	VALUES
  ('a277431b-4b42-4f61-af43-b262a0467807', '43ea77cc-93cb-4df3-b731-5244c0de9cc8', 'b71b72a5-902f-4a16-a91d-1a4463b801db', true),
  ('2d34ec3b-6ca3-4a05-9301-8462b46e1cc0', '688f8278-2f0f-4710-bb7c-6b0cca04c1bc', 'b71b72a5-902f-4a16-a91d-1a4463b801db', true),
  ('74af34d6-255d-495d-bbce-aa5bcea9736d', '9cc2dd1a-905e-4eaf-af85-0b14f95aab89', 'b71b72a5-902f-4a16-a91d-1a4463b801db', true),
  ('26991ad9-c5c1-4173-a31e-88f7a98de8c0', '21b7049d-b978-482f-a882-4de6bb9d699c', 'dabc1391-ff31-427a-8000-69037deb2d3a', true);


-- Update entrant payments to have chargeback and refunded statuses in payments audit table
UPDATE caz_payment.t_clean_air_zone_entrant_payment
SET payment_status = 'CHARGEBACK', update_actor = 'LA'
WHERE clean_air_zone_entrant_payment_id = '688f8278-2f0f-4710-bb7c-6b0cca04c1bc';

UPDATE caz_payment.t_clean_air_zone_entrant_payment
SET payment_status = 'REFUNDED', update_actor = 'LA'
WHERE clean_air_zone_entrant_payment_id = '9cc2dd1a-905e-4eaf-af85-0b14f95aab89';

UPDATE caz_payment.t_clean_air_zone_entrant_payment
SET tariff_code = 'tariff-2', update_actor = 'USER'
WHERE clean_air_zone_entrant_payment_id = '21b7049d-b978-482f-a882-4de6bb9d699c';

