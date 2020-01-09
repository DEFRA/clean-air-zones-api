package uk.gov.caz.taxiregister.controller;

import static org.hamcrest.core.IsCollectionContaining.hasItems;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.caz.taxiregister.controller.Constants.CORRELATION_ID_HEADER;
import static uk.gov.caz.testutils.NtrAssertions.assertThat;
import static uk.gov.caz.testutils.TestObjects.NOT_EXISTING_REGISTER_JOB_NAME;
import static uk.gov.caz.testutils.TestObjects.S3_REGISTER_JOB_NAME;
import static uk.gov.caz.testutils.TestObjects.S3_RUNNING_REGISTER_JOB;
import static uk.gov.caz.testutils.TestObjects.TYPICAL_CORRELATION_ID;
import static uk.gov.caz.testutils.TestObjects.TYPICAL_REGISTER_JOB_UPLOADER_ID;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import java.util.Set;

import org.hamcrest.core.IsNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.caz.taxiregister.dto.RegisterJobStatusDto;
import uk.gov.caz.taxiregister.dto.StartRegisterCsvFromS3JobCommand;
import uk.gov.caz.taxiregister.model.LicensingAuthority;
import uk.gov.caz.taxiregister.model.registerjob.RegisterJob;
import uk.gov.caz.taxiregister.model.registerjob.RegisterJobName;
import uk.gov.caz.taxiregister.model.registerjob.RegisterJobStatus;
import uk.gov.caz.taxiregister.model.registerjob.RegisterJobTrigger;
import uk.gov.caz.taxiregister.service.AsyncBackgroundJobStarter;
import uk.gov.caz.taxiregister.service.RegisterJobSupervisor;
import uk.gov.caz.taxiregister.service.RegisterJobSupervisor.StartParams;
import uk.gov.caz.taxiregister.service.UploaderIdS3MetadataExtractor;
import uk.gov.caz.testutils.TestObjects;

@WebMvcTest(RegisterCsvFromS3Controller.class)
class RegisterCsvFromS3ControllerTest {

  private static final String S3_BUCKET = "s3Bucket";
  private static final String CSV_FILE = "fileName.csv";
  private static final String CSV_FILE_UPPERCASE = "FILENAME.CSV";

  @MockBean
  private AsyncBackgroundJobStarter mockedAsyncBackgroundJobStarter;

  @MockBean
  private RegisterJobSupervisor mockedRegisterJobSupervisor;

  @MockBean
  private UploaderIdS3MetadataExtractor mockedUploaderIdS3MetadataExtractor;

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Captor
  private ArgumentCaptor<RegisterJobSupervisor.StartParams> startParamsArgumentCaptor;

  @BeforeEach
  public void beforeEach() {
    Mockito.reset(mockedAsyncBackgroundJobStarter, mockedRegisterJobSupervisor);
  }

  @Test
  public void testRegisterJobStartWhenSuccessfullyObtainedUploaderIdMetadata() throws Exception {
    // given
    mockSupervisor();
    mockUploaderIdS3MetadataExtractorForSuccess(CSV_FILE);
    mockSupervisorForNotFindingStartingOrRunningJob();

    // when
    postToStartRegisterJobAndCheckIfItStartedOk(CSV_FILE);

    // then
    StartParams startParams = verifyThatSupervisorStartedJobAndCaptureItsParams();
    assertThat(startParams)
        .wasTriggeredBy(RegisterJobTrigger.CSV_FROM_S3)
        .hasJobNameSuffix("fileName")
        .wasUploadedBy(TYPICAL_REGISTER_JOB_UPLOADER_ID)
        .invokedJob(mockedAsyncBackgroundJobStarter, S3_BUCKET, CSV_FILE);
  }

  @Test
  public void testRegisterJobStartWhenCsvFileHasUppercaseLetters() throws Exception {
    // given
    mockSupervisor();
    mockUploaderIdS3MetadataExtractorForSuccess(CSV_FILE_UPPERCASE);
    mockSupervisorForNotFindingStartingOrRunningJob();

    // when
    postToStartRegisterJobAndCheckIfItStartedOk(CSV_FILE_UPPERCASE);

    // then
    StartParams startParams = verifyThatSupervisorStartedJobAndCaptureItsParams();
    assertThat(startParams)
        .wasTriggeredBy(RegisterJobTrigger.CSV_FROM_S3)
        .hasJobNameSuffix("FILENAME")
        .wasUploadedBy(TYPICAL_REGISTER_JOB_UPLOADER_ID)
        .invokedJob(mockedAsyncBackgroundJobStarter, S3_BUCKET, CSV_FILE_UPPERCASE);
  }

  @Test
  public void testRegisterJobStartWhenUnableToObtainUploaderIdMetadata() throws Exception {
    // given
    mockSupervisor();
    mockUploaderIdS3MetadataExtractorForError();
    mockSupervisorForNotFindingStartingOrRunningJob();

    // when
    postToStartRegisterJobAndCheckIfItReturns500WithMessage(
        "Unable to fetch \"uploader-id\" metadata from S3 Bucket: s3Bucket; File: fileName.csv");
  }

  @Test
  public void testQueryForExistingRegisterJobStatus() throws Exception {
    mockSupervisorForFindingRunningRegisterJob();
    mockSupervisorForNotFindingStartingOrRunningJob();

    mockMvc.perform(
        get(RegisterCsvFromS3Controller.PATH + "/{registerJobName}",
            S3_REGISTER_JOB_NAME)
            .header(CORRELATION_ID_HEADER, TYPICAL_CORRELATION_ID)
            .accept(MediaType.APPLICATION_JSON_UTF8_VALUE))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(RegisterJobStatus.RUNNING.toString()))
        .andExpect(jsonPath("$.errors[*]").value(hasItems("error 1", "error 2")));
  }

  @Test
  public void testQueryForNotExistingRegisterJobStatus() throws Exception {
    mockSupervisorForNotFindingRegisterJob();
    mockSupervisorForNotFindingStartingOrRunningJob();

    mockMvc.perform(
        get(RegisterCsvFromS3Controller.PATH + "/{registerJobName}",
            NOT_EXISTING_REGISTER_JOB_NAME)
            .header(CORRELATION_ID_HEADER, TYPICAL_CORRELATION_ID)
            .accept(MediaType.APPLICATION_JSON_UTF8_VALUE))
        .andExpect(status().isNotFound()) ;
  }

  @Test
  public void testQueryForSuccessfullyCompletedRegisterJobStatus() throws Exception {
    mockSupervisorForFindingRegisterJob(TestObjects.S3_FINISHED_REGISTER_JOB);

    mockMvc.perform(
        get(RegisterCsvFromS3Controller.PATH + "/{registerJobName}",
            S3_REGISTER_JOB_NAME)
            .header(CORRELATION_ID_HEADER, TYPICAL_CORRELATION_ID)
            .accept(MediaType.APPLICATION_JSON_UTF8_VALUE))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(RegisterJobStatusDto.SUCCESS.toString()))
        .andExpect(jsonPath("$.errors").value(IsNull.nullValue()));
  }

  private void mockSupervisorForFindingRegisterJob(RegisterJob registerJob) {
    given(mockedRegisterJobSupervisor.findJobWithName(new RegisterJobName(S3_REGISTER_JOB_NAME)))
        .willReturn(Optional.of(registerJob));
  }

  @Test
  public void missingCorrelationIdShouldResultIn400AndValidMessage() throws Exception {
    // given
    mockForFailureByPresentActiveJobs();

    // when
    StartRegisterCsvFromS3JobCommand cmd = new
        StartRegisterCsvFromS3JobCommand(S3_BUCKET, CSV_FILE);

    mockMvc.perform(
        post(RegisterCsvFromS3Controller.PATH)
            .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
            .content(objectMapper.writeValueAsString(cmd))
            .accept(MediaType.APPLICATION_JSON_UTF8_VALUE))
        .andExpect(status().isBadRequest())
        .andExpect(content().string(
            "Missing request header 'X-Correlation-ID' for method parameter of type String"));
  }

  private void mockSupervisor() {
    given(mockedRegisterJobSupervisor.start(Mockito.any(StartParams.class)))
        .willReturn(new RegisterJobName(S3_REGISTER_JOB_NAME));
  }

  private void mockUploaderIdS3MetadataExtractorForSuccess(String csvFileName) {
    given(mockedUploaderIdS3MetadataExtractor.getUploaderId(S3_BUCKET, csvFileName))
        .willReturn(Optional.of(TYPICAL_REGISTER_JOB_UPLOADER_ID));
  }

  private void mockUploaderIdS3MetadataExtractorForError() {
    given(mockedUploaderIdS3MetadataExtractor.getUploaderId(S3_BUCKET, CSV_FILE))
        .willReturn(Optional.empty());
  }

  private void mockSupervisorForFindingRunningRegisterJob() {
    mockSupervisorForFindingRegisterJob(S3_RUNNING_REGISTER_JOB);
  }

  private void mockSupervisorForNotFindingRegisterJob() {
    given(mockedRegisterJobSupervisor
        .findJobWithName(new RegisterJobName(NOT_EXISTING_REGISTER_JOB_NAME)))
        .willReturn(Optional.empty());
  }

  private void mockSupervisorForFindingStartingOrRunningJob() {
    given(mockedRegisterJobSupervisor.hasActiveJobs(ArgumentMatchers.<Set<LicensingAuthority>>any()))
        .willReturn(true);
  }

  private void mockSupervisorForNotFindingStartingOrRunningJob() {
    given(mockedRegisterJobSupervisor.hasActiveJobs(ArgumentMatchers.<Set<LicensingAuthority>>any()))
        .willReturn(false);
  }

  private void mockForFailureByPresentActiveJobs() {
    mockSupervisor();
    mockUploaderIdS3MetadataExtractorForSuccess(CSV_FILE);
    mockSupervisorForFindingStartingOrRunningJob();
  }

  private void postToStartRegisterJobAndCheckIfItStartedOk(String csvFile) throws Exception {
    StartRegisterCsvFromS3JobCommand cmd = new
        StartRegisterCsvFromS3JobCommand(S3_BUCKET, csvFile);

    mockMvc.perform(
        post(RegisterCsvFromS3Controller.PATH)
            .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
            .content(objectMapper.writeValueAsString(cmd))
            .header(CORRELATION_ID_HEADER, TYPICAL_CORRELATION_ID)
            .accept(MediaType.APPLICATION_JSON_UTF8_VALUE))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.jobName")
            .value(S3_REGISTER_JOB_NAME));
  }

  private void postToStartRegisterJobAndCheckIfItReturns500WithMessage(String expectedMessage)
      throws Exception {
    StartRegisterCsvFromS3JobCommand cmd = new
        StartRegisterCsvFromS3JobCommand(S3_BUCKET, CSV_FILE);

    mockMvc.perform(
        post(RegisterCsvFromS3Controller.PATH)
            .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
            .content(objectMapper.writeValueAsString(cmd))
            .header(CORRELATION_ID_HEADER, TYPICAL_CORRELATION_ID)
            .accept(MediaType.APPLICATION_JSON_UTF8_VALUE))
        .andExpect(status().isInternalServerError())
        .andExpect(content().string(expectedMessage));
  }

  private StartParams verifyThatSupervisorStartedJobAndCaptureItsParams() {
    verify(mockedRegisterJobSupervisor).start(startParamsArgumentCaptor.capture());
    return startParamsArgumentCaptor.getValue();
  }
}
