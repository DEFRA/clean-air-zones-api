package uk.gov.caz.vcc.domain.service.vehicleidentifiers;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.caz.vcc.domain.Vehicle;
import uk.gov.caz.vcc.domain.VehicleType;
import uk.gov.caz.vcc.domain.exceptions.UnidentifiableVehicleException;

class M2VehicleIdentifierTest {
  
  private M2VehicleIdentifier identifier;
  private Vehicle testVehicle;
  
  @BeforeEach
  void init() {
    testVehicle = new Vehicle();
    identifier = new M2VehicleIdentifier();
  }

  @Test
  void minibusCorrectlyIdentified() {
    testVehicle.setRevenueWeight(5000);
    testVehicle.setSeatingCapacity(10);
    
    identifier.identifyVehicle(testVehicle);
    
    assertEquals(VehicleType.MINIBUS, testVehicle.getVehicleType());
  }
  
  @Test
  void greaterThanEqualTo5TonnesRaisesException() {
    testVehicle.setRevenueWeight(5001);

    assertThrows(UnidentifiableVehicleException.class,
        () -> identifier.identifyVehicle(testVehicle));
  }

  @Test
  void lessThanOrEqualTo9SeatsRaisesException() {
    testVehicle.setRevenueWeight(5000);
    testVehicle.setSeatingCapacity(9);

    assertThrows(UnidentifiableVehicleException.class,
        () -> identifier.identifyVehicle(testVehicle));
  }
  
  @Test
  void nullRevenueWeightRaisesException() {
    testVehicle.setRevenueWeight(null);

    assertThrows(UnidentifiableVehicleException.class,
        () -> identifier.identifyVehicle(testVehicle));
  }

  @Test
  void nullSeatingCapacityRaisesException() {
    testVehicle.setRevenueWeight(5000);
    testVehicle.setSeatingCapacity(null);

    assertThrows(UnidentifiableVehicleException.class,
        () -> identifier.identifyVehicle(testVehicle));
  }

  @Test
  void zeroRevenueWeightRaisesException() {
    testVehicle.setRevenueWeight(0);

    assertThrows(UnidentifiableVehicleException.class,
        () -> identifier.identifyVehicle(testVehicle));
  }

  @Test
  void zeroSeatingCapacityRaisesException() {
    testVehicle.setRevenueWeight(5000);
    testVehicle.setSeatingCapacity(0);

    assertThrows(UnidentifiableVehicleException.class,
        () -> identifier.identifyVehicle(testVehicle));
  }
  
}