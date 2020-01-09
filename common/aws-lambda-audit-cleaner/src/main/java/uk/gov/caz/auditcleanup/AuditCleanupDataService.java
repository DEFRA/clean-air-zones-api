package uk.gov.caz.auditcleanup;

import static com.google.common.base.Preconditions.checkArgument;

import java.time.LocalDate;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AuditCleanupDataService {

  private final AuditPostgresRepository auditPostgresRepository;
  private final int cleanupDays;

  AuditCleanupDataService(AuditPostgresRepository auditPostgresRepository, int cleanupDays) {
    checkArgument(cleanupDays > 0, "Cleanup days must be greater than 0");
    this.auditPostgresRepository = auditPostgresRepository;
    this.cleanupDays = cleanupDays;
  }

  /**
   * Removes audit events from DB that are older than configured days.
   */
  public void cleanupOldAuditData() {
    log.info("Removing old audit data older than {} days", cleanupDays);
    auditPostgresRepository.removeAuditEventsBeforeDate(
        LocalDate.now().minusDays(cleanupDays)
    );
  }
}