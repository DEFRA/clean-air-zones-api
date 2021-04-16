CREATE TABLE table_for_whitelist_audit_test(VRN VARCHAR(14) NOT NULL, MANUFACTURER VARCHAR(50), REASON_UPDATED VARCHAR(50), uploader_id UUID NOT NULL);

CREATE TRIGGER table_for_whitelist_audit_test
AFTER INSERT OR UPDATE OR DELETE ON table_for_whitelist_audit_test
FOR EACH ROW EXECUTE PROCEDURE CAZ_WHITELIST_VEHICLES_AUDIT.if_modified_func();