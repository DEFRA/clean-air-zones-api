package uk.gov.caz.taxiregister.controller;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_XML_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.caz.taxiregister.controller.Constants.CORRELATION_ID_HEADER;

import java.time.LocalDate;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.caz.GlobalExceptionHandlerConfiguration;
import uk.gov.caz.correlationid.Configuration;
import uk.gov.caz.taxiregister.DateHelper;
import uk.gov.caz.taxiregister.service.ReportingService;
import uk.gov.caz.taxiregister.tasks.ActiveLicencesInReportingWindowStarter.ActiveLicencesInReportingWindowOutput;
import uk.gov.caz.testutils.TestObjects;
import uk.gov.caz.testutils.TestObjects.Licences;

@ContextConfiguration(classes = {GlobalExceptionHandlerConfiguration.class, Configuration.class,
    ReportingController.class, ExceptionController.class})
@WebMvcTest
class ReportingControllerTest {

  @MockBean
  private ReportingService reportingService;

  @MockBean
  private ActiveLicencesInReportingWindowOutput output;

  @Autowired
  private MockMvc mockMvc;

  @BeforeEach
  public void reset() {
    Mockito.reset(reportingService, output);
  }

  @Nested
  class LicensingAuthoritiesAudit {

    @Test
    public void shouldReturnActiveLicencesForValidInput() throws Exception {
      String licensingAuthority = "la-1";
      String vrm = Licences.validVrm();
      LocalDate date = DateHelper.monthAgo();
      given(reportingService.getLicensingAuthoritiesOfActiveLicencesForVrmOn(vrm, date))
          .willReturn(Collections.singleton(licensingAuthority));

      mockMvc.perform(get(ReportingController.LICENSING_AUTHORITIES_AUDIT_PATH, vrm)
          .param("date", date.toString())
          .accept(MediaType.APPLICATION_JSON_VALUE)
          .contentType(MediaType.APPLICATION_JSON_VALUE)
          .header(CORRELATION_ID_HEADER, TestObjects.Registration.correlationId()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.licensingAuthoritiesNames[*]").value(hasItem(licensingAuthority)));
    }

    @Test
    public void shouldReturnActiveLicencesForTodayWhenInputDateIsNull() throws Exception {
      LocalDate today = LocalDate.now();
      String licensingAuthority = "la-1";
      String vrm = Licences.validVrm();
      given(
          reportingService.getLicensingAuthoritiesOfActiveLicencesForVrmOn(eq(vrm), eq(today)))
          .willReturn(Collections.singleton(licensingAuthority));

      mockMvc.perform(get(ReportingController.LICENSING_AUTHORITIES_AUDIT_PATH, vrm)
          .accept(MediaType.APPLICATION_JSON_VALUE)
          .contentType(MediaType.APPLICATION_JSON_VALUE)
          .header(CORRELATION_ID_HEADER, TestObjects.Registration.correlationId()))
          .andExpect(status().isOk())
          .andExpect(
              header().string(CORRELATION_ID_HEADER, TestObjects.Registration.correlationId()))
          .andExpect(jsonPath("$.licensingAuthoritiesNames[*]").value(hasItem(licensingAuthority)));

      verify(reportingService).getLicensingAuthoritiesOfActiveLicencesForVrmOn(vrm, today);
    }

    @Test
    public void shouldReturn400StatusCodeIfMissingCorrelationId() throws Exception {
      String vrm = Licences.validVrm();

      mockMvc.perform(get(ReportingController.LICENSING_AUTHORITIES_AUDIT_PATH, vrm)
          .contentType(MediaType.APPLICATION_JSON_VALUE)
          .accept(MediaType.APPLICATION_JSON_VALUE))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.message").value("Missing request header 'X-Correlation-ID'"));
    }

    @Test
    public void shouldReturn405StatusCodeForPostRequest() throws Exception {
      mockMvc
          .perform(post(ReportingController.LICENSING_AUTHORITIES_AUDIT_PATH, Licences.validVrm())
              .header(CORRELATION_ID_HEADER, TestObjects.Registration.correlationId())
              .contentType(MediaType.APPLICATION_JSON_VALUE)
              .accept(MediaType.APPLICATION_JSON_VALUE))
          .andExpect(status().isMethodNotAllowed());
    }

    @Test
    public void shouldReturn500StatusCodeIfUnhandledExceptionIsThrown() throws Exception {
      given(reportingService.getLicensingAuthoritiesOfActiveLicencesForVrmOn(any(), any()))
          .willThrow(new RuntimeException());

      mockMvc.perform(get(ReportingController.LICENSING_AUTHORITIES_AUDIT_PATH, Licences.validVrm())
          .header(CORRELATION_ID_HEADER, TestObjects.Registration.correlationId())
          .contentType(MediaType.APPLICATION_JSON_VALUE)
          .accept(MediaType.APPLICATION_JSON_VALUE))
          .andExpect(status().isInternalServerError())
          .andExpect(jsonPath("$.message").value("Cannot process request"));
    }

    @Test
    public void shouldReturn400StatusCodeForInvalidVrm() throws Exception {
      String vrm = "invalid-vrm";

      mockMvc.perform(get(ReportingController.LICENSING_AUTHORITIES_AUDIT_PATH, vrm)
          .header(CORRELATION_ID_HEADER, TestObjects.Registration.correlationId())
          .contentType(MediaType.APPLICATION_JSON_VALUE)
          .accept(MediaType.APPLICATION_JSON_VALUE))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.errors[0].title").value("Validation error"))
          .andExpect(jsonPath("$.errors[0].detail")
              .value(startsWith("getLicensingAuthoritiesOfActiveLicencesForVrmOn.vrm")));
    }

    @ParameterizedTest
    @MethodSource("uk.gov.caz.taxiregister.controller.ReportingControllerTest#futureDatesProvider")
    public void shouldReturn400StatusCodeForInvalidDate(LocalDate date) throws Exception {
      String vrm = Licences.validVrm();

      mockMvc.perform(get(ReportingController.LICENSING_AUTHORITIES_AUDIT_PATH, vrm)
          .header(CORRELATION_ID_HEADER, TestObjects.Registration.correlationId())
          .param("date", date.toString())
          .contentType(MediaType.APPLICATION_JSON_VALUE)
          .accept(MediaType.APPLICATION_JSON_VALUE))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.errors[0].title").value("Validation error"))
          .andExpect(
              jsonPath("$.errors[0].detail").value(containsString("Cannot process a future date")));
    }
  }

  @Nested
  class ActiveLicencesInReportingWindow {

    @Test
    public void shouldStartReportExecutionAndReturn200() throws Exception {
      callReportingEndpointAndVerifyExpectations();
    }

    @Test
    public void shouldProperlyHandleErrorDuringReportGenerationAndStillReturn200()
        throws Exception {
      willThrow(new RuntimeException()).given(output).writeToCsv(anyList(), eq("report.csv"));
      callReportingEndpointAndVerifyExpectations();
    }

    private void callReportingEndpointAndVerifyExpectations() throws Exception {
      mockMvc.perform(get(ReportingController.ACTIVE_LICENCES_IN_REPORTING_WINDOW_PATH)
          .header(CORRELATION_ID_HEADER, TestObjects.Registration.correlationId())
          .param("startDate", "2019-01-01")
          .param("endDate", "2020-01-01")
          .param("csvFileName", "report.csv")
          .contentType(MediaType.APPLICATION_JSON_VALUE)
          .accept(MediaType.APPLICATION_JSON_VALUE))
          .andExpect(status().isOk())
          .andExpect(content().string("Started"));

      Awaitility.await()
          .atMost(1, TimeUnit.SECONDS)
          .untilAsserted(() -> {
            verify(
                reportingService).runReporting(LocalDate.of(2019, 1, 1), LocalDate.of(2020, 1, 1));
            verify(output).writeToCsv(anyList(), eq("report.csv"));
          });
    }
  }

  @Test
  public void shouldReturn406StatusCodeForNotAcceptable() throws Exception {
    String vrm = "invalid-vrm";

    mockMvc.perform(get(ReportingController.LICENSING_AUTHORITIES_AUDIT_PATH, vrm)
        .header(CORRELATION_ID_HEADER, TestObjects.Registration.correlationId())
        .accept(APPLICATION_XML_VALUE)
        .contentType(APPLICATION_JSON_VALUE))
        .andExpect(status().isNotAcceptable());
  }

  @Test
  public void shouldReturn415StatusCodeForUnsupportedMediaType() throws Exception {
    String vrm = "invalid-vrm";

    mockMvc.perform(get(ReportingController.LICENSING_AUTHORITIES_AUDIT_PATH, vrm)
        .header(CORRELATION_ID_HEADER, TestObjects.Registration.correlationId())
        .accept(APPLICATION_XML_VALUE)
        .contentType(APPLICATION_XML_VALUE))
        .andExpect(status().isUnsupportedMediaType());
  }

  private static Stream<LocalDate> futureDatesProvider() {
    return Stream.of(
        DateHelper.tomorrow(),
        DateHelper.nextWeek()
    );
  }
}