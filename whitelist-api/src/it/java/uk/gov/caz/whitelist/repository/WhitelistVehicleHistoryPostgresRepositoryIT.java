package uk.gov.caz.whitelist.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
import uk.gov.caz.whitelist.annotation.IntegrationTest;
import uk.gov.caz.whitelist.model.WhitelistVehicleHistory;

@IntegrationTest
@Sql(scripts = {"classpath:data/sql/clear-whitelist-vehicles-data.sql",
    "classpath:data/sql/whitelist-vehicles-data.sql",
    "classpath:data/sql/whitelist-vehicles-data-historical.sql"}, executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "classpath:data/sql/clear-whitelist-vehicles-data.sql", executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
@Slf4j
class WhitelistVehicleHistoryPostgresRepositoryIT {

  private static final String VRM = "CAS310";
  @Autowired
  private WhitelistVehicleHistoryPostgresRepository historyRepository;

  @Test
  void shouldProperlyReadDataAndMapResponse() {
    // given

    // when
    List<WhitelistVehicleHistory> history = historyRepository.findByVrnInRange(VRM,
        LocalDateTime.MIN, LocalDateTime.MAX, 2L, 0);
    // then
    assertThat(history).hasSize(1);
    Iterator<WhitelistVehicleHistory> iterator = history.iterator();
    assertValues(iterator.next(), "Created", LocalDate.now());
  }

  @Test
  void shouldProperlyCountData() {
    // when
    Long count = historyRepository.count(VRM, LocalDateTime.MIN, LocalDateTime.MAX);

    // then
    assertThat(count).isEqualTo(1);
  }

  private void assertValues(WhitelistVehicleHistory licenceHistory, String action,
      LocalDate actionDate) {
    assertThat(licenceHistory.getModifyDate()).isEqualTo(actionDate);
    assertThat(licenceHistory.getAction()).isEqualTo(action);
    assertThat(licenceHistory.getCategory()).isEqualTo("Other");
    assertThat(licenceHistory.getManufacturer()).isEqualTo("manufacturer");
    assertThat(licenceHistory.getReasonUpdated()).isEqualTo("reasonUpdated");
    assertThat(licenceHistory.getModifierId()).isNull();
    assertThat(licenceHistory.getModifierEmail()).isNull();
  }


  @ParameterizedTest
  @MethodSource("pagingProperties")
  void shouldReturnResponseDataForHistoricalSearch(LocalDate startDate, LocalDate endDate,
      long pageNumber, long pageSize, int expectedResultsCount) {
    // given
    LocalDateTime startDateTime = startDate.atStartOfDay();
    LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay().minusNanos(1);

    // when
    List<WhitelistVehicleHistory> histories = historyRepository
        .findByVrnInRange("EB12QMD", startDateTime, endDateTime, pageSize, pageNumber);

    // then
    assertThat(histories).hasSize(expectedResultsCount);
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