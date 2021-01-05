package uk.gov.caz.accounts.model;

import static com.google.common.collect.Sets.newHashSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class VehiclesToCalculateChargeabilityTest {

  private final static UUID ACCOUNT_VEHICLE_ID = UUID.randomUUID();

  @Test
  void getAccountVehicleIdFor() {
    // given
    Set<AccountVehicleBare> setOfVehicles = newHashSet(
        AccountVehicleBare.from(ACCOUNT_VEHICLE_ID.toString(), "VrN"));
    VehiclesToCalculateChargeability vehiclesToCalculateChargeability = new VehiclesToCalculateChargeability(
        setOfVehicles);

    // when
    UUID foundAccountVehicleId = vehiclesToCalculateChargeability.getAccountVehicleIdFor("vrn");
    assertThat(foundAccountVehicleId).isEqualByComparingTo(ACCOUNT_VEHICLE_ID);

    foundAccountVehicleId = vehiclesToCalculateChargeability.getAccountVehicleIdFor("  VRN  ");
    assertThat(foundAccountVehicleId).isEqualByComparingTo(ACCOUNT_VEHICLE_ID);

    foundAccountVehicleId = vehiclesToCalculateChargeability.getAccountVehicleIdFor("00VRN  ");
    assertThat(foundAccountVehicleId).isEqualByComparingTo(ACCOUNT_VEHICLE_ID);

    Throwable throwable = catchThrowable(
        () -> vehiclesToCalculateChargeability.getAccountVehicleIdFor("non existent"));

    assertThat(throwable).isInstanceOf(NoSuchElementException.class);
  }
}