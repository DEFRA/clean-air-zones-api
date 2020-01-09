package uk.gov.caz.auditcleanup;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.assertj.core.api.Java6Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doNothing;

import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuditCleanupDataServiceTest {

  public static final int CLEANUP_DAYS = 10;

  @Mock
  private AuditPostgresRepository auditPostgresRepository;

  private AuditCleanupDataService auditCleanupDataService;

  @Captor
  private ArgumentCaptor<LocalDate> localDateArgumentCaptor;

  @BeforeEach
  public void setup() {
    auditCleanupDataService = new AuditCleanupDataService(
        auditPostgresRepository,
        CLEANUP_DAYS
    );
  }

  @Test
  public void shouldNotAllowCreatingSutObjectWithCleanupDaysLessThanOne() {
    assertThatThrownBy(() -> new AuditCleanupDataService(auditPostgresRepository, 0))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Cleanup days must be greater than 0");
  }

  @Test
  public void shouldCallRepositoryWithDateInThePast() {
    //given
    doNothing().when(auditPostgresRepository)
        .removeAuditEventsBeforeDate(localDateArgumentCaptor.capture());

    //when
    auditCleanupDataService.cleanupOldAuditData();

    //then
    assertThat(localDateArgumentCaptor.getValue()).matches(localDate -> {
      LocalDate nowMinusCleanupDays = LocalDate.now().minusDays(CLEANUP_DAYS);

      return localDate.getDayOfMonth() == nowMinusCleanupDays.getDayOfMonth()
          && localDate.getMonthValue() == nowMinusCleanupDays.getMonthValue()
          && localDate.getYear() == nowMinusCleanupDays.getYear();
    });
  }
}