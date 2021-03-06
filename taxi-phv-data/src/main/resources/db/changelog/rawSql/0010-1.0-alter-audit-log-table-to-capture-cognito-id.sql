-- create a schema named "audit"
CREATE schema IF NOT EXISTS audit;
REVOKE CREATE ON schema audit FROM public;

ALTER TABLE audit.logged_actions
	ADD COLUMN IF NOT EXISTS modifier_id varchar(256);
	
CREATE TABLE IF NOT EXISTS audit.transaction_to_modifier(
	transaction_id BIGINT NOT NULL DEFAULT txid_current(),
	modifier_id varchar(256) NOT NULL
);

REVOKE ALL ON audit.logged_actions FROM public;
REVOKE ALL ON audit.transaction_to_modifier FROM public;

GRANT SELECT ON audit.logged_actions TO public;
GRANT SELECT ON audit.transaction_to_modifier TO public;
 
--
-- Now, define the actual trigger function:
--
CREATE OR REPLACE FUNCTION audit.if_modified_func() RETURNS TRIGGER AS $body$
DECLARE
    v_old_data jsonb;
    v_new_data jsonb;
    modifier_id varchar(256);
BEGIN
    /*  If this actually for real auditing (where you need to log EVERY action),
        then you would need to use something like dblink or plperl that could log outside the transaction,
        regardless of whether the transaction committed or rolled back.
    */
 
    /* This dance with casting the NEW and OLD values to a ROW is not necessary in pg 9.0+ */
	
	 select ttm.modifier_id into modifier_id from audit.transaction_to_modifier as ttm where transaction_id = txid_current();
 
    IF (TG_OP = 'UPDATE') THEN
        v_old_data := to_jsonb(OLD);
        v_new_data := to_jsonb(NEW);
        INSERT INTO audit.logged_actions (schema_name,table_name,user_name,action,original_data,new_data,query,modifier_id) 
        VALUES (TG_TABLE_SCHEMA::TEXT,TG_TABLE_NAME::TEXT,current_user::TEXT,substring(TG_OP,1,1),v_old_data,v_new_data, current_query(), modifier_id);
        RETURN NEW;
    ELSIF (TG_OP = 'DELETE') THEN
        v_old_data := to_jsonb(OLD);
        INSERT INTO audit.logged_actions (schema_name,table_name,user_name,action,original_data,query,modifier_id)
        VALUES (TG_TABLE_SCHEMA::TEXT,TG_TABLE_NAME::TEXT,current_user::TEXT,substring(TG_OP,1,1),v_old_data, current_query(), modifier_id);
        RETURN OLD;
    ELSIF (TG_OP = 'INSERT') THEN
        v_new_data := to_jsonb(NEW);
        INSERT INTO audit.logged_actions (schema_name,table_name,user_name,action,new_data,query,modifier_id)
        VALUES (TG_TABLE_SCHEMA::TEXT,TG_TABLE_NAME::TEXT,current_user::TEXT,substring(TG_OP,1,1),v_new_data, current_query(), modifier_id);
        RETURN NEW;
    ELSE
        RAISE WARNING '[AUDIT.IF_MODIFIED_FUNC] - Other action occurred: %, at %',TG_OP,now();
        RETURN NULL;
    END IF;
 
EXCEPTION
    WHEN data_exception THEN
        RAISE WARNING '[AUDIT.IF_MODIFIED_FUNC] - UDF ERROR [DATA EXCEPTION] - SQLSTATE: %, SQLERRM: %',SQLSTATE,SQLERRM;
        RETURN NULL;
    WHEN unique_violation THEN
        RAISE WARNING '[AUDIT.IF_MODIFIED_FUNC] - UDF ERROR [UNIQUE] - SQLSTATE: %, SQLERRM: %',SQLSTATE,SQLERRM;
        RETURN NULL;
    WHEN OTHERS THEN
        RAISE WARNING '[AUDIT.IF_MODIFIED_FUNC] - UDF ERROR [OTHER] - SQLSTATE: %, SQLERRM: %',SQLSTATE,SQLERRM;
        RETURN NULL;
END;
$body$
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = pg_catalog, audit;