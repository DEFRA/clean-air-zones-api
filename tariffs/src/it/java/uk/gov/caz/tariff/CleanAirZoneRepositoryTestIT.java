package uk.gov.caz.tariff;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;
import uk.gov.caz.tariff.annotation.IntegrationTest;
import uk.gov.caz.tariff.dto.CleanAirZones;
import uk.gov.caz.tariff.service.CleanAirZonesRepository;

@IntegrationTest
@Sql(scripts = {
    "classpath:data/sql/clear.sql",
    "classpath:data/sql/sample-data.sql"},
    executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
public class CleanAirZoneRepositoryTestIT {

  @Autowired
  private CleanAirZonesRepository cleanAirZonesRepository;

  @Test
  public void shouldReturnCleanAirZones() {
    // given

    // when
    CleanAirZones cleanAirZones = cleanAirZonesRepository.findAll();

    // then
    assertThat(cleanAirZones.getCleanAirZones()).hasSize(2);
  }
}