package uk.gov.caz.vcc.domain.service.compliance;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.caz.definitions.domain.Vehicle;

/**
 * A bespoke compliance service implementation to account for compliance rule
 * variance (in relation to hybrid fuel types) unique to the Bathnes Clean Air
 * Zone.
 *
 */
@Slf4j
@Service
public class BathComplianceService {

  private Set<String> compliantHybridFuelTypes;

  /**
   * Create an instance of {@link BathComplianceService}.
   * 
   * @param compliantHybridFuelTypes list of non-chargeable fuel types in Bath
   */
  public BathComplianceService(
      @Value("${application.bath.default-hybrid-fuel-types:"
              + "hybrid electric,gas bi-fuel,gas/petrol,petrol/gas,electric diesel,gas diesel}")
          String[] compliantHybridFuelTypes) {
    this.compliantHybridFuelTypes =
        new HashSet<>(Arrays.asList(compliantHybridFuelTypes));
  }

  /**
   * Determines if a vehicle is compliant in Bath based on Hybrid fuel types.
   * 
   * @param vehicle Vehicle whose compliance is to be determined.
   * @return Optional(true) if compliant, else empty Optional.
   */
  public Optional<Boolean> isVehicleCompliant(Vehicle vehicle) {
    try {
      if (compliantHybridFuelTypes.contains(vehicle.getFuelType().toLowerCase())) {
        return Optional.of(true);
      } else {
        return Optional.empty();
      }

    } catch (NullPointerException ex) {
      log.warn(String.format("NullPointer encountered for fuelType atrributes "
          + "Must use default compliance calculation."));
      return Optional.empty();
    }
  }

}
