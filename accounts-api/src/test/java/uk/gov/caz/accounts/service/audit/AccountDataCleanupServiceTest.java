package uk.gov.caz.accounts.service.audit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import uk.gov.caz.accounts.repository.audit.AccountLoggedActionRepository;
import uk.gov.caz.accounts.service.audit.AccountDataCleanupService;

@ExtendWith(MockitoExtension.class)
public class AccountDataCleanupServiceTest {

  @Mock
  private AccountLoggedActionRepository repository;

  private AccountDataCleanupService service;

  @Test
  public void shouldThrowExceptionWhenRepositoryFailToExecuteSQLStatement() {
    // given
    when(repository.deleteLogsBeforeDate(any(LocalDate.class))).thenThrow(RuntimeException.class);
    service = new AccountDataCleanupService(repository, 12);
    // then
    Assertions.assertThrows(RuntimeException.class, () -> {
      service.cleanupData();
    });
  }
}