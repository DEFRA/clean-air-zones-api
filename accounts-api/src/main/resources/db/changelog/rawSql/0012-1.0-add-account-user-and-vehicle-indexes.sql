CREATE INDEX IF NOT EXISTS account_vehicle_id_vrn_idx ON caz_account.t_account_vehicle (account_id, vrn);
CREATE INDEX IF NOT EXISTS account_user_pk_account_id_idx ON caz_account.t_account_user (account_user_id, account_id);
CREATE INDEX IF NOT EXISTS account_user_user_id_idx ON caz_account.t_account_user (user_id);