package uk.gov.caz.whitelist.model;

import java.util.List;
import java.util.UUID;
import lombok.Value;
import uk.gov.caz.whitelist.dto.WhitelistedVehicleDto;

@Value
public class CsvFindResult {

  String email;
  UUID uploaderId;
  List<WhitelistedVehicleDto> vehicles;
  List<ValidationError> validationErrors;
}
