package uk.gov.caz.whitelist.dto.validation;

import java.util.List;
import uk.gov.caz.whitelist.dto.WhitelistedVehicleDto;
import uk.gov.caz.whitelist.model.ValidationError;

public interface WhitelistedVehicleValidator {

  List<ValidationError> validate(WhitelistedVehicleDto whitelistedVehicleDto);
}
