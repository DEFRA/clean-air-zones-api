package uk.gov.caz.whitelist.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.caz.correlationid.Constants.X_CORRELATION_ID_HEADER;
import static uk.gov.caz.testutils.TestObjects.TYPICAL_CORRELATION_ID;
import static uk.gov.caz.whitelist.controller.WhitelistController.BASE_PATH;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import uk.gov.caz.GlobalExceptionHandlerConfiguration;
import uk.gov.caz.correlationid.Configuration;
import uk.gov.caz.whitelist.dto.WhitelistedVehicleRequestDto;
import uk.gov.caz.whitelist.model.WhitelistVehicle;
import uk.gov.caz.whitelist.service.WhitelistService;

@ContextConfiguration(classes = {GlobalExceptionHandlerConfiguration.class, Configuration.class,
    WhitelistController.class})
@WebMvcTest
class WhitelistControllerTest {

  @MockBean
  private WhitelistService whitelistService;

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Test
  public void shouldReturnValidEntity() throws Exception {
    // given
    WhitelistedVehicleRequestDto dto = WhitelistedVehicleRequestDto.builder()
        .vrn("vrn")
        .category("Early Adopter")
        .reasonUpdated("some reason")
        .manufacturer(null)
        .uploaderId(UUID.randomUUID())
        .build();
    String payload = buildPayloadWith(dto);

    // when
    ResultActions callResult = performCallWith(payload);

    // then
    callResult.andExpect(status().isCreated());
  }

  @Nested
  class Validation {

    @Test
    public void shouldReturn400BadRequestStatusCodeWhenValidationVrnFails() throws Exception {
      WhitelistedVehicleRequestDto dto = WhitelistedVehicleRequestDto.builder()
          .vrn(null)
          .category("Early Adopter")
          .reasonUpdated("some reason")
          .uploaderId(UUID.randomUUID())
          .build();
      String payload = buildPayloadWith(dto);

      performCallWith(payload).andExpect(status().isBadRequest())
          .andDo(print()).andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.errors[0].status").value(HttpStatus.BAD_REQUEST.value()))
          .andExpect(jsonPath("$.errors[0].vrn").value(dto.getVrn()))
          .andExpect(jsonPath("$.errors[0].detail")
              .value("Data does not include the 'vrn' field which is mandatory."));
    }

    @Test
    public void shouldReturn400BadRequestStatusCodeWhenValidationCategoryFails() throws Exception {
      WhitelistedVehicleRequestDto dto = WhitelistedVehicleRequestDto.builder()
          .vrn("vrn")
          .category("Early Adopter2323")
          .reasonUpdated("some reason")
          .uploaderId(UUID.randomUUID())
          .build();
      String payload = buildPayloadWith(dto);

      performCallWith(payload).andExpect(status().isBadRequest())
          .andDo(print()).andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.errors[0].status").value(HttpStatus.BAD_REQUEST.value()))
          .andExpect(jsonPath("$.errors[0].vrn").value(dto.getVrn()))
          .andExpect(jsonPath("$.errors[0].detail").value(
              "Category field should contain one of: Early Adopter, Exemption, Problematic VRN, Non-UK Vehicle, Other"));
    }

    @Test
    public void shouldReturn400BadRequestStatusCodeWhenValidationReasonFails() throws Exception {
      WhitelistedVehicleRequestDto dto = WhitelistedVehicleRequestDto.builder()
          .vrn("vrn")
          .category("Early Adopter")
          .reasonUpdated(null)
          .uploaderId(UUID.randomUUID())
          .build();
      String payload = buildPayloadWith(dto);

      performCallWith(payload)
          .andDo(print()).andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.errors[0].status").value(HttpStatus.BAD_REQUEST.value()))
          .andExpect(jsonPath("$.errors[0].vrn").value(dto.getVrn()))
          .andExpect(jsonPath("$.errors[0].detail")
              .value("Data does not include the 'reason' field which is mandatory."));
    }

    @Test
    public void shouldReturn400BadRequestStatusCodeWhenValidationManufacturerFails()
        throws Exception {
      WhitelistedVehicleRequestDto dto = WhitelistedVehicleRequestDto.builder()
          .vrn("vrn")
          .category("Early Adopter")
          .reasonUpdated("Reason")
          .manufacturer("ManufacturerMoreThan50Charactersssssssssssssssssssssssss")
          .uploaderId(UUID.randomUUID())
          .build();
      String payload = buildPayloadWith(dto);

      performCallWith(payload)
          .andDo(print()).andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.errors[0].status").value(HttpStatus.BAD_REQUEST.value()))
          .andExpect(jsonPath("$.errors[0].vrn").value(dto.getVrn()))
          .andExpect(jsonPath("$.errors[0].detail")
              .value(
                  "Invalid length of Manufacturer field (actual length: 56, max allowed length: 50)."));
    }

    @Test
    public void shouldNotAcceptEmptyInput() throws Exception {
      String payload = "";

      performCallWith(payload)
          .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldNotAcceptEmptyJson() throws Exception {
      String payload = "{}";

      performCallWith(payload)
          .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldNotAcceptMalformedJson() throws Exception {
      String payload = "{,}";

      performCallWith(payload)
          .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldReturn400BadRequestStatusCodeWhenDeletingWithoutModifierHeader()
        throws Exception {
      performDeleteCallWithoutModifierHeader()
          .andDo(print()).andExpect(status().isBadRequest());
    }
  }

  @SneakyThrows
  private String buildPayloadWith(WhitelistedVehicleRequestDto dto) {
    Mockito.when(whitelistService.save(any())).thenReturn(WhitelistVehicle.builder().build());
    return objectMapper.writeValueAsString(dto);
  }

  private ResultActions performCallWith(String payload) throws Exception {
    return mockMvc.perform(post(BASE_PATH)
        .content(payload)
        .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
        .accept(MediaType.APPLICATION_JSON_UTF8_VALUE)
        .header(X_CORRELATION_ID_HEADER, TYPICAL_CORRELATION_ID));
  }

  private ResultActions performDeleteCallWithoutModifierHeader() throws Exception {
    return mockMvc.perform(delete(BASE_PATH + "/somevrn")
        .accept(MediaType.APPLICATION_JSON_UTF8_VALUE)
        .header(X_CORRELATION_ID_HEADER, TYPICAL_CORRELATION_ID));
  }
}