package uk.gov.caz.whitelist.dto.validation;

import com.google.common.annotations.VisibleForTesting;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import uk.gov.caz.whitelist.dto.WhitelistedVehicleDto;
import uk.gov.caz.whitelist.model.ValidationError;

/**
 * Specialized validator class that check whether manufacturer field of {@link
 * WhitelistedVehicleDto} is valid.
 */
public class ManufacturerValidator implements WhitelistedVehicleValidator {

  public static final int MAX_LENGTH = 50;

  @VisibleForTesting
  static final String INVALID_STRING_FORMAT =
      "Invalid length of Manufacturer field (actual length: %d, max allowed length: %d).";

  @Override
  public List<ValidationError> validate(WhitelistedVehicleDto whitelistedVehicleDto) {
    Optional<String> manufacturer = whitelistedVehicleDto.getManufacturer();
    int lineNumber = whitelistedVehicleDto.getLineNumber();

    if (!manufacturer.isPresent()) {
      return Collections.emptyList();
    }

    if (manufacturer.get().length() <= MAX_LENGTH) {
      return Collections.emptyList();
    }

    return Collections.singletonList(
        ValidationError.valueError(
            whitelistedVehicleDto.getVrn(), String.format(
                INVALID_STRING_FORMAT, manufacturer.get().length(), MAX_LENGTH,
                lineNumber
            )
        )
    );
  }
}
