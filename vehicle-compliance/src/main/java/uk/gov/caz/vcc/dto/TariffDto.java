package uk.gov.caz.vcc.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;
import uk.gov.caz.definitions.dto.InformationUrlsDto;
import uk.gov.caz.definitions.dto.RatesDto;



/**
 * Value object that holds tariff details.
 */
@Value
@Builder
public class TariffDto {

  UUID cleanAirZoneId;

  String name;

  char tariffClass;

  boolean motorcyclesChargeable;

  RatesDto rates;

  InformationUrlsDto informationUrls;

  String chargeIdentifier;

  @JsonProperty("chargingDisabledVehicles")
  boolean disabledTaxClassChargeable;

}
