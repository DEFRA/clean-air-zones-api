package uk.gov.caz.vcc.domain;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class VehicleTypeCharge implements Serializable {

  private static final long serialVersionUID = 2883181850214677876L;
  private VehicleType vehicleType;
  private float charge;

}
