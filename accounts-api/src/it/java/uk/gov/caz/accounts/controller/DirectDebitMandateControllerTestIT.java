package uk.gov.caz.accounts.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import uk.gov.caz.accounts.annotation.MockedMvcIntegrationTest;
import uk.gov.caz.accounts.dto.DirectDebitMandateRequest;
import uk.gov.caz.accounts.repository.DirectDebitMandateRepository;
import uk.gov.caz.accounts.service.DirectDebitMandateService;
import uk.gov.caz.correlationid.Constants;

@Sql(
    scripts = {
        "classpath:data/sql/add-account.sql",
        "classpath:data/sql/add-direct-debit-mandates.sql"
    },
    executionPhase = ExecutionPhase.BEFORE_TEST_METHOD
)
@Sql(
    scripts = {
        "classpath:data/sql/delete-direct-debit-mandates.sql",
        "classpath:data/sql/delete-user-data.sql"
    },
    executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
@MockedMvcIntegrationTest
class DirectDebitMandateControllerTestIT {

  private static final String DIRECT_DEBIT_MANDATE_PATH =
      DirectDebitMandateController.DIRECT_DEBIT_MANDATE_PATH;

  private static final String ANY_CORRELATION_ID = "63be7528-7efd-4f31-ae68-11a6b709ff1c";

  private static final String VALID_ACCOUNT_ID = "457a23f1-3df9-42b9-a42e-435aef201d93";
  private static final String VALID_ACCOUNT_ID_NOT_FOR_MANDATE = "b94a1a64-85b4-4dba-b8d2-e6676e91e6c2";
  private static final String INVALID_ACCOUNT_ID = "732a23f1-2df9-28c1-b33d-435aef201d9";
  private static final String ANY_CLEAN_AIR_ZONE_ID = "ed2f3499-f888-4b0a-9bc1-2a9f2c91b0d8";
  private static final String ANY_MANDATE_ID = "abc123";
  private static final String EXISTING_MANDATE_ID = "jhjcvaiqlediuhh23d89hd3";
  private static final String VALID_DIRECT_DEBIT_MANDATE_ID = "1825761b-304e-416f-89ab-c74177591345";
  private static final String INVALID_DIRECT_DEBIT_MANDATE_ID = "9b910a77-f9f1-425d-a2b4-9f709b57924c";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Autowired
  private DirectDebitMandateService directDebitMandateService;

  @Autowired
  private DirectDebitMandateRepository directDebitMandateRepository;

  @Test
  public void shouldCreateNewDirectDebitMandateOnValidRequest() throws Exception {
    performValidCreateRequestWithPayload(validRequestPayload(ANY_MANDATE_ID))
        .andExpect(header().string(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.directDebitMandateId").exists())
        .andExpect(jsonPath("$.accountId").value(VALID_ACCOUNT_ID))
        .andExpect(jsonPath("$.cleanAirZoneId").value(ANY_CLEAN_AIR_ZONE_ID))
        .andExpect(jsonPath("$.paymentProviderMandateId").value(ANY_MANDATE_ID));

    verifyThatDirectDebitMandateCountIsEqualTo(2);
  }

  @Test
  public void shouldNotCreateNewDirectDebitMandateOnInvalidRequest() throws Exception {
    performValidCreateRequestWithPayload(emptyRequestPayload())
        .andExpect(header().string(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID))
        .andExpect(status().isBadRequest());

    verifyThatDirectDebitMandateCountIsEqualTo(1);
  }

  @Test
  public void shouldReturnErrorWhenAccountDoesNotExist() throws Exception {
    mockMvc.perform(post(DIRECT_DEBIT_MANDATE_PATH, INVALID_ACCOUNT_ID)
        .content(validRequestPayload(ANY_MANDATE_ID))
        .header(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID)
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Account was not found."));
  }

  @Test
  public void shouldReturnErrorWhenDirectDebitMandateIdAlreadyExists() throws Exception {
    mockMvc.perform(post(DIRECT_DEBIT_MANDATE_PATH, VALID_ACCOUNT_ID)
        .content(validRequestPayload(EXISTING_MANDATE_ID))
        .header(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID)
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.message").value("Direct debit mandate with id "
            + EXISTING_MANDATE_ID + " already exists."));

  }

  @Test
  public void shouldFindDirectDebitMandateIfExistsInDB() throws Exception {
    mockMvc.perform(get(DIRECT_DEBIT_MANDATE_PATH, VALID_ACCOUNT_ID)
        .header(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID)
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON))
        .andExpect(header().string(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.directDebitMandates").exists())
        .andExpect(jsonPath("$.directDebitMandates").isNotEmpty());
  }

  @Test
  public void shouldNotFindDirectDebitMandateIfDoesNotExistsInDB() throws Exception {
    mockMvc.perform(get(DIRECT_DEBIT_MANDATE_PATH, UUID.randomUUID())
        .header(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID)
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON))
        .andExpect(header().string(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.directDebitMandates").exists())
        .andExpect(jsonPath("$.directDebitMandates").isEmpty());
  }

  @Test
  public void shouldDeleteDirectDebitMandateOnValidRequest() throws Exception {
    verifyThatDirectDebitMandateCountIsEqualTo(1);

    performDeleteRequestWithPayload(VALID_ACCOUNT_ID, VALID_DIRECT_DEBIT_MANDATE_ID)
        .andExpect(header().string(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID))
        .andExpect(status().isNoContent());

    verifyThatDirectDebitMandateCountIsEqualTo(0);
  }

  @Test
  public void shouldNotDeleteDirectDebitMandateOnInvalidRequest() throws Exception {
    verifyThatDirectDebitMandateCountIsEqualTo(1);

    performDeleteRequestWithPayload(INVALID_ACCOUNT_ID, VALID_DIRECT_DEBIT_MANDATE_ID)
        .andExpect(header().string(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Account was not found."));

    verifyThatDirectDebitMandateCountIsEqualTo(1);

    performDeleteRequestWithPayload(VALID_ACCOUNT_ID, INVALID_DIRECT_DEBIT_MANDATE_ID)
        .andExpect(header().string(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Direct Debit Mandate not found."));

    verifyThatDirectDebitMandateCountIsEqualTo(1);

    performDeleteRequestWithPayload(VALID_ACCOUNT_ID_NOT_FOR_MANDATE, VALID_DIRECT_DEBIT_MANDATE_ID)
        .andExpect(header().string(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID))
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.message").value("DirectDebitMandate does not belongs to provided Account"));

    verifyThatDirectDebitMandateCountIsEqualTo(1);
  }

  private ResultActions performValidCreateRequestWithPayload(String payload) throws Exception {
    return mockMvc.perform(post(DIRECT_DEBIT_MANDATE_PATH, VALID_ACCOUNT_ID)
        .content(payload)
        .header(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID)
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON));
  }

  private ResultActions performDeleteRequestWithPayload(String accountId, String mandateId)
      throws Exception {
    return mockMvc.perform(delete(DIRECT_DEBIT_MANDATE_PATH + "/{mandateId}", accountId, mandateId)
        .header(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID)
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON));
  }

  private void verifyThatDirectDebitMandateCountIsEqualTo(int expectedCount) {
    int mandateCount = JdbcTestUtils
        .countRowsInTable(jdbcTemplate, "caz_account.T_ACCOUNT_DIRECT_DEBIT_MANDATE");
    assertThat(mandateCount).isEqualTo(expectedCount);
  }

  private String emptyRequestPayload() {
    DirectDebitMandateRequest request = DirectDebitMandateRequest.builder().build();
    return toJson(request);
  }

  private String validRequestPayload(String paymentProviderMandateId) {
    DirectDebitMandateRequest request = DirectDebitMandateRequest.builder()
        .cleanAirZoneId(UUID.fromString(ANY_CLEAN_AIR_ZONE_ID))
        .mandateId(paymentProviderMandateId)
        .build();
    return toJson(request);
  }

  @SneakyThrows
  private String toJson(Object object) {
    return objectMapper.writeValueAsString(object);
  }
}