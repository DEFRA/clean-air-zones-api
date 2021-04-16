package uk.gov.caz.whitelist.dto.validation;

import static uk.gov.caz.whitelist.model.CategoryType.availableCategories;

import com.google.common.annotations.VisibleForTesting;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import uk.gov.caz.whitelist.dto.WhitelistedVehicleDto;
import uk.gov.caz.whitelist.model.ValidationError;
import uk.gov.caz.whitelist.util.MapPreservingOrderBuilder;


/**
 * Specialized validator class that check whether category field of {@link WhitelistedVehicleDto} is
 * valid.
 */
public class CategoryValidator implements WhitelistedVehicleValidator {

  @VisibleForTesting
  static final String CATEGORY_FIELD_ERROR_MESSAGE = "Category field should contain one of: ";

  @VisibleForTesting
  static final Set<String> ALLOWABLE_CATEGORIES = availableCategories();

  private Map<Predicate<WhitelistedVehicleDto>, Function<WhitelistedVehicleDto, String>>
      validators = MapPreservingOrderBuilder
      .<Predicate<WhitelistedVehicleDto>, Function<WhitelistedVehicleDto, String>>builder()
      .put(vehicle -> ALLOWABLE_CATEGORIES.stream()
              .noneMatch(vehicle.getCategory()::equalsIgnoreCase),
          vehicle -> CATEGORY_FIELD_ERROR_MESSAGE + String
              .join(", ", ALLOWABLE_CATEGORIES))
      .build();

  @Override
  public List<ValidationError> validate(WhitelistedVehicleDto whitelistedVehicleDto) {
    int lineNumber = whitelistedVehicleDto.getLineNumber();
    String category = whitelistedVehicleDto.getCategory();

    if (category == null) {
      return Arrays.asList(ValidationError.valueError(whitelistedVehicleDto.getVrn(),
          CATEGORY_FIELD_ERROR_MESSAGE + String
              .join(", ", ALLOWABLE_CATEGORIES), lineNumber));
    }

    return validators.entrySet().stream()
        .filter(entry -> entry.getKey().test(whitelistedVehicleDto))
        .map(entry -> entry.getValue().apply(whitelistedVehicleDto))
        .map(errorMessage -> ValidationError
            .valueError(whitelistedVehicleDto.getVrn(), errorMessage, lineNumber))
        .collect(Collectors.toList());
  }
}
