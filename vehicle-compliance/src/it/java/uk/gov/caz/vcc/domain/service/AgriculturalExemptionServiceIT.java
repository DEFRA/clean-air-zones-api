package uk.gov.caz.vcc.domain.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import uk.gov.caz.definitions.domain.Vehicle;
import uk.gov.caz.definitions.domain.VehicleType;
import uk.gov.caz.vcc.annotation.IntegrationTest;

@IntegrationTest
public class AgriculturalExemptionServiceIT {
  @Autowired
  private AgriculturalExemptionService agriculturalExemptionService;

  @ParameterizedTest
  @MethodSource("createExemptVehicles")
  public void givenValidTypesThenPositiveExemptionCheck(Vehicle vehicle) {
    assertTrue(agriculturalExemptionService.isExemptAgriculturalVehicle(vehicle));
  }
  
  private static List<Vehicle> createExemptVehicles() {
    List<String> exemptAgriculturalBodyTypes = 
        Arrays.asList("root crop harvester", "forage harvester", "windrower", "sprayer", "viner/picker",
            "rootcrop harvester", "forageharvester", "wind rower", "viner / picker");
    List<Vehicle> exemptVehicles = new ArrayList<Vehicle>();
    Vehicle agriculturalMachineVehicle = createAgriculturalVehicle("agricultural machine", "combine harvester");
    exemptVehicles.add(agriculturalMachineVehicle);
    exemptAgriculturalBodyTypes.forEach(bodyType -> exemptVehicles.add(createAgriculturalVehicle("DIGGING MACHINE", bodyType)));
    
    return exemptVehicles;
  }
  
  private static Vehicle createAgriculturalVehicle(String taxClass, String bodyType) {
    Vehicle vehicle = new Vehicle();
    vehicle.setRegistrationNumber("CAS300");
    vehicle.setColour("pink");
    vehicle.setEuroStatus("EURO 3");
    vehicle.setMake("Fiat");
    vehicle.setModel("500");
    vehicle.setFuelType("Diesel");
    vehicle.setVehicleType(VehicleType.AGRICULTURAL);
    vehicle.setTaxClass(taxClass);
    vehicle.setBodyType(bodyType);
    return vehicle;
  }

  @ParameterizedTest
  @MethodSource("createNonExemptVehicles")
  void givenNonExemptBodyTypesThenNegativeExemptionCheck(Vehicle vehicle) {
    assertFalse(agriculturalExemptionService.isExemptAgriculturalVehicle(vehicle));
  }

  private static List<Vehicle> createNonExemptVehicles() {
    List<String> nonExemptAgriculturalBodyTypes = 
        Arrays.asList("Tel Material Handler", "Agricultural Tractor", "Combine Harvester", "Agricultural Machine", "Mowing machine");
    List<Vehicle> nonExemptVehicles = new ArrayList<Vehicle>();
    nonExemptAgriculturalBodyTypes.forEach(bodyType -> nonExemptVehicles.add(createAgriculturalVehicle("DIGGING MACHINE", bodyType)));
    return nonExemptVehicles;
  }
}