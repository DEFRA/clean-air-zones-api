CREATE TABLE table_for_retrofit_audit_test(VRN VARCHAR(14) NOT NULL, VEHICLE_CATEGORY VARCHAR(40), MODEL VARCHAR(30));

CREATE TRIGGER table_for_retrofit_audit_test
AFTER INSERT OR UPDATE OR DELETE ON table_for_retrofit_audit_test
FOR EACH ROW EXECUTE PROCEDURE audit.if_modified_func();