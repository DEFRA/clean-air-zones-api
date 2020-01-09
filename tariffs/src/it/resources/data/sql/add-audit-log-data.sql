CREATE TABLE table_for_audit_test(caz_class CHAR(1) NOT NULL, caz_class_desc TEXT NOT NULL);

CREATE TRIGGER table_for_audit_test_trigger
AFTER INSERT OR UPDATE OR DELETE ON table_for_audit_test
FOR EACH ROW EXECUTE PROCEDURE audit.if_modified_func();