package uk.gov.caz.vcc.repository;

import java.util.Optional;
import org.springframework.cache.annotation.Cacheable;
import uk.gov.caz.vcc.domain.Vehicle;

public interface VehicleDetailsRepository {
  
  @Cacheable(value = "vehicles")
  Optional<Vehicle> findByRegistrationNumber(String vrn);
   
}
