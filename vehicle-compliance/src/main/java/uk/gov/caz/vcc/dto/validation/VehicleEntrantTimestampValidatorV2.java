package uk.gov.caz.vcc.dto.validation;

import static uk.gov.caz.vcc.controller.v2.VehicleEntrantsControllerV2.DATE_TIME_FORMATTER;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import uk.gov.caz.vcc.dto.VehicleEntrantDto;

/**
 * Method that validates "timestamp" field from {@link VehicleEntrantDto}. It's composed of multiple
 * tiny, highly specialised validators.
 */
public class VehicleEntrantTimestampValidatorV2 implements VehicleEntrantValidator<String> {

  public static final VehicleEntrantTimestampValidatorV2 INSTANCE =
      new VehicleEntrantTimestampValidatorV2();

  private static final List<SingleFieldValidator<String>> TIMESTAMP_VALIDATORS = ImmutableList
      .<SingleFieldValidator<String>>builder()
      .add(new TimestampNotNullValidator())
      .add(new TimestampValidFormatValidator())
      .build();

  @Override
  public List<SingleFieldValidator<String>> getValidators() {
    return TIMESTAMP_VALIDATORS;
  }

  @Override
  public String getValidatedField(VehicleEntrantDto vehicleEntrantDto) {
    return vehicleEntrantDto.getTimestamp();
  }

  @VisibleForTesting
  static class TimestampNotNullValidator implements SingleFieldValidator<String> {

    @Override
    public Optional<ValidationError> validate(String vrn, String timestamp) {
      if (timestamp != null) {
        return Optional.empty();
      }

      return Optional.of(ValidationError.missingFieldError(vrn, "timestamp"));
    }
  }

  @VisibleForTesting
  static class TimestampValidFormatValidator implements SingleFieldValidator<String> {

    @Override
    public Optional<ValidationError> validate(String vrn, String timestamp) {
      if (parseTimestamp(timestamp).isPresent()) {
        return Optional.empty();
      }

      return Optional.of(ValidationError.invalidTimestampFormat(vrn, timestamp));
    }

    private static Optional<LocalDateTime> parseTimestamp(String entrantTimestamp) {
      try {
        return Optional.of(LocalDateTime.parse(entrantTimestamp, DATE_TIME_FORMATTER));
      } catch (Exception e) {
        return Optional.empty();
      }
    }
  }
}
