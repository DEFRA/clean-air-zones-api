package uk.gov.caz.cucumber;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import uk.gov.caz.definitions.domain.Vehicle;
import uk.gov.caz.vcc.domain.CalculationResult;
import uk.gov.caz.vcc.domain.service.vehicleidentifiers.VehicleIdentifier;

@Component
@Scope("cucumber-glue")
public class World {

  Vehicle vehicle = new Vehicle();
  CalculationResult result = new CalculationResult();
  VehicleIdentifier identifier;

  public World() {
    vehicle.setRegistrationNumber("TEST");
  }

}
