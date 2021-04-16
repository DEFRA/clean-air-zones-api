package uk.gov.caz.definitions.domain;

import java.text.SimpleDateFormat;
import java.util.List;
import javax.persistence.Transient;

import lombok.Getter;
import lombok.Setter;

/**
 * Domain object representation of a vehicle for compliance calculation purposes.
 *
 */
@Getter
@Setter
public class Vehicle extends RemoteVehicleDataResponse {

  private static final long serialVersionUID = 1L;

  @Transient
  private Boolean isTaxiOrPhv;

  @Transient
  private Boolean isWav;

  @Transient
  private VehicleType vehicleType;

  @Transient
  private List<String> licensingAuthoritiesNames;

  @Override
  public String toString() {
    SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy");
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append("Vehicle Type: " + this.vehicleType);
    if (this.getDateOfFirstRegistration() != null) {
      stringBuilder.append(
          "; Date of FirstRegistration: " + formatter.format(this.getDateOfFirstRegistration()));
    }
    stringBuilder.append("; Euro status: " + this.getEuroStatus());
    stringBuilder.append("; Type Approval: " + this.getTypeApproval());
    stringBuilder.append("; Seating Capacity: " + this.getSeatingCapacity());
    stringBuilder.append("; Mass in service: " + this.getMassInService());
    stringBuilder.append("; Body type: " + this.getBodyType());
    stringBuilder.append("; Make: " + this.getMake());
    stringBuilder.append("; Model: " + this.getModel());
    stringBuilder.append("; Tax class: " + this.getTaxClass());
    stringBuilder.append("; Fuel type: " + this.getFuelType());
    stringBuilder.append("; isTaxiOrPhv: " + this.isTaxiOrPhv);
    return stringBuilder.toString();
  }
}