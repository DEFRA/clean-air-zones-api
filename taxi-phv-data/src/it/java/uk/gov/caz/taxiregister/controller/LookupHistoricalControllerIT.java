package uk.gov.caz.taxiregister.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.caz.taxiregister.controller.Constants.CORRELATION_ID_HEADER;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import uk.gov.caz.taxiregister.annotation.MockedMvcIntegrationTest;

@MockedMvcIntegrationTest
@Sql(scripts = {
    "classpath:data/sql/licensing-authority-data.sql",
    "classpath:data/sql/taxi-phv-data.sql",
    "classpath:data/sql/taxi-phv-data-historical.sql"
},
    executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "classpath:data/sql/clear.sql", executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
@Slf4j
class LookupHistoricalControllerIT {

  private static final String VRM = "EB12QMD";
  private static final String START_DATE = "2000-01-01";
  private static final String END_DATE = "2020-02-02";
  private static final String PAGE_NUMBER = "0";
  private static final String PAGE_SIZE = "2";

  @Autowired
  private MockMvc mockMvc;

  @Test
  void shouldReturnExpectedValue() throws Exception {
    //given
    MockHttpServletRequestBuilder accept = get(LookupHistoricalController.PATH, VRM)
        .header(CORRELATION_ID_HEADER, UUID.randomUUID().toString())
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .accept(MediaType.APPLICATION_JSON_VALUE)
        .param("startDate", "2017-03-01")
        .param("endDate", "2017-03-01")
        .param("pageNumber", "0")
        .param("pageSize", "10");
    //when
    ResultActions perform = mockMvc.perform(accept);

    //then
    perform.andExpect(status().isOk())
        .andExpect(jsonPath("$.changes", hasSize(1)))
        .andExpect(jsonPath("$.page").value(0))
        .andExpect(jsonPath("$.totalChangesCount").value(1))
        .andExpect(jsonPath("$.perPage").value(10))
        .andExpect(jsonPath("$.pageCount").value(1))
        .andExpect(jsonPath("$.changes[0].modifyDate").value("2017-03-01"))
        .andExpect(jsonPath("$.changes[0].action").value("Created"))
        .andExpect(jsonPath("$.changes[0].licensingAuthorityName").value("la-2"))
        .andExpect(jsonPath("$.changes[0].plateNumber").value("plate-no-91"))
        .andExpect(jsonPath("$.changes[0].licenceStartDate").value("2019-05-22"))
        .andExpect(
            jsonPath("$.changes[0].licenceEndDate").value("2099-11-27"))
        .andExpect(jsonPath("$.changes[0].wheelchairAccessible").isEmpty());
  }

  @ParameterizedTest
  @MethodSource(
      "uk.gov.caz.taxiregister.repository.TaxiPhvLicenceHistoryPostgresRepositoryIT#pagingProperties")
  void shouldReturnExpectedResultsSize(LocalDate startDate, LocalDate endDate,
      long pageNumber, long pageSize, int expectedResultsCount) throws Exception {
    //given
    MockHttpServletRequestBuilder accept = get(LookupHistoricalController.PATH, VRM)
        .header(CORRELATION_ID_HEADER, UUID.randomUUID().toString())
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .accept(MediaType.APPLICATION_JSON_VALUE)
        .param("startDate", String.valueOf(startDate))
        .param("endDate", String.valueOf(endDate))
        .param("pageNumber", String.valueOf(pageNumber))
        .param("pageSize", String.valueOf(pageSize));
    //when
    ResultActions perform = mockMvc.perform(accept);

    //then
    perform.andExpect(status().isOk())
        .andExpect(jsonPath("$.changes", hasSize(expectedResultsCount)));
  }

  @Test
  public void shouldReturnDataForTheProvidedDateRangeUsingLocalTimeWhenBST() throws Exception {
    //given
    String vrm = "BST123";
    MockHttpServletRequestBuilder accept = get(LookupHistoricalController.PATH, vrm)
        .header(CORRELATION_ID_HEADER, UUID.randomUUID().toString())
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .accept(MediaType.APPLICATION_JSON_VALUE)
        .param("startDate", "2020-07-16") // 2020-07-15 in UTC
        .param("endDate", "2020-07-16")
        .param("pageNumber", "0")
        .param("pageSize", "10");
    //when
    ResultActions perform = mockMvc.perform(accept);

    //then
    perform.andExpect(status().isOk())
        .andExpect(jsonPath("$.changes", hasSize(1)))
        .andExpect(jsonPath("$.page").value(0))
        .andExpect(jsonPath("$.totalChangesCount").value(1))
        .andExpect(jsonPath("$.perPage").value(10))
        .andExpect(jsonPath("$.pageCount").value(1))
        .andExpect(jsonPath("$.changes[0].modifyDate").value("2020-07-16"))
        .andExpect(jsonPath("$.changes[0].action").value("Updated"));
  }

  @Test
  public void shouldReturnDataForTheProvidedDateRangeUsingLocalTimeWhenWinterTime()
      throws Exception {
    //given
    String vrm = "WNTR123";
    MockHttpServletRequestBuilder accept = get(LookupHistoricalController.PATH, vrm)
        .header(CORRELATION_ID_HEADER, UUID.randomUUID().toString())
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .accept(MediaType.APPLICATION_JSON_VALUE)
        .param("startDate", "2020-12-15")
        .param("endDate", "2020-12-15")
        .param("pageNumber", "0")
        .param("pageSize", "10");
    //when
    ResultActions perform = mockMvc.perform(accept);

    //then
    perform.andExpect(status().isOk())
        .andExpect(jsonPath("$.changes", hasSize(1)))
        .andExpect(jsonPath("$.page").value(0))
        .andExpect(jsonPath("$.totalChangesCount").value(1))
        .andExpect(jsonPath("$.perPage").value(10))
        .andExpect(jsonPath("$.pageCount").value(1))
        .andExpect(jsonPath("$.changes[0].modifyDate").value("2020-12-15"))
        .andExpect(jsonPath("$.changes[0].action").value("Updated"));
  }

  @Nested
  class BadRequest {

    @ParameterizedTest
    @MethodSource(
        "uk.gov.caz.taxiregister.controller.LookupHistoricalControllerIT#nullableRequestProperties")
    void shouldReturnBadRequestIfAnyRequestPropertyIsNull(String vrm, String startDate,
        String endDate,
        String pageNumber, String pageSize) throws Exception {
      //given
      MockHttpServletRequestBuilder accept = get(LookupHistoricalController.PATH, vrm)
          .header(CORRELATION_ID_HEADER, UUID.randomUUID().toString())
          .contentType(MediaType.APPLICATION_JSON_VALUE)
          .accept(MediaType.APPLICATION_JSON_VALUE);
      Optional.ofNullable(startDate).ifPresent(s -> accept.param("startDate", s));
      Optional.ofNullable(endDate).ifPresent(s -> accept.param("endDate", s));
      Optional.ofNullable(pageNumber).ifPresent(s -> accept.param("pageNumber", s));
      Optional.ofNullable(pageSize).ifPresent(s -> accept.param("pageSize", s));

      //when
      ResultActions perform = mockMvc.perform(accept);

      //then
      perform.andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnNotFoundIfVrmIsNull() throws Exception {
      //given
      String vrm = null;
      MockHttpServletRequestBuilder accept = get(LookupHistoricalController.PATH, vrm)
          .header(CORRELATION_ID_HEADER, UUID.randomUUID().toString())
          .contentType(MediaType.APPLICATION_JSON_VALUE)
          .accept(MediaType.APPLICATION_JSON_VALUE)
          .param("startDate", START_DATE)
          .param("endDate", END_DATE)
          .param("pageNumber", PAGE_NUMBER)
          .param("pageSize", PAGE_SIZE);

      //when
      ResultActions perform = mockMvc.perform(accept);

      //then
      perform.andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @MethodSource(
        "uk.gov.caz.taxiregister.controller.LookupHistoricalControllerTest#notParsableRequestProperties")
    void shouldReturnBadRequestIfAnyRequestPropertyIsNotParsable(String vrm, String startDate,
        String endDate, String pageNumber, String pageSize) throws Exception {
      //given
      MockHttpServletRequestBuilder accept = get(LookupHistoricalController.PATH, vrm)
          .header(CORRELATION_ID_HEADER, UUID.randomUUID().toString())
          .contentType(MediaType.APPLICATION_JSON_VALUE)
          .accept(MediaType.APPLICATION_JSON_VALUE)
          .param("startDate", startDate)
          .param("endDate", endDate)
          .param("pageNumber", pageNumber)
          .param("pageSize", pageSize);
      //when
      ResultActions perform = mockMvc.perform(accept);
      //then
      perform.andExpect(status().isBadRequest());
    }
  }

  static Stream<Arguments> nullableRequestProperties() {
    return Stream.of(
        Arguments.arguments(VRM, null, END_DATE, PAGE_NUMBER, PAGE_SIZE),
        Arguments.arguments(VRM, START_DATE, null, PAGE_NUMBER, PAGE_SIZE),
        Arguments.arguments(VRM, START_DATE, END_DATE, null, PAGE_SIZE),
        Arguments.arguments(VRM, START_DATE, END_DATE, PAGE_NUMBER, null)
    );
  }
}