package uk.gov.caz.auditcleanup;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Setter
@Getter
@ConfigurationProperties(prefix = "uk.gov.caz.auditcleanup")
public class AuditCleanupProperties {
  private int days = 2555;
}