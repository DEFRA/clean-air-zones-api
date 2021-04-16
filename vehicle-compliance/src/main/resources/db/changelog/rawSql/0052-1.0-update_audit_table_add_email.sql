ALTER TABLE CAZ_WHITELIST_VEHICLES_AUDIT.transaction_to_modifier
    ADD COLUMN IF NOT EXISTS modifier_email varchar(256);

ALTER TABLE CAZ_WHITELIST_VEHICLES_AUDIT.logged_actions
    ADD COLUMN IF NOT EXISTS modifier_email varchar(256);

--
-- Now, define the actual trigger function:
--
CREATE OR REPLACE FUNCTION CAZ_WHITELIST_VEHICLES_AUDIT.if_modified_func() RETURNS TRIGGER AS $body$
DECLARE
    v_old_data jsonb;
    v_new_data jsonb;
    vrn varchar(14);
    modifier_id varchar(256);
    modifier_email varchar(256);
BEGIN
    /*  If this actually for real auditing (where you need to log EVERY action),
        then you would need to use something like dblink or plperl that could log outside the transaction,
        regardless of whether the transaction committed or rolled back.
    */

    /* This dance with casting the NEW and OLD values to a ROW is not necessary in pg 9.0+ */


    select ttm.modifier_id, ttm.modifier_email into modifier_id, modifier_email from CAZ_WHITELIST_VEHICLES_AUDIT.transaction_to_modifier as ttm where transaction_id = txid_current();
    IF (TG_OP = 'UPDATE') THEN
        v_old_data := to_jsonb(OLD.*);
        v_new_data := to_jsonb(NEW.*);
        vrn := NEW.vrn;
        INSERT INTO CAZ_WHITELIST_VEHICLES_AUDIT.logged_actions (vrn,schema_name,table_name,user_name,action,original_data,new_data,query,modifier_id, modifier_email)
        VALUES (vrn,TG_TABLE_SCHEMA::TEXT,TG_TABLE_NAME::TEXT,session_user::TEXT,substring(TG_OP,1,1),v_old_data,v_new_data, current_query(), modifier_id, modifier_email);
        RETURN NEW;
    ELSIF (TG_OP = 'DELETE') THEN
        v_old_data := to_jsonb(OLD.*);
        vrn := OLD.vrn;
        INSERT INTO CAZ_WHITELIST_VEHICLES_AUDIT.logged_actions (vrn,schema_name,table_name,user_name,action,original_data,query,modifier_id, modifier_email)
        VALUES (vrn,TG_TABLE_SCHEMA::TEXT,TG_TABLE_NAME::TEXT,session_user::TEXT,substring(TG_OP,1,1),v_old_data, current_query(), modifier_id, modifier_email);
        RETURN OLD;
    ELSIF (TG_OP = 'INSERT') THEN
        v_new_data := to_jsonb(NEW.*);
        vrn := NEW.vrn;
        INSERT INTO CAZ_WHITELIST_VEHICLES_AUDIT.logged_actions (vrn,schema_name,table_name,user_name,action,new_data,query,modifier_id, modifier_email)
        VALUES (vrn,TG_TABLE_SCHEMA::TEXT,TG_TABLE_NAME::TEXT,session_user::TEXT,substring(TG_OP,1,1),v_new_data, current_query(), modifier_id, modifier_email);
        RETURN NEW;
    ELSE
        RAISE WARNING '[CAZ_WHITELIST_VEHICLES_AUDIT.IF_MODIFIED_FUNC] - Other action occurred: %, at %',TG_OP,now();
        RETURN NULL;
    END IF;

EXCEPTION
    WHEN data_exception THEN
        RAISE WARNING '[CAZ_WHITELIST_VEHICLES_AUDIT.IF_MODIFIED_FUNC] - UDF ERROR [DATA EXCEPTION] - SQLSTATE: %, SQLERRM: %',SQLSTATE,SQLERRM;
        RETURN NULL;
    WHEN unique_violation THEN
        RAISE WARNING '[CAZ_WHITELIST_VEHICLES_AUDIT.IF_MODIFIED_FUNC] - UDF ERROR [UNIQUE] - SQLSTATE: %, SQLERRM: %',SQLSTATE,SQLERRM;
        RETURN NULL;
    WHEN OTHERS THEN
        RAISE WARNING '[CAZ_WHITELIST_VEHICLES_AUDIT.IF_MODIFIED_FUNC] - UDF ERROR [OTHER] - SQLSTATE: %, SQLERRM: %',SQLSTATE,SQLERRM;
        RETURN NULL;
END;
$body$
    LANGUAGE plpgsql
    SECURITY DEFINER
    SET search_path = pg_catalog, CAZ_WHITELIST_VEHICLES_AUDIT;

