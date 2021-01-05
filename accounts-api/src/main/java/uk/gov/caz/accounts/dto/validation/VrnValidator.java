package uk.gov.caz.accounts.dto.validation;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.regex.Pattern;
import uk.gov.caz.accounts.dto.AccountVehicleDto;
import uk.gov.caz.accounts.model.registerjob.ValidationError;

public class VrnValidator implements AccountVehicleValidator {

  public static final int MAX_LENGTH = 7;

  public static final int MIN_LENGTH = 2;

  @VisibleForTesting
  static final String INVALID_LENGTH_MESSAGE_TEMPLATE = "Number plates need to be between %d and "
      + "%d characters instead of %d.";

  @VisibleForTesting
  static final String INVALID_VRN_FORMAT_MESSAGE =
      "%s - this number plate is not in a valid format.";

  private static final Pattern vrnPattern = Pattern.compile("[a-zA-Z0-9]+");

  @Override
  public List<ValidationError> validate(AccountVehicleDto accountVehicleDto) {
    ImmutableList.Builder<ValidationError> validationErrorsBuilder = ImmutableList.builder();
    VrnErrorMessageResolver errorResolver = new VrnErrorMessageResolver(
        accountVehicleDto);

    String vrn = accountVehicleDto.getVrn();

    if (!hasRequiredSize(vrn)) {
      validationErrorsBuilder.add(errorResolver.invalidLength(vrn));
    }

    if (!vrn.isEmpty() && vrn.length() <= MAX_LENGTH
        && !vrnPattern.matcher(vrn).matches()) {
      validationErrorsBuilder.add(errorResolver.invalidFormat(vrn));
    }

    return validationErrorsBuilder.build();
  }

  private boolean hasRequiredSize(String vrn) {
    return !vrn.isEmpty()
        && vrn.length() <= MAX_LENGTH
        && vrn.length() >= MIN_LENGTH;
  }

  private static class VrnErrorMessageResolver extends ValidationErrorResolver {

    private VrnErrorMessageResolver(AccountVehicleDto accountVehicleDto) {
      super(accountVehicleDto);
    }

    private ValidationError invalidLength(String vrn) {
      return valueError(vrn, invalidLengthMessage(vrn));
    }

    private ValidationError invalidFormat(String vrn) {
      return valueError(vrn, invalidFormatMessage(vrn));
    }

    private String invalidLengthMessage(String vrn) {
      return String.format(INVALID_LENGTH_MESSAGE_TEMPLATE, MIN_LENGTH, MAX_LENGTH, vrn.length());
    }

    private String invalidFormatMessage(String vrn) {
      return String.format(INVALID_VRN_FORMAT_MESSAGE, vrn);
    }
  }
}
