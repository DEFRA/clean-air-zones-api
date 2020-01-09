package uk.gov.caz.tariff;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;
import uk.gov.caz.tariff.annotation.IntegrationTest;
import uk.gov.caz.tariff.dto.Rates;
import uk.gov.caz.tariff.dto.Tariff;
import uk.gov.caz.tariff.service.TariffRepository;

@IntegrationTest
@Sql(scripts = {
    "classpath:data/sql/clear.sql",
    "classpath:data/sql/sample-data.sql"},
    executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
public class TariffRepositoryTestIT {

  private static final UUID CLEAN_AIR_ZONE_ID = UUID
      .fromString("5cd7441d-766f-48ff-b8ad-1809586fea37");

  @Autowired
  private TariffRepository tariffRepository;

  @Test
  public void shouldReturnSampleTariff() {
    // given

    // when
    Tariff tariff = tariffRepository.findByCleanAirZoneId(CLEAN_AIR_ZONE_ID).get();

    // then
    assertThat(tariff)
        .isEqualToComparingOnlyGivenFields(expectedTariff(), "cleanAirZoneId", "name", "tariffClass",
            "chargeIdentifier", "rates.hgv", "rates.car", "rates.miniBus", "rates.taxi");
  }

  @Test
  public void shouldReturnEmptyWhenTariffNotExist() {
    // given
    UUID cleanAirZoneId = UUID.fromString("39cf92a1-de38-4c93-b676-3aba2b3b2e1d");

    // when
    Optional<Tariff> tariff = tariffRepository.findByCleanAirZoneId(cleanAirZoneId);

    // then
    assertThat(tariff).isEmpty();
  }

  private Tariff expectedTariff() {
    Rates rates = Rates.builder()
        .hgv(rate(50.00))
        .car(rate(8.00))
        .miniBus(rate(50.00))
        .taxi(rate(8.00))
        .build();
    return Tariff.builder()
        .cleanAirZoneId(CLEAN_AIR_ZONE_ID)
        .name("Birmingham")
        .tariffClass('D')
        .chargeIdentifier("BCC01")
        .rates(rates)
        .build();
  }

  private BigDecimal rate(double rate) {
    return new BigDecimal(rate).setScale(2);
  }
}