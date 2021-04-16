package uk.gov.caz.vcc.domain.service.compliance;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.caz.definitions.domain.Vehicle;
import uk.gov.caz.vcc.annotation.IntegrationTest;

@IntegrationTest
public class BathComplianceServiceTestIT {
  @Autowired
  private BathComplianceService cazComplianceService;

  @ParameterizedTest
  @MethodSource("BathCompliantHybrid")
  void givenBathCompliantHybridThenPostiveComplianceCheck(Vehicle vehicle) {
    assertEquals(Optional.of(true), cazComplianceService.isVehicleCompliant(vehicle));
  }

  private static Stream<Arguments> BathCompliantHybrid() {
    Collection<String> fuelTypes = Arrays.asList("gas/petrol", "petrol/gas",
        "gas bi-fuel", "electric diesel", "gas diesel", "hybrid electric");
    return fuelTypes.stream().map(fuelType -> {
      Vehicle vehicle = new Vehicle();
      vehicle.setSeatingCapacity(4);
      vehicle.setFuelType(fuelType);
      return Arguments.of(vehicle);
    });
  }

  @Test
  void givenBathUnqualifiedVehicleInfoThenEmptyOptionalIsReturned() {
    Vehicle missingFuelTypeInfoVehicle = new Vehicle();
    missingFuelTypeInfoVehicle.setFuelType(null);
    assertEquals(Optional.empty(), cazComplianceService.isVehicleCompliant(missingFuelTypeInfoVehicle));
  }

  @Test
  void dieselNotDeterminedCompliantInBath() {
    Vehicle testVehicle = new Vehicle();
    testVehicle.setFuelType("diesel");
  
    assertEquals(Optional.empty(), cazComplianceService.isVehicleCompliant(testVehicle));
  }

  @Test
  void petrolNotDeterminedCompliantInBath() {
    Vehicle testVehicle = new Vehicle();
    testVehicle.setFuelType("petrol");
  
    assertEquals(Optional.empty(), cazComplianceService.isVehicleCompliant(testVehicle));
  }

  @Test
  void heavyOilNotDeterminedCompliantInBath() {
    Vehicle testVehicle = new Vehicle();
    testVehicle.setFuelType("heavy oil");

    assertEquals(Optional.empty(), cazComplianceService.isVehicleCompliant(testVehicle));
  }
}
