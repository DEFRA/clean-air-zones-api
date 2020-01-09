package uk.gov.caz.vcc.dto.validation;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import uk.gov.caz.vcc.dto.VehicleEntrantDto;

public interface VehicleEntrantValidator<T> {

  /**
   * Validates given object using all validator registered by caller in {@link
   * VehicleEntrantValidator#getValidators()}.
   */
  default List<ValidationError> validate(VehicleEntrantDto vehicleEntrantDto) {
    T validatedField = getValidatedField(vehicleEntrantDto);

    return getValidators()
        .stream()
        .map(validator -> validator.validate(vehicleEntrantDto.getVrn(), validatedField))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .collect(Collectors.toList());
  }

  /**
   * Factory method that provides validator for given field.
   */
  List<SingleFieldValidator<T>> getValidators();

  /**
   * Method that extracts field to be validated from {@link VehicleEntrantDto}.
   */
  T getValidatedField(VehicleEntrantDto vehicleEntrantDto);
}
