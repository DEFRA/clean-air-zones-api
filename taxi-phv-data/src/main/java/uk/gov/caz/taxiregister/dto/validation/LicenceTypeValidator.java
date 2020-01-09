package uk.gov.caz.taxiregister.dto.validation;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import java.util.List;
import uk.gov.caz.taxiregister.dto.VehicleDto;
import uk.gov.caz.taxiregister.model.ValidationError;

/**
 * A validation class to ensure details of a given Taxi or PHV operating license
 * adhere to data rule definitions regarding presence and length.
 *
 */
public class LicenceTypeValidator implements LicenceValidator {

  @VisibleForTesting
  static final int MAX_LENGTH = 100;
  
  @VisibleForTesting
  static final String MISSING_LICENCE_TYPE_MESSAGE = "Missing taxi/PHV value";

  @VisibleForTesting
  static final String LICENCE_TYPE_TOO_LONG_MESSAGE =
      "Taxi/PHV value must be less than 100 characters";
  
  /**
   * Performs validation against a given operating license.
   */
  @Override
  public List<ValidationError> validate(VehicleDto vehicleDto) {
    ImmutableList.Builder<ValidationError> validationErrorsBuilder = ImmutableList.builder();
    LicenceTypeErrorMessageResolver errorMessageResolver = new LicenceTypeErrorMessageResolver(
        vehicleDto);

    String vrm = vehicleDto.getVrm();
    String licenceType = vehicleDto.getDescription();

    if (Strings.isNullOrEmpty(licenceType)) {
      validationErrorsBuilder.add(errorMessageResolver.missing(vrm));
      return validationErrorsBuilder.build();
    }
    
    if (licenceType.length() > MAX_LENGTH) {
      validationErrorsBuilder
          .add(errorMessageResolver.valueError(vrm, LICENCE_TYPE_TOO_LONG_MESSAGE));
    }

    return validationErrorsBuilder.build();
  }

  private static class LicenceTypeErrorMessageResolver extends ValidationErrorResolver {

    private LicenceTypeErrorMessageResolver(VehicleDto vehicleDto) {
      super(vehicleDto);
    }

    private ValidationError missing(String vrm) {
      return missingFieldError(vrm, MISSING_LICENCE_TYPE_MESSAGE);
    }
  }
}
