package uk.gov.caz.taxiregister.dto.validation;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import java.util.List;
import uk.gov.caz.taxiregister.dto.VehicleDto;
import uk.gov.caz.taxiregister.model.ValidationError;

/**
 * A class which is responsible for validating {@code VehicleDto#wheelchairAccessibleVehicle} flag.
 */
public class WheelchairAccessibleVehicleValidator implements LicenceValidator {

  @VisibleForTesting
  static final String INVALID_BOOLEAN_VALUE_MESSAGE = "Invalid wheelchair accessible value."
      + " Can only be True or False";

  /**
   * Validates {@code wheelchairAccessibleVehicle} flag of the passed {@code vehicleDto} object. The
   * following values are accepted: "true" (any capitalization), "false" (any capitalization), null
   * or "".
   */
  @Override
  public List<ValidationError> validate(VehicleDto vehicleDto) {
    ImmutableList.Builder<ValidationError> validationErrorsBuilder = ImmutableList.builder();
    WheelchairAccessibleErrorMessageResolver errorMessageResolver =
        new WheelchairAccessibleErrorMessageResolver(vehicleDto);

    String wheelchairAccessibleVehicle = vehicleDto.getWheelchairAccessibleVehicle();

    if (Strings.isNullOrEmpty(wheelchairAccessibleVehicle)) {
      // an empty string indicates the lack of an optional value
      return validationErrorsBuilder.build();
    }

    // wheelchairAccessibleVehicle is not null and not empty
    if (!wheelchairAccessibleVehicle.equalsIgnoreCase(TRUE.toString())
        && !wheelchairAccessibleVehicle.equalsIgnoreCase(FALSE.toString())) {
      validationErrorsBuilder.add(
          errorMessageResolver.valueError(vehicleDto.getVrm()));
    }
    return validationErrorsBuilder.build();
  }

  /**
   * Resolves errors related to parsing wheelchairAccesibleFlag to Boolean into {@link
   * ValidationError}.
   */
  private static class WheelchairAccessibleErrorMessageResolver extends ValidationErrorResolver {

    /**
     * Constructs new instance of {@link WheelchairAccessibleErrorMessageResolver} class.
     *
     * @param vehicleDto {@link VehicleDto} that contains data to be registered but not yet
     *     validated.
     */
    private WheelchairAccessibleErrorMessageResolver(VehicleDto vehicleDto) {
      super(vehicleDto);
    }

    /**
     * Provides correctly filled {@link ValidationError} including VRM.
     *
     * @param vrm VRM to use in {@link ValidationError} message.
     * @return correctly filled {@link ValidationError} including VRM and invalid value.
     */
    private ValidationError valueError(String vrm) {
      return valueError(vrm, INVALID_BOOLEAN_VALUE_MESSAGE);
    }
  }
}
