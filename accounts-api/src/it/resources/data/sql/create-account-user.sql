INSERT INTO caz_account.t_account(
	account_id, account_name)
	VALUES ('1f30838f-69ee-4486-95b4-7dfcd5c6c67c', 'test');

INSERT INTO caz_account.t_account_user(
	account_user_id, account_id, user_id, is_owner, is_administrated_by)
	VALUES
	  ('4e581c88-3ba3-4df0-91a3-ad46fb48bfd1', '1f30838f-69ee-4486-95b4-7dfcd5c6c67c', 'd2b55341-551e-498d-a7be-a6e7f8639161', true, NULL),
	  ('49401c98-2141-4cf4-8cec-2ab9635806a9', '1f30838f-69ee-4486-95b4-7dfcd5c6c67c', '54f04990-fea5-4ca2-9c60-834a5d9ba411', false, '4e581c88-3ba3-4df0-91a3-ad46fb48bfd1'),
	  ('c11826f3-3ec7-4bd2-9b26-3653bb46c889', '1f30838f-69ee-4486-95b4-7dfcd5c6c67c', '1b93e6e3-cff8-45b9-bcda-f7245defaeb5', true, NULL),
	  ('f54554b1-d582-43da-9899-ee33b679e49f', '1f30838f-69ee-4486-95b4-7dfcd5c6c67c', '08d84742-b196-481e-b2d2-1bb0d7324d0d', false, NULL),
	  ('cf89d141-0dfd-4f50-bf29-a0a444279637', '1f30838f-69ee-4486-95b4-7dfcd5c6c67c', NULL, false, NULL);

INSERT INTO caz_account.t_account_user_permission(account_user_id, account_permission_id)
VALUES ('49401c98-2141-4cf4-8cec-2ab9635806a9', 1);
INSERT INTO caz_account.t_account_user_permission(account_user_id, account_permission_id)
VALUES ('49401c98-2141-4cf4-8cec-2ab9635806a9', 2);
INSERT INTO caz_account.t_account_user_permission(account_user_id, account_permission_id)
VALUES ('49401c98-2141-4cf4-8cec-2ab9635806a9', 3);
INSERT INTO caz_account.t_account_user_permission(account_user_id, account_permission_id)
VALUES ('49401c98-2141-4cf4-8cec-2ab9635806a9', 4);
INSERT INTO caz_account.t_account_user_permission(account_user_id, account_permission_id)
VALUES ('49401c98-2141-4cf4-8cec-2ab9635806a9', 5);

INSERT INTO caz_account.t_account_user_code(
	account_user_code_id, account_user_id, code, expiration, code_type, status)
	VALUES (124, 'f54554b1-d582-43da-9899-ee33b679e49f', 'blablabla', '2020-01-01 10:10', 'PASSWORD_RESET', 'ACTIVE');