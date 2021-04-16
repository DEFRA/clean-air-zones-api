package uk.gov.caz.vcc.service.audit;

import java.time.LocalDate;
import javax.transaction.Transactional;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import uk.gov.caz.vcc.repository.IdentificationErrorRepository;
import uk.gov.caz.vcc.repository.audit.VehicleEntrantLoggedActionRepository;

/**
 * Service class that removes unidentifiable vehicle data
 * from database records.
 */
@Service
@Slf4j
public class VehicleEntrantDataCleanupService {
  
  private final IdentificationErrorRepository identificationErrorRepository;
  private final VehicleEntrantLoggedActionRepository auditLoggedActionRepository;
  private final int identificationErrorCleanupDays;
  private final int loggedActionCleanupMonths;

  /**
   * Class constructor.
   * @param identificationErrorRepository instance of {@IdentificationErrorRepository}
   * @param auditLoggedActionRepository instance of {@VehicleEntrantLoggedActionRepository}
   * @param identificationErrorCleanupDays identification error record age in days
   * @param loggedActionCleanupMonths logged action record age in months
   */
  public VehicleEntrantDataCleanupService(
      IdentificationErrorRepository identificationErrorRepository,
      VehicleEntrantLoggedActionRepository auditLoggedActionRepository,
      @Value("${services.audit.vehicle-entrants-cleanup:30}") int identificationErrorCleanupDays,
      @Value("${services.audit.vehicle-entrants-logged-action-cleanup:18}")
      int loggedActionCleanupMonths) {
    this.identificationErrorRepository = identificationErrorRepository;
    this.identificationErrorCleanupDays = identificationErrorCleanupDays;
    this.auditLoggedActionRepository = auditLoggedActionRepository;
    this.loggedActionCleanupMonths = loggedActionCleanupMonths;
  }

  /**
   * Cleans up old log data before a given date.
   */
  @Transactional
  public void cleanupData() {
    try {
      cleanupIdentificationErrorAuditData();
      cleanupLoggedActionAuditData();
      log.info("VehicleEntrantLogDataCleanupService cleanup finished sucessfully");
    } catch (Exception ex) {
      log.info("VehicleEntrantLogDataCleanupService cleanup failed due to {}", ex.getMessage());
      throw ex;
    }
  }
  
  /**
   * Cleans up identification log records.
   */
  private void cleanupIdentificationErrorAuditData() {
    log.info("VehicleEntrantLogDataCleanupService started cleaning up"
        + " identification log records older than {} days", identificationErrorCleanupDays);
    identificationErrorRepository.deleteLogsBeforeDate(
        LocalDate.now().minusDays(identificationErrorCleanupDays));
  }

  /**
   * Cleans up logged actions records.
   */
  private void cleanupLoggedActionAuditData() {
    log.info("VehicleEntrantLogDataCleanupService started cleaning up"
        + " logged action records older than {} months", loggedActionCleanupMonths);
    auditLoggedActionRepository.deleteLogsBeforeDate(
        LocalDate.now().minusMonths(loggedActionCleanupMonths));
  }
}