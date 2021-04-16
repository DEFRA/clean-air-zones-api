package uk.gov.caz.vcc.domain.service.vehicleidentifiers;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import uk.gov.caz.definitions.domain.Vehicle;
import uk.gov.caz.definitions.domain.VehicleType;

class M1VehicleIdentifierTest {

  @Test
  void vehicleTypeProperlySet() {
    //given
    Vehicle vehicle = new Vehicle();
    //when
    new M1VehicleIdentifier().identifyVehicle(vehicle);
    //then
    assertThat(vehicle.getVehicleType()).isEqualTo(VehicleType.PRIVATE_CAR);

  }
  
  @Test
  void motorhomeVehicleTypeProperlySet() {
  //given
    Vehicle vehicle = new Vehicle();
    vehicle.setBodyType("motorhome/caravan");
    //when
    new M1VehicleIdentifier().identifyVehicle(vehicle);
    //then
    assertThat(vehicle.getVehicleType()).isEqualTo(VehicleType.VAN);
  }
}