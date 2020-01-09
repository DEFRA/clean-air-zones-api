package uk.gov.caz.taxiregister.service;

import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static uk.gov.caz.testutils.NtrAssertions.assertThat;
import static uk.gov.caz.testutils.TestObjects.S3_REGISTER_JOB_ID;
import static uk.gov.caz.testutils.TestObjects.S3_REGISTER_JOB_NAME;
import static uk.gov.caz.testutils.TestObjects.S3_REGISTER_JOB_TRIGGER;
import static uk.gov.caz.testutils.TestObjects.TYPICAL_CORRELATION_ID;
import static uk.gov.caz.testutils.TestObjects.TYPICAL_REGISTER_JOB_UPLOADER_ID;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import uk.gov.caz.taxiregister.model.LicensingAuthority;
import uk.gov.caz.taxiregister.model.ValidationError;
import uk.gov.caz.taxiregister.model.registerjob.RegisterJob;
import uk.gov.caz.taxiregister.model.registerjob.RegisterJobError;
import uk.gov.caz.taxiregister.model.registerjob.RegisterJobName;
import uk.gov.caz.taxiregister.model.registerjob.RegisterJobStatus;
import uk.gov.caz.taxiregister.repository.LicensingAuthorityPostgresRepository;
import uk.gov.caz.taxiregister.repository.RegisterJobInfoRepository;
import uk.gov.caz.taxiregister.repository.RegisterJobRepository;
import uk.gov.caz.taxiregister.service.RegisterJobSupervisor.StartParams;
import uk.gov.caz.taxiregister.service.exception.JobNameDuplicateException;
import uk.gov.caz.testutils.TestObjects.LicensingAuthorities;

@ExtendWith(MockitoExtension.class)
class RegisterJobSupervisorTest {

  private static final String CSV_FILE = "csv-file";

  @Mock
  private LicensingAuthorityPostgresRepository mockedLicensingAuthorityRepository;

  @Mock
  private RegisterJobRepository mockedRegisterJobRepository;

  @Mock
  private RegisterJobInfoRepository mockedRegisterJobInfoRepository;

  @Mock
  private RegisterJobNameGenerator mockedRegisterJobNameGenerator;

  @InjectMocks
  private RegisterJobSupervisor registerJobSupervisor;

  @Captor
  private ArgumentCaptor<RegisterJob> registerJobArgumentCaptor;

  @Captor
  private ArgumentCaptor<Set<LicensingAuthority>> licensingAuthoritySetArgumentCaptor;
  
  @Captor
  private ArgumentCaptor<List<RegisterJobError>> registerJobErrorsArgumentCaptor;

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
    assertThat(capturedRegisterJob)
        .matchesAttributesOfTypicalStartingRegisterJob();
  }

  @Test
  public void testUpdateStatus() {
    // when
    registerJobSupervisor
        .updateStatus(S3_REGISTER_JOB_ID, RegisterJobStatus.FINISHED_SUCCESS);

    // then
    verify(mockedRegisterJobRepository)
        .updateStatus(S3_REGISTER_JOB_ID, RegisterJobStatus.FINISHED_SUCCESS);
  }

  @Test
  public void testUpdateErrorsOrdering() {
    // given
    int firstErrorLineNo = 100;
    int secondErrorLineNo = 90;
    int thirdErrorLineNo = 95;
    List<ValidationError> validationErrors = IntStream.builder()
        .add(firstErrorLineNo)
        .add(secondErrorLineNo)
        .add(thirdErrorLineNo).build()
        .mapToObj(lineNo -> ValidationError.valueError("vrm", "some error", lineNo))
        .collect(Collectors.toList());

    // when
    registerJobSupervisor.markFailureWithValidationErrors(S3_REGISTER_JOB_ID,
            RegisterJobStatus.FINISHED_FAILURE_VALIDATION_ERRORS, validationErrors);

    // then
    verify(mockedRegisterJobRepository)
        .updateErrors(eq(S3_REGISTER_JOB_ID), registerJobErrorsArgumentCaptor.capture());
    assertThat(registerJobErrorsArgumentCaptor.getValue()).hasSameSizeAs(validationErrors);
    assertThat(getJobErrorsDetailByIndex(0)).startsWith("Line " + secondErrorLineNo);
    assertThat(getJobErrorsDetailByIndex(1)).startsWith("Line " + thirdErrorLineNo);
    assertThat(getJobErrorsDetailByIndex(2)).startsWith("Line " + firstErrorLineNo);
  }

  private String getJobErrorsDetailByIndex(int i) {
    return registerJobErrorsArgumentCaptor.getValue().get(i).getDetail();
  }

  @Test
  public void testMarkSuccessfullyFinished() {
    // given
    int registerJobId = 67;
    Set<LicensingAuthority> licensingAuthorities = LicensingAuthorities.existingAsSingleton();

    // when
    registerJobSupervisor.markSuccessfullyFinished(registerJobId, licensingAuthorities);

    // then
    verify(mockedRegisterJobRepository).updateStatus(registerJobId,
        RegisterJobStatus.FINISHED_SUCCESS);
    verify(mockedRegisterJobInfoRepository).insert(registerJobId, licensingAuthorities);
  }

  @Test
  public void isRunningOrStartingJob() {
    // given
    Set<LicensingAuthority> set = LicensingAuthorities.existingAsSingleton();
    // when
    registerJobSupervisor.hasActiveJobs(set);
    // then
    verify(mockedRegisterJobRepository)
        .countActiveJobsBy(licensingAuthoritySetArgumentCaptor.capture());
    assertThat(licensingAuthoritySetArgumentCaptor.getValue().equals(set));
  }
  
  @Test
  public void testFindByName() {
    // when
    registerJobSupervisor.findJobWithName(new RegisterJobName(S3_REGISTER_JOB_NAME));

    // then
    verify(mockedRegisterJobRepository).findByName(S3_REGISTER_JOB_NAME);
  }

  
  @Test
  public void testFindById() {
    // when
    registerJobSupervisor.findJobById(1);

    // then
    verify(mockedRegisterJobRepository).findById(1);
  }
  
  @Test
  public void testMarkFailureWithValidationErrors() {
    // given
    RegisterJobStatus jobStatus = RegisterJobStatus.STARTUP_FAILURE_NO_ACCESS_TO_S3;
    ValidationError internalError = ValidationError.internal();
    ValidationError unknownError = ValidationError.unknown();
    List<ValidationError> validationErrors = Arrays.asList(internalError, unknownError);
    List<RegisterJobError> errors = Stream.of(internalError, unknownError)
        .map(RegisterJobError::from).collect(Collectors.toList());

    // when
    registerJobSupervisor.markFailureWithValidationErrors(S3_REGISTER_JOB_ID,
        jobStatus, validationErrors);

    // then
    verify(mockedRegisterJobRepository).updateStatus(S3_REGISTER_JOB_ID, jobStatus);
    verify(mockedRegisterJobRepository).updateErrors(S3_REGISTER_JOB_ID, errors);
  }

  @Test
  public void shouldThrowDuplicateKeyExceptionWhenThereIsAlreadyJobNameInDB() {
    // given
    prepareMockForNameGeneration();
    prepareMockForThrowDuplicateKeyException();
    AtomicBoolean capturedJobStarted = new AtomicBoolean(false);
    AtomicInteger capturedRegisterJobId = new AtomicInteger();

    // when
    StartParams startParams = prepareStartParams(capturedJobStarted, capturedRegisterJobId);
    Throwable throwable = catchThrowable(() -> registerJobSupervisor.start(startParams));

    // then
    assertThat(throwable).isInstanceOf(JobNameDuplicateException.class);
    verifyZeroInteractions(mockedRegisterJobRepository);
  }

  @Nested
  class activeJobs {

    @Test
    public void testHasActiveJobsIsFalseWhenNoJobsArePresent() {
      // given
      given(
          mockedRegisterJobRepository.countActiveJobsBy(ArgumentMatchers.<Set<LicensingAuthority>>any()))
          .willReturn(null);
      // when
      boolean areThereActiveJobs = registerJobSupervisor
          .hasActiveJobs(LicensingAuthorities.existingAsSingleton());

      // then
      assertThat(areThereActiveJobs).isFalse();
    }

    @Test
    public void testHasActiveJobsIsFalseWhenAllJobsAreFinished() {
      // given
      given(
          mockedRegisterJobRepository.countActiveJobsBy(ArgumentMatchers.<Set<LicensingAuthority>>any()))
          .willReturn(0);
     
      // when
      boolean areThereActiveJobs = registerJobSupervisor
          .hasActiveJobs(LicensingAuthorities.existingAsSingleton());

      // then
      assertThat(areThereActiveJobs).isFalse();
    }

    @Test
    public void testHasActiveJobsIsTrueWhenAtLeastOneJobHasNotFinished() {
      // given
      given(
          mockedRegisterJobRepository.countActiveJobsBy(ArgumentMatchers.<Set<LicensingAuthority>>any()))
          .willReturn(1);

      // when
      boolean areThereActiveJobs = registerJobSupervisor
          .hasActiveJobs(LicensingAuthorities.existingAsSingleton());

      // then
      assertThat(areThereActiveJobs).isTrue();
    }
  }

  private StartParams prepareStartParams(AtomicBoolean capturedJobStarted,
      AtomicInteger capturedRegisterJobId) {
    return StartParams.builder()
        .registerJobTrigger(S3_REGISTER_JOB_TRIGGER)
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
    prepareMockForNameGeneration();
    given(mockedRegisterJobRepository.insert(any(RegisterJob.class)))
        .willReturn(S3_REGISTER_JOB_ID);
  }

  private void prepareMockForNameGeneration() {
    given(mockedRegisterJobNameGenerator.generate(CSV_FILE, S3_REGISTER_JOB_TRIGGER))
        .willReturn(new RegisterJobName(S3_REGISTER_JOB_NAME));
  }

  private void prepareMockForThrowDuplicateKeyException() {
    given(mockedRegisterJobRepository.insert(any(RegisterJob.class)))
        .willThrow(new JobNameDuplicateException("There is already job with given name"));
  }

  private RegisterJob verifyThatNewRegisterJobWasInsertedIntoRepositoryAndCaptureIt() {
    verify(mockedRegisterJobRepository).insert(registerJobArgumentCaptor.capture());
    return registerJobArgumentCaptor.getValue();
  }
}
