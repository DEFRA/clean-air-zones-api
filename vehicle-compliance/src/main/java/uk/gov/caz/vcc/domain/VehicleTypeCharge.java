package uk.gov.caz.vcc.domain;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.caz.definitions.domain.VehicleType;

/**
 * A class to bind specific Clean Air Zone charges to different vehicle types
 * (e.g. Car, Van, Bus etc.).
 *
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
public class VehicleTypeCharge implements Serializable {

  private static final long serialVersionUID = 2883181850214677876L;
  private VehicleType vehicleType;
  private float charge;

  @Override
  public String toString() {
    return String.valueOf(vehicleType) + ": " + charge;
  }
}
