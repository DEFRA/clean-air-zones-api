package uk.gov.caz.taxiregister.dto.validation;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import java.util.List;
import uk.gov.caz.taxiregister.dto.VehicleDto;
import uk.gov.caz.taxiregister.model.ValidationError;

public class LicensingAuthorityNameValidator implements LicenceValidator {

  private static final int MAX_LENGTH = 50;

  @VisibleForTesting
  static final String MISSING_LICENCING_AUTHORITY_NAME_MESSAGE = "Missing licensing authority name";

  @VisibleForTesting
  static final String INVALID_LICENCING_AUTHORITY_NAME_MESSAGE = "Invalid licensing authority name";

  @Override
  public List<ValidationError> validate(VehicleDto vehicleDto) {
    ImmutableList.Builder<ValidationError> validationErrorsBuilder = ImmutableList.builder();
    LicensingAuthorityNameErrorMessageResolver errorMessageResolver =
        new LicensingAuthorityNameErrorMessageResolver(vehicleDto);

    String vrm = vehicleDto.getVrm();
    String licensingAuthorityName = vehicleDto.getLicensingAuthorityName();

    if (Strings.isNullOrEmpty(licensingAuthorityName)) {
      validationErrorsBuilder.add(errorMessageResolver.missing(vrm));
    }

    if (!Strings.isNullOrEmpty(licensingAuthorityName)
        && licensingAuthorityName.length() > MAX_LENGTH) {
      validationErrorsBuilder.add(errorMessageResolver.invalidFormat(vrm));
    }

    return validationErrorsBuilder.build();
  }

  private static class LicensingAuthorityNameErrorMessageResolver extends
      ValidationErrorResolver {

    private LicensingAuthorityNameErrorMessageResolver(VehicleDto vehicleDto) {
      super(vehicleDto);
    }

    private ValidationError missing(String vrm) {
      return missingFieldError(vrm, MISSING_LICENCING_AUTHORITY_NAME_MESSAGE);
    }

    private ValidationError invalidFormat(String vrm) {
      return valueError(vrm, INVALID_LICENCING_AUTHORITY_NAME_MESSAGE);
    }
  }
}
