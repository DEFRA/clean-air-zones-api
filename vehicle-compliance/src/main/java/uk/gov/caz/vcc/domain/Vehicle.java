package uk.gov.caz.vcc.domain;

import java.util.List;
import javax.persistence.Entity;
import javax.persistence.Transient;

import lombok.Getter;
import lombok.Setter;

/**
 * Domain object representation of a vehicle for compliance calculation purposes.
 *
 */
@Getter
@Setter
@Entity
public class Vehicle extends RemoteVehicleDataResponse {
  
  private static final long serialVersionUID = 1L;

  @Transient
  private Boolean isTaxiOrPhv;

  private Boolean isWav;

  @Transient
  private VehicleType vehicleType;

  @Transient
  private List<String> licensingAuthoritiesNames;
}
