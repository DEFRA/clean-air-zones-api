CREATE TABLE table_for_audit_test(name VARCHAR(32) NOT NULL, vrn VARCHAR(7) NOT NULL);

CREATE TRIGGER table_for_audit_test_trigger
AFTER INSERT OR UPDATE OR DELETE ON table_for_audit_test
FOR EACH ROW EXECUTE PROCEDURE audit.if_modified_func();
