package uk.gov.caz.accounts.service.generatecsv;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;
import uk.gov.caz.accounts.annotation.IntegrationTest;
import uk.gov.caz.accounts.service.ExternalCallsIT;

@IntegrationTest
public class CsvContentGeneratorTestIT extends ExternalCallsIT {

  @Autowired
  private CsvContentGenerator csvGeneratorService;

  @Test
  @Sql(scripts = {"classpath:data/sql/create-vehicles-and-chargeability-cache-data-for-csv.sql"},
      executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(scripts = "classpath:data/sql/delete-chargeability-cache-data.sql",
      executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
  public void shouldGetAllCsvRowsWithHeader() {
    // given
    mockVccsCleanAirZonesCallForCsvGenerator();
    UUID accountId = UUID.fromString("1f30838f-69ee-4486-95b4-7dfcd5c6c67a");

    // when
    List<String[]> csvRowResults = csvGeneratorService.generateCsvRows(accountId);

    // then
    assertThat(csvRowResults).hasSize(6);
    assertThat(String.join(",", csvRowResults.get(0))).isEqualTo(
        "Number plate,Vehicle Type,Bath (Upcoming),Birmingham (Live),Leeds (Live),Leicester (Upcoming)");
    assertThat(String.join(",", csvRowResults.get(1))).isEqualTo("VRN1,Van,,12,Undetermined,");
    assertThat(String.join(",", csvRowResults.get(2))).isEqualTo("VRN2,Car,,18,,");
    assertThat(String.join(",", csvRowResults.get(3))).isEqualTo("VRN3,Van,,,25,");
    assertThat(String.join(",", csvRowResults.get(4)))
        .isEqualTo("VRN4,Undetermined,,Undetermined,Undetermined,");
    assertThat(String.join(",", csvRowResults.get(5)))
        .isEqualTo("VRN5,Heavy Goods Vehicle,,No charge,No charge,");
  }
}