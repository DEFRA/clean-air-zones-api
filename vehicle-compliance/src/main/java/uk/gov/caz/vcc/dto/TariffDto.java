package uk.gov.caz.vcc.dto;

import java.util.UUID;
import lombok.Builder;
import lombok.Value;


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
  
  boolean disabledTaxClassChargeable;

}
