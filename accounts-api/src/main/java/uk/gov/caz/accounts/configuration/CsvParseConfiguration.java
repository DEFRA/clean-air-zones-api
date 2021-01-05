package uk.gov.caz.accounts.configuration;

import com.opencsv.CSVParser;
import com.opencsv.ICSVParser;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.caz.accounts.service.CsvAccountVehicleObjectMapper;
import uk.gov.caz.accounts.service.CsvAccountVehicleParser;
import uk.gov.caz.csv.CsvParseExceptionResolver;

/**
 * Spring configuration for {@link ICSVParser}.
 */
@Configuration
public class CsvParseConfiguration {

  /**
   * Creates an instance of {@link CsvAccountVehicleObjectMapper} that skips the first line in
   * a CSV file.
   */
  @Bean
  public CsvAccountVehicleObjectMapper csvAccountVehicleObjectMapper(
      CsvParseExceptionResolver csvParseExceptionResolver) {
    return new CsvAccountVehicleObjectMapper(
        new CsvAccountVehicleParser(new CSVParser()),
        csvParseExceptionResolver,
        csvReaderBuilder -> csvReaderBuilder.withSkipLines(1) // skip header
    );
  }
}
