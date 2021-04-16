package uk.gov.caz.whitelist.dto.validation;

import static uk.gov.caz.whitelist.model.Actions.CREATE;
import static uk.gov.caz.whitelist.model.Actions.DELETE;
import static uk.gov.caz.whitelist.model.Actions.UPDATE;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Sets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import uk.gov.caz.whitelist.dto.WhitelistedVehicleDto;
import uk.gov.caz.whitelist.model.ValidationError;
import uk.gov.caz.whitelist.util.MapPreservingOrderBuilder;


/**
 * Specialized validator class that check whether action field of {@link WhitelistedVehicleDto} is
 * valid.
 */
public class ActionValidator implements WhitelistedVehicleValidator {

  @VisibleForTesting
  static final String INVALID_STRING_FORMAT = "Invalid format of Action field (regex validation).";

  private static final Set<String> ALLOWABLE_UPDATE_CHARACTERS = Sets.newHashSet(
      CREATE.getActionCharacter(), DELETE.getActionCharacter(), UPDATE.getActionCharacter()
  );

  private static final String REGEX = "[a-zA-Z0-9]+";

  private static final Pattern regexPattern = Pattern.compile(REGEX);

  private Map<Predicate<WhitelistedVehicleDto>, Function<WhitelistedVehicleDto, String>>
      validators = MapPreservingOrderBuilder
      .<Predicate<WhitelistedVehicleDto>, Function<WhitelistedVehicleDto, String>>builder()
      .put(vehicle -> vehicle.getAction() != null && !regexPattern.matcher(vehicle.getAction())
              .matches(),
          ignored -> INVALID_STRING_FORMAT)
      .put(vehicle -> !ALLOWABLE_UPDATE_CHARACTERS.contains(vehicle.getAction()),
          vehicle -> "Action field should be empty or contain one of: " + String
              .join(", ", ALLOWABLE_UPDATE_CHARACTERS))
      .build();

  @Override
  public List<ValidationError> validate(WhitelistedVehicleDto whitelistedVehicleDto) {
    String action = whitelistedVehicleDto.getAction();
    int lineNumber = whitelistedVehicleDto.getLineNumber();

    if (StringUtils.isBlank(action)) {
      return Collections.emptyList();
    }

    return validators.entrySet().stream()
        .filter(entry -> entry.getKey().test(whitelistedVehicleDto))
        .map(entry -> entry.getValue().apply(whitelistedVehicleDto))
        .map(errorMessage -> ValidationError
            .valueError(whitelistedVehicleDto.getVrn(), errorMessage, lineNumber))
        .collect(Collectors.toList());
  }
}
