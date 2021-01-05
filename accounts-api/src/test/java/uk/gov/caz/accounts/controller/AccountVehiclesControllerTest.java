package uk.gov.caz.accounts.controller;

import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import uk.gov.caz.GlobalExceptionHandlerConfiguration;
import uk.gov.caz.accounts.controller.util.QueryStringValidator;
import uk.gov.caz.accounts.dto.AccountVehicleRequest;
import uk.gov.caz.accounts.model.AccountVehicle;
import uk.gov.caz.accounts.model.VehicleChargeability;
import uk.gov.caz.accounts.service.AccountVehicleService;
import uk.gov.caz.accounts.service.generatecsv.CsvFileSupervisor;
import uk.gov.caz.correlationid.Configuration;
import uk.gov.caz.correlationid.Constants;

@ContextConfiguration(classes = {
    GlobalExceptionHandlerConfiguration.class,
    Configuration.class,
    AccountVehiclesController.class,
    QueryStringValidator.class
})
@WebMvcTest
class AccountVehiclesControllerTest {

  @MockBean
  private AccountVehicleService accountVehicleService;

  @MockBean
  private CsvFileSupervisor csvFileSupervisor;

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  private static final String ACCOUNT_VEHICLE_PATH = AccountVehiclesController.ACCOUNT_VEHICLES_PATH;
  private static final String ACCOUNT_SINGLE_VEHICLE_PATH =
      ACCOUNT_VEHICLE_PATH + AccountVehiclesController.SINGLE_VEHICLE_PATH_SEGMENT;

  private static final String ANY_CORRELATION_ID = "03d339e2-875f-4b3f-9dfa-1f6aa57cc119";
  private static final String ANY_ACCOUNT_ID = "b6968560-cb56-4248-9f8f-d75b0aff726e";
  private static final String ANY_VRN = "CAS246";
  private static final String DEFINITELY_TOO_LONG_VRN = "This VRN is definitely too long";

  @Nested
  public class AddingSingleVehicle {

    @Test
    public void shouldReturn400WhenVrnIsMissing() throws Exception {
      String payload = emptyRequestPayload();

      performRequestWithPayload(payload)
          .andExpect(header().string(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.message").value("VRN cannot be blank"));
    }

    @Test
    public void shouldReturn400WhenVrnIsTooLong() throws Exception {
      String tooLongVrn = "CAS123CAS234CAS345";
      String payload = requestWithVrn(tooLongVrn);

      performRequestWithPayload(payload)
          .andExpect(header().string(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.message").value("VRN is too long"));
    }

    @Test
    public void shouldReturn200WhenVrnIsCorrect() throws Exception {
      String payload = requestWithVrn(ANY_VRN);
      mockValidAccountVehicleCreation();

      performRequestWithPayload(payload)
          .andExpect(header().string(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.vrn").value(ANY_VRN));
    }

    private void mockValidAccountVehicleCreation() {
      AccountVehicle createdAccountVehicle = AccountVehicle.builder()
          .accountId(UUID.fromString(ANY_ACCOUNT_ID))
          .vrn(ANY_VRN)
          .build();

      when(accountVehicleService.createAccountVehicle(any(), any()))
          .thenReturn(createdAccountVehicle);
    }

    private ResultActions performRequestWithPayload(String payload) throws Exception {
      return mockMvc.perform(post(ACCOUNT_VEHICLE_PATH, ANY_ACCOUNT_ID).content(payload)
          .header(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID)
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON));
    }

    private String emptyRequestPayload() {
      return toJson(emptyRequest());
    }

    private String requestWithVrn(String vrn) {
      AccountVehicleRequest request = AccountVehicleRequest.builder().vrn(vrn).build();
      return toJson(request);
    }

    private AccountVehicleRequest emptyRequest() {
      return AccountVehicleRequest.builder().build();
    }
  }

  @Nested
  public class DeletingVehicle {

    @Test
    public void shouldReturn400WhenVrnIsEmpty() throws Exception {
      performDeleteRequest(ANY_ACCOUNT_ID, DEFINITELY_TOO_LONG_VRN)
          .andExpect(header().string(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.message").value("VRN cannot be longer than 15 characters"));
    }

    private ResultActions performDeleteRequest(String accountId, String vrn) throws Exception {
      return mockMvc.perform(delete(ACCOUNT_SINGLE_VEHICLE_PATH, accountId, vrn)
          .header(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID)
          .accept(MediaType.APPLICATION_JSON));
    }
  }

  @Nested
  public class RetrievingSingleVehicle {

    @Test
    public void shouldReturn400WhenVrnIsTooLong() throws Exception {
      String tooLongVrn = "CAS123CAS234CAS345";

      performGetRequest(ANY_ACCOUNT_ID, tooLongVrn)
          .andExpect(header().string(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.message").value("VRN cannot be longer than 15 characters"));
    }

    @Test
    public void shouldReturn200WhenVrnIsFound() throws Exception {
      String validAccountId = "724539f5-ffb7-478d-98bf-29c490ba2b0d";
      String vrn = "CAS123";

      mockSingleVehicleResponse(vrn);

      performGetRequest(validAccountId, vrn)
          .andExpect(header().string(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID))
          .andExpect(status().is2xxSuccessful());
    }

    @Test
    public void shouldReturn404WhenVrnIsNotFound() throws Exception {
      String validAccountId = "724539f5-ffb7-478d-98bf-29c490ba2b0d";
      String vrn = "CAS123";

      mockSingleVehicleNotFoundResponse();

      performGetRequest(validAccountId, vrn)
          .andExpect(header().string(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID))
          .andExpect(status().is4xxClientError());
    }

    private void mockSingleVehicleResponse(String mockVrn) {
      AccountVehicle accountVehicleResponse =
          AccountVehicle
              .builder()
              .vrn(mockVrn)
              .accountId(UUID.randomUUID())
              .vehicleChargeability(singletonList(
                  VehicleChargeability
                      .builder()
                      .accountVehicleId(UUID.randomUUID())
                      .isExempt(true)
                      .isRetrofitted(true)
                      .build()))
              .build();

      when(accountVehicleService.getAccountVehicleWithChargeability(any(), any()))
          .thenReturn(Optional.ofNullable(accountVehicleResponse));
    }

    private void mockSingleVehicleNotFoundResponse() {
      when(accountVehicleService.getAccountVehicleWithChargeability(any(), any()))
          .thenReturn(Optional.empty());
    }

    private ResultActions performGetRequest(String accountId, String vrn) throws Exception {
      return mockMvc.perform(get(ACCOUNT_SINGLE_VEHICLE_PATH, accountId, vrn)
          .header(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID)
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON));
    }
  }

  @SneakyThrows
  private String toJson(Object object) {
    return objectMapper.writeValueAsString(object);
  }
}