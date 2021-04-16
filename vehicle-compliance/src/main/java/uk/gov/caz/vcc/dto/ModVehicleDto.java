package uk.gov.caz.vcc.dto;

import lombok.Builder;
import lombok.ToString;
import lombok.Value;

@Value
@Builder
public class ModVehicleDto {

  @ToString.Exclude
  String vrn;
  String whitelistDiscountCode;
}
