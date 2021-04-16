package uk.gov.caz.testutils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;
import uk.gov.caz.taxiregister.model.ActiveLicenceInReportingWindow;
import uk.gov.caz.taxiregister.model.ActiveLicenceInReportingWindow.StatusInReportingWindow;

public class ActiveLicenceInReportingWindowAssert extends
    AbstractAssert<ActiveLicenceInReportingWindowAssert, ActiveLicenceInReportingWindow> {

  public ActiveLicenceInReportingWindowAssert(ActiveLicenceInReportingWindow actual) {
    super(actual, ActiveLicenceInReportingWindowAssert.class);
  }

  public static ActiveLicenceInReportingWindowAssert assertThat(
      ActiveLicenceInReportingWindow actual) {
    return new ActiveLicenceInReportingWindowAssert(actual);
  }

  public ActiveLicenceInReportingWindowAssert hasStatus(StatusInReportingWindow expectedStatus) {
    Assertions.assertThat(actual.getStatusInReportingWindow()).isEqualTo(expectedStatus);
    return this;
  }

  public ActiveLicenceInReportingWindowAssert isFor(String expectedVrm) {
    Assertions.assertThat(actual.getLicenceEvent().getVrm()).isEqualTo(expectedVrm);
    return this;
  }

  public ActiveLicenceInReportingWindowAssert in(int expectedLa) {
    Assertions.assertThat(actual.getLicenceEvent().getLicensingAuthorityId()).isEqualTo(expectedLa);
    return this;
  }

  public ActiveLicenceInReportingWindowAssert happenedOn(LocalDateTime expectedEventTimestamp) {
    Assertions.assertThat(actual.getLicenceEvent().getEventTimestamp())
        .isEqualTo(expectedEventTimestamp);
    return this;
  }

  public ActiveLicenceInReportingWindowAssert withLicenceStartDate(LocalDate expectedDate) {
    Assertions.assertThat(actual.getLicenceEvent().getLicenceStartDate())
        .isEqualTo(expectedDate);
    return this;
  }

  public ActiveLicenceInReportingWindowAssert withLicenceEndDate(LocalDate expectedDate) {
    Assertions.assertThat(actual.getLicenceEvent().getLicenceEndDate())
        .isEqualTo(expectedDate);
    return this;
  }
}
