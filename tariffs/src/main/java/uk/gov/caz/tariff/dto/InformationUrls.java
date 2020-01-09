package uk.gov.caz.tariff.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Value;

/**
 * Value object that holds information urls.
 */
@Value
@Builder
public class InformationUrls {

  @ApiModelProperty(value = "${swagger.model.descriptions.informationUrl.emissionsStandardsUrl}")
  String emissionsStandards;

  @ApiModelProperty(value = "${swagger.model.descriptions.informationUrl.mainInfoUrl}")
  String mainInfo;

  @ApiModelProperty(value = "${swagger.model.descriptions.informationUrl.hoursOfOperationUrl}")
  String hoursOfOperation;

  @ApiModelProperty(value = "${swagger.model.descriptions.informationUrl.pricingUrl}")
  String pricing;

  @ApiModelProperty(value = "${swagger.model.descriptions.informationUrl.exemptionOrDiscountUrl}")
  String exemptionOrDiscount;

  @ApiModelProperty(value = "${swagger.model.descriptions.informationUrl.payCazUrl}")
  String payCaz;

  @ApiModelProperty(value = "${swagger.model.descriptions.informationUrl.becomeCompliantUrl}")
  String becomeCompliant;

  @ApiModelProperty(value = "${swagger.model.descriptions.informationUrl.financialAssistanceUrl}")
  String financialAssistance;

  @ApiModelProperty(value = "${swagger.model.descriptions.informationUrl.boundaryUrl}")
  String boundary;

  @ApiModelProperty(value = "${swagger.model.descriptions.informationUrl.additionalInfoUrl}")
  String additionalInfo;
}