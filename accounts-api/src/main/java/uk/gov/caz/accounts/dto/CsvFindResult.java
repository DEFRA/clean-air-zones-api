package uk.gov.caz.accounts.dto;

import java.util.List;
import java.util.UUID;
import lombok.Value;
import uk.gov.caz.csv.model.CsvValidationError;

/**
 * Auxiliary class representing results of CSV parsing.
 */
@Value
public class CsvFindResult {

  UUID uploaderId;
  List<AccountVehicleDto> vehicles;
  List<CsvValidationError> validationErrors;
}
