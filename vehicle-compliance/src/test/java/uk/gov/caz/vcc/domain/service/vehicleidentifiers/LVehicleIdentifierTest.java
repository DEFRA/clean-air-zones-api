package uk.gov.caz.vcc.domain.service.vehicleidentifiers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import uk.gov.caz.vcc.domain.Vehicle;
import uk.gov.caz.vcc.domain.VehicleType;
import uk.gov.caz.vcc.domain.exceptions.UnidentifiableVehicleException;

public class LVehicleIdentifierTest {
  private LVehicleIdentifier lVehicleIdentifier = new LVehicleIdentifier();

  @ParameterizedTest
  @ValueSource(strings = {"L1", "L2", "L3", "L4", "L5", "L6", "L7"})
  void shouldSetCorrectVehicleTypeToMotorcycle(String typeApproval) {
    //given
    Vehicle vehicle = new Vehicle();
    vehicle.setTypeApproval(typeApproval);
    //when
    lVehicleIdentifier.identifyVehicle(vehicle);
    //then
    assertThat(vehicle.getVehicleType()).isEqualTo(VehicleType.MOTORCYCLE);
  }

  @Test
  void shouldThrowUnidentifiableVehicleException() {
    //given
    Vehicle vehicle = new Vehicle();
    vehicle.setTypeApproval("any not listed string");
    // expect
    assertThrows(UnidentifiableVehicleException.class,
        () -> lVehicleIdentifier.identifyVehicle(vehicle));
  }
}