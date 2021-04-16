package uk.gov.caz.taxiregister.controller;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.google.common.collect.ImmutableMap;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import uk.gov.caz.taxiregister.controller.exception.RequestParamsValidationException;
import uk.gov.caz.taxiregister.dto.LicenceInfoHistoricalRequest;
import uk.gov.caz.taxiregister.dto.LicenceInfoHistoricalResponse;
import uk.gov.caz.taxiregister.dto.LicenceInfoHistoricalResult;
import uk.gov.caz.taxiregister.model.LicenceInfoHistorical;
import uk.gov.caz.taxiregister.model.TaxiPhvVehicleLicenceHistory;
import uk.gov.caz.taxiregister.service.TaxiPhvLicenceHistoryService;

@ExtendWith(MockitoExtension.class)
class LookupHistoricalControllerTest {

  private static final String VRM = "vrm";
  private static final String SART_DATE = "2000-01-01";
  private static final String END_DATE = "2020-02-02";
  private static final String PAGE_NUMBER = "0";
  private static final String PAGE_SIZE = "2";
  @Mock
  private TaxiPhvLicenceHistoryService service;

  @InjectMocks
  LookupHistoricalController lookupHistoricalController;

  @Test
  void shouldReturnExpectedObject() {
    //given
    String vrm = "any";
    Map<String, String> queryStrings = ImmutableMap.of(
        "startDate", SART_DATE,
        "endDate", END_DATE,
        "pageNumber", PAGE_NUMBER,
        "pageSize", PAGE_SIZE
    );

    LicenceInfoHistoricalResponse expectedResponse = createExpected();
    LicenceInfoHistoricalRequest request = LicenceInfoHistoricalRequest.from(vrm, queryStrings);
    LicenceInfoHistorical dto = LicenceInfoHistorical.builder()
        .totalChangesCount(1)
        .changes(getChanges())
        .build();

    Mockito.when(service.findByVrmInRange(request)).thenReturn(dto);

    //when
    ResponseEntity<LicenceInfoHistoricalResponse> licenceInfoFor = lookupHistoricalController
        .getLicenceInfoFor(vrm, queryStrings);
    //then
    assertThat(licenceInfoFor.getBody()).isEqualTo(expectedResponse);
  }

  @ParameterizedTest
  @MethodSource(
      "uk.gov.caz.taxiregister.controller.LookupHistoricalControllerTest#nullableRequestProperties")
  void shouldThrowExceptionIfAnyRequestPropertyIsNull(String vrm, String startDate, String endDate,
      String pageNumber, String pageSize) {
    //given
    Map<String, String> queryStrings = new HashMap<>();
    queryStrings.put("startDate", startDate);
    queryStrings.put("endDate", endDate);
    queryStrings.put("pageNumber", pageNumber);
    queryStrings.put("pageSize", pageSize);

    // when
    Throwable throwable = catchThrowable(() -> lookupHistoricalController
        .getLicenceInfoFor(vrm, queryStrings));

    // then
    assertThat(throwable)
        .isInstanceOf(RequestParamsValidationException.class);
  }

  @ParameterizedTest
  @MethodSource(
      "uk.gov.caz.taxiregister.controller.LookupHistoricalControllerTest#notParsableRequestProperties")
  void shouldThrowExceptionIfAnyRequestPropertyIsNotParsable(String vrm, String startDate,
      String endDate,
      String pageNumber, String pageSize) {
    //given
    Map<String, String> queryStrings = new HashMap<>();
    queryStrings.put("startDate", startDate);
    queryStrings.put("endDate", endDate);
    queryStrings.put("pageNumber", pageNumber);
    queryStrings.put("pageSize", pageSize);

    // when
    Throwable throwable = catchThrowable(() -> lookupHistoricalController
        .getLicenceInfoFor(vrm, queryStrings));

    // then
    assertThat(throwable)
        .isInstanceOf(RequestParamsValidationException.class);
  }

  @Nested
  class PageCount {

    @ParameterizedTest
    @MethodSource(
        "uk.gov.caz.taxiregister.controller.LookupHistoricalControllerTest#pageValues")
    public void shouldProperlyCalculate(long totalCount, long perPage, long expectedValue) {
      // when
      long calculatedPageCount = lookupHistoricalController.calculatePageCount(totalCount, perPage);

      // then
      assertThat(calculatedPageCount).isEqualTo(expectedValue);
    }

    @ParameterizedTest
    @MethodSource(
        "uk.gov.caz.taxiregister.controller.LookupHistoricalControllerTest#wrongNegativeValues")
    public void shouldStopParsing(long totalCount, long perPage) {
      // when
      Throwable throwable = catchThrowable(
          () -> lookupHistoricalController.calculatePageCount(totalCount, perPage));

      // then
      assertThat(throwable)
          .isInstanceOf(IllegalArgumentException.class);
    }
  }

  protected static LicenceInfoHistoricalResponse createExpected() {
    return LicenceInfoHistoricalResponse.builder()
        .page(0)
        .pageCount(1)
        .perPage(2)
        .totalChangesCount(1)
        .changes(getChanges().stream().map(LicenceInfoHistoricalResult::from).collect(toList()))
        .build();
  }

  private static List<TaxiPhvVehicleLicenceHistory> getChanges() {
    return Collections.singletonList(TaxiPhvVehicleLicenceHistory.builder()
        .wheelchairAccessible(true)
        .licenceStartDate(LocalDate.now())
        .licencePlateNumber("plateNumber")
        .licensingAuthorityName("la")
        .action("Created")
        .licenceEndDate(LocalDate.now())
        .modifyDate(LocalDate.now())
        .build());
  }


  static Stream<Arguments> nullableRequestProperties() {
    return Stream.of(
        Arguments.arguments(null, SART_DATE, END_DATE, PAGE_NUMBER, PAGE_SIZE),
        Arguments.arguments(VRM, null, END_DATE, PAGE_NUMBER, PAGE_SIZE),
        Arguments.arguments(VRM, SART_DATE, null, PAGE_NUMBER, PAGE_SIZE),
        Arguments.arguments(VRM, SART_DATE, END_DATE, null, PAGE_SIZE),
        Arguments.arguments(VRM, SART_DATE, END_DATE, PAGE_NUMBER, null)
    );
  }

  static Stream<Arguments> notParsableRequestProperties() {
    return Stream.of(
        Arguments.arguments(VRM, "not date", END_DATE, PAGE_NUMBER, PAGE_SIZE),
        Arguments.arguments(VRM, "20200101", END_DATE, PAGE_NUMBER, PAGE_SIZE),
        Arguments.arguments(VRM, SART_DATE, "not date", PAGE_NUMBER, PAGE_SIZE),
        Arguments.arguments(VRM, SART_DATE, "20200101", PAGE_NUMBER, PAGE_SIZE),
        Arguments.arguments(VRM, SART_DATE, END_DATE, "-1", PAGE_SIZE),
        Arguments.arguments(VRM, SART_DATE, END_DATE, PAGE_NUMBER, "-1"),
        Arguments.arguments(VRM, SART_DATE, END_DATE, PAGE_NUMBER, "0"),
        Arguments.arguments(VRM, SART_DATE, END_DATE, "some text", PAGE_SIZE),
        Arguments.arguments(VRM, SART_DATE, END_DATE, PAGE_NUMBER, "some text")
    );
  }

  static Stream<Arguments> pageValues() {
    return Stream.of(
        Arguments.arguments(15, 10, 2),
        Arguments.arguments(150, 10, 15),
        Arguments.arguments(2, 10, 1),
        Arguments.arguments(15, 3, 5),
        Arguments.arguments(13, 3, 5),
        Arguments.arguments(0, 10, 0)
    );
  }

  static Stream<Arguments> wrongNegativeValues() {
    return Stream.of(
        Arguments.arguments(-2, 10),
        Arguments.arguments(10, -2)
    );
  }
}