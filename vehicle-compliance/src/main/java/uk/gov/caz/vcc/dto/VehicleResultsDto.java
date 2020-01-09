package uk.gov.caz.vcc.dto;

import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import lombok.Value;

/**
 * Class for Vehicle Entrants API response object.
 */
@Value
public class VehicleResultsDto {

  /**
   * List of results for given vehicleEntrants.
   */
  @ApiModelProperty(notes = "${swagger.model.descriptions.vehicleResult.vehicleResultDetails}")
  List<VehicleResultDto> vehicleResult;
}