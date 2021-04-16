package uk.gov.caz.whitelist.dto.validation;

import static org.apache.commons.lang3.StringUtils.isBlank;

import com.google.common.annotations.VisibleForTesting;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import uk.gov.caz.whitelist.dto.WhitelistedVehicleDto;
import uk.gov.caz.whitelist.model.ValidationError;
import uk.gov.caz.whitelist.util.MapPreservingOrderBuilder;

/**
 * Specialized validator class that check whether reason field of {@link WhitelistedVehicleDto} is
 * valid.
 */
public class ReasonValidator implements WhitelistedVehicleValidator {

  @VisibleForTesting
  static final int MAX_LENGTH = 50;

  @VisibleForTesting
  static final String MISSING_REASON_MESSAGE = "Data does not include the 'reason' field which "
      + "is mandatory.";

  @VisibleForTesting
  static final String INVALID_LENGTH_MESSAGE_TEMPLATE =
      "Reason field contains %d number of characters. "
      + "The length of this field must be between 1 and %d characters.";

  @VisibleForTesting
  static final String INVALID_STRING_FORMAT = "Invalid format of Reason field (regex validation).";

  public static final String REGEX = "[a-zA-Z0-9\\s]+";

  private static final Pattern regexPattern = Pattern.compile(REGEX);

  private Map<Predicate<WhitelistedVehicleDto>, Function<WhitelistedVehicleDto, String>>
      validators = MapPreservingOrderBuilder
      .<Predicate<WhitelistedVehicleDto>, Function<WhitelistedVehicleDto, String>>builder()
      .put(vehicle -> isBlank(vehicle.getReason()), ignored -> MISSING_REASON_MESSAGE)
      .put(vehicle -> StringUtils.length(vehicle.getReason()) > MAX_LENGTH,
          vehicle -> invalidLengthMessage(vehicle.getReason()))
      .put(vehicle -> vehicle.getReason() != null && !regexPattern.matcher(vehicle.getReason())
              .matches(),
          ignored -> INVALID_STRING_FORMAT)
      .build();

  @Override
  public List<ValidationError> validate(WhitelistedVehicleDto whitelistedVehicleDto) {
    int lineNumber = whitelistedVehicleDto.getLineNumber();
    return validators.entrySet().stream()
        .filter(entry -> entry.getKey().test(whitelistedVehicleDto))
        .map(entry -> entry.getValue().apply(whitelistedVehicleDto))
        .map(errorMessage -> ValidationError
            .valueError(whitelistedVehicleDto.getVrn(), errorMessage, lineNumber))
        .collect(Collectors.toList());
  }

  static String invalidLengthMessage(String reason) {
    return String.format(INVALID_LENGTH_MESSAGE_TEMPLATE, reason.length(), MAX_LENGTH);
  }
}
