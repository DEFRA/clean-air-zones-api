package uk.gov.caz.vcc.controller;

import static com.google.common.collect.Lists.newArrayList;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import static uk.gov.caz.vcc.controller.VehicleEntrantsController.CAZ_ID;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.util.Collections;
import java.util.UUID;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.ResourceUtils;
import uk.gov.caz.vcc.annotation.MockedMvcIntegrationTest;
import uk.gov.caz.vcc.dto.VehicleEntrantDto;
import uk.gov.caz.vcc.dto.VehicleEntrantsDto;
import uk.gov.caz.vcc.dto.VehicleEntrantsSaveRequestDto;
import uk.gov.caz.vcc.dto.VehicleResultDto;
import uk.gov.caz.vcc.service.VehicleEntrantsService;

@MockedMvcIntegrationTest
public class VehicleEntrantsControllerTestIT {

  private static final String SOME_CORRELATION_ID = "63be7528-7efd-4f31-ae68-11a6b709ff1c";

  private static final String SOME_CAZ_ID = "63be7528-7efd-4f31-ae68-11a6b709ff1b";

  public static final VehicleResultDto GOLF = VehicleResultDto.builder()
      .vrn("SW61BYD")
      .make("volkswagen")
      .model("golf")
      .colour("black")
      .typeApproval("m1")
      .status("notCompliantNotPaid")
      .tariffCode("271TC001")
      .isTaxiOrPhv(true)
      .licensingAuthority(newArrayList("Authority A", "Authority B"))
      .build();

  public static final VehicleResultDto FORD = VehicleResultDto.builder()
      .vrn("MK16YZR")
      .make("ford")
      .model("mondeo")
      .colour("white")
      .typeApproval("m1")
      .status("exempt")
      .exemptionCode("NEC001")
      .isTaxiOrPhv(false)
      .build();

  @MockBean
  private VehicleEntrantsService vehicleEntrantsService;

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Test
  public void shouldReturnOneResultForGivenPayload() throws Exception {
    given(vehicleEntrantsService.save(
        new VehicleEntrantsSaveRequestDto(
            UUID.fromString(SOME_CAZ_ID), SOME_CORRELATION_ID,
            createSingleVehicleEntrant().getVehicleEntrants()
        )
    )).willReturn(Collections.singletonList(GOLF));

    mockMvc.perform(post(VehicleEntrantsController.VEHICLE_ENTRANT_PATH)
        .content(createPayloadWithSingleVehicleEntrant())
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .accept(MediaType.APPLICATION_JSON_VALUE)
        .header(X_CORRELATION_ID_HEADER, SOME_CORRELATION_ID)
        .header(CAZ_ID, SOME_CAZ_ID))
        .andExpect(status().isOk())
        .andExpect(content().json(readJson("classpath:data/json/single-result.json")))
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
  public void shouldReturnTwoResultsForPayload() throws Exception {
    given(vehicleEntrantsService.save(
        new VehicleEntrantsSaveRequestDto(
            UUID.fromString(SOME_CAZ_ID), SOME_CORRELATION_ID,
            createMultipleVehicleEntrants().getVehicleEntrants()
        )
    )).willReturn(newArrayList(GOLF, FORD));

    mockMvc.perform(post(VehicleEntrantsController.VEHICLE_ENTRANT_PATH)
        .content(createPayloadWithMultipleVehicleEntrants())
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .accept(MediaType.APPLICATION_JSON_VALUE)
        .header(X_CORRELATION_ID_HEADER, SOME_CORRELATION_ID)
        .header(CAZ_ID, SOME_CAZ_ID))
        .andExpect(status().isOk())
        .andExpect(content().json(readJson("classpath:data/json/multiple-results.json")))
        .andExpect(header().string(X_CORRELATION_ID_HEADER, SOME_CORRELATION_ID));
  }

  @Test
  public void shouldReturn5ValidationErrorsFromPayloadWith6Errors() throws Exception {
    mockMvc.perform(post(VehicleEntrantsController.VEHICLE_ENTRANT_PATH)
        .content(createMultipleInvalidVehicleEntrants())
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .accept(MediaType.APPLICATION_JSON_VALUE)
        .header(X_CORRELATION_ID_HEADER, SOME_CORRELATION_ID)
        .header(CAZ_ID, SOME_CAZ_ID))
        .andExpect(status().isBadRequest())
        .andExpect(content().json(readJson("classpath:data/json/invalid-vehicles.json")))
        .andExpect(header().string(X_CORRELATION_ID_HEADER, SOME_CORRELATION_ID));
  }

  private String createPayloadWithSingleVehicleEntrant() {
    return writeAsString(createSingleVehicleEntrant());
  }

  private VehicleEntrantsDto createSingleVehicleEntrant() {
    return new VehicleEntrantsDto(
        newArrayList(new VehicleEntrantDto("SW61BYD", "2017-10-01T155300Z")));
  }

  private String createPayloadWithMultipleVehicleEntrants() {
    return writeAsString(createMultipleVehicleEntrants());
  }

  private VehicleEntrantsDto createMultipleVehicleEntrants() {
    return new VehicleEntrantsDto(
        newArrayList(new VehicleEntrantDto("SW61BYD", "2017-10-01T155300Z"),
            new VehicleEntrantDto("MK16YZR", "2017-10-01T155300Z")));
  }

  private String createMultipleInvalidVehicleEntrants() {
    return writeAsString(new VehicleEntrantsDto(
        newArrayList(
            new VehicleEntrantDto("SW61BYDDDDDDDDDDDDDDDDD", "2012/10-01T155300Z"),
            new VehicleEntrantDto("FFFFFFFFFFFFFFFFFFF", "2017-10-01T155300Z"),
            new VehicleEntrantDto(null, "2017-10-01T155300Z"),
            new VehicleEntrantDto("MK16YZR", null),
            new VehicleEntrantDto("", ""),
            new VehicleEntrantDto("", null)
        )));
  }

  @SneakyThrows
  private String writeAsString(Object object) {
    return objectMapper.writeValueAsString(object);
  }

  @SneakyThrows
  private String readJson(String file) {
    return new String(Files.readAllBytes(ResourceUtils.getFile(file).toPath()));
  }
}