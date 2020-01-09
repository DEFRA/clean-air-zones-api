package uk.gov.caz.vcc.domain.service.vehicleidentifiers;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import uk.gov.caz.vcc.annotation.IntegrationTest;
import uk.gov.caz.vcc.domain.Vehicle;
import uk.gov.caz.vcc.domain.VehicleType;
import uk.gov.caz.vcc.domain.exceptions.UnidentifiableVehicleException;
import uk.gov.caz.vcc.domain.service.vehicleidentifiers.TVehicleIdentifier;

@IntegrationTest
public class TVehicleIdentifierTestIT {
  @Autowired
  private TVehicleIdentifier vehicleIdentifier;

  @Test
  void givenAgriculturalTypeApprovalsThenVehicleTypeAreSetToAgricultural(){
    Collection<String> typeApprovals = Arrays.asList("T1","T2","T3","T4","T5");
    List<Vehicle> vehicles = typeApprovals
                              .stream()
                              .map(type -> {
                                Vehicle vehicle = new Vehicle();
                                vehicle.setTypeApproval(type);
                                vehicleIdentifier.identifyVehicle(vehicle);
                                return vehicle;
                              })
                              .filter( vehicle -> vehicle.getVehicleType() != VehicleType.AGRICULTURAL)
                              .collect(Collectors.toList());
    assertTrue(vehicles.size() == 0);
  }

  @Test
  void givenAgriculturalTypeNotApprovalsThenExceptionIsThrown() {
    Vehicle vehicle = new Vehicle();
    vehicle.setTypeApproval("typeNotApproval");
    assertThrows(UnidentifiableVehicleException.class, () -> vehicleIdentifier.identifyVehicle(vehicle));
  }
}