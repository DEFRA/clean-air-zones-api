TRUNCATE caz_account_audit.t_logged_actions;

CREATE TABLE table_for_audit_test(account_name varchar(100) NOT NULL);

CREATE TRIGGER table_for_audit_test_trigger
AFTER INSERT OR UPDATE OR DELETE ON table_for_audit_test
FOR EACH ROW EXECUTE PROCEDURE caz_account_audit.if_modified_func();