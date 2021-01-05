package uk.gov.caz.accounts.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.caz.accounts.util.TestObjects.NOT_EXISTING_REGISTER_JOB_NAME;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.caz.GlobalExceptionHandlerConfiguration;
import uk.gov.caz.accounts.dto.StartRegisterCsvFromS3JobCommand;
import uk.gov.caz.accounts.model.registerjob.RegisterJobName;
import uk.gov.caz.accounts.service.exception.FatalErrorWithCsvFileMetadataException;
import uk.gov.caz.accounts.service.registerjob.RegisterJobStarter;
import uk.gov.caz.accounts.service.registerjob.RegisterJobStarter.InitialJobParams;
import uk.gov.caz.accounts.service.registerjob.RegisterJobSupervisor;
import uk.gov.caz.correlationid.Configuration;
import uk.gov.caz.correlationid.Constants;

@ContextConfiguration(classes = {
    ExceptionController.class,
    GlobalExceptionHandlerConfiguration.class,
    Configuration.class,
    RegisterCsvFromS3Controller.class})
@WebMvcTest
class RegisterCsvFromS3ControllerTest {

  private static final String ANY_CORRELATION_ID = "f9608304-17d5-4513-b72b-b6c13f525171";
  private static final String S3_BUCKET = "s3Bucket";
  private static final String CSV_FILE = "fileName.csv";
  private static final boolean SEND_EMAILS_AFTER_SUCCESSFUL_JOB_COMPLETION = true;

  @MockBean
  private RegisterJobStarter registerJobStarter;

  @MockBean
  private RegisterJobSupervisor registerJobSupervisor;

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @BeforeEach
  public void beforeEach() {
    Mockito.reset(registerJobStarter, registerJobSupervisor);
  }

  @Test
  public void testSuccessfulJobStart() throws Exception {
    String jobName = mockRegisterJobStarterThatReturnsJobName();
    StartRegisterCsvFromS3JobCommand cmd = new StartRegisterCsvFromS3JobCommand(S3_BUCKET,
        CSV_FILE, SEND_EMAILS_AFTER_SUCCESSFUL_JOB_COMPLETION);

    mockMvc.perform(
        post(RegisterCsvFromS3Controller.PATH)
            .contentType(MediaType.APPLICATION_JSON)
            .header(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID)
            .content(objectMapper.writeValueAsString(cmd))
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.jobName").value(jobName));
  }

  @Test
  public void testQueryForNotExistingRegisterJobStatus() throws Exception {
    mockSupervisorForNotFindingRegisterJob();

    mockMvc.perform(
        get(RegisterCsvFromS3Controller.PATH + "/{registerJobName}",
            NOT_EXISTING_REGISTER_JOB_NAME)
            .header(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID)
            .accept(MediaType.APPLICATION_JSON_VALUE))
        .andExpect(status().isNotFound())
        .andExpect(header().string(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID));
  }

  @Nested
  class Validation {
    @Test
    public void shouldReturn400StatusCodeWhenFilenameIsNull() throws Exception {
      StartRegisterCsvFromS3JobCommand cmd = new StartRegisterCsvFromS3JobCommand(S3_BUCKET,
          null, SEND_EMAILS_AFTER_SUCCESSFUL_JOB_COMPLETION);

      mockMvc.perform(
          post(RegisterCsvFromS3Controller.PATH)
              .contentType(MediaType.APPLICATION_JSON)
              .header(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID)
              .content(objectMapper.writeValueAsString(cmd))
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.message").value("'filename' cannot be null or empty"));
    }

    @Test
    public void shouldReturn400StatusCodeWhenFilenameIsEmpty() throws Exception {
      StartRegisterCsvFromS3JobCommand cmd = new StartRegisterCsvFromS3JobCommand(S3_BUCKET,
          "", SEND_EMAILS_AFTER_SUCCESSFUL_JOB_COMPLETION);

      mockMvc.perform(
          post(RegisterCsvFromS3Controller.PATH)
              .contentType(MediaType.APPLICATION_JSON)
              .header(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID)
              .content(objectMapper.writeValueAsString(cmd))
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.message").value("'filename' cannot be null or empty"));
    }

    @Test
    public void shouldReturn400StatusCodeWhenS3BucketIsNull() throws Exception {
      StartRegisterCsvFromS3JobCommand cmd = new StartRegisterCsvFromS3JobCommand(null,
          CSV_FILE, SEND_EMAILS_AFTER_SUCCESSFUL_JOB_COMPLETION);

      mockMvc.perform(
          post(RegisterCsvFromS3Controller.PATH)
              .contentType(MediaType.APPLICATION_JSON)
              .header(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID)
              .content(objectMapper.writeValueAsString(cmd))
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.message").value("'s3Bucket' cannot be null or empty"));
    }

    @Test
    public void shouldReturn400StatusCodeWhenS3BucketIsEmpty() throws Exception {
      StartRegisterCsvFromS3JobCommand cmd = new StartRegisterCsvFromS3JobCommand("",
          CSV_FILE, SEND_EMAILS_AFTER_SUCCESSFUL_JOB_COMPLETION);

      mockMvc.perform(
          post(RegisterCsvFromS3Controller.PATH)
              .contentType(MediaType.APPLICATION_JSON)
              .header(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID)
              .content(objectMapper.writeValueAsString(cmd))
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.message").value("'s3Bucket' cannot be null or empty"));
    }
  }

  private String mockRegisterJobStarterThatReturnsJobName() {
    InitialJobParams jobParams = InitialJobParams.builder()
        .correlationId(ANY_CORRELATION_ID)
        .filename(CSV_FILE)
        .s3Bucket(S3_BUCKET)
        .sendEmails(true)
        .build();
    String jobName = "my-job-name";
    given(registerJobStarter.start(jobParams)).willReturn(new RegisterJobName(jobName));
    return jobName;
  }

  @Test
  public void testRegisterJobStartWhenUnableToObtainMetadata() throws Exception {
    // given
    mockCsvFileOnS3MetadataExtractorForError();

    // when
    postToStartRegisterJobAndCheckIfItReturns500WithMessage("Fatal Error with Metadata");
  }

  @Test
  public void missingCorrelationIdShouldResultIn400AndValidMessage() throws Exception {
    StartRegisterCsvFromS3JobCommand cmd = new StartRegisterCsvFromS3JobCommand(S3_BUCKET,
        CSV_FILE, SEND_EMAILS_AFTER_SUCCESSFUL_JOB_COMPLETION);

    mockMvc.perform(
        post(RegisterCsvFromS3Controller.PATH)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(cmd))
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Missing request header 'X-Correlation-ID'"));
  }

  private void mockCsvFileOnS3MetadataExtractorForError() {
    given(registerJobStarter.start(any()))
        .willThrow(new FatalErrorWithCsvFileMetadataException("Fatal Error with Metadata"));
  }

  private void postToStartRegisterJobAndCheckIfItReturns500WithMessage(String expectedMessage)
      throws Exception {
    StartRegisterCsvFromS3JobCommand cmd = new StartRegisterCsvFromS3JobCommand(S3_BUCKET,
        CSV_FILE, SEND_EMAILS_AFTER_SUCCESSFUL_JOB_COMPLETION);

    mockMvc.perform(
        post(RegisterCsvFromS3Controller.PATH)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(cmd))
            .header(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID)
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.message").value(expectedMessage));
  }

  private void mockSupervisorForNotFindingRegisterJob() {
    RegisterJobName registerJobName = new RegisterJobName(NOT_EXISTING_REGISTER_JOB_NAME);
    given(registerJobSupervisor.findJobWithName(registerJobName))
        .willReturn(Optional.empty());
  }
}