package uk.gov.caz.vcc.dto;

import io.swagger.annotations.ApiModelProperty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.ToString;
import lombok.Value;

/**
 * Wrapper class for single Vehicle Entrant request object.
 */
@Value
@Builder
@AllArgsConstructor
public class VehicleEntrantDto {

  /**
   * String containing unique Vehicle registration number.
   */
  @ToString.Exclude
  @ApiModelProperty(notes = "${swagger.model.descriptions.vehicleEntrants.vrn}")
  @NotNull
  @Size(min = 2, max = 15)
  String vrn;

  /**
   * ISO-8601 formatted datetime indicating  when the vehicle was witnessed entering the CAZ.
   */
  @ApiModelProperty(notes = "${swagger.model.descriptions.vehicleEntrants.timestamp}")
  @NotNull
  @Size(min = 18, max = 25)
  String timestamp;
}