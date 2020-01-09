package uk.gov.caz.versionlogger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.info.GitProperties;

@ExtendWith(MockitoExtension.class)
class ServiceVersionLoggerTest {

  @Mock
  private GitProperties gitProperties;

  @Test
  public void shouldCallGetCommitIdForNonNullBean() {
    // given
    Mockito.when(gitProperties.getCommitId()).thenReturn("00ff");
    ServiceVersionLogger versionLogger = new ServiceVersionLogger(gitProperties);

    // when
    versionLogger.logProjectVersion();

    // then
    Mockito.verify(gitProperties).getCommitId();
  }

  @Test
  public void shouldNotCallGetCommitIdForNullBean() {
    // given
    ServiceVersionLogger versionLogger = new ServiceVersionLogger(null);

    // when
    versionLogger.logProjectVersion();

    // then
    Mockito.verifyZeroInteractions(gitProperties);
  }
}