package uk.gov.caz.accounts.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.Streams;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.caz.accounts.annotation.IntegrationTest;
import uk.gov.caz.accounts.model.VehicleChargeability;
import uk.gov.caz.accounts.model.VehiclesToCalculateChargeability;

@Sql(scripts = {"classpath:data/sql/create-chargeability-cache-data.sql"},
    executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "classpath:data/sql/delete-chargeability-cache-data.sql", executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
@IntegrationTest
class VehicleChargeabilityRepositoryTestIT {

  private static final int CAZES_COUNT = 2;
  private static final Timestamp CACHE_REFRESH_DATE = Timestamp
      .valueOf(LocalDateTime.now().minusDays(2));
  private static final UUID FLEET1_ID = UUID.fromString("1f30838f-69ee-4486-95b4-7dfcd5c6c67c");
  private static final UUID FLEET2_ID = UUID.fromString("3de21da7-86fc-4ccc-bab3-130f3a10e380");
  private static final UUID VRN1_IN_FLEET1_ID = UUID
      .fromString("ccbc6bea-4b0f-45ec-bbf2-021451823441");
  private static final UUID VRN2_IN_FLEET2_ID = UUID
      .fromString("63a744b3-7ece-4246-849c-9267005a710a");
  private static final UUID EST121_IN_FLEET3_ID = UUID
      .fromString("e0e8eea6-cd51-4b65-ae03-ec6cbd9ec9d1");
  private static final UUID EST122_IN_FLEET3_ID = UUID
      .fromString("2ac2ee5e-0fc6-429f-affc-eec7ec9ee51b");
  private static final UUID EST123_IN_FLEET3_ID = UUID
      .fromString("a2aa5422-8a9b-4f47-be8b-6fab6529d25a");
  private static final UUID EST124_IN_FLEET3_ID = UUID
      .fromString("7b896ddf-424e-4bdb-bbb3-67fce4c154a2");
  private static final UUID EST125_IN_FLEET3_ID = UUID
      .fromString("86076c35-8e1c-47e0-9def-881749528b00");

  @Autowired
  private VehicleChargeabilityRepository repository;

  @Test
  @Transactional
  @Rollback
  void fleet1OperationsRelatedToChargeabilityCache() {
    // See create-chargeability-cache-data.sql script for test seed data. In Fleet 1 there is 5 vehicles
    // and only first vehicle has chargeability cache for 2 (All) CAZes. The rest 4 are either missing
    // completely from T_VEHICLE_CHARGEABILITY table (which means they are freshly added to the fleet)
    // or have some calculation but no in ALL Cazes.

    // Select all vehicles that do not have chargeability cache in all CAZes
    VehiclesToCalculateChargeability vehiclesToUpdate = repository
        .findAllForFleetThatDoNotHaveChargeabilityCacheInEachCaz(FLEET1_ID, CAZES_COUNT);
    assertThat(vehiclesToUpdate.setOfVrns())
        .containsExactlyInAnyOrder("VRN2", "VRN3", "VRN4", "VRN5");

    // Delete from T_VEHICLE_CHARGEABILITY where cache will be calculated (there were records for
    // vehicles but not in all CAZes)
    repository.deleteFromVehicleChargeability(vehiclesToUpdate.setOfIDs());

    // Check whether T_VEHICLE_CHARGEABILITY is fine for Fleet 1, Fleet 2 and Fleet 3 (which should not be touched)
    List<VehicleChargeability> allChargeabilityEntries = Streams
        .stream(repository.findAll().iterator())
        .collect(Collectors.toList());

    assertThat(allChargeabilityEntries).hasSize(12);
    assertThat(
        allChargeabilityEntries.stream().map(entry -> entry.getAccountVehicleId().toString()))
        .containsExactlyInAnyOrder(VRN1_IN_FLEET1_ID.toString(),
            VRN1_IN_FLEET1_ID.toString(),
            VRN2_IN_FLEET2_ID.toString(),
            VRN2_IN_FLEET2_ID.toString(),
            EST121_IN_FLEET3_ID.toString(),
            EST121_IN_FLEET3_ID.toString(),
            EST122_IN_FLEET3_ID.toString(),
            EST123_IN_FLEET3_ID.toString(),
            EST124_IN_FLEET3_ID.toString(),
            EST124_IN_FLEET3_ID.toString(),
            EST125_IN_FLEET3_ID.toString(),
            EST125_IN_FLEET3_ID.toString());
  }

  @Test
  void shouldSelectValidVrnsInFleet2ThatDoNotHaveChargeabilityCacheInAllCazes() {
    // See create-chargeability-cache-data.sql script for test seed data. In Fleet 2 there is 3 vehicles
    // and only second vehicle has chargeability cache for 2 (All) CAZes. The rest 2 are missing
    // completely from T_VEHICLE_CHARGEABILITY table (which means they are freshly added to the fleet)
    VehiclesToCalculateChargeability vehiclesToUpdate = repository
        .findAllForFleetThatDoNotHaveChargeabilityCacheInEachCaz(FLEET2_ID, CAZES_COUNT);
    assertThat(vehiclesToUpdate.setOfVrns()).containsExactlyInAnyOrder("VRN1", "VRN3");
  }


  @Test
  void shouldSelectValidVrnsInServiceThatDoNotHaveChargeabilityCacheInAllCazesOrHasExpired() {
    // See create-chargeability-cache-data.sql script for test seed data. In Fleet 3 there is 5 vehicles
    // and only first vehicle has chargeability cache for 2 (All) CAZes. The rest 4 are either missing
    // completely from T_VEHICLE_CHARGEABILITY table (which means they are freshly added to the fleet)
    // or have some calculation but no in ALL Cazes or has expired cache.

    // Select all vehicles that do not have chargeability cache in all CAZes
    VehiclesToCalculateChargeability vehiclesToUpdate = repository
        .findAllThatDoNotHaveChargeabilityCacheInEachCazOrExpired(CAZES_COUNT, CACHE_REFRESH_DATE);
    assertThat(vehiclesToUpdate.setOfVrns())
        .containsExactlyInAnyOrder("EST122", "EST123", "EST124", "EST125", "EST126", "VRN1", "VRN2",
            "VRN3", "VRN4", "VRN5");
  }
}