package uk.gov.caz.auditcleanup;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

/**
 * Class that provides Spring beans.
 */
@Configuration
public class AuditCleanupConfiguration {

  @Bean
  public AuditPostgresRepository auditPostgresRepository(
      NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
    return new AuditPostgresRepository(namedParameterJdbcTemplate);
  }

  @Bean
  public AuditCleanupDataService auditCleanupDataService(
      AuditPostgresRepository auditPostgresRepository,
      AuditCleanupProperties auditCleanupProperties
  ) {
    return new AuditCleanupDataService(auditPostgresRepository, auditCleanupProperties.getDays());
  }

  @Bean
  public AuditCleanupProperties auditCleanupProperties() {
    return new AuditCleanupProperties();
  }
}
