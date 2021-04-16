package uk.gov.caz.vcc.dto;

import com.google.common.collect.ImmutableList;
import io.swagger.annotations.ApiModelProperty;
import java.util.LinkedList;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import uk.gov.caz.vcc.dto.validation.ValidationError;
import uk.gov.caz.vcc.dto.validation.VehicleEntrantTimestampValidator;
import uk.gov.caz.vcc.dto.validation.VehicleEntrantValidator;
import uk.gov.caz.vcc.dto.validation.VehicleEntrantVrnValidator;

/**
 * Wrapper class for Vehicle Entrants API request object.
 */
@Value
@Builder
@AllArgsConstructor
public class VehicleEntrantsDto {

  private static final List<VehicleEntrantValidator<String>> VALIDATORS =
      ImmutableList.<VehicleEntrantValidator<String>>builder()
      .add(VehicleEntrantVrnValidator.INSTANCE)
      .add(VehicleEntrantTimestampValidator.INSTANCE)
      .build();

  /**
   * Provides the flexibility to submit one or many vehicle entrants in one request.
   */
  @ApiModelProperty(notes = "${swagger.model.descriptions.vehicleEntrants.vehicleEntrantsDetails}")
  @NotNull
  @Valid
  List<VehicleEntrantDto> vehicleEntrants;

  /**
   * Method that validates this instance of DTO and returns all errors combined.
   */
  public List<ValidationError> validate() {
    List<ValidationError> validationErrors = new LinkedList<>();

    for (int i = 0; i < vehicleEntrants.size(); i++) {
      for (VehicleEntrantValidator validator : VALIDATORS) {
        validationErrors.addAll(validator.validate(vehicleEntrants.get(i)));
      }
    }

    return validationErrors;
  }
}