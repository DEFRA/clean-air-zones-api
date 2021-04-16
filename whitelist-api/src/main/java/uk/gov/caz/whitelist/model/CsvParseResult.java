package uk.gov.caz.whitelist.model;

import java.util.List;
import lombok.Value;
import uk.gov.caz.whitelist.dto.WhitelistedVehicleDto;

@Value
public class CsvParseResult {
  List<WhitelistedVehicleDto> whitelistedVehicles;
  List<ValidationError> validationErrors;
}
