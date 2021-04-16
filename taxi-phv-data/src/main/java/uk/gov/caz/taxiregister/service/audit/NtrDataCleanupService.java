package uk.gov.caz.taxiregister.service.audit;

import java.time.LocalDate;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import uk.gov.caz.taxiregister.repository.audit.NtrLoggedActionRepository;

@Service
@Slf4j
public class NtrDataCleanupService {
  private final NtrLoggedActionRepository repository;
  private final int cleanupMonths;

  public NtrDataCleanupService(NtrLoggedActionRepository repository,
      @Value("${services.audit.logged-actions-cleanup:18}") int cleanupMonths) {
    this.repository = repository;
    this.cleanupMonths = cleanupMonths;
  }

  /**
  * clean up old log data before a given date.
  */
  public void cleanupData() {
    log.info("NtrDataCleanupService start cleaning up"
        + " logged action records older than {} months", cleanupMonths);
    try {
      repository.deleteLogsBeforeDate(
          LocalDate.now().minusMonths(cleanupMonths));
      log.info("NtrDataCleanupService cleanup finished sucessfully");
    } catch (Exception ex) {
      log.info("NtrDataCleanupService cleanup failed due to {}", ex.getMessage());
      throw ex;
    }
  }
}