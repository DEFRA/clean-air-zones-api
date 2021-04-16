INSERT INTO caz_account.t_account_user(account_user_id, account_id, user_id, is_owner, is_administrated_by)
  VALUES ('4e581c88-3ba3-4df0-91a3-ad46fb48bfd1', '457a23f1-3df9-42b9-a42e-435aef201d93', 'd2b55341-551e-498d-a7be-a6e7f8639161', true, NULL);

insert into caz_account.t_account_direct_debit_mandate(direct_debit_mandate_id, account_id, account_user_id, clean_air_zone_id, payment_provider_mandate_id, status)
  values ('1825761b-304e-416f-89ab-c74177591345', '457a23f1-3df9-42b9-a42e-435aef201d93', '4e581c88-3ba3-4df0-91a3-ad46fb48bfd1', 'ed2f3499-f888-4b0a-9bc1-2a9f2c91b0d8', 'jhjcvaiqlediuhh23d89hd3', 'SUBMITTED');