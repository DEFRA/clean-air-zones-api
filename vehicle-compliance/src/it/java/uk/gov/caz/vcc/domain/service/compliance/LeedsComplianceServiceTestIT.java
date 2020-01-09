package uk.gov.caz.vcc.domain.service.compliance;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import uk.gov.caz.vcc.annotation.IntegrationTest;
import uk.gov.caz.vcc.domain.Vehicle;
import uk.gov.caz.vcc.domain.service.FuelTypeService;

@IntegrationTest
public class LeedsComplianceServiceTestIT {
  @Autowired
  private LeedsComplianceService cazComplianceService;
  
  @ParameterizedTest
  @MethodSource("LeedsNonCompliantTaxiOrPhv")
  void givenLeedsNonCompliantTaxiOrPhvThenNegativeComplianceCheck(Vehicle vehicle) {
    assertFalse(cazComplianceService.isVehicleCompliance(vehicle));
  }
  
  private static Stream<Arguments> LeedsNonCompliantTaxiOrPhv() {
    Collection<String> fuelTypes = Arrays.asList("petrol","gas bi-fuel","gas/petrol","diesel","heavy oil","electric diesel","gas diesel");
    return fuelTypes
            .stream()
            .map(fuelType -> 
                      {
                        Vehicle vehicle = new Vehicle();
                        vehicle.setIsTaxiOrPhv(Boolean.valueOf("true"));
                        vehicle.setSeatingCapacity(4);
                        vehicle.setFuelType(fuelType);
                        return Arguments.of(vehicle);
                      });
  }

  @ParameterizedTest
  @MethodSource("LeedsUnableToIdentifyVehicleCompliance")
  void givenLeedsUnqualifiedVehicleInfoThenExceptionIsThrown(Vehicle vehicle) {
     assertThrows(UnableToIdentifyVehicleComplianceException.class, () -> cazComplianceService.isVehicleCompliance(vehicle));
  }

  private static Stream<Arguments> LeedsUnableToIdentifyVehicleCompliance() {
    Vehicle nonTaxiOrPhv = new Vehicle();
    nonTaxiOrPhv.setIsTaxiOrPhv(Boolean.valueOf("false"));
    
    Vehicle fiveSeatersVehicle = new Vehicle();
    fiveSeatersVehicle.setIsTaxiOrPhv(Boolean.valueOf("true"));
    fiveSeatersVehicle.setFuelType(FuelTypeService.PETROL);
    fiveSeatersVehicle.setSeatingCapacity(Integer.valueOf(5));

    Vehicle missingSeatingInfoVehicle = new Vehicle();
    missingSeatingInfoVehicle.setSeatingCapacity(null);

    Vehicle missingFuelTypeInfoVehicle = new Vehicle();
    missingFuelTypeInfoVehicle.setFuelType(null);

    Vehicle hybridElectric4Seater = new Vehicle();
    hybridElectric4Seater.setIsTaxiOrPhv(Boolean.valueOf("true"));
    hybridElectric4Seater.setFuelType("hybrid electric");
    hybridElectric4Seater.setSeatingCapacity(Integer.valueOf(4));

    return Stream.of(Arguments.of(nonTaxiOrPhv),
                      Arguments.of(fiveSeatersVehicle),
                      Arguments.of(missingSeatingInfoVehicle),
                      Arguments.of(missingFuelTypeInfoVehicle));
  }
}