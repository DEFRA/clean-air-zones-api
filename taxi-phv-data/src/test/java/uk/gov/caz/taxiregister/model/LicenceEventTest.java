package uk.gov.caz.taxiregister.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class LicenceEventTest {

  @ParameterizedTest
  @MethodSource("datesProvider")
  void testActiveLicenceOverlapsWindow(String licenceStart, String licenceEnd, String windowStart,
      String windowEnd, boolean expectedOverlap) {
    // given
    LicenceEvent licenceEvent = LicenceEvent.builder()
        .licenceStartDate(LocalDate.parse(licenceStart))
        .licenceEndDate(LocalDate.parse(licenceEnd))
        .build();

    // when
    boolean overlaps = licenceEvent
        .activeLicenceOverlapsWindow(LocalDate.parse(windowStart), LocalDate.parse(windowEnd));

    // then
    assertThat(overlaps).isEqualTo(expectedOverlap);
  }

  static Stream<Arguments> datesProvider() {
    return Stream.of(
        // Licence Start Date, Licence End Date, Window Start Date, Window End Date, overlaps?
        Arguments.arguments("2020-05-01", "2020-06-01", "2020-01-01", "2020-10-01", true),
        Arguments.arguments("2020-05-01", "2020-06-01", "2020-01-01", "2020-02-01", false),
        Arguments.arguments("2020-05-01", "2020-05-01", "2020-01-01", "2020-10-01", true),
        Arguments.arguments("2020-10-01", "2020-10-01", "2020-01-01", "2020-10-01", true),
        Arguments.arguments("2020-10-01", "2020-10-01", "2020-10-01", "2020-10-01", true),
        Arguments.arguments("2020-09-30", "2020-09-30", "2020-10-01", "2020-10-01", false)
    );
  }
}