package uk.gov.caz.whitelist.service.audit;

import java.time.LocalDate;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import uk.gov.caz.whitelist.repository.audit.WhitelistLoggedActionRepository;

@Service
@Slf4j
public class WhitelistDataCleanupService {
  private final WhitelistLoggedActionRepository repository;
  private final int cleanupMonths;

  public WhitelistDataCleanupService(WhitelistLoggedActionRepository repository,
      @Value("${services.audit.logged-actions-cleanup:18}") int cleanupMonths) {
    this.repository = repository;
    this.cleanupMonths = cleanupMonths;
  }

  /**
  * clean up old log data before a given date.
  */
  public void cleanupData() {
    log.info("WhitelistDataCleanupService start cleaning up"
        + " logged action records older than {} months", cleanupMonths);
    try {
      repository.deleteLogsBeforeDate(
          LocalDate.now().minusMonths(cleanupMonths));
      log.info("WhitelistDataCleanupService cleanup finished sucessfully");
    } catch (Exception ex) {
      log.info("WhitelistDataCleanupService cleanup failed due to {}", ex.getMessage());
      throw ex;
    }
  }
}