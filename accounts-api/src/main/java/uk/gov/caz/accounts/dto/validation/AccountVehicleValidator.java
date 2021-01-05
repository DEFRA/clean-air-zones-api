package uk.gov.caz.accounts.dto.validation;

import java.util.List;
import uk.gov.caz.accounts.dto.AccountVehicleDto;
import uk.gov.caz.accounts.model.registerjob.ValidationError;

public interface AccountVehicleValidator {

  List<ValidationError> validate(AccountVehicleDto accountVehicleDto);
}
