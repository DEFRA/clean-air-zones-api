package uk.gov.caz.tariff;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
import static uk.gov.caz.tariff.util.JsonReader.cleanAirZonesJson;
import static uk.gov.caz.tariff.util.JsonReader.tariffJson;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.caz.tariff.annotation.MockedMvcIntegrationTest;
import uk.gov.caz.tariff.controller.CleanAirZonesController;

@MockedMvcIntegrationTest
@Sql(scripts = {
    "classpath:data/sql/clear.sql",
    "classpath:data/sql/sample-data.sql"},
    executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
public class CazTestIT {

  private static final String SOME_CORRELATION_ID = "63be7528-7efd-4f31-ae68-11a6b709ff1c";

  @Autowired
  private MockMvc mockMvc;

  @Test
  public void shouldReturnCleanAirZones() throws Exception {
    mockMvc.perform(get(CleanAirZonesController.PATH)
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .accept(MediaType.APPLICATION_JSON_VALUE)
        .header(X_CORRELATION_ID_HEADER, SOME_CORRELATION_ID))
        .andExpect(status().isOk())
        .andExpect(content().json(cleanAirZonesJson()))
        .andExpect(header().string(X_CORRELATION_ID_HEADER, SOME_CORRELATION_ID))
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

  @Test
  public void shouldReturnTariffAndStatusOk() throws Exception {
    mockMvc.perform(get(tariffWithCleanAirZoneId("5cd7441d-766f-48ff-b8ad-1809586fea37"))
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .accept(MediaType.APPLICATION_JSON_VALUE)
        .header(X_CORRELATION_ID_HEADER, SOME_CORRELATION_ID))
        .andExpect(content().json(tariffJson()))
        .andExpect(status().isOk())
        .andExpect(header().string(X_CORRELATION_ID_HEADER, SOME_CORRELATION_ID));
  }

  @Test
  public void shouldReturnNotFoundWhenTariffNotExist() throws Exception {
    mockMvc.perform(get(tariffWithCleanAirZoneId("dc1efcaf-a2cf-41ec-aa37-ea4b28a20a1d"))
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .accept(MediaType.APPLICATION_JSON_VALUE)
        .header(X_CORRELATION_ID_HEADER, SOME_CORRELATION_ID))
        .andExpect(status().isNotFound())
        .andExpect(header().string(X_CORRELATION_ID_HEADER, SOME_CORRELATION_ID));
  }

  @Test
  public void shouldReturnNotFoundWhenInvalidUUID() throws Exception {
    // when
    Exception resolvedException = mockMvc.perform(get(tariffWithCleanAirZoneId("asd"))
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .accept(MediaType.APPLICATION_JSON_VALUE)
        .header(X_CORRELATION_ID_HEADER, SOME_CORRELATION_ID))
        .andExpect(status().isNotFound())
        .andReturn()
        .getResolvedException();

    // then
    assertThat(resolvedException).isNotNull();
    assertThat(resolvedException).isInstanceOf(IllegalArgumentException.class);
  }

  private static String tariffWithCleanAirZoneId(String cleanAirZoneId) {
    return CleanAirZonesController.PATH + "/" + cleanAirZoneId + "/tariff";
  }
}