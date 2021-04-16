package uk.gov.caz.whitelist.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.caz.whitelist.controller.Constants.CORRELATION_ID_HEADER;

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
import uk.gov.caz.whitelist.annotation.MockedMvcIntegrationTest;

@MockedMvcIntegrationTest
@Sql(scripts = {"classpath:data/sql/clear-whitelist-vehicles-data.sql",
    "classpath:data/sql/whitelist-vehicles-data.sql",
    "classpath:data/sql/whitelist-vehicles-data-historical.sql"}, executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "classpath:data/sql/clear-whitelist-vehicles-data.sql", executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
@Slf4j
class HistoricalInfoControllerIT {

  private static final String VRM = "EB12QMD";
  private static final String SUMMER_VRN = "SUM123";
  private static final String WINTER_VRN = "WIN123";
  private static final String START_DATE = "2000-01-01";
  private static final String END_DATE = "2020-02-02";
  private static final String PAGE_NUMBER = "0";
  private static final String PAGE_SIZE = "2";

  @Autowired
  private MockMvc mockMvc;

  @Test
  void shouldReturnExpectedValue() throws Exception {
    //given
    MockHttpServletRequestBuilder accept = get(HistoricalInfoController.BASE_PATH, VRM)
        .header(CORRELATION_ID_HEADER, UUID.randomUUID().toString())
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .accept(MediaType.APPLICATION_JSON_VALUE)
        .param("startDate", "2017-03-01")
        .param("endDate", "2017-03-02")
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
        .andExpect(jsonPath("$.changes[0].category").value("Other"))
        .andExpect(jsonPath("$.changes[0].manufacturer").value("manufacturer"))
        .andExpect(jsonPath("$.changes[0].reasonUpdated").value("reasonUpdated"))
        .andExpect(jsonPath("$.changes[0].uploaderId").isEmpty())
        .andExpect(jsonPath("$.changes[0].uploaderEmail").isEmpty());
  }

  @Test
  void shouldReturnExpectedValueIfStartDateMatchesEndDate() throws Exception {
    //given
    MockHttpServletRequestBuilder accept = get(HistoricalInfoController.BASE_PATH, VRM)
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
        .andExpect(jsonPath("$.changes[0].category").value("Other"))
        .andExpect(jsonPath("$.changes[0].manufacturer").value("manufacturer"))
        .andExpect(jsonPath("$.changes[0].reasonUpdated").value("reasonUpdated"))
        .andExpect(jsonPath("$.changes[0].uploaderId").isEmpty())
        .andExpect(jsonPath("$.changes[0].uploaderEmail").isEmpty());
  }

  @ParameterizedTest
  @MethodSource(
      "uk.gov.caz.whitelist.repository.WhitelistVehicleHistoryPostgresRepositoryIT#pagingProperties")
  void shouldReturnExpectedResultsSize(LocalDate startDate, LocalDate endDate,
      long pageNumber, long pageSize, int expectedResultsCount) throws Exception {
    //given
    MockHttpServletRequestBuilder accept = get(HistoricalInfoController.BASE_PATH, VRM)
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

  @Nested
  class BadRequest {

    @ParameterizedTest
    @MethodSource(
        "uk.gov.caz.whitelist.controller.HistoricalInfoControllerIT#nullableRequestProperties")
    void shouldReturnBadRequestIfAnyRequestPropertyIsNull(String vrm, String startDate,
        String endDate,
        String pageNumber, String pageSize) throws Exception {
      //given
      MockHttpServletRequestBuilder accept = get(HistoricalInfoController.BASE_PATH, vrm)
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
      MockHttpServletRequestBuilder accept = get(HistoricalInfoController.BASE_PATH, vrm)
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
        "uk.gov.caz.whitelist.controller.HistoricalInfoControllerIT#notParsableRequestProperties")
    void shouldReturnBadRequestIfAnyRequestPropertyIsNotParsable(String vrm, String startDate,
        String endDate, String pageNumber, String pageSize) throws Exception {
      //given
      MockHttpServletRequestBuilder accept = get(HistoricalInfoController.BASE_PATH, vrm)
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

  @Test
  void shouldReturnExpectedValueWhenUkWinterTime() throws Exception {
    //given
    MockHttpServletRequestBuilder accept = get(HistoricalInfoController.BASE_PATH, WINTER_VRN)
        .header(CORRELATION_ID_HEADER, UUID.randomUUID().toString())
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .accept(MediaType.APPLICATION_JSON_VALUE)
        .param("startDate", "2020-11-15")
        .param("endDate", "2020-11-15")
        .param("pageNumber", "0")
        .param("pageSize", "10");
    //when
    ResultActions perform = mockMvc.perform(accept);

    //then
    perform.andExpect(status().isOk())
        .andExpect(jsonPath("$.changes", hasSize(2)))
        .andExpect(jsonPath("$.page").value(0))
        .andExpect(jsonPath("$.totalChangesCount").value(2))
        .andExpect(jsonPath("$.perPage").value(10))
        .andExpect(jsonPath("$.pageCount").value(1))
        .andExpect(jsonPath("$.changes[0].modifyDate").value("2020-11-15"))
        .andExpect(jsonPath("$.changes[0].action").value("Removed"))
        .andExpect(jsonPath("$.changes[0].category").value("Other"))
        .andExpect(jsonPath("$.changes[0].manufacturer").value("winter manufacturer"))
        .andExpect(jsonPath("$.changes[0].reasonUpdated").value("reasonUpdated"))
        .andExpect(jsonPath("$.changes[0].uploaderId").isEmpty())
        .andExpect(jsonPath("$.changes[0].uploaderEmail").isEmpty())

        .andExpect(jsonPath("$.changes[1].modifyDate").value("2020-11-15"))
        .andExpect(jsonPath("$.changes[1].action").value("Updated"))
        .andExpect(jsonPath("$.changes[1].category").value("Other"))
        .andExpect(jsonPath("$.changes[1].manufacturer").value("winter manufacturer"))
        .andExpect(jsonPath("$.changes[1].reasonUpdated").value("reasonUpdated"))
        .andExpect(jsonPath("$.changes[1].uploaderId").isEmpty())
        .andExpect(jsonPath("$.changes[1].uploaderEmail").isEmpty());
  }

  @Test
  void shouldReturnExpectedValueWhenUkSummerTime() throws Exception {
    //given
    MockHttpServletRequestBuilder accept = get(HistoricalInfoController.BASE_PATH, SUMMER_VRN)
        .header(CORRELATION_ID_HEADER, UUID.randomUUID().toString())
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .accept(MediaType.APPLICATION_JSON_VALUE)
        .param("startDate", "2020-09-15")
        .param("endDate", "2020-09-15")
        .param("pageNumber", "0")
        .param("pageSize", "10");
    //when
    ResultActions perform = mockMvc.perform(accept);

    //then
    perform.andExpect(status().isOk())
        .andExpect(jsonPath("$.changes", hasSize(2)))
        .andExpect(jsonPath("$.page").value(0))
        .andExpect(jsonPath("$.totalChangesCount").value(2))
        .andExpect(jsonPath("$.perPage").value(10))
        .andExpect(jsonPath("$.pageCount").value(1))
        .andExpect(jsonPath("$.changes[0].modifyDate").value("2020-09-15"))
        .andExpect(jsonPath("$.changes[0].action").value("Updated"))
        .andExpect(jsonPath("$.changes[0].category").value("Other"))
        .andExpect(jsonPath("$.changes[0].manufacturer").value("summer manufacturer"))
        .andExpect(jsonPath("$.changes[0].reasonUpdated").value("reasonUpdated"))
        .andExpect(jsonPath("$.changes[0].uploaderId").isEmpty())
        .andExpect(jsonPath("$.changes[0].uploaderEmail").isEmpty())

        .andExpect(jsonPath("$.changes[1].modifyDate").value("2020-09-15"))
        .andExpect(jsonPath("$.changes[1].action").value("Created"))
        .andExpect(jsonPath("$.changes[1].category").value("Other"))
        .andExpect(jsonPath("$.changes[1].manufacturer").value("manufacturer"))
        .andExpect(jsonPath("$.changes[1].reasonUpdated").value("reasonUpdated"))
        .andExpect(jsonPath("$.changes[1].uploaderId").isEmpty())
        .andExpect(jsonPath("$.changes[1].uploaderEmail").isEmpty());
  }

  static Stream<Arguments> nullableRequestProperties() {
    return Stream.of(
        Arguments.arguments(VRM, null, END_DATE, PAGE_NUMBER, PAGE_SIZE),
        Arguments.arguments(VRM, START_DATE, null, PAGE_NUMBER, PAGE_SIZE),
        Arguments.arguments(VRM, START_DATE, END_DATE, null, PAGE_SIZE),
        Arguments.arguments(VRM, START_DATE, END_DATE, PAGE_NUMBER, null)
    );
  }

  static Stream<Arguments> notParsableRequestProperties() {
    return Stream.of(
        Arguments.arguments(VRM, "not date", END_DATE, PAGE_NUMBER, PAGE_SIZE),
        Arguments.arguments(VRM, "20200101", END_DATE, PAGE_NUMBER, PAGE_SIZE),
        Arguments.arguments(VRM, START_DATE, "not date", PAGE_NUMBER, PAGE_SIZE),
        Arguments.arguments(VRM, START_DATE, "20200101", PAGE_NUMBER, PAGE_SIZE),
        Arguments.arguments(VRM, START_DATE, END_DATE, "-1", PAGE_SIZE),
        Arguments.arguments(VRM, START_DATE, END_DATE, PAGE_NUMBER, "-1"),
        Arguments.arguments(VRM, START_DATE, END_DATE, PAGE_NUMBER, "0"),
        Arguments.arguments(VRM, START_DATE, END_DATE, "some text", PAGE_SIZE),
        Arguments.arguments(VRM, START_DATE, END_DATE, PAGE_NUMBER, "some text")
    );
  }
}