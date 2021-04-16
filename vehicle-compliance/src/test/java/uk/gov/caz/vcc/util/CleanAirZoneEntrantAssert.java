package uk.gov.caz.vcc.util;

import java.time.LocalDateTime;
import java.util.UUID;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;
import uk.gov.caz.vcc.domain.CleanAirZoneEntrant;

public class CleanAirZoneEntrantAssert extends
    AbstractAssert<CleanAirZoneEntrantAssert, CleanAirZoneEntrant> {

  CleanAirZoneEntrantAssert(CleanAirZoneEntrant actual) {
    super(actual, CleanAirZoneEntrantAssert.class);
  }

  public static CleanAirZoneEntrantAssert assertThat(CleanAirZoneEntrant actual) {
    return new CleanAirZoneEntrantAssert(actual);
  }

  public CleanAirZoneEntrantAssert hasVrn(String expectedVrn) {
    Assertions.assertThat(actual.getVrn()).isEqualTo(expectedVrn);
    return this;
  }

  public CleanAirZoneEntrantAssert hasEntrantTimestamp(LocalDateTime expectedEntrantTimestamp) {
    Assertions.assertThat(actual.getEntrantTimestamp()).isEqualTo(expectedEntrantTimestamp);
    return this;
  }

  public CleanAirZoneEntrantAssert hasInsertTimestamp(LocalDateTime expectedInsertTimestamp) {
    Assertions.assertThat(actual.getInsertTimestamp()).isEqualTo(expectedInsertTimestamp);
    return this;
  }

  public CleanAirZoneEntrantAssert insertTimestampIsAfter(LocalDateTime afterDate) {
    Assertions.assertThat(actual.getInsertTimestamp()).isAfter(afterDate);
    return this;
  }

  public CleanAirZoneEntrantAssert hasCorrelationId(String expectedCorrelationId) {
    Assertions.assertThat(actual.getCorrelationId()).isEqualTo(expectedCorrelationId);
    return this;
  }

  public CleanAirZoneEntrantAssert hasChargeValidityCode(String expectedChargeValidityCode) {
    Assertions.assertThat(actual.getChargeValidityCode().getChargeValidityCode())
        .isEqualTo(expectedChargeValidityCode);
    return this;
  }

  public CleanAirZoneEntrantAssert hasCleanAirZoneId(UUID expectedCleanAirZoneId) {
    Assertions.assertThat(actual.getCleanAirZoneId()).isEqualTo(expectedCleanAirZoneId);
    return this;
  }

  public CleanAirZoneEntrantAssert hasEntrantPaymentId(UUID entrantPaymentId) {
    Assertions.assertThat(actual.getEntrantPaymentId()).isEqualTo(entrantPaymentId);
    return this;
  }

  public CleanAirZoneEntrantAssert hasAnyEntrantPaymentId() {
    Assertions.assertThat(actual.getEntrantPaymentId()).isNotNull();
    return this;
  }
}