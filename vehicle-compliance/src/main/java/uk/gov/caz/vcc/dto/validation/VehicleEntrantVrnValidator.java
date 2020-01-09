package uk.gov.caz.vcc.dto.validation;

import static org.apache.logging.log4j.util.Strings.isNotEmpty;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.CharMatcher;
import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import uk.gov.caz.vcc.dto.VehicleEntrantDto;

/**
 * Method that validates "vrn" field from {@link VehicleEntrantDto}.
 * It's composed of multiple tiny, highly specialised validators.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class VehicleEntrantVrnValidator implements VehicleEntrantValidator<String> {

  public static final VehicleEntrantVrnValidator INSTANCE = new VehicleEntrantVrnValidator();

  private static final List<SingleFieldValidator<String>> VRN_VALIDATORS = ImmutableList
      .<SingleFieldValidator<String>>builder()
      .add(new VrnNonNullValidator())
      .add(new VrnNonEmptyValidator())
      .add(new VrnWithinSizeRangeValidator())
      .build();

  @Override
  public List<SingleFieldValidator<String>> getValidators() {
    return VRN_VALIDATORS;
  }

  @Override
  public String getValidatedField(VehicleEntrantDto vehicleEntrantDto) {
    return Optional.ofNullable(vehicleEntrantDto.getVrn())
        .map(e -> CharMatcher.whitespace().removeFrom(e))
        .orElse(null);
  }

  @VisibleForTesting
  static class VrnNonNullValidator implements SingleFieldValidator<String> {

    @Override
    public Optional<ValidationError> validate(String vrn, String validatedVrn) {
      if (validatedVrn != null) {
        return Optional.empty();
      }
      return Optional.of(ValidationError.missingFieldError(vrn, "vrn"));
    }
  }

  @VisibleForTesting
  static class VrnNonEmptyValidator implements SingleFieldValidator<String> {

    @Override
    public Optional<ValidationError> validate(String vrn, String validatedVrn) {
      if (isNotEmpty(validatedVrn)) {
        return Optional.empty();
      }
      return Optional.of(ValidationError.emptyFieldError(vrn, "vrn"));
    }
  }

  @VisibleForTesting
  static class VrnWithinSizeRangeValidator implements SingleFieldValidator<String> {

    @Override
    public Optional<ValidationError> validate(String vrn, String validatedVrn) {
      if (vrn != null && vrn.length() <= 15) {
        return Optional.empty();
      }
      return Optional.of(ValidationError.invalidVrnFormat(vrn));
    }
  }
}
