package uk.gov.caz.accounts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
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
import uk.gov.caz.accounts.controller.DirectDebitMandateController;
import uk.gov.caz.accounts.dto.DirectDebitMandatesUpdateRequest;
import uk.gov.caz.accounts.dto.DirectDebitMandatesUpdateRequest.SingleDirectDebitMandateUpdate;
import uk.gov.caz.accounts.model.DirectDebitMandateStatus;
import uk.gov.caz.accounts.repository.AccountRepository;
import uk.gov.caz.accounts.repository.DirectDebitMandateRepository;
import uk.gov.caz.accounts.service.DirectDebitMandateUpdateErrorsCollector;
import uk.gov.caz.accounts.service.DirectDebitMandatesBulkUpdater;
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
    executionPhase = ExecutionPhase.AFTER_TEST_METHOD
)
@MockedMvcIntegrationTest
public class UpdateListOfDirectDebitMandatesTestIT {

  private static final String DIRECT_DEBIT_MANDATE_PATH =
      DirectDebitMandateController.DIRECT_DEBIT_MANDATE_PATH;

  private static final String ANY_CORRELATION_ID = "63be7528-7efd-4f31-ae68-11a6b709ff1c";
  private static final String VALID_ACCOUNT_ID = "457a23f1-3df9-42b9-a42e-435aef201d93";
  private static final String ANY_MANDATE_ID = "jhjcvaiqlediuhh23d89hd3";
  private static final String INVALID_STATUS = "INVALID-STATUS";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Autowired
  private DirectDebitMandatesBulkUpdater directDebitMandatesBulkUpdater;

  @Autowired
  private AccountRepository accountRepository;

  @Autowired
  private DirectDebitMandateRepository directDebitMandateRepository;

  @Autowired
  private DirectDebitMandateUpdateErrorsCollector directDebitMandateUpdateErrorsCollector;

  @Test
  public void shouldReturnErrorsListOnInvalidRequest() throws Exception {
    String payload = requestPayloadWithStatus(INVALID_STATUS);

    performRequestWithPayload(payload)
        .andExpect(header().string(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].message").value("Invalid direct debit mandate status"));
  }

  @Test
  public void shouldNotUpdateRecordsOnInvalidRequest() throws Exception {
    String payload = requestPayloadWithStatus(INVALID_STATUS);
    performRequestWithPayload(payload);

    verifyThatExistingDirectDebitMandateHasSubmittedStatus();
  }

  @Test
  public void shouldUpdateRecordsOnValidRequest() throws Exception {
    String payload = requestPayloadWithStatus(DirectDebitMandateStatus.ACTIVE.toString());

    performRequestWithPayload(payload)
        .andExpect(header().string(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("Direct debit mandates updated successfully"));

  }

  private String requestPayloadWithStatus(String status) {
    SingleDirectDebitMandateUpdate singleUpdate = SingleDirectDebitMandateUpdate.builder()
        .status(status)
        .mandateId(ANY_MANDATE_ID)
        .build();

    DirectDebitMandatesUpdateRequest request = DirectDebitMandatesUpdateRequest.builder()
        .directDebitMandates(Arrays.asList(singleUpdate))
        .build();

    return toJson(request);
  }

  private ResultActions performRequestWithPayload(String payload) throws Exception {
    return mockMvc.perform(patch(DIRECT_DEBIT_MANDATE_PATH, VALID_ACCOUNT_ID)
        .content(payload)
        .header(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID)
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON));
  }

  private void verifyThatExistingDirectDebitMandateHasSubmittedStatus() {
    int createdMandatesCount = JdbcTestUtils
        .countRowsInTableWhere(jdbcTemplate,
            "caz_account.T_ACCOUNT_DIRECT_DEBIT_MANDATE",
            "status = 'SUBMITTED'");

    assertThat(createdMandatesCount).isEqualTo(1);
  }

  @SneakyThrows
  private String toJson(Object object) {
    return objectMapper.writeValueAsString(object);
  }
}
