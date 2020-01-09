package uk.gov.caz.vcc.dto;

import io.swagger.annotations.ApiModelProperty;
import java.util.UUID;
import lombok.Value;

/**
 * A data transport layer object for returning an individual compliance outcome against
 * a given Clean Air Zone to external callers.
 *
 */
@Value
public class ChargeDto {
  @ApiModelProperty(value = "${swagger.model.descriptions.complianceResult.cleanAirZoneId}")
  UUID cleanAirZoneId;

  @ApiModelProperty(value = "${swagger.model.descriptions.complianceResult.name}")
  String name;

  @ApiModelProperty(value = "${swagger.model.descriptions.complianceResult.charge}")
  float charge;
}
