package uk.gov.caz.vcc.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uk.gov.caz.vcc.dto.ModVehicleDto;

/**
 * Domain object to represent a MOD vehicle.
 *
 * @author Informed Solutions
 */
@AllArgsConstructor
@NoArgsConstructor
public class MilitaryVehicle {

  @Getter
  @Setter
  String vrn;

  @Getter
  @Setter
  String modWhitelistType;

  @Getter
  @Setter
  String whitelistDiscountCode;

  /**
   * Cast from ModVehicleDto to MilitaryVehicle.
   */
  public static MilitaryVehicle fromDto(ModVehicleDto modVehicleDto) {
    return new MilitaryVehicle(
        modVehicleDto.getVrn(),
        "WHITE VEHICLE",
        modVehicleDto.getWhitelistDiscountCode()
    );
  }
}
