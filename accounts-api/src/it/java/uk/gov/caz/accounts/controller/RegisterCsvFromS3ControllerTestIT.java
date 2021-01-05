package uk.gov.caz.accounts.controller;

import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.caz.correlationid.Constants.X_CORRELATION_ID_HEADER;
import static uk.gov.caz.security.SecurityHeadersInjector.CACHE_CONTROL_HEADER;
import static uk.gov.caz.security.SecurityHeadersInjector.CACHE_CONTROL_VALUE;
import static uk.gov.caz.security.SecurityHeadersInjector.CONTENT_SECURITY_POLICY_HEADER;
import static uk.gov.caz.security.SecurityHeadersInjector.CONTENT_SECURITY_POLICY_VALUE;
import static uk.gov.caz.security.SecurityHeadersInjector.PRAGMA_HEADER;
import static uk.gov.caz.security.SecurityHeadersInjector.PRAGMA_HEADER_VALUE;
import static uk.gov.caz.security.SecurityHeadersInjector.STRICT_TRANSPORT_SECURITY_HEADER;
import static uk.gov.caz.security.SecurityHeadersInjector.STRICT_TRANSPORT_SECURITY_VALUE;
import static uk.gov.caz.security.SecurityHeadersInjector.X_CONTENT_TYPE_OPTIONS_HEADER;
import static uk.gov.caz.security.SecurityHeadersInjector.X_CONTENT_TYPE_OPTIONS_VALUE;
import static uk.gov.caz.security.SecurityHeadersInjector.X_FRAME_OPTIONS_HEADER;
import static uk.gov.caz.security.SecurityHeadersInjector.X_FRAME_OPTIONS_VALUE;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import uk.gov.caz.accounts.annotation.MockedMvcIntegrationTest;
import uk.gov.caz.accounts.model.registerjob.RegisterJobName;

@MockedMvcIntegrationTest
@Sql(scripts = {"classpath:data/sql/registerjob/clear-jobs-and-vrns.sql",
    "classpath:data/sql/registerjob/add-job-registers.sql"},
    executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "classpath:data/sql/registerjob/clear-jobs-and-vrns.sql",
    executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
public class RegisterCsvFromS3ControllerTestIT {

  private static final String SOME_CORRELATION_ID = "63be7528-7efd-4f31-ae68-11a6b709ff1c";

  @Autowired
  private MockMvc mockMvc;

  @Test
  public void shouldFindRegisterJob() throws Exception {
    //given
    RegisterJobName registerJobName = new RegisterJobName("TEST_JOB_NAME");

    //when
    ResultActions callResult = callQueryForStatusOfRegisterJob(registerJobName);

    //then
    assertProperResponseSchema(callResult);
  }

  private ResultActions callQueryForStatusOfRegisterJob(RegisterJobName registerJobName)
      throws Exception {
    return mockMvc.perform(
        get(RegisterCsvFromS3Controller.PATH + "/{registerJobName}", registerJobName.getValue())
            .accept(MediaType.APPLICATION_JSON)
            .header(X_CORRELATION_ID_HEADER, SOME_CORRELATION_ID));
  }

  private void assertProperResponseSchema(ResultActions callResult)
      throws Exception {
    callResult
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status", notNullValue()))
        .andExpect(jsonPath("$.errors", nullValue()))
        .andExpect(
            header().string(STRICT_TRANSPORT_SECURITY_HEADER, STRICT_TRANSPORT_SECURITY_VALUE))
        .andExpect(
            header().string(PRAGMA_HEADER, PRAGMA_HEADER_VALUE))
        .andExpect(
            header().string(X_CONTENT_TYPE_OPTIONS_HEADER, X_CONTENT_TYPE_OPTIONS_VALUE))
        .andExpect(
            header().string(X_FRAME_OPTIONS_HEADER, X_FRAME_OPTIONS_VALUE))
        .andExpect(
            header().string(CONTENT_SECURITY_POLICY_HEADER, CONTENT_SECURITY_POLICY_VALUE))
        .andExpect(
            header().string(CACHE_CONTROL_HEADER, CACHE_CONTROL_VALUE));
  }
}
