package uk.gov.caz.tariff.dto;

import io.swagger.annotations.ApiModelProperty;
import java.util.UUID;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import lombok.Builder;
import lombok.Value;


/**
 * Value object that holds tariff details.
 */
@Value
@Builder
public class Tariff {

  @ApiModelProperty(value = "${swagger.model.descriptions.tariff.cleanAirZoneId}")
  @NotNull
  UUID cleanAirZoneId;

  @ApiModelProperty(value = "${swagger.model.descriptions.tariff.name}")
  @NotNull
  @Size(min = 1, max = 50)
  String name;

  @ApiModelProperty(value = "${swagger.model.descriptions.tariff.tariffClass}")
  char tariffClass;

  @ApiModelProperty(value = "${swagger.model.descriptions.tariff.chargeIdentifier}")
  String chargeIdentifier;

  @ApiModelProperty(value = "${swagger.model.descriptions.tariff.rates}")
  Rates rates;

  @ApiModelProperty(value = "${swagger.model.descriptions.tariff.informationUrls}")
  InformationUrls informationUrls;

  @ApiModelProperty(value = "${swagger.model.descriptions.tariff.chargingDisabledVehicles}")
  boolean chargingDisabledVehicles;

  @ApiModelProperty(value = "${swagger.model.descriptions.tariff.activeChargeStartDate}")
  String activeChargeStartDate;
}
