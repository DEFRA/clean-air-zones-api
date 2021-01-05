package uk.gov.caz.accounts.service.generatecsv;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.util.UUID;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;
import org.springframework.util.ResourceUtils;
import uk.gov.caz.accounts.annotation.IntegrationTest;
import uk.gov.caz.accounts.service.ExternalCallsIT;

@IntegrationTest
class CsvWriterTestIT extends ExternalCallsIT {

  @Autowired
  private CsvWriter csvWriter;

  @Test
  @Sql(scripts = {"classpath:data/sql/create-vehicles-and-chargeability-cache-data-for-csv.sql"},
      executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(scripts = "classpath:data/sql/delete-chargeability-cache-data.sql",
      executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
  public void shouldCreateWriterWithCsvContent() throws IOException {
    // given
    mockVccsCleanAirZonesCallForCsvGenerator();
    UUID accountId = UUID.fromString("1f30838f-69ee-4486-95b4-7dfcd5c6c67a");

    // when
    Writer writer = csvWriter.createWriterWithCsvContent(accountId);

    // then
    assertThat(writer.toString()).isEqualTo(readExpectedCsv());
  }

  @SneakyThrows
  private static String readExpectedCsv() {
    return new String(Files.readAllBytes(
        ResourceUtils.getFile("classpath:data/csv/export/expected-vehicles-with-charges.csv")
            .toPath()));
  }
}