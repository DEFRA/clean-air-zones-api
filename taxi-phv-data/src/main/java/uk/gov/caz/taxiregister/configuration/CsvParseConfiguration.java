package uk.gov.caz.taxiregister.configuration;

import com.opencsv.CSVParser;
import com.opencsv.ICSVParser;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import uk.gov.caz.taxiregister.service.CsvLicenceParser;

@Configuration
public class CsvParseConfiguration {
  @Bean
  public ICSVParser icsvParser() {
    return new CsvLicenceParser(new CSVParser());
  }
}
