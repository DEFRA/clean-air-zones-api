package uk.gov.caz.whitelist.service.audit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import uk.gov.caz.whitelist.repository.audit.WhitelistLoggedActionRepository;

@ExtendWith(MockitoExtension.class)
public class WhitelistDataCleanupServiceTest {

  @Mock
  private WhitelistLoggedActionRepository repository;

  private WhitelistDataCleanupService service;

  @Test
  public void shouldThrowExceptionWhenRepositoryFailToExecuteSQLStatement() {
    // given
    when(repository.deleteLogsBeforeDate(any(LocalDate.class))).thenThrow(RuntimeException.class);
    service = new WhitelistDataCleanupService(repository, 18);
    // then
    Assertions.assertThrows(RuntimeException.class, () -> {
      service.cleanupData();
    });
  }
}