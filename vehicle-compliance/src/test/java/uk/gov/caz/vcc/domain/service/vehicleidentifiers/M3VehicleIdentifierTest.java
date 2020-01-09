package uk.gov.caz.vcc.domain.service.vehicleidentifiers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import uk.gov.caz.vcc.domain.Vehicle;
import uk.gov.caz.vcc.domain.VehicleType;
import uk.gov.caz.vcc.domain.exceptions.UnidentifiableVehicleException;

class M3VehicleIdentifierTest {

  private M3VehicleIdentifier m3VehicleIdentifier= new M3VehicleIdentifier();

  @Test
  void vehicleTypeProperlySetToMinibus() {
    //given
    Vehicle vehicle = new Vehicle();
    vehicle.setRevenueWeight(4999);
    //when
    m3VehicleIdentifier.identifyVehicle(vehicle);
    //then
    assertThat(vehicle.getVehicleType()).isEqualTo(VehicleType.MINIBUS);
  }

  @Test
  void vehicleTypeProperlySetToBus() {
    //given
    Vehicle vehicle = new Vehicle();
    vehicle.setRevenueWeight(5001);
    //when
    m3VehicleIdentifier.identifyVehicle(vehicle);
    //then
    assertThat(vehicle.getVehicleType()).isEqualTo(VehicleType.BUS);
  }

  @Test
  void shouldTrowUnidentifiableVehicleException() {
    //given
    Vehicle vehicle = new Vehicle();
    vehicle.setRevenueWeight(null);
    // expect
    assertThrows(UnidentifiableVehicleException.class,
        () -> m3VehicleIdentifier.identifyVehicle(vehicle));
  }
}