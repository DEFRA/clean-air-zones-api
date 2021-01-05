package uk.gov.caz.accounts.service.audit;

import java.time.LocalDate;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import uk.gov.caz.accounts.repository.audit.AccountLoggedActionRepository;

@Service
@Slf4j
public class AccountDataCleanupService {
  private final AccountLoggedActionRepository repository;
  private final int cleanupMonths;

  public AccountDataCleanupService(AccountLoggedActionRepository repository,
      @Value("${services.audit.logged-actions-cleanup:12}") int cleanupMonths) {
    this.repository = repository;
    this.cleanupMonths = cleanupMonths;
  }
  
  /**
   * clean up old log data before a given date.
   */
  @Transactional
  public void cleanupData() {
    log.info("AuditDataCleanupService start cleaning up"
        + " logged action records older than {} months", cleanupMonths);
    try {
      repository.deleteLogsBeforeDate(
          LocalDate.now().minusMonths(cleanupMonths));
      log.info("AuditDataCleanupService cleanup finished sucessfully");
    } catch (Exception ex) {
      log.info("AuditDataCleanupService cleanup failed due to {}", ex.getMessage());
      throw ex;
    }
  }
}