-- create a new schema named "CAZ_WHITELIST_VEHICLES_AUDIT"
CREATE SCHEMA IF NOT EXISTS CAZ_WHITELIST_VEHICLES_AUDIT;
REVOKE CREATE ON SCHEMA CAZ_WHITELIST_VEHICLES_AUDIT FROM public;
 
ALTER TABLE CAZ_WHITELIST_VEHICLES_AUDIT.logged_actions
    ADD COLUMN IF NOT EXISTS modifier_id varchar(256);

CREATE TABLE IF NOT EXISTS CAZ_WHITELIST_VEHICLES_AUDIT.transaction_to_modifier(
	transaction_id BIGINT NOT NULL DEFAULT txid_current(),
	modifier_id varchar(256) NOT NULL
);

REVOKE ALL ON CAZ_WHITELIST_VEHICLES_AUDIT.logged_actions FROM public;
REVOKE ALL ON CAZ_WHITELIST_VEHICLES_AUDIT.transaction_to_modifier FROM public;

-- You may wish to use different permissions; this lets anybody
-- see the full audit data. In Pg 9.0 and above you can use column
-- permissions for fine-grained control.

GRANT SELECT ON CAZ_WHITELIST_VEHICLES_AUDIT.logged_actions TO public;
GRANT SELECT ON CAZ_WHITELIST_VEHICLES_AUDIT.transaction_to_modifier TO public;

CREATE INDEX IF NOT EXISTS logged_actions_vrn_idx
ON CAZ_WHITELIST_VEHICLES_AUDIT.logged_actions(vrn);

CREATE INDEX IF NOT EXISTS logged_actions_schema_table_idx
ON CAZ_WHITELIST_VEHICLES_AUDIT.logged_actions(((schema_name||'.'||TABLE_NAME)::TEXT));

CREATE INDEX IF NOT EXISTS logged_actions_action_tstamp_idx
ON CAZ_WHITELIST_VEHICLES_AUDIT.logged_actions(action_tstamp);

CREATE INDEX IF NOT EXISTS logged_actions_action_idx
ON CAZ_WHITELIST_VEHICLES_AUDIT.logged_actions(action);

--
-- Now, define the actual trigger function:
--
CREATE OR REPLACE FUNCTION CAZ_WHITELIST_VEHICLES_AUDIT.if_modified_func() RETURNS TRIGGER AS $body$
DECLARE
    v_old_data TEXT;
    v_new_data TEXT;
    vrn varchar(14);
    modifier_id varchar(256);
BEGIN
    /*  If this actually for real auditing (where you need to log EVERY action),
        then you would need to use something like dblink or plperl that could log outside the transaction,
        regardless of whether the transaction committed or rolled back.
    */
 
    /* This dance with casting the NEW and OLD values to a ROW is not necessary in pg 9.0+ */
	

    select ttm.modifier_id into modifier_id from CAZ_WHITELIST_VEHICLES_AUDIT.transaction_to_modifier as ttm where transaction_id = txid_current();
    IF (TG_OP = 'UPDATE') THEN
        v_old_data := ROW(OLD.*);
        v_new_data := ROW(NEW.*);
        vrn := NEW.vrn;
        INSERT INTO CAZ_WHITELIST_VEHICLES_AUDIT.logged_actions (vrn,schema_name,table_name,user_name,action,original_data,new_data,query,modifier_id)
        VALUES (vrn,TG_TABLE_SCHEMA::TEXT,TG_TABLE_NAME::TEXT,session_user::TEXT,substring(TG_OP,1,1),v_old_data,v_new_data, current_query(), modifier_id);
        RETURN NEW;
    ELSIF (TG_OP = 'DELETE') THEN
        v_old_data := ROW(OLD.*);
        vrn := OLD.vrn;
        INSERT INTO CAZ_WHITELIST_VEHICLES_AUDIT.logged_actions (vrn,schema_name,table_name,user_name,action,original_data,query,modifier_id)
        VALUES (vrn,TG_TABLE_SCHEMA::TEXT,TG_TABLE_NAME::TEXT,session_user::TEXT,substring(TG_OP,1,1),v_old_data, current_query(), modifier_id);
        RETURN OLD;
    ELSIF (TG_OP = 'INSERT') THEN
        v_new_data := ROW(NEW.*);
        vrn := NEW.vrn;
        INSERT INTO CAZ_WHITELIST_VEHICLES_AUDIT.logged_actions (vrn,schema_name,table_name,user_name,action,new_data,query,modifier_id)
        VALUES (vrn,TG_TABLE_SCHEMA::TEXT,TG_TABLE_NAME::TEXT,session_user::TEXT,substring(TG_OP,1,1),v_new_data, current_query(), modifier_id);
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