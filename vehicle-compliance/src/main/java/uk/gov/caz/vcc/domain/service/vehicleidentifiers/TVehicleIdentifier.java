package uk.gov.caz.vcc.domain.service.vehicleidentifiers;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import uk.gov.caz.vcc.domain.Vehicle;
import uk.gov.caz.vcc.domain.VehicleType;
import uk.gov.caz.vcc.domain.exceptions.UnidentifiableVehicleException;

/**
 * VehicleIdentifer class for vehicles with T* typeApproval.
 * 
 * @author informed
 */
@Service
public class TVehicleIdentifier extends VehicleIdentifier {

  private List<String> agriculturalTypeApprovals;

  /**
   * Default public constructor for VehicleIdentifier. Populates the necessary
   * collections for checking vehicle type.
   */
  public TVehicleIdentifier(
    @Value("${application.vehicle-identifier.agricultural-type-approvals:T1,T2,T3,T4,T5}") String[] agriculturalTypeApprovals) {
    this.agriculturalTypeApprovals = Arrays.asList(agriculturalTypeApprovals);
  }

  @Override
  public void identifyVehicle(Vehicle vehicle) {

    if (agriculturalTypeApprovals.contains(vehicle.getTypeApproval())) {
      vehicle.setVehicleType(VehicleType.AGRICULTURAL);
    } else {
      throw new UnidentifiableVehicleException("typeApproval not recognised.");
    }
  }

}
