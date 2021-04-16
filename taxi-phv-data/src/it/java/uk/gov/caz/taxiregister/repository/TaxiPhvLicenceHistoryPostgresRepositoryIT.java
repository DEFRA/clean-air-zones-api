package uk.gov.caz.taxiregister.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;
import uk.gov.caz.taxiregister.annotation.IntegrationTest;
import uk.gov.caz.taxiregister.model.TaxiPhvVehicleLicenceHistory;

@IntegrationTest
@Sql(scripts = {
    "classpath:data/sql/licensing-authority-data.sql",
    "classpath:data/sql/taxi-phv-data.sql",
    "classpath:data/sql/taxi-phv-data-historical.sql"
},
    executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "classpath:data/sql/clear.sql", executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
@Slf4j
class TaxiPhvLicenceHistoryPostgresRepositoryIT {

  private static final String VRM = "EB12QMD";
  @Autowired
  private TaxiPhvLicenceHistoryPostgresRepository historyRepository;
  @Autowired
  private TaxiPhvLicencePostgresRepository repository;

  @Test
  void shouldProperlyReadDataAndMapResponse() {
    //given

    //when
    List<TaxiPhvVehicleLicenceHistory> history = historyRepository.findByVrmInRange(VRM,
        LocalDateTime.MIN, LocalDateTime.MAX, 2L, 0);

    //then
    assertThat(history).hasSize(2);
    Iterator<TaxiPhvVehicleLicenceHistory> iterator = history.iterator();
    assertValues(iterator.next(), "Removed", LocalDate.of(2017, 3, 31));
    assertValues(iterator.next(), "Updated", LocalDate.of(2017, 3, 15));

    //when
    history = historyRepository.findByVrmInRange(VRM,
        LocalDateTime.MIN, LocalDateTime.MAX, 2L, 1);
    //then
    assertThat(history).hasSize(1);
    iterator = history.iterator();
    assertValues(iterator.next(), "Created", LocalDate.of(2017, 3, 1));
  }

  @Test
  void shouldProperlyCountData() {
    //when
    Long count = historyRepository.count(VRM, LocalDateTime.MIN, LocalDateTime.MAX);

    //then
    assertThat(count).isEqualTo(3);
  }

  @ParameterizedTest
  @MethodSource(
      "uk.gov.caz.taxiregister.repository.TaxiPhvLicenceHistoryPostgresRepositoryIT#pagingProperties")
  void shouldThrowExceptionIfAnyRequestPropertyIsNotParsable(LocalDate startDate, LocalDate endDate,
      long pageNumber, long pageSize, int expectedResultsCount) {
    //given
    LocalDateTime startDateTime = LocalDateTime.of(startDate, LocalTime.MIDNIGHT);
    LocalDateTime endDateTime = LocalDateTime
        .of(endDate, LocalTime.MIDNIGHT.minusNanos(1));

    //when
    List<TaxiPhvVehicleLicenceHistory> histories = historyRepository
        .findByVrmInRange(VRM, startDateTime, endDateTime, pageSize, pageNumber);

    //then
    assertThat(histories).hasSize(expectedResultsCount);
  }

  private void assertValues(TaxiPhvVehicleLicenceHistory licenceHistory, String action,
      LocalDate actionDate) {
    assertThat(licenceHistory.getModifyDate()).isEqualTo(actionDate);
    assertThat(licenceHistory.getLicencePlateNumber()).isEqualTo("plate-no-91");
    assertThat(licenceHistory.getLicensingAuthorityName()).isEqualTo("la-2");
    assertThat(licenceHistory.getLicenceStartDate()).isEqualTo(LocalDate.of(2019, 5, 22));
    assertThat(licenceHistory.getLicenceEndDate()).isEqualTo(LocalDate.of(2099, 11, 27));
    assertThat(licenceHistory.getWheelchairAccessible()).isNull();
    assertThat(licenceHistory.getAction()).isEqualTo(action);
  }

  static Stream<Arguments> pagingProperties() {
    return Stream.of(
        Arguments.arguments(LocalDate.of(2000, 1, 1), LocalDate.of(2030, 1, 1), 0, 10, 3),
        Arguments.arguments(LocalDate.of(2017, 3, 1), LocalDate.of(2017, 3, 31), 0, 10, 3),
        Arguments.arguments(LocalDate.of(2017, 3, 2), LocalDate.of(2017, 3, 31), 0, 10, 2),
        Arguments.arguments(LocalDate.of(2017, 3, 1), LocalDate.of(2017, 3, 30), 0, 10, 2),
        Arguments.arguments(LocalDate.of(2017, 3, 16), LocalDate.of(2017, 3, 31), 0, 10, 1),
        Arguments.arguments(LocalDate.of(2017, 3, 1), LocalDate.of(2017, 3, 14), 0, 10, 1),
        Arguments.arguments(LocalDate.of(2017, 3, 1), LocalDate.of(2017, 3, 31), 0, 2, 2),
        Arguments.arguments(LocalDate.of(2017, 3, 1), LocalDate.of(2017, 3, 31), 1, 2, 1),
        Arguments.arguments(LocalDate.of(2016, 3, 1), LocalDate.of(2016, 3, 31), 1, 2, 0),
        Arguments.arguments(LocalDate.of(2018, 3, 1), LocalDate.of(2018, 3, 31), 1, 2, 0)
    );
  }
}