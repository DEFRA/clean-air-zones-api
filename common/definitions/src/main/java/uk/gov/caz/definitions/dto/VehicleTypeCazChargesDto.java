package uk.gov.caz.definitions.dto;

import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import lombok.Value;

/**
 * A data transport layer object for returning a collection of
 * charges to external clients.
 */
@Value
public class VehicleTypeCazChargesDto {

  @ApiModelProperty(value = "${swagger.model.descriptions.complianceResults.charges}")
  List<ComplianceOutcomeDto> charges;

}