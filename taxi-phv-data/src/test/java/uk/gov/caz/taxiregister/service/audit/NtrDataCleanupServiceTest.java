package uk.gov.caz.taxiregister.service.audit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import uk.gov.caz.taxiregister.repository.audit.NtrLoggedActionRepository;

@ExtendWith(MockitoExtension.class)
public class NtrDataCleanupServiceTest {
  @Mock
  private NtrLoggedActionRepository repository;

  private NtrDataCleanupService service;

  @Test
  public void shouldThrowExceptionWhenRepositoryFailToExecuteSQLStatement() {
    // given
    when(repository.deleteLogsBeforeDate(any(LocalDate.class))).thenThrow(RuntimeException.class);
    service = new NtrDataCleanupService(repository, 18);
    // then
    Assertions.assertThrows(RuntimeException.class, () -> {
      service.cleanupData();
    });
  }
}