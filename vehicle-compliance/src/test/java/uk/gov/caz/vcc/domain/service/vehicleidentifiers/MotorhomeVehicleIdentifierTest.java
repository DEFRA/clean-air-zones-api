package uk.gov.caz.vcc.domain.service.vehicleidentifiers;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import uk.gov.caz.definitions.domain.Vehicle;
import uk.gov.caz.definitions.domain.VehicleType;

class MotorhomeVehicleIdentifierTest {
  
  private MotorhomeVehicleIdentifier identifier;
  private Vehicle testVehicle;
  
  @BeforeEach
  void init() {
    testVehicle = new Vehicle();
    identifier = new MotorhomeVehicleIdentifier();
  }

  @Test
  void nullWeightCorrectlyIdentified() {
    testVehicle.setRevenueWeight(null);
    identifier.identifyVehicle(testVehicle);
    assertEquals(VehicleType.VAN, testVehicle.getVehicleType());
  }
  
  @Test
  void lessThanOrEqualTo3500kgIdentified() {
    testVehicle.setRevenueWeight(3500); 
    identifier.identifyVehicle(testVehicle);
    assertEquals(VehicleType.VAN, testVehicle.getVehicleType());
  }
  
  @Test
  void nullSeatsCorrectlyIdentified() {
    testVehicle.setRevenueWeight(3501);
    testVehicle.setSeatingCapacity(null);
    identifier.identifyVehicle(testVehicle);
    assertEquals(VehicleType.HGV, testVehicle.getVehicleType());
  }
  
  @Test
  void lessThan9SeatsCorrectlyIdentified() {
    testVehicle.setRevenueWeight(3501);
    testVehicle.setSeatingCapacity(8);
    identifier.identifyVehicle(testVehicle);
    assertEquals(VehicleType.HGV, testVehicle.getVehicleType());
  }
  
  @Test
  void minibusCorrectlyIdentified() {
    testVehicle.setRevenueWeight(3501);
    testVehicle.setSeatingCapacity(9);
    identifier.identifyVehicle(testVehicle);
    assertEquals(VehicleType.MINIBUS, testVehicle.getVehicleType());
  }
  
  @Test
  void busCorrectlyIdentified() {
    testVehicle.setRevenueWeight(5001);
    testVehicle.setSeatingCapacity(9);
    identifier.identifyVehicle(testVehicle);
    assertEquals(VehicleType.BUS, testVehicle.getVehicleType());
  }



  
}