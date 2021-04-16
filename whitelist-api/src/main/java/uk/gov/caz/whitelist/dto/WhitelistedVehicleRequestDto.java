package uk.gov.caz.whitelist.dto;

import io.swagger.annotations.ApiModelProperty;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;

/**
 * Request object of whitelist vehicle.
 */
@Value
@Builder
public class WhitelistedVehicleRequestDto {

  /**
   * String containing vehicle registration number.
   */
  @ApiModelProperty(value = "${swagger.model.descriptions.whitelisted-vehicle-details.vrn}")
  String vrn;

  /**
   * String containing category.
   */
  @ApiModelProperty(value = "${swagger.model.descriptions.whitelisted-vehicle-details.category}")
  String category;

  /**
   * Reason given in csv file.
   */
  @ApiModelProperty(value
      = "${swagger.model.descriptions.whitelisted-vehicle-details.reasonUpdated}")
  String reasonUpdated;

  /**
   * User's sub.
   */
  @ApiModelProperty(value = "${swagger.model.descriptions.whitelisted-vehicle-details.uploaderId}")
  UUID uploaderId;

  /**
   * User's email.
   */
  @ApiModelProperty(value = "${swagger.model.descriptions.whitelisted-vehicle-details.email}")
  String email;

  /**
   * String containing a company, which build vehicle.
   */
  @ApiModelProperty(value
      = "${swagger.model.descriptions.whitelisted-vehicle-details.manufacturer}")
  String manufacturer;
}