package uk.gov.caz.definitions.dto;

import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import lombok.Builder;
import lombok.Value;

/**
 * A data transport layer object for returning a collection of 
 * compliance results to external callers.
 */
@Value
@Builder
public class ComplianceResultsDto {
  
  @ApiModelProperty(value = "${swagger.model.descriptions.complianceResults.registrationNumber}")
  String registrationNumber;
  
  @ApiModelProperty(value = "${swagger.model.descriptions.complianceResults.isRetrofitted}")
  Boolean isRetrofitted;
  
  @ApiModelProperty(value = "${swagger.model.descriptions.complianceResults.isExempt}")
  Boolean isExempt;
  
  @ApiModelProperty(value = "${swagger.model.descriptions.complianceResults.vehicleType}")
  String vehicleType;
  
  @ApiModelProperty(value = "${swagger.model.descriptions.complianceResults.complianceOutcomes}")
  List<ComplianceOutcomeDto> complianceOutcomes;
  
  @ApiModelProperty(value = "${swagger.model.descriptions.complianceResults.note}")
  String note;

  @ApiModelProperty(value = "${swagger.model.descriptions.complianceResults.phgvDiscountAvailable}")
  boolean phgvDiscountAvailable;
}
