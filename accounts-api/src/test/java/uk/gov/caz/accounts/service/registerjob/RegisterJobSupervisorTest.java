package uk.gov.caz.accounts.service.registerjob;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.caz.accounts.util.TestObjects.S3_REGISTER_JOB_ID;
import static uk.gov.caz.accounts.util.TestObjects.S3_REGISTER_JOB_NAME;
import static uk.gov.caz.accounts.util.TestObjects.S3_RUNNING_REGISTER_JOB;
import static uk.gov.caz.accounts.util.TestObjects.TYPICAL_CORRELATION_ID;
import static uk.gov.caz.accounts.util.TestObjects.TYPICAL_REGISTER_JOB_UPLOADER_ID;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.caz.accounts.model.registerjob.RegisterJob;
import uk.gov.caz.accounts.model.registerjob.RegisterJobName;
import uk.gov.caz.accounts.model.registerjob.RegisterJobStatus;
import uk.gov.caz.accounts.model.registerjob.RegisterJobTrigger;
import uk.gov.caz.accounts.model.registerjob.ValidationError;
import uk.gov.caz.accounts.repository.RegisterJobRepository;
import uk.gov.caz.accounts.service.registerjob.RegisterJobSupervisor.StartParams;

@ExtendWith(MockitoExtension.class)
class RegisterJobSupervisorTest {

  private static final String CSV_FILE = "csv-file";

  @Mock
  private RegisterJobRepository mockedRegisterJobRepository;

  @Mock
  private RegisterJobNameGenerator mockedRegisterJobNameGenerator;

  @InjectMocks
  private RegisterJobSupervisor registerJobSupervisor;

  @Captor
  private ArgumentCaptor<RegisterJob> registerJobArgumentCaptor;

  @Test
  public void testStartingAndSupervisingNewRegisterJob() {
    // given
    prepareMocksForNameGenerationAndRegisterJobInsertion();
    AtomicBoolean capturedJobStarted = new AtomicBoolean(false);
    AtomicInteger capturedRegisterJobId = new AtomicInteger();

    // when
    StartParams startParams = prepareStartParams(capturedJobStarted, capturedRegisterJobId);
    RegisterJobName registerJobName = registerJobSupervisor.start(startParams);

    // then
    assertThat(registerJobName.getValue()).isEqualTo(S3_REGISTER_JOB_NAME);
    assertThat(capturedRegisterJobId.get()).isEqualTo(S3_REGISTER_JOB_ID);
    assertThat(capturedJobStarted).isTrue();

    RegisterJob capturedRegisterJob =
        verifyThatNewRegisterJobWasInsertedIntoRepositoryAndCaptureIt();
    assertThat(capturedRegisterJob).isNotNull();
    assertThat(capturedRegisterJob.getId()).isNull();
  }

  private StartParams prepareStartParams(AtomicBoolean capturedJobStarted,
      AtomicInteger capturedRegisterJobId) {
    return StartParams.builder()
        .registerJobTrigger(RegisterJobTrigger.CSV_FROM_S3)
        .registerJobNameSuffix(CSV_FILE)
        .correlationId(TYPICAL_CORRELATION_ID)
        .uploaderId(TYPICAL_REGISTER_JOB_UPLOADER_ID)
        .registerJobInvoker(registerJobId -> {
          capturedJobStarted.set(true);
          capturedRegisterJobId.set(registerJobId);
        })
        .build();
  }

  private void prepareMocksForNameGenerationAndRegisterJobInsertion() {
    given(mockedRegisterJobNameGenerator.generate(CSV_FILE, RegisterJobTrigger.CSV_FROM_S3))
        .willReturn(new RegisterJobName(S3_REGISTER_JOB_NAME));
    given(mockedRegisterJobRepository.save(any(RegisterJob.class)))
        .willAnswer(answer -> {
          RegisterJob registerJob = answer.getArgument(0);
          return registerJob.toBuilder()
              .id(S3_REGISTER_JOB_ID)
              .build();
        });
  }

  private RegisterJob verifyThatNewRegisterJobWasInsertedIntoRepositoryAndCaptureIt() {
    verify(mockedRegisterJobRepository).save(registerJobArgumentCaptor.capture());
    return registerJobArgumentCaptor.getValue();
  }

  @Test
  public void testUpdateStatus() {
    // given
    RegisterJob job = S3_RUNNING_REGISTER_JOB.toBuilder().build();
    when(mockedRegisterJobRepository.findById(any())).thenReturn(Optional.of(job));

    // when
    RegisterJob updatedJob = registerJobSupervisor
        .updateStatus(S3_REGISTER_JOB_ID, RegisterJobStatus.FINISHED_SUCCESS);

    // then
    assertThat(updatedJob.getStatus()).isEqualTo(RegisterJobStatus.FINISHED_SUCCESS);
  }

  @Nested
  class MarkFailureWithValidationErrors {

    @Test
    public void shouldUpdateRegisteredJob() {
      // given
      RegisterJobStatus jobStatus = RegisterJobStatus.STARTUP_FAILURE_NO_ACCESS_TO_S3;
      ValidationError internalError = ValidationError.internal();
      ValidationError unknownError = ValidationError.unknown();
      List<ValidationError> validationErrors = Arrays.asList(internalError, unknownError);

      when(mockedRegisterJobRepository.findById(S3_REGISTER_JOB_ID))
          .thenReturn(Optional.of(S3_RUNNING_REGISTER_JOB));

      // when
      RegisterJob registerJob = registerJobSupervisor.markFailureWithValidationErrors(
          S3_REGISTER_JOB_ID, jobStatus, validationErrors);

      // then
      assertThat(registerJob.getErrors()).isNotNull();
      assertThat(registerJob.getErrors().getErrors()).hasSameSizeAs(validationErrors);
      assertThat(registerJob.getStatus()).isEqualTo(jobStatus);
    }

    @Test
    public void shouldThrowIllegalStateExceptionWhenJobIsNotFound() {
      // given
      int registerJobId = S3_REGISTER_JOB_ID;
      mockJobAbsenceInDatabase(registerJobId);
      RegisterJobStatus jobStatus = RegisterJobStatus.STARTUP_FAILURE_NO_ACCESS_TO_S3;
      List<ValidationError> validationErrors = Collections.emptyList();

      // when
      Throwable throwable = catchThrowable(() ->
          registerJobSupervisor.markFailureWithValidationErrors(S3_REGISTER_JOB_ID, jobStatus,
              validationErrors));

      // then
      assertThat(throwable).isInstanceOf(IllegalStateException.class)
          .hasMessage("Job (id: " + registerJobId + ") not found");
    }

    private void mockJobAbsenceInDatabase(int registerJobId) {
      given(mockedRegisterJobRepository.findById(registerJobId)).willReturn(Optional.empty());
    }
  }
}
