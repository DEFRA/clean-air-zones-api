package uk.gov.caz.vcc.domain.service;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.caz.vcc.annotation.IntegrationTest;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.caz.vcc.domain.CalculationResult;
import uk.gov.caz.vcc.domain.GeneralWhitelistVehicle;
import uk.gov.caz.vcc.repository.GeneralWhitelistRepository;
import uk.gov.caz.definitions.domain.Vehicle;
import uk.gov.caz.definitions.domain.VehicleType;

@IntegrationTest
public class ComplianceServiceTestIT {
  
  private String bathCazIdentifier = "131af03c-f7f4-4aef-81ee-aae4f56dbeb5";
  private String arbitraryCazIdentifier = "dd487c1e-11d3-11ea-8d71-362b9e155667";
  private CalculationResult result;
  
  @Autowired
  private ComplianceService complianceService;
  
  @Autowired
  private GeneralWhitelistRepository generalWhitelistRepository;

  @AfterEach
  void post() {
    generalWhitelistRepository.deleteAll();
  }

  
  @BeforeEach
  private void init() {
    ReflectionTestUtils.setField(complianceService, "bathCazIdentifier", bathCazIdentifier);
    result = new CalculationResult();
  }
  
  @Test
  public void givenVehicleCompliantOnWhiteListCompliantTrue() {
    result.setCazIdentifier(UUID.fromString(this.arbitraryCazIdentifier));
    Vehicle vehicle = createNonCompliantVehicle(false, 4);
    generalWhitelistRepository.save(buildGeneralWhitelistVehicle(true));
    complianceService.updateCalculationResult(vehicle, result);
    assertTrue(result.getCompliant());
  }
  
  @Test
  public void givenVehicleNotOnWhiteListCompliantFalse() {
    result.setCazIdentifier(UUID.fromString(this.arbitraryCazIdentifier));
    Vehicle vehicle = createNonCompliantVehicle(false, 4);
    complianceService.updateCalculationResult(vehicle, result);
    assertFalse(result.getCompliant());
  }
  
  @Test 
  public void givenBathNonCompliantTaxiWhitelistCompliantFalse() {
    result.setCazIdentifier(UUID.fromString(this.bathCazIdentifier));
    Vehicle vehicle = createNonCompliantVehicle(true, 4);
    generalWhitelistRepository.save(buildGeneralWhitelistVehicle(false));
    complianceService.updateCalculationResult(vehicle, result);
    assertFalse(result.getCompliant());
  }
  
  @Test 
  public void givenBathCompliantTaxiWhitelistCompliantFalse() {
    result.setCazIdentifier(UUID.fromString(this.bathCazIdentifier));
    Vehicle vehicle = createNonCompliantVehicle(true, 6);
    generalWhitelistRepository.save(buildGeneralWhitelistVehicle(true));
    complianceService.updateCalculationResult(vehicle, result);
    assertTrue(result.getCompliant());
  }
  
  @ParameterizedTest
  @MethodSource("BathCompliantHybrid")
  public void givenHybridVehicleEnterBathCazCompliantTrue(Vehicle vehicle) {
    result.setCazIdentifier(UUID.fromString(this.bathCazIdentifier));
    complianceService.updateCalculationResult(vehicle, result);
    assertTrue(result.getCompliant());
  }
  
  private GeneralWhitelistVehicle buildGeneralWhitelistVehicle(boolean isCompliant) {
    return GeneralWhitelistVehicle.builder()
        .vrn("CAS300")
        .reasonUpdated("For testing")
        .uploaderId(UUID.randomUUID())
        .updateTimestamp(LocalDateTime.now())
        .category("OTHER")
        .compliant(isCompliant)
        .exempt(false)
        .build();
  }
  
  private Vehicle createNonCompliantVehicle(boolean isTaxiPhv, int seatingCapacity){
    Vehicle vehicle = new Vehicle();
    vehicle.setRegistrationNumber("CAS300");
    vehicle.setColour("pink");
    vehicle.setDateOfFirstRegistration(new Date(2016 - 1900,2,1));
    vehicle.setEuroStatus("EURO 3");
    vehicle.setTypeApproval("M1");
    vehicle.setMake("Fiat");
    vehicle.setModel("500");
    vehicle.setFuelType("Diesel");
    vehicle.setVehicleType(VehicleType.PRIVATE_CAR);
    vehicle.setIsTaxiOrPhv(isTaxiPhv);
    vehicle.setSeatingCapacity(seatingCapacity);
    return vehicle;
  }
  
  private static Stream<Arguments> BathCompliantHybrid() {
    Collection<String> fuelTypes = Arrays.asList("gas/petrol", "petrol/gas",
        "gas bi-fuel", "electric diesel", "gas diesel", "hybrid electric");
    return fuelTypes.stream().map(fuelType -> {
      Vehicle vehicle = createHybridVehicle(fuelType);
      return Arguments.of(vehicle);
    });
  }
  
  private static Vehicle createHybridVehicle(String fuelType){
    Vehicle vehicle = new Vehicle();
    vehicle.setRegistrationNumber("CAS300");
    vehicle.setColour("pink");
    vehicle.setDateOfFirstRegistration(new Date(2016 - 1900,2,1));
    vehicle.setEuroStatus("EURO 3");
    vehicle.setTypeApproval("M1");
    vehicle.setMake("Fiat");
    vehicle.setModel("500");
    vehicle.setFuelType(fuelType);
    vehicle.setVehicleType(VehicleType.PRIVATE_CAR);
    vehicle.setIsTaxiOrPhv(true);
    vehicle.setSeatingCapacity(3);
    return vehicle;
  }

}
