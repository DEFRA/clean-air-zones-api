package uk.gov.caz.accounts.service;

import com.opencsv.CSVReaderBuilder;
import com.opencsv.ICSVParser;
import java.io.InputStream;
import java.util.Set;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import uk.gov.caz.accounts.dto.AccountVehicleDto;
import uk.gov.caz.accounts.model.AccountVehicle;
import uk.gov.caz.csv.CsvObjectMapper;
import uk.gov.caz.csv.CsvParseExceptionResolver;

/**
 * A class that provides methods to map an {@link InputStream} of a CSV data to a {@link Set} of
 * {@link AccountVehicle}.
 */
@Slf4j
public class CsvAccountVehicleObjectMapper extends CsvObjectMapper<AccountVehicleDto> {

  /**
   * Creates an instance of this class with {@code csvReaderModifier} that allows to configure
   * the {@link com.opencsv.CSVReader}.
   */
  public CsvAccountVehicleObjectMapper(ICSVParser csvParser,
      CsvParseExceptionResolver csvParseExceptionResolver,
      Consumer<CSVReaderBuilder> csvReaderModifier) {
    super(csvParser, -1, csvParseExceptionResolver, csvReaderModifier);
  }

  @Override
  public AccountVehicleDto mapToObject(String[] fields, int lineNo) {
    return AccountVehicleDto.builder()
        .vrn(StringUtils.deleteWhitespace(fields[0]))
        .lineNumber(lineNo)
        .build();
  }
}
