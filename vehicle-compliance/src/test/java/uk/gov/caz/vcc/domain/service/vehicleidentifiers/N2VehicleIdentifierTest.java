package uk.gov.caz.vcc.domain.service.vehicleidentifiers;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.caz.vcc.domain.Vehicle;
import uk.gov.caz.vcc.domain.VehicleType;
import uk.gov.caz.vcc.domain.exceptions.UnidentifiableVehicleException;

class N2VehicleIdentifierTest {
  
  private N2VehicleIdentifier identifier;
  private Vehicle testVehicle;
  
  @BeforeEach
  void init() {
    testVehicle = new Vehicle();
    identifier = new N2VehicleIdentifier();
  }

  @Test
  void hgvCorrectlyIdentified() {
    testVehicle.setRevenueWeight(3501);
    
    identifier.identifyVehicle(testVehicle);
    
    assertEquals(VehicleType.HGV, testVehicle.getVehicleType());
  }
  
  @Test
  void lessThanOrEqualTo3500kgRaisesException() {
    testVehicle.setRevenueWeight(3500);

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
  void zeroRevenueWeightRaisesException() {
    testVehicle.setRevenueWeight(0);

    assertThrows(UnidentifiableVehicleException.class,
        () -> identifier.identifyVehicle(testVehicle));
  }
  
}