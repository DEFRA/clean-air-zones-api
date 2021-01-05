INSERT INTO caz_account.t_account(account_id, account_name)
VALUES ('1f30838f-69ee-4486-95b4-7dfcd5c6c67c', 'test');

INSERT INTO caz_account.t_account_user(
account_user_id, account_id, user_id, is_owner, is_administrated_by, last_sign_in_timestmp)
VALUES
('4e581c88-3ba3-4df0-91a3-ad46fb48bfd1', '1f30838f-69ee-4486-95b4-7dfcd5c6c67c', 'd2b55341-551e-498d-a7be-a6e7f8639161', true, NULL, (CURRENT_TIMESTAMP - INTERVAL '165 days'))
