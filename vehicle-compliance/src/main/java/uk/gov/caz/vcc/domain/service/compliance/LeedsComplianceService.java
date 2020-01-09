package uk.gov.caz.vcc.domain.service.compliance;

import com.google.common.base.Preconditions;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import uk.gov.caz.vcc.domain.Vehicle;

@Service
public class LeedsComplianceService implements CazComplianceService {
  private Set<String> chargeableFuelTypes;
   
  /**
   * Create an instance of {@link LeedsComplianceService}.
   * @param chargeableFuelTypes list of chargeable fuel types
   */
  public LeedsComplianceService(
      @Value("${application.leeds.chargeable-fuel-types:hybrid electric}")
      String[] chargeableFuelTypes) {
    this.chargeableFuelTypes = new HashSet<>(Arrays.asList(chargeableFuelTypes));
  }

  @Override
  public boolean isVehicleCompliance(Vehicle vehicle) {
    try {
      Preconditions.checkNotNull(vehicle.getSeatingCapacity());
      Preconditions.checkNotNull(vehicle.getFuelType());
      if (vehicle.getIsTaxiOrPhv() && vehicle.getSeatingCapacity() < 5
          && !chargeableFuelTypes.contains(vehicle.getFuelType())) {
        return false;
      } else {
        throw new UnableToIdentifyVehicleComplianceException("");
      }
    } catch (NullPointerException ex){
      throw new UnableToIdentifyVehicleComplianceException(String.format(
        "NullPointer encountered for seatingCapacity or fuelType atrributes "
            + "on vehicle with VRN: %s "
            + "when checking Leeds-specific compliance."
            + "Must use default compliance calculation.",
        vehicle.getRegistrationNumber()));
    }
  }
}