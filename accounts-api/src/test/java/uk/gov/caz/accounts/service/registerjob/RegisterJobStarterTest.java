package uk.gov.caz.accounts.service.registerjob;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

import com.google.common.collect.ImmutableList;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.caz.accounts.model.User;
import uk.gov.caz.accounts.model.registerjob.RegisterJobName;
import uk.gov.caz.accounts.model.registerjob.RegisterJobStatus;
import uk.gov.caz.accounts.repository.AccountUserRepository;
import uk.gov.caz.accounts.repository.RegisterJobRepository;
import uk.gov.caz.accounts.service.exception.FatalErrorWithCsvFileMetadataException;
import uk.gov.caz.accounts.service.registerjob.RegisterJobStarter.InitialJobParams;
import uk.gov.caz.accounts.service.registerjob.exception.ActiveJobsCountExceededException;
import uk.gov.caz.accounts.util.TestObjects;

@ExtendWith(MockitoExtension.class)
class RegisterJobStarterTest {

  private static final UUID ANY_ACCOUNT_USER_ID = UUID.fromString("525afaf8-7e63-40a8-9785-e047991d78bf");
  private static final UUID ANY_ACCOUNT_ID = UUID.fromString("c5d7fa7c-11e4-4fca-ac92-3f7d0a23ddfe");

  @Mock
  private CsvFileOnS3MetadataExtractor csvFileOnS3MetadataExtractor;

  @Mock
  private RegisterJobSupervisor registerJobSupervisor;

  @Mock
  private AsyncBackgroundJobStarter asyncBackgroundJobStarter;

  @Mock
  private RegisterJobRepository registerJobRepository;

  @Mock
  private AccountUserRepository accountUserRepository;

  @InjectMocks
  private RegisterJobStarter registerJobStarter;

  @Test
  public void shouldRethrowExceptionFromS3MetadataExtractorUponItsFailure() {
    // given
    String s3Bucket = "some-bucket";
    String filename = "some-filename";
    mockCsvMetadataExtractorThrowsMetadataException(s3Bucket, filename);
    InitialJobParams initialJobParams = buildJobParams(s3Bucket, filename);

    // when
    Throwable throwable = catchThrowable(() -> registerJobStarter.start(initialJobParams));

    // then
    assertThat(throwable).isInstanceOf(FatalErrorWithCsvFileMetadataException.class);
  }

  @Test
  public void shouldCallJobSupervisor() {
    // given
    String s3Bucket = "some-bucket";
    String filename = "some-filename";
    mockValidAccountUserIdExtraction(s3Bucket, filename);
    mockUserPresenceInDatabase();
    given(registerJobSupervisor.start(any())).willReturn(new RegisterJobName("some-name"));
    InitialJobParams initialJobParams = buildJobParams(s3Bucket, filename);

    // when
    RegisterJobName jobName = registerJobStarter.start(initialJobParams);

    // then
    assertThat(jobName).isEqualTo(new RegisterJobName("some-name"));
  }

  private void mockValidAccountUserIdExtraction(String s3Bucket, String filename) {
    given(csvFileOnS3MetadataExtractor.getAccountUserId(s3Bucket, filename))
        .willReturn(ANY_ACCOUNT_USER_ID);
  }

  private void mockUserPresenceInDatabase() {
    User user = User.builder()
        .accountId(ANY_ACCOUNT_ID)
        .id(ANY_ACCOUNT_USER_ID)
        .build();
    given(accountUserRepository.findById(ANY_ACCOUNT_USER_ID)).willReturn(Optional.of(user));
  }

  @Nested
  class WhenThereIsUnfinishedJobForTheSameAccount {

    @Test
    public void shouldThrowActiveJobsCountExceededException() {
      // given
      String s3Bucket = "some-bucket";
      String filename = "some-filename";
      mockValidAccountUserIdExtraction(s3Bucket, filename);
      mockUserPresenceInDatabase();
      mockActiveJobsCountExceededExceptionThrown();
      InitialJobParams initialJobParams = buildJobParams(s3Bucket, filename);

      // when
      Throwable throwable = catchThrowable(() -> registerJobStarter.start(initialJobParams));

      // then
      assertThat(throwable).isInstanceOf(ActiveJobsCountExceededException.class);
      verifyNoInteractions(registerJobSupervisor);
    }

    private void mockActiveJobsCountExceededExceptionThrown() {
      given(registerJobRepository.countAllByUploaderIdAndStatusIn(ANY_ACCOUNT_ID,
          ImmutableList.of(RegisterJobStatus.STARTING, RegisterJobStatus.RUNNING)))
          .willThrow(ActiveJobsCountExceededException.class);
    }
  }

  @Nested
  class WhenThereIsNoUserInDatabaseForItProvidedByFileMetadata {

    @Test
    public void shouldThrowIllegalStateException() {
      // given
      String s3Bucket = "some-bucket";
      String filename = "some-filename";
      mockValidAccountUserIdExtraction(s3Bucket, filename);
      mockUserAbsenceInDb();
      InitialJobParams initialJobParams = buildJobParams(s3Bucket, filename);

      // when
      Throwable throwable = catchThrowable(() -> registerJobStarter.start(initialJobParams));

      // then
      assertThat(throwable).isInstanceOf(IllegalStateException.class);
      verifyNoInteractions(registerJobSupervisor);
    }

    private void mockUserAbsenceInDb() {
      given(accountUserRepository.findById(ANY_ACCOUNT_USER_ID)).willReturn(Optional.empty());
    }
  }

  private void mockCsvMetadataExtractorThrowsMetadataException(String s3Bucket, String filename) {
    given(csvFileOnS3MetadataExtractor.getAccountUserId(s3Bucket, filename)).willThrow(new FatalErrorWithCsvFileMetadataException(""));
  }

  private InitialJobParams buildJobParams(String s3Bucket, String filename) {
    return InitialJobParams.builder()
        .correlationId(TestObjects.TYPICAL_CORRELATION_ID)
        .s3Bucket(s3Bucket)
        .filename(filename)
        .build();
  }

}