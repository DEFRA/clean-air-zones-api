  UPDATE caz_account.t_account SET multi_payer_account = TRUE
  WHERE account_id IN (
    SELECT ac.account_id FROM caz_account.t_account ac
    JOIN caz_account.t_account_user acu ON ac.account_id = acu.account_id
    JOIN caz_account.t_account_user_permission acup ON acu.account_user_id = acup.account_user_id
    JOIN caz_account.t_account_permission ap ON ap.account_permission_id = acup.account_permission_id
    WHERE ap.name = 'MAKE_PAYMENTS' AND acu.is_owner = FALSE
  )