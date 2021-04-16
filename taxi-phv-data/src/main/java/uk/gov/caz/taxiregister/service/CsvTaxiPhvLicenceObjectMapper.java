package uk.gov.caz.taxiregister.service;

import com.opencsv.ICSVParser;
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
      CsvParseExceptionResolver csvParseExceptionResolver) {
    super(csvParser, csvParseExceptionResolver);
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
        .wheelchairAccessibleVehicle(fields[6])
        .lineNumber(lineNo)
        .registerJobTrigger(RegisterJobTrigger.CSV_FROM_S3)
        .build();
  }
}
