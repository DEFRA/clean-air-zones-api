package uk.gov.caz.whitelist.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.caz.correlationid.Constants.X_CORRELATION_ID_HEADER;
import static uk.gov.caz.whitelist.controller.HistoricalInfoController.BASE_PATH;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.Collections;
import java.util.UUID;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.caz.GlobalExceptionHandlerConfiguration;
import uk.gov.caz.correlationid.Configuration;
import uk.gov.caz.whitelist.model.WhitelistVehicleHistorical;
import uk.gov.caz.whitelist.model.WhitelistVehicleHistory;
import uk.gov.caz.whitelist.service.WhitelistVehicleHistoryService;

@ContextConfiguration(classes = {GlobalExceptionHandlerConfiguration.class, Configuration.class,
    HistoricalInfoController.class})
@WebMvcTest
class HistoricalInfoControllerTest {

  private static final String VALID_VRN = "PAY001";
  private static final LocalDate VALID_START_DATE = LocalDate.of(2020, 1, 1);
  private static final LocalDate VALID_END_DATE = LocalDate.of(2020, 2, 29);
  private static final String VALID_PAGE_NUMBER = "2";
  private static final String VALID_PAGE_SIZE = "10";
  private static final String VALID_CORRELATION_ID = UUID.randomUUID().toString();

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockBean
  private WhitelistVehicleHistoryService service;

  @Test
  public void missingCorrelationIdShouldResultIn400AndValidMessage()
      throws Exception {
    mockMvc
        .perform(get(BASE_PATH, VALID_VRN)
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message")
            .value("Missing request header 'X-Correlation-ID'"));
  }

  @Test
  public void missingStartDateShouldResultIn400AndValidMessage()
      throws Exception {
    mockMvc
        .perform(get(BASE_PATH, VALID_VRN)
            .param("endDate", VALID_END_DATE.toString())
            .param("pageNumber", VALID_PAGE_NUMBER)
            .param("pageSize", VALID_PAGE_SIZE)
            .header(X_CORRELATION_ID_HEADER, VALID_CORRELATION_ID)
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message")
            .value("'startDate' cannot be null"));
  }

  @Test
  public void missingEndDateShouldResultIn400AndValidMessage()
      throws Exception {
    mockMvc
        .perform(get(BASE_PATH, VALID_VRN)
            .param("startDate", VALID_START_DATE.toString())
            .param("pageNumber", VALID_PAGE_NUMBER)
            .param("pageSize", VALID_PAGE_SIZE)
            .header(X_CORRELATION_ID_HEADER, VALID_CORRELATION_ID)
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message")
            .value("'endDate' cannot be null"));
  }

  @Test
  public void endaDateBeforeStartDateShouldResultIn400AndValidMessage()
      throws Exception {
    mockMvc
        .perform(get(BASE_PATH, VALID_VRN)
            .param("startDate", VALID_START_DATE.toString())
            .param("endDate", VALID_START_DATE.minusDays(2).toString())
            .param("pageNumber", VALID_PAGE_NUMBER)
            .param("pageSize", VALID_PAGE_SIZE)
            .header(X_CORRELATION_ID_HEADER, VALID_CORRELATION_ID)
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message")
            .value("'startDate' need to be before 'endDate'"));
  }

  @Test
  public void missingPageNumberShouldResultIn400AndValidMessage()
      throws Exception {
    mockMvc
        .perform(get(BASE_PATH, VALID_VRN)
            .param("startDate", VALID_START_DATE.toString())
            .param("endDate", VALID_END_DATE.toString())
            .param("pageSize", VALID_PAGE_SIZE)
            .header(X_CORRELATION_ID_HEADER, VALID_CORRELATION_ID)
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message")
            .value("'pageNumber' cannot be null"));
  }

  @Test
  public void missingPageSizeShouldResultIn400AndValidMessage()
      throws Exception {
    mockMvc
        .perform(get(BASE_PATH, VALID_VRN)
            .param("startDate", VALID_START_DATE.toString())
            .param("endDate", VALID_END_DATE.toString())
            .param("pageNumber", VALID_PAGE_NUMBER)
            .header(X_CORRELATION_ID_HEADER, VALID_CORRELATION_ID)
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message")
            .value("'pageSize' cannot be null"));
  }

  @Test
  public void validRequestShouldReturn200()
      throws Exception {
    Mockito.when(service.findByVrnInRange(any(), any())).thenReturn(
        WhitelistVehicleHistorical.builder()
            .changes(Collections.emptyList())
            .totalChangesCount(0)
            .build());
    mockMvc
        .perform(get(BASE_PATH, VALID_VRN)
            .param("startDate", VALID_START_DATE.toString())
            .param("endDate", VALID_END_DATE.toString())
            .param("pageNumber", VALID_PAGE_NUMBER)
            .param("pageSize", VALID_PAGE_SIZE)
            .header(X_CORRELATION_ID_HEADER, VALID_CORRELATION_ID)
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk());
  }

  @Test
  public void validRequestShouldReturnUploaderData()
      throws Exception {
    //given
    String uploaderEmail = RandomStringUtils.randomAlphabetic(10);
    UUID uploaderId = UUID.randomUUID();
    WhitelistVehicleHistory change = WhitelistVehicleHistory.builder()
        .modifierId(uploaderId.toString())
        .modifierEmail(uploaderEmail)
        .build();
    Mockito.when(service.findByVrnInRange(any(), any())).thenReturn(
        WhitelistVehicleHistorical.builder()
            .changes(Collections.singletonList(change))
            .totalChangesCount(1)
            .build());

    // when
    // then
    mockMvc
        .perform(get(BASE_PATH, VALID_VRN)
            .param("startDate", VALID_START_DATE.toString())
            .param("endDate", VALID_END_DATE.toString())
            .param("pageNumber", VALID_PAGE_NUMBER)
            .param("pageSize", VALID_PAGE_SIZE)
            .header(X_CORRELATION_ID_HEADER, VALID_CORRELATION_ID)
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect((jsonPath("$.changes[0].uploaderEmail")
            .value(uploaderEmail)))
        .andExpect((jsonPath("$.changes[0].uploaderId")
            .value(uploaderId.toString())));;
  }

}