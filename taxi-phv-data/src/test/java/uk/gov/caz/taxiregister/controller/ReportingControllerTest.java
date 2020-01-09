package uk.gov.caz.taxiregister.controller;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.caz.taxiregister.controller.Constants.CORRELATION_ID_HEADER;

import java.time.LocalDate;
import java.util.Collections;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.BDDMockito;
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
import uk.gov.caz.testutils.TestObjects;
import uk.gov.caz.testutils.TestObjects.Licences;

@ContextConfiguration(classes = {GlobalExceptionHandlerConfiguration.class, Configuration.class,
    ReportingController.class, ExceptionController.class})
@WebMvcTest
class ReportingControllerTest {

  @MockBean
  private ReportingService reportingService;

  @Autowired
  private MockMvc mockMvc;

  @BeforeEach
  public void reset() {
    Mockito.reset(reportingService);
  }

  @Nested
  class VrmsAudit {
    @Test
    public void shouldReturnVrmsForValidInput() throws Exception {
      int licensingAuthorityId = 1;
      LocalDate date = LocalDate.now();
      String vrm = Licences.validVrm();
      BDDMockito.given(reportingService.getActiveLicencesForLicensingAuthorityOn(
          licensingAuthorityId, date)).willReturn(Collections.singleton(vrm));

      mockMvc.perform(get(ReportingController.ACTIVE_LICENCES_AUDIT_PATH, licensingAuthorityId)
          .param("date", date.toString())
          .accept(MediaType.APPLICATION_JSON_UTF8_VALUE)
          .header(CORRELATION_ID_HEADER, TestObjects.Registration.correlationId()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.licensingAuthorityId").value(licensingAuthorityId))
          .andExpect(jsonPath("$.auditDate").value(date.toString()))
          .andExpect(jsonPath("$.vrmsWithActiveLicences[*]").value(hasItem(vrm)));
    }

    @Test
    public void shouldReturnVrmsForTodayWhenInputDateIsNull() throws Exception {
      int licensingAuthorityId = 2;
      String vrm = Licences.validVrm();
      BDDMockito.given(reportingService.getActiveLicencesForLicensingAuthorityOn(
          eq(licensingAuthorityId), any())).willReturn(Collections.singleton(vrm));
      LocalDate auditDate = LocalDate.now();

      mockMvc.perform(get(ReportingController.ACTIVE_LICENCES_AUDIT_PATH, licensingAuthorityId)
          .accept(MediaType.APPLICATION_JSON_UTF8_VALUE)
          .header(CORRELATION_ID_HEADER, TestObjects.Registration.correlationId()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.licensingAuthorityId").value(licensingAuthorityId))
          .andExpect(jsonPath("$.auditDate").value(auditDate.toString()))
          .andExpect(jsonPath("$.vrmsWithActiveLicences[*]").value(hasItem(vrm)));

      verify(reportingService).getActiveLicencesForLicensingAuthorityOn(eq(licensingAuthorityId),
          eq(auditDate));
    }

    @Test
    public void shouldReturn400StatusCodeIfMissingCorrelationId() throws Exception {
      int licensingAuthorityId = 3;

      mockMvc.perform(get(ReportingController.ACTIVE_LICENCES_AUDIT_PATH, licensingAuthorityId)
          .accept(MediaType.APPLICATION_JSON_UTF8_VALUE))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.message").value("Missing request header 'X-Correlation-ID'"));
    }

    @Test
    public void shouldReturn405StatusCodeForPostRequest() throws Exception {
      int licensingAuthorityId = 4;

      mockMvc.perform(post(ReportingController.ACTIVE_LICENCES_AUDIT_PATH, licensingAuthorityId)
          .header(CORRELATION_ID_HEADER, TestObjects.Registration.correlationId())
          .accept(MediaType.APPLICATION_JSON_UTF8_VALUE))
          .andExpect(status().isMethodNotAllowed());
    }

    @Test
    public void shouldReturn500StatusCodeIfUnhandledExceptionIsThrown() throws Exception {
      int licensingAuthorityId = 5;
      BDDMockito.given(reportingService.getActiveLicencesForLicensingAuthorityOn(eq(licensingAuthorityId), any()))
          .willThrow(new RuntimeException());

      mockMvc.perform(get(ReportingController.ACTIVE_LICENCES_AUDIT_PATH, licensingAuthorityId)
          .header(CORRELATION_ID_HEADER, TestObjects.Registration.correlationId())
          .accept(MediaType.APPLICATION_JSON_UTF8_VALUE))
          .andExpect(status().isInternalServerError())
          .andExpect(jsonPath("$.message").value("Cannot process request"));
    }

    @Test
    public void shouldReturn400StatusCodeForInvalidLicensingAuthorityId() throws Exception {
      String invalidLicensingAuthorityId = "not an integer";

      mockMvc.perform(get(ReportingController.ACTIVE_LICENCES_AUDIT_PATH, invalidLicensingAuthorityId)
          .header(CORRELATION_ID_HEADER, TestObjects.Registration.correlationId())
          .accept(MediaType.APPLICATION_JSON_UTF8_VALUE))
          .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @MethodSource("uk.gov.caz.taxiregister.controller.ReportingControllerTest#futureDatesProvider")
    public void shouldReturn400StatusCodeForInvalidDate(LocalDate date) throws Exception {
      int licensingAuthorityId = 6;

      mockMvc.perform(get(ReportingController.ACTIVE_LICENCES_AUDIT_PATH, licensingAuthorityId)
          .param("date", date.toString())
          .header(CORRELATION_ID_HEADER, TestObjects.Registration.correlationId())
          .accept(MediaType.APPLICATION_JSON_UTF8_VALUE))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.errors[0].title").value("Validation error"))
          .andExpect(jsonPath("$.errors[0].detail").value(containsString("Cannot process a future date")));
    }
  }

  @Nested
  class LicensingAuthoritiesAudit {
    @Test
    public void shouldReturnActiveLicencesForValidInput() throws Exception {
      String licensingAuthority = "la-1";
      String vrm = Licences.validVrm();
      LocalDate date = DateHelper.monthAgo();
      BDDMockito.given(reportingService.getLicensingAuthoritiesOfActiveLicencesForVrmOn(vrm, date))
          .willReturn(Collections.singleton(licensingAuthority));

      mockMvc.perform(get(ReportingController.LICENSING_AUTHORITIES_AUDIT_PATH, vrm)
          .param("date", date.toString())
          .accept(MediaType.APPLICATION_JSON_UTF8_VALUE)
          .header(CORRELATION_ID_HEADER, TestObjects.Registration.correlationId()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.licensingAuthoritiesNames[*]").value(hasItem(licensingAuthority)));
    }

    @Test
    public void shouldReturnActiveLicencesForTodayWhenInputDateIsNull() throws Exception {
      LocalDate today = LocalDate.now();
      String licensingAuthority = "la-1";
      String vrm = Licences.validVrm();
      BDDMockito.given(reportingService.getLicensingAuthoritiesOfActiveLicencesForVrmOn(eq(vrm), eq(today)))
          .willReturn(Collections.singleton(licensingAuthority));

      mockMvc.perform(get(ReportingController.LICENSING_AUTHORITIES_AUDIT_PATH, vrm)
          .accept(MediaType.APPLICATION_JSON_UTF8_VALUE)
          .header(CORRELATION_ID_HEADER, TestObjects.Registration.correlationId()))
          .andExpect(status().isOk())
          .andExpect(header().string(CORRELATION_ID_HEADER, TestObjects.Registration.correlationId()))
          .andExpect(jsonPath("$.licensingAuthoritiesNames[*]").value(hasItem(licensingAuthority)));

      verify(reportingService).getLicensingAuthoritiesOfActiveLicencesForVrmOn(vrm, today);
    }

    @Test
    public void shouldReturn400StatusCodeIfMissingCorrelationId() throws Exception {
      String vrm = Licences.validVrm();

      mockMvc.perform(get(ReportingController.LICENSING_AUTHORITIES_AUDIT_PATH, vrm)
          .accept(MediaType.APPLICATION_JSON_UTF8_VALUE))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.message").value("Missing request header 'X-Correlation-ID'"));
    }

    @Test
    public void shouldReturn405StatusCodeForPostRequest() throws Exception {
      mockMvc.perform(post(ReportingController.LICENSING_AUTHORITIES_AUDIT_PATH, Licences.validVrm())
          .header(CORRELATION_ID_HEADER, TestObjects.Registration.correlationId())
          .accept(MediaType.APPLICATION_JSON_UTF8_VALUE))
          .andExpect(status().isMethodNotAllowed());
    }

    @Test
    public void shouldReturn500StatusCodeIfUnhandledExceptionIsThrown() throws Exception {
      BDDMockito.given(reportingService.getLicensingAuthoritiesOfActiveLicencesForVrmOn(any(), any()))
          .willThrow(new RuntimeException());

      mockMvc.perform(get(ReportingController.LICENSING_AUTHORITIES_AUDIT_PATH, Licences.validVrm())
          .header(CORRELATION_ID_HEADER, TestObjects.Registration.correlationId())
          .accept(MediaType.APPLICATION_JSON_UTF8_VALUE))
          .andExpect(status().isInternalServerError())
          .andExpect(jsonPath("$.message").value("Cannot process request"));
    }

    @Test
    public void shouldReturn400StatusCodeForInvalidVrm() throws Exception {
      String vrm = "invalid-vrm";

      mockMvc.perform(get(ReportingController.LICENSING_AUTHORITIES_AUDIT_PATH, vrm)
          .header(CORRELATION_ID_HEADER, TestObjects.Registration.correlationId())
          .accept(MediaType.APPLICATION_JSON_UTF8_VALUE))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.errors[0].title").value("Validation error"))
          .andExpect(jsonPath("$.errors[0].detail").value(startsWith("getLicensingAuthoritiesOfActiveLicencesForVrmOn.vrm")));
    }

    @ParameterizedTest
    @MethodSource("uk.gov.caz.taxiregister.controller.ReportingControllerTest#futureDatesProvider")
    public void shouldReturn400StatusCodeForInvalidDate(LocalDate date) throws Exception {
      String vrm = Licences.validVrm();

      mockMvc.perform(get(ReportingController.LICENSING_AUTHORITIES_AUDIT_PATH, vrm)
          .header(CORRELATION_ID_HEADER, TestObjects.Registration.correlationId())
          .param("date", date.toString())
          .accept(MediaType.APPLICATION_JSON_UTF8_VALUE))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.errors[0].title").value("Validation error"))
          .andExpect(jsonPath("$.errors[0].detail").value(containsString("Cannot process a future date")));
    }
  }

  private static Stream<LocalDate> futureDatesProvider() {
    return Stream.of(
        DateHelper.tomorrow(),
        DateHelper.nextWeek()
    );
  }
}