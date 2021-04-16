package uk.gov.caz.accounts.controller;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.List;
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
import uk.gov.caz.accounts.dto.DirectDebitMandateRequest;
import uk.gov.caz.accounts.dto.DirectDebitMandatesUpdateRequest;
import uk.gov.caz.accounts.dto.DirectDebitMandatesUpdateRequest.SingleDirectDebitMandateUpdate;
import uk.gov.caz.accounts.model.DirectDebitMandate;
import uk.gov.caz.accounts.model.DirectDebitMandateStatus;
import uk.gov.caz.accounts.service.DirectDebitMandateService;
import uk.gov.caz.accounts.service.DirectDebitMandatesBulkUpdater;
import uk.gov.caz.accounts.service.exception.AccountNotFoundException;
import uk.gov.caz.accounts.service.exception.DirectDebitMandateDoesNotBelongsToAccountException;
import uk.gov.caz.accounts.service.exception.DirectDebitMandateNotFoundException;
import uk.gov.caz.correlationid.Configuration;
import uk.gov.caz.correlationid.Constants;

@ContextConfiguration(classes = {
    Configuration.class,
    GlobalExceptionHandlerConfiguration.class,
    DirectDebitMandateController.class
})
@WebMvcTest
class DirectDebitMandateControllerTest {

  @MockBean
  private DirectDebitMandateService directDebitMandateService;

  @MockBean
  private DirectDebitMandatesBulkUpdater directDebitMandatesBulkUpdater;

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  private static final String DIRECT_DEBIT_MANDATE_PATH =
      DirectDebitMandateController.DIRECT_DEBIT_MANDATE_PATH;

  private static final String ANY_CORRELATION_ID = "03d339e2-875f-4b3f-9dfa-1f6aa57cc119";
  private static final String ANY_ACCOUNT_ID = "b6968560-cb56-4248-9f8f-d75b0aff726e";
  private static final String ANY_ACCOUNT_USER_ID = "f64a06aa-347b-4852-966a-1441b04679f0";
  private static final String ANY_MANDATE_ID = "jhjcvaiqlediuhh23d89hd3";
  private static final String ANY_CAZ_ID = "6dda58e0-d215-4ce1-a7ca-653f860eaa3c";
  private static final String ANY_STATUS = "ACTIVE";
  private static final String ANY_DIRECT_DEBIT_MANDATE_ID = "69f2ad7e-34c3-40d1-9137-1f2f2e93d9db";

  @Nested
  class Create {

    @Test
    public void shouldReturn400WhenDirectDebitMandateIdIsNull() throws Exception {
      UUID cleanAirZoneId = UUID.fromString(ANY_CAZ_ID);
      UUID accountUserId = UUID.fromString(ANY_ACCOUNT_USER_ID);
      String payload = requestPayloadFor(null, cleanAirZoneId, accountUserId);

      performRequestWithPayload(payload)
          .andExpect(header().string(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID))
          .andExpect(status().is4xxClientError());
    }

    @Test
    public void shouldReturn400WhenCleanAirZoneIdIsNull() throws Exception {
      UUID accountUserId = UUID.fromString(ANY_ACCOUNT_USER_ID);
      String payload = requestPayloadFor(ANY_MANDATE_ID, null, accountUserId);

      performRequestWithPayload(payload)
          .andExpect(header().string(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID))
          .andExpect(status().is4xxClientError());

    }

    @Test
    public void shouldReturn400WhenAccountUserIdIsNull() throws Exception {
      UUID cleanAirZoneId = UUID.fromString(ANY_CAZ_ID);
      String payload = requestPayloadFor(ANY_MANDATE_ID, cleanAirZoneId, null);

      performRequestWithPayload(payload)
          .andExpect(header().string(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID))
          .andExpect(status().is4xxClientError());
    }

    @Test
    public void shouldReturn200WhenParamsAreCorrect() throws Exception {
      UUID cleanAirZoneId = UUID.fromString(ANY_CAZ_ID);
      UUID accountUserId = UUID.fromString(ANY_ACCOUNT_USER_ID);
      String payload = requestPayloadFor(ANY_MANDATE_ID, cleanAirZoneId, accountUserId);
      mockSuccessfulDirectDebitMandateCreation();

      performRequestWithPayload(payload)
          .andExpect(header().string(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID))
          .andExpect(status().isCreated());
    }

    private void mockSuccessfulDirectDebitMandateCreation() {
      when(directDebitMandateService.create(any(), any()))
          .thenReturn(persistedDirectDebitMandateMock());
    }

    private DirectDebitMandate persistedDirectDebitMandateMock() {
      return DirectDebitMandate.builder()
          .id(UUID.randomUUID())
          .accountId(UUID.fromString(ANY_ACCOUNT_ID))
          .accountUserId(UUID.fromString(ANY_ACCOUNT_USER_ID))
          .cleanAirZoneId(UUID.fromString(ANY_CAZ_ID))
          .paymentProviderMandateId(ANY_MANDATE_ID)
          .build();
    }

    private String requestPayloadFor(String debitMandateId, UUID cleanAirZoneId,
        UUID accountUserId) {
      DirectDebitMandateRequest request = DirectDebitMandateRequest.builder()
          .mandateId(debitMandateId)
          .cleanAirZoneId(cleanAirZoneId)
          .accountUserId(accountUserId)
          .build();

      return toJson(request);
    }

    private ResultActions performRequestWithPayload(String payload) throws Exception {
      return mockMvc.perform(post(DIRECT_DEBIT_MANDATE_PATH, ANY_ACCOUNT_ID)
          .content(payload)
          .header(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID)
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON));
    }
  }

  @Nested
  class GetDirectDebitMandates {

    @Test
    public void missingCorrelationIdShouldResultIn400AndValidMessage() throws Exception {
      mockMvc.perform(get(DIRECT_DEBIT_MANDATE_PATH, ANY_ACCOUNT_ID)
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldReturn200WhenParamsAreCorrect() throws Exception {
      mockSuccessfulFoundDirectDebitMandate();

      mockMvc.perform(get(DIRECT_DEBIT_MANDATE_PATH, ANY_ACCOUNT_ID)
          .header(Constants.X_CORRELATION_ID_HEADER, UUID.randomUUID())
          .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk());
    }

    private void mockSuccessfulFoundDirectDebitMandate() {
      when(directDebitMandateService.findAllByAccountId(any()))
          .thenReturn(persistedDirectDebitMandatesMock());
    }

    private List<DirectDebitMandate> persistedDirectDebitMandatesMock() {
      return Arrays.asList(
          DirectDebitMandate.builder()
              .id(UUID.randomUUID())
              .accountId(UUID.fromString(ANY_ACCOUNT_ID))
              .accountUserId(UUID.fromString(ANY_ACCOUNT_USER_ID))
              .cleanAirZoneId(UUID.fromString(ANY_CAZ_ID))
              .paymentProviderMandateId(ANY_MANDATE_ID)
              .status(DirectDebitMandateStatus.valueOf(ANY_STATUS))
              .build()
      );
    }
  }

  @Nested
  class UpdateList {

    @Test
    public void shouldReturn400WhenDirectDebitMandatesIsNull() throws Exception {
      String payload = emptyUpdateRequestPayload();

      performRequestWithPayload(payload)
          .andExpect(header().string(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.message").value("directDebitMandates cannot be null."));
    }

    @Test
    public void shouldReturn400WhenNestedMandateIdIsBlank() throws Exception {
      String payload = payloadWithParameters("", ANY_STATUS);

      performRequestWithPayload(payload)
          .andExpect(header().string(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.message").value("mandateId cannot be empty."));
    }

    @Test
    public void shouldReturn400WhenNestedStatusIsBlank() throws Exception {
      String payload = payloadWithParameters(ANY_MANDATE_ID, "");

      performRequestWithPayload(payload)
          .andExpect(header().string(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.message").value("status cannot be empty."));
    }

    @Test
    public void shouldReturn200WhenParamsAreValid() throws Exception {
      String payload = payloadWithParameters(ANY_MANDATE_ID, ANY_STATUS);
      mockValidUpdate();

      performRequestWithPayload(payload)
          .andExpect(header().string(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.message").value("Direct debit mandates updated successfully"));

    }

    private void mockValidUpdate() {
      doNothing()
          .when(directDebitMandatesBulkUpdater)
          .updateStatuses(any(), any());
    }

    private String payloadWithParameters(String mandateId, String status) {
      SingleDirectDebitMandateUpdate singleUpdate = buildSingleDirectDebitMandateUpdate(
          mandateId, status);
      DirectDebitMandatesUpdateRequest request = buildUpdateRequestWith(
          Arrays.asList(singleUpdate));

      return toJson(request);
    }

    private String emptyUpdateRequestPayload() {
      DirectDebitMandatesUpdateRequest request = buildUpdateRequestWith(null);
      return toJson(request);
    }

    private DirectDebitMandatesUpdateRequest buildUpdateRequestWith(
        List<SingleDirectDebitMandateUpdate> update) {
      return DirectDebitMandatesUpdateRequest.builder()
          .directDebitMandates(update)
          .build();
    }

    private SingleDirectDebitMandateUpdate buildSingleDirectDebitMandateUpdate(String mandateId,
        String status) {
      return SingleDirectDebitMandateUpdate.builder()
          .mandateId(mandateId)
          .status(status)
          .build();
    }

    private ResultActions performRequestWithPayload(String payload) throws Exception {
      return mockMvc.perform(patch(DIRECT_DEBIT_MANDATE_PATH, ANY_ACCOUNT_ID)
          .content(payload)
          .header(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID)
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON));
    }
  }

  @Nested
  class Delete {

    @Test
    public void shouldReturn404WhenAccountIdDoesNotExists() throws Exception {
      mockAccountNotFound();

      performDeleteRequest()
          .andExpect(header().string(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID))
          .andExpect(jsonPath("$.message", is("Account was not found.")))
          .andExpect(status().isNotFound());
    }

    @Test
    public void shouldReturn404WhenDirectDebitMandateDoesNotExists() throws Exception {
      mockDirectDebitMandateNotFound();

      performDeleteRequest()
          .andExpect(header().string(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID))
          .andExpect(jsonPath("$.message", is("Direct Debit Mandate not found.")))
          .andExpect(status().isNotFound());
    }

    @Test
    public void shouldReturn400WhenDirectDebitMandateDoesNotBelongsToAccountExists()
        throws Exception {
      mockDirectDebitMandateDoesNotBelongsToAccount();

      performDeleteRequest()
          .andExpect(header().string(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID))
          .andExpect(
              jsonPath("$.message", is("DirectDebitMandate does not belongs to provided Account")))
          .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldReturn204WhenDirectDebitMandateDeleted() throws Exception {
      mockDirectDebitMandateDeleted();

      performDeleteRequest()
          .andExpect(header().string(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID))
          .andExpect(status().isNoContent());
    }

    private void mockAccountNotFound() {
      doThrow(new AccountNotFoundException("Account was not found."))
          .when(directDebitMandateService).delete(any(UUID.class), any(UUID.class));
    }

    private void mockDirectDebitMandateNotFound() {
      doThrow(new DirectDebitMandateNotFoundException("Direct Debit Mandate not found."))
          .when(directDebitMandateService).delete(any(UUID.class), any(UUID.class));
    }

    private void mockDirectDebitMandateDoesNotBelongsToAccount() {
      doThrow(new DirectDebitMandateDoesNotBelongsToAccountException(
          "DirectDebitMandate does not belongs to provided Account"))
          .when(directDebitMandateService).delete(any(UUID.class), any(UUID.class));
    }

    private void mockDirectDebitMandateDeleted() {
      doNothing().when(directDebitMandateService).delete(any(UUID.class), any(UUID.class));
    }

    private ResultActions performDeleteRequest() throws Exception {
      return mockMvc.perform(delete(DIRECT_DEBIT_MANDATE_PATH + "/{mandateId}", ANY_ACCOUNT_ID,
          ANY_DIRECT_DEBIT_MANDATE_ID)
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
