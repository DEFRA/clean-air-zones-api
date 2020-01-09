package uk.gov.caz.taxiregister.dto.validation;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.regex.Pattern;
import uk.gov.caz.taxiregister.dto.VehicleDto;
import uk.gov.caz.taxiregister.model.ValidationError;

public class VrmValidator implements LicenceValidator {

  @VisibleForTesting
  static final int MAX_LENGTH = 7;

  @VisibleForTesting
  static final String MISSING_VRM_MESSAGE = "Missing VRM value";

  @VisibleForTesting
  static final String INVALID_LENGTH_MESSAGE_TEMPLATE = "Too long VRM. VRM should should have "
      + "from 1-%d characters";

  @VisibleForTesting
  static final String INVALID_VRM_FORMAT_MESSAGE = "Invalid format of VRM";

  public static final String REGEX = "^"
      + "([A-Za-z]{3}[0-9]{1,4})"
      + "|([A-Za-z][0-9]{1,3}[A-Za-z]{3})"
      + "|([A-Za-z]{3}[0-9]{1,3}[A-Za-z])"
      + "|([A-Za-z]{2}[0-9]{2}[A-Za-z]{3})"
      + "|([A-Za-z]{1,3}[0-9]{1,3})"
      + "|([0-9]{1,4}[A-Za-z]{1,3})"
      + "|([A-Za-z]{1,2}[0-9]{1,4})"
      + "$";

  private static final Pattern vrmPattern = Pattern.compile(REGEX);

  @Override
  public List<ValidationError> validate(VehicleDto vehicleDto) {
    ImmutableList.Builder<ValidationError> validationErrorsBuilder = ImmutableList.builder();
    VrmErrorMessageResolver errorResolver = new VrmErrorMessageResolver(
        vehicleDto);

    String vrm = vehicleDto.getVrm();

    if (Strings.isNullOrEmpty(vrm)) {
      validationErrorsBuilder.add(errorResolver.missing());
    }

    if (!Strings.isNullOrEmpty(vrm) && vrm.length() > MAX_LENGTH) {
      validationErrorsBuilder.add(errorResolver.invalidLength(vrm));
    }

    if (!Strings.isNullOrEmpty(vrm) && vrm.length() <= MAX_LENGTH
        && !vrmPattern.matcher(vrm).matches()) {
      validationErrorsBuilder.add(errorResolver.invalidFormat(vrm));
    }

    return validationErrorsBuilder.build();
  }

  private static class VrmErrorMessageResolver extends ValidationErrorResolver {

    private VrmErrorMessageResolver(VehicleDto vehicleDto) {
      super(vehicleDto);
    }

    private ValidationError missing() {
      return missingFieldError(null, MISSING_VRM_MESSAGE);
    }

    private ValidationError invalidLength(String vrm) {
      return valueError(vrm, invalidLengthMessage());
    }

    private ValidationError invalidFormat(String vrm) {
      return valueError(vrm, INVALID_VRM_FORMAT_MESSAGE);
    }

    private String invalidLengthMessage() {
      return String.format(INVALID_LENGTH_MESSAGE_TEMPLATE, MAX_LENGTH);
    }
  }
}
