package uk.gov.caz.whitelist.dto.validation;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.regex.Pattern;
import uk.gov.caz.whitelist.dto.WhitelistedVehicleDto;
import uk.gov.caz.whitelist.model.ValidationError;

public class VrnValidator implements WhitelistedVehicleValidator {

  @VisibleForTesting
  static final int MAX_LENGTH = 14;

  @VisibleForTesting
  static final int MIN_LENGTH = 2;

  @VisibleForTesting
  static final String MISSING_VRN_MESSAGE = "Data does not include the 'vrn' field which "
      + "is mandatory.";

  @VisibleForTesting
  static final String INVALID_LENGTH_MESSAGE_TEMPLATE = "VRN should have from %d to %d characters "
      + "instead of %d.";

  @VisibleForTesting
  static final String INVALID_VRN_FORMAT_MESSAGE = "Invalid format of VRN.";

  public static final String REGEX = "[a-zA-Z0-9]+";

  private static final Pattern vrnPattern = Pattern.compile(REGEX);

  @Override
  public List<ValidationError> validate(WhitelistedVehicleDto whitelistedVehicleDto) {
    ImmutableList.Builder<ValidationError> validationErrorsBuilder = ImmutableList.builder();
    VrnErrorMessageResolver errorResolver = new VrnErrorMessageResolver(
        whitelistedVehicleDto);

    String vrn = whitelistedVehicleDto.getVrn();

    if (vrn == null) {
      validationErrorsBuilder.add(errorResolver.missing());
    }

    if (vrn != null && !hasRequiredSize(vrn)) {
      validationErrorsBuilder.add(errorResolver.invalidLength(vrn));
    }

    if (vrn != null && !vrn.isEmpty() && vrn.length() <= MAX_LENGTH
        && !vrnPattern.matcher(vrn).matches()) {
      validationErrorsBuilder.add(errorResolver.invalidFormat(vrn));
    }

    return validationErrorsBuilder.build();
  }

  private boolean hasRequiredSize(String vrn) {
    return vrn != null
        && !vrn.isEmpty()
        && vrn.length() <= MAX_LENGTH
        && vrn.length() >= MIN_LENGTH;
  }

  private static class VrnErrorMessageResolver extends ValidationErrorResolver {

    private VrnErrorMessageResolver(WhitelistedVehicleDto whitelistedVehicleDto) {
      super(whitelistedVehicleDto);
    }

    private ValidationError missing() {
      return missingFieldError(null, MISSING_VRN_MESSAGE);
    }

    private ValidationError invalidLength(String vrn) {
      return valueError(vrn, invalidLengthMessage(vrn));
    }

    private ValidationError invalidFormat(String vrn) {
      return valueError(vrn, INVALID_VRN_FORMAT_MESSAGE);
    }

    private String invalidLengthMessage(String vrn) {
      int length = vrn == null ? 0 : vrn.length();
      return String.format(INVALID_LENGTH_MESSAGE_TEMPLATE, MIN_LENGTH, MAX_LENGTH, length);
    }
  }
}
