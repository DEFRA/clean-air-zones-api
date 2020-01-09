package uk.gov.caz.taxiregister.service;

import com.google.common.base.Strings;
import com.opencsv.ICSVParser;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.caz.csv.CsvObjectMapper;
import uk.gov.caz.csv.CsvParseExceptionResolver;
import uk.gov.caz.taxiregister.dto.VehicleDto;
import uk.gov.caz.taxiregister.model.registerjob.RegisterJobTrigger;

/**
 * A class that maps a {@link String} array (which represents the records of a line from a CSV file)
 * to an instance of {@link VehicleDto}.
 */
@Service
public class CsvTaxiPhvLicenceObjectMapper extends CsvObjectMapper<VehicleDto> {

  /**
   * Creates an instance of this class.
   */
  public CsvTaxiPhvLicenceObjectMapper(ICSVParser csvParser,
      CsvParseExceptionResolver csvParseExceptionResolver,
      @Value("${application.validation.max-errors-count}") int maxErrorsCount) {
    super(csvParser, maxErrorsCount, csvParseExceptionResolver);
  }

  /**
   * Maps {@code fields} to an instance of {@link VehicleDto}.
   *
   * @param fields A {@link String} array of records/fields from the given line of the input CSV
   *     file.
   * @param lineNo The line number of the input file which has been parsed to {@code fields}.
   * @return An instance of {@link VehicleDto}.
   */
  @Override
  public VehicleDto mapToObject(String[] fields, int lineNo) {
    return VehicleDto.builder()
        .vrm(fields[0])
        .start(fields[1])
        .end(fields[2])
        .description(fields[3])
        .licensingAuthorityName(fields[4])
        .licensePlateNumber(fields[5])
        .wheelchairAccessibleVehicle(getIsWheelchairAccessible(fields[6]))
        .lineNumber(lineNo)
        .registerJobTrigger(RegisterJobTrigger.CSV_FROM_S3)
        .build();
  }

  /**
   * Maps the optional boolean value parameter (represented by a string) to {@link Boolean}.
   *
   * @param booleanValue An optional (nullable) boolean value represented as a string.
   * @return {@code null} if {@code booleanValue} is null or empty, a converted value to {@link
   *     Boolean#TRUE}/{@link Boolean#FALSE} otherwise.
   */
  private Boolean getIsWheelchairAccessible(String booleanValue) {
    return Optional.ofNullable(Strings.emptyToNull(booleanValue))
        .map(String::toLowerCase)
        .map(Boolean::valueOf)
        .orElse(null);
  }
}
