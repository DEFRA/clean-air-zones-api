package uk.gov.caz.vcc.dto;

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

  private static final long serialVersionUID = -3962783531671588131L;

  @ApiModelProperty(value = "${swagger.model.descriptions.informationUrls.emissionsStandardsUrl}")
  String emissionsStandards;

  @ApiModelProperty(value = "${swagger.model.descriptions.informationUrls.mainInfoUrl}")
  String mainInfo;

  @ApiModelProperty(value = "${swagger.model.descriptions.informationUrls.hoursOfOperationUrl}")
  String hoursOfOperation;

  @ApiModelProperty(value = "${swagger.model.descriptions.informationUrls.pricingUrl}")
  String pricing;

  @ApiModelProperty(value = "${swagger.model.descriptions.informationUrls.exemptionOrDiscountUrl}")
  String exemptionOrDiscount;

  @ApiModelProperty(value = "${swagger.model.descriptions.informationUrls.payCazUrl}")
  String payCaz;

  @ApiModelProperty(value = "${swagger.model.descriptions.informationUrls.becomeCompliantUrl}")
  String becomeCompliant;

  @ApiModelProperty(value = "${swagger.model.descriptions.informationUrls.financialAssistanceUrl}")
  String financialAssistance;

  @ApiModelProperty(value = "${swagger.model.descriptions.informationUrls.boundaryUrl}")
  String boundary;

  @ApiModelProperty(value = "${swagger.model.descriptions.informationUrl.additionalInfoUrl}")
  String additionalInfo;
}