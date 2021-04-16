package uk.gov.caz.taxiregister.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.caz.correlationid.Constants.X_CORRELATION_ID_HEADER;
import static uk.gov.caz.taxiregister.controller.Constants.CORRELATION_ID_HEADER;
import static uk.gov.caz.taxiregister.controller.LookupController.PATH;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.caz.GlobalExceptionHandlerConfiguration;
import uk.gov.caz.correlationid.Configuration;
import uk.gov.caz.taxiregister.model.VehicleLicenceLookupInfo;
import uk.gov.caz.taxiregister.service.LookupService;

@ContextConfiguration(classes = {GlobalExceptionHandlerConfiguration.class, Configuration.class,
    LookupController.class})
@WebMvcTest
class LookupControllerTest {

  private static final String ANY_VRM = "8839GF";
  private static final String ANY_CORRELATION_ID = UUID.randomUUID().toString();
  private static final String TAXI_DESCRIPTION = "taxi";
  private static final String PHV_DESCRIPTION = "PHV";

  @MockBean
  private LookupService lookupService;

  @Autowired
  private MockMvc mockMvc;

  @Test
  public void shouldReturn404NotFoundStatusCodeWhenThereIsNoLicenceForGivenVrm() throws Exception {
    given(lookupService.getLicenceInfoBy(ANY_VRM)).willReturn(Optional.empty());

    mockMvc.perform(get(PATH, ANY_VRM)
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .accept(MediaType.APPLICATION_JSON_VALUE)
        .header(CORRELATION_ID_HEADER, ANY_CORRELATION_ID))
        .andExpect(status().isNotFound())
        .andExpect(header().string(CORRELATION_ID_HEADER, ANY_CORRELATION_ID));
  }

  @Test
  public void missingCorrelationIdShouldResultIn400AndValidMessage() throws Exception {
    given(lookupService.getLicenceInfoBy(ANY_VRM)).willReturn(Optional.empty());

    mockMvc.perform(get(PATH, ANY_VRM)
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .accept(MediaType.APPLICATION_JSON_VALUE))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Missing request header 'X-Correlation-ID'"));
  }

  @ParameterizedTest
  @ValueSource(strings = {
      MediaType.APPLICATION_FORM_URLENCODED_VALUE,
      MediaType.APPLICATION_ATOM_XML_VALUE,
      MediaType.APPLICATION_XML_VALUE,
      MediaType.APPLICATION_OCTET_STREAM_VALUE
  })
  public void shouldReturn406NotAcceptableStatusCodeForUnsupportedMediaType(String mediaType)
      throws Exception {
    mockMvc.perform(get(PATH, ANY_VRM)
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .accept(mediaType)
        .header(X_CORRELATION_ID_HEADER, UUID.randomUUID().toString()))
        .andExpect(status().isNotAcceptable());
  }

  @ParameterizedTest
  @ValueSource(strings = {
      MediaType.APPLICATION_FORM_URLENCODED_VALUE,
      MediaType.APPLICATION_ATOM_XML_VALUE,
      MediaType.APPLICATION_XML_VALUE,
      MediaType.APPLICATION_OCTET_STREAM_VALUE
  })
  public void shouldReturn415UnsupportedMediaType(String mediaType)
      throws Exception {
    mockMvc.perform(get(PATH, ANY_VRM)
        .accept(mediaType)
        .header(X_CORRELATION_ID_HEADER, UUID.randomUUID().toString()))
        .andExpect(status().isUnsupportedMediaType());
  }

  @ParameterizedTest
  @MethodSource("licenceInfoProvider")
  public void shouldReturnLicenceInfoWhenExistsForGivenVrm(VehicleLicenceLookupInfo result)
      throws Exception {
    given(lookupService.getLicenceInfoBy(ANY_VRM)).willReturn(Optional.of(result));

    mockMvc.perform(get(PATH, ANY_VRM)
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .accept(MediaType.APPLICATION_JSON_VALUE)
        .header(CORRELATION_ID_HEADER, ANY_CORRELATION_ID))
        .andExpect(status().isOk())
        .andExpect(header().string(CORRELATION_ID_HEADER, ANY_CORRELATION_ID))
        .andExpect(jsonPath("$.active").value(result.hasAnyOperatingLicenceActive()))
        .andExpect(jsonPath("$.description").value(result.getDescription()))
        .andExpect(jsonPath("$.wheelchairAccessible").value(result.getWheelchairAccessible()))
        .andExpect(jsonPath("$.addedTimestamp").isNotEmpty());
  }

  static Stream<VehicleLicenceLookupInfo> licenceInfoProvider() {
    return Stream.of(
        VehicleLicenceLookupInfo.builder()
            .hasAnyOperatingLicenceActive(true)
            .description(TAXI_DESCRIPTION)
            .wheelchairAccessible(true)
            .addedTimestamp(LocalDateTime.now())
            .build(),
        VehicleLicenceLookupInfo.builder()
            .hasAnyOperatingLicenceActive(true)
            .description(TAXI_DESCRIPTION)
            .wheelchairAccessible(false)
            .addedTimestamp(LocalDateTime.now())
            .build(),
        VehicleLicenceLookupInfo.builder()
            .hasAnyOperatingLicenceActive(false)
            .description(TAXI_DESCRIPTION)
            .wheelchairAccessible(false)
            .addedTimestamp(LocalDateTime.now())
            .build(),
        VehicleLicenceLookupInfo.builder()
            .hasAnyOperatingLicenceActive(false)
            .description(TAXI_DESCRIPTION)
            .wheelchairAccessible(null)
            .addedTimestamp(LocalDateTime.now())
            .build(),
        VehicleLicenceLookupInfo.builder()
            .hasAnyOperatingLicenceActive(true)
            .description(TAXI_DESCRIPTION)
            .wheelchairAccessible(null)
            .addedTimestamp(LocalDateTime.now())
            .build(),
        VehicleLicenceLookupInfo.builder()
            .hasAnyOperatingLicenceActive(true)
            .description(PHV_DESCRIPTION)
            .wheelchairAccessible(true)
            .addedTimestamp(LocalDateTime.now())
            .build(),
        VehicleLicenceLookupInfo.builder()
            .hasAnyOperatingLicenceActive(true)
            .description(PHV_DESCRIPTION)
            .wheelchairAccessible(false)
            .addedTimestamp(LocalDateTime.now())
            .build(),
        VehicleLicenceLookupInfo.builder()
            .hasAnyOperatingLicenceActive(false)
            .description(PHV_DESCRIPTION)
            .wheelchairAccessible(false)
            .addedTimestamp(LocalDateTime.now())
            .build(),
        VehicleLicenceLookupInfo.builder()
            .hasAnyOperatingLicenceActive(false)
            .description(PHV_DESCRIPTION)
            .wheelchairAccessible(null)
            .addedTimestamp(LocalDateTime.now())
            .build(),
        VehicleLicenceLookupInfo.builder()
            .hasAnyOperatingLicenceActive(true)
            .description(PHV_DESCRIPTION)
            .wheelchairAccessible(null)
            .addedTimestamp(LocalDateTime.now())
            .build()
    );
  }
}