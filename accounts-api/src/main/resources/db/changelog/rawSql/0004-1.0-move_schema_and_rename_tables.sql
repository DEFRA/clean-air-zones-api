-- T_ACCOUNT_DIRECT_DEBIT_MANDATE
ALTER TABLE public.direct_debit_mandate RENAME TO t_account_direct_debit_mandate;
ALTER TABLE public.t_account_direct_debit_mandate SET SCHEMA caz_account;
ALTER TABLE caz_account.t_account_direct_debit_mandate RENAME caz_id TO clean_air_zone_id;

-- T_ACCOUNT
ALTER TABLE public.account RENAME TO t_account;
ALTER TABLE public.t_account SET SCHEMA caz_account;

-- T_ACCOUNT_USER
ALTER TABLE public.account_user RENAME TO t_account_user;
ALTER TABLE public.t_account_user SET SCHEMA caz_account;

-- T_ACCOUNT_VEHICLE
ALTER TABLE public.account_vehicle RENAME TO t_account_vehicle;
ALTER TABLE public.t_account_vehicle SET SCHEMA caz_account;
