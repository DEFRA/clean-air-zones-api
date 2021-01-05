-- move and rename table
ALTER TABLE audit.logged_actions RENAME TO t_logged_actions;
ALTER TABLE audit.t_logged_actions SET SCHEMA caz_account_audit;

-- delete old and create indexes
DROP INDEX IF EXISTS logged_actions_schema_table_idx;
DROP INDEX IF EXISTS logged_actions_action_tstamp_idx;
DROP INDEX IF EXISTS logged_actions_action_idx;

CREATE INDEX t_logged_actions_schema_table_idx
ON caz_account_audit.t_logged_actions(((schema_name||'.'||TABLE_NAME)::TEXT));

CREATE INDEX t_logged_actions_action_tstamp_idx
ON caz_account_audit.t_logged_actions(action_tstamp);

CREATE INDEX t_logged_actions_action_idx
ON caz_account_audit.t_logged_actions(action);

--
-- Now, define the actual trigger function:
--
CREATE OR REPLACE FUNCTION caz_account_audit.if_modified_func() RETURNS TRIGGER AS $body$
DECLARE
    v_old_data jsonb;
    v_new_data jsonb;
BEGIN
    /*  If this actually for real auditing (where you need to log EVERY action),
        then you would need to use something like dblink or plperl that could log outside the transaction,
        regardless of whether the transaction committed or rolled back.
    */

    /* This dance with casting the NEW and OLD values to a ROW is not necessary in pg 9.0+ */

    IF (TG_OP = 'UPDATE') THEN
        v_old_data := to_jsonb(OLD);
        v_new_data := to_jsonb(NEW);
        INSERT INTO caz_account_audit.t_logged_actions (schema_name,table_name,user_name,action,original_data,new_data,query)
        VALUES (TG_TABLE_SCHEMA::TEXT,TG_TABLE_NAME::TEXT,session_user::TEXT,substring(TG_OP,1,1),v_old_data,v_new_data, current_query());
        RETURN NEW;
    ELSIF (TG_OP = 'DELETE') THEN
        v_old_data := to_jsonb(OLD);
        INSERT INTO caz_account_audit.t_logged_actions (schema_name,table_name,user_name,action,original_data,query)
        VALUES (TG_TABLE_SCHEMA::TEXT,TG_TABLE_NAME::TEXT,session_user::TEXT,substring(TG_OP,1,1),v_old_data, current_query());
        RETURN OLD;
    ELSIF (TG_OP = 'INSERT') THEN
        v_new_data := to_jsonb(NEW);
        INSERT INTO caz_account_audit.t_logged_actions (schema_name,table_name,user_name,action,new_data,query)
        VALUES (TG_TABLE_SCHEMA::TEXT,TG_TABLE_NAME::TEXT,session_user::TEXT,substring(TG_OP,1,1),v_new_data, current_query());
        RETURN NEW;
    ELSE
        RAISE WARNING '[CAZ_ACCOUNT_AUDIT.IF_MODIFIED_FUNC] - Other action occurred: %, at %',TG_OP,now();
        RETURN NULL;
    END IF;

EXCEPTION
    WHEN data_exception THEN
        RAISE WARNING '[CAZ_ACCOUNT_AUDIT.IF_MODIFIED_FUNC] - UDF ERROR [DATA EXCEPTION] - SQLSTATE: %, SQLERRM: %',SQLSTATE,SQLERRM;
        RETURN NULL;
    WHEN unique_violation THEN
        RAISE WARNING '[CAZ_ACCOUNT_AUDIT.IF_MODIFIED_FUNC] - UDF ERROR [UNIQUE] - SQLSTATE: %, SQLERRM: %',SQLSTATE,SQLERRM;
        RETURN NULL;
    WHEN OTHERS THEN
        RAISE WARNING '[CAZ_ACCOUNT_AUDIT.IF_MODIFIED_FUNC] - UDF ERROR [OTHER] - SQLSTATE: %, SQLERRM: %',SQLSTATE,SQLERRM;
        RETURN NULL;
END;
$body$
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = pg_catalog, caz_account_audit;

-- Delete old triggers
DROP TRIGGER IF EXISTS ACCOUNT_AUDIT ON CAZ_ACCOUNT.T_ACCOUNT;
DROP TRIGGER IF EXISTS ACCOUNT_USER_AUDIT ON CAZ_ACCOUNT.T_ACCOUNT_USER;
DROP TRIGGER IF EXISTS ACCOUNT_VEHICLE_AUDIT ON CAZ_ACCOUNT.T_ACCOUNT_VEHICLE;
DROP TRIGGER IF EXISTS DIRECT_DEBIT_MANDATE_AUDIT ON CAZ_ACCOUNT.T_ACCOUNT_DIRECT_DEBIT_MANDATE;
DROP TRIGGER IF EXISTS T_ACCOUNT_JOB_REGISTER_AUDIT ON CAZ_ACCOUNT.T_ACCOUNT_JOB_REGISTER;

-- Create triggers
CREATE TRIGGER T_ACCOUNT_AUDIT
AFTER INSERT OR UPDATE OR DELETE ON CAZ_ACCOUNT.T_ACCOUNT
FOR EACH ROW EXECUTE PROCEDURE caz_account_audit.if_modified_func();

CREATE TRIGGER T_ACCOUNT_USER_AUDIT
AFTER INSERT OR UPDATE OR DELETE ON CAZ_ACCOUNT.T_ACCOUNT_USER
FOR EACH ROW EXECUTE PROCEDURE caz_account_audit.if_modified_func();

CREATE TRIGGER T_ACCOUNT_VEHICLE_AUDIT
AFTER INSERT OR UPDATE OR DELETE ON CAZ_ACCOUNT.T_ACCOUNT_VEHICLE
FOR EACH ROW EXECUTE PROCEDURE caz_account_audit.if_modified_func();

CREATE TRIGGER T_ACCOUNT_DIRECT_DEBIT_MANDATE_AUDIT
AFTER INSERT OR UPDATE OR DELETE ON CAZ_ACCOUNT.T_ACCOUNT_DIRECT_DEBIT_MANDATE
FOR EACH ROW EXECUTE PROCEDURE caz_account_audit.if_modified_func();

CREATE TRIGGER T_ACCOUNT_JOB_REGISTER_AUDIT
AFTER INSERT OR UPDATE OR DELETE ON CAZ_ACCOUNT.T_ACCOUNT_JOB_REGISTER
FOR EACH ROW EXECUTE PROCEDURE caz_account_audit.if_modified_func();