package uk.gov.caz.definitions.dto;

import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;

import lombok.Builder;
import lombok.Value;

/**
 * Value object that holds information urls.
 */
@Value
@Builder
public class InformationUrlsDto implements Serializable {

  private static final long serialVersionUID = -5170390683229958475L;

  @ApiModelProperty(value = "${swagger.model.descriptions.informationUrls.mainInfoUrl}")
  String mainInfo;

  @ApiModelProperty(value = "${swagger.model.descriptions.informationUrls.exemptionOrDiscountUrl}")
  String exemptionOrDiscount;

  @ApiModelProperty(value = "${swagger.model.descriptions.informationUrls.becomeCompliantUrl}")
  String becomeCompliant;

  @ApiModelProperty(value = "${swagger.model.descriptions.informationUrls.boundaryUrl}")
  String boundary;

  @ApiModelProperty(value = "${swagger.model.descriptions.informationUrl.paymentsComplianceUrl}")
  String paymentsCompliance;

  @ApiModelProperty(value = "${swagger.model.descriptions.informationUrl.fleetsComplianceUrl}")
  String fleetsCompliance;

  @ApiModelProperty(
      value = "${swagger.model.descriptions.informationUrl.publicTransportOptionsUrl}")
  String publicTransportOptions;
}
