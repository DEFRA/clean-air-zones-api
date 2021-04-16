package uk.gov.caz.whitelist.dto.validation;

import static uk.gov.caz.whitelist.model.Actions.CREATE;
import static uk.gov.caz.whitelist.model.Actions.DELETE;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import uk.gov.caz.whitelist.dto.WhitelistedVehicleDto;
import uk.gov.caz.whitelist.model.ValidationError;
import uk.gov.caz.whitelist.model.WhitelistVehicle;
import uk.gov.caz.whitelist.service.WhitelistService;

/**
 * Specialized validator class that check whether action field of {@link WhitelistedVehicleDto} is
 * valid.
 */
@Component
@AllArgsConstructor
public class DatabaseCheckValidator implements WhitelistedVehicleValidator {

  private final WhitelistService whitelistService;

  @Override
  public List<ValidationError> validate(WhitelistedVehicleDto whitelistedVehicleDto) {
    String action = whitelistedVehicleDto.getAction();
    String vrn = whitelistedVehicleDto.getVrn();
    int lineNumber = whitelistedVehicleDto.getLineNumber();

    if (StringUtils.isBlank(action) || StringUtils.isBlank(vrn)) {
      return Collections.emptyList();
    }

    if (!checkActionIsAllowedAgainstDb(vrn, action)) {
      if (action.equals(CREATE.getActionCharacter())) {
        return Arrays.asList(ValidationError.valueError(vrn,
            "You can't add that number plate as it already exists in the database.", lineNumber));
      } else if (action.equals(DELETE.getActionCharacter())) {
        return Arrays.asList(ValidationError.valueError(vrn,
            "You can't delete that number plate as it doesn't exist in the database.", lineNumber));
      }
    }
    return Collections.emptyList();
  }

  /**
   * Checks action for a VRN against the VRNs state in the database.
   *
   * @return a boolean indicating whether the action is allowed for the VRN.
   */
  private boolean checkActionIsAllowedAgainstDb(String vrn, String action) {
    Optional<WhitelistVehicle> result = whitelistService.findBy(vrn);
    return !(actionIsCreateAndResultIsPresent(action, result)
        || actionIsDeleteAndResultIsNotPresent(action, result));
  }

  private boolean actionIsDeleteAndResultIsNotPresent(String action,
      Optional<WhitelistVehicle> result) {
    return action.contentEquals(DELETE.getActionCharacter()) && !result.isPresent();
  }

  private boolean actionIsCreateAndResultIsPresent(String action,
      Optional<WhitelistVehicle> result) {
    return action.contentEquals(CREATE.getActionCharacter()) && result.isPresent();
  }
}
