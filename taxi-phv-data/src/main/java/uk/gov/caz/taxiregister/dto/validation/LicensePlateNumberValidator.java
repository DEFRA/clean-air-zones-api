package uk.gov.caz.taxiregister.dto.validation;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import java.util.List;
import uk.gov.caz.taxiregister.dto.VehicleDto;
import uk.gov.caz.taxiregister.model.ValidationError;

public class LicensePlateNumberValidator implements LicenceValidator {

  private static final int MAX_LENGTH = 15;

  @VisibleForTesting
  static final String MISSING_LICENCE_PLATE_NUMBER_MESSAGE = "Missing license plate number";

  @VisibleForTesting
  static final String INVALID_PLATE_NUMBER_MESSAGE = "Invalid format of license plate "
      + "number";

  @Override
  public List<ValidationError> validate(VehicleDto vehicleDto) {
    ImmutableList.Builder<ValidationError> validationErrorsBuilder = ImmutableList.builder();
    LicencePlateNoErrorMessageResolver errorMessageResolver =
        new LicencePlateNoErrorMessageResolver(vehicleDto);

    String vrm = vehicleDto.getVrm();
    String plateNumber = vehicleDto.getLicensePlateNumber();

    if (Strings.isNullOrEmpty(plateNumber)) {
      validationErrorsBuilder.add(errorMessageResolver.missing(vrm));
    }

    if (!Strings.isNullOrEmpty(plateNumber) && plateNumber.length() > MAX_LENGTH) {
      validationErrorsBuilder.add(errorMessageResolver.invalidFormat(vrm));
    }

    return validationErrorsBuilder.build();
  }

  private static class LicencePlateNoErrorMessageResolver extends ValidationErrorResolver {

    private LicencePlateNoErrorMessageResolver(VehicleDto vehicleDto) {
      super(vehicleDto);
    }

    private ValidationError missing(String vrm) {
      return missingFieldError(vrm, MISSING_LICENCE_PLATE_NUMBER_MESSAGE);
    }

    private ValidationError invalidFormat(String vrm) {
      return valueError(vrm, INVALID_PLATE_NUMBER_MESSAGE);
    }
  }
}
