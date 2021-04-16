package uk.gov.caz.accounts.service;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableSet;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;
import uk.gov.caz.accounts.annotation.IntegrationTest;
import uk.gov.caz.accounts.model.AccountVehicle;
import uk.gov.caz.accounts.model.VehicleChargeability;
import uk.gov.caz.accounts.repository.AccountVehicleRepository;
import uk.gov.caz.accounts.repository.VehicleChargeabilityRepository;
import uk.gov.caz.accounts.service.ChargeCalculationService.CachePopulationResult;

@Sql(scripts = {"classpath:data/sql/create-chargeability-cache-data.sql"},
    executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "classpath:data/sql/delete-chargeability-cache-data.sql", executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
@IntegrationTest
public class ChargeCalculationServiceTestIT extends ExternalCallsIT {

  private static final UUID FLEET1_ID = UUID.fromString("1f30838f-69ee-4486-95b4-7dfcd5c6c67c");
  private static final UUID BIRMINGHAM_CAZ_ID = UUID
      .fromString("53e03a28-0627-11ea-9511-ffaaee87e375");
  private static final UUID BATH_CAZ_ID = UUID.fromString("742b343f-6ce6-42d3-8324-df689ad4c515");
  private static final String VRN_2 = "VRN2";
  private static final String VRN_3 = "VRN3";
  private static final String VRN_4 = "VRN4";
  private static final String VRN_5 = "VRN5";
  private static final UUID FLEET1_VEHICLE_1_ID = UUID
      .fromString("ccbc6bea-4b0f-45ec-bbf2-021451823441");
  private static final UUID FLEET1_VEHICLE_2_ID = UUID
      .fromString("13c52f66-fdc1-43e2-b6af-a67d04987776");
  private static final UUID FLEET1_VEHICLE_3_ID = UUID
      .fromString("6d3c83de-2c89-443c-be17-662bdde3841a");
  private static final UUID FLEET1_VEHICLE_4_ID = UUID
      .fromString("e8704ce7-5038-4c2c-a263-c5006ad9423f");
  private static final UUID FLEET1_VEHICLE_5_ID = UUID
      .fromString("0b498d32-3fc6-4814-b8ac-41211a8a395d");
  private static final UUID FLEET2_VEHICLE_2_ID = UUID
      .fromString("63a744b3-7ece-4246-849c-9267005a710a");
  private final static int PROCESS_ALL_VEHICLES = 0;
  private final static List<UUID> FLEET3_VEHICLE_IDS = Arrays.asList(
      UUID.fromString("e0e8eea6-cd51-4b65-ae03-ec6cbd9ec9d1"),
      UUID.fromString("2ac2ee5e-0fc6-429f-affc-eec7ec9ee51b"),
      UUID.fromString("a2aa5422-8a9b-4f47-be8b-6fab6529d25a"),
      UUID.fromString("7b896ddf-424e-4bdb-bbb3-67fce4c154a2"),
      UUID.fromString("86076c35-8e1c-47e0-9def-881749528b00"),
      UUID.fromString("5d670686-143e-4017-b2af-2ef45072678c"));

  @Autowired
  private ChargeCalculationService chargeCalculationService;

  @Autowired
  private VehicleChargeabilityRepository vehicleChargeabilityRepository;

  @Autowired
  private AccountVehicleRepository accountVehicleRepository;

  @Test
  public void testChargeabilityCachePopulation() {
    mockVccsCleanAirZonesCall();
    mockVccsBulkComplianceCall(ImmutableSet.of(VRN_2, VRN_3, VRN_4, VRN_5), BIRMINGHAM_CAZ_ID,
        BATH_CAZ_ID,
        "vehicle-compliance-response.json", 200);
    CachePopulationResult result = chargeCalculationService
        .populateCache(FLEET1_ID, PROCESS_ALL_VEHICLES);

    assertThat(result).isEqualByComparingTo(CachePopulationResult.ALL_RECORDS_CACHED);

    List<VehicleChargeability> allCachedCharges = newArrayList(
        vehicleChargeabilityRepository.findAll());

    Page<AccountVehicle> allByAccountId = accountVehicleRepository
        .findAllByAccountId(FLEET1_ID, PageRequest.of(0, 10, Sort.by("vrn")));
    List<AccountVehicle> allWithChargeability = accountVehicleRepository
        .findAllWithChargeability(allByAccountId.get().map(AccountVehicle::getAccountVehicleId)
            .collect(Collectors.toList()));

    // Fleet 1 has 5 vehicles and there are 2 CAZEs, so 10 entries for Fleet 1 after complete
    // cache population.
    // Fleet 2 has 3 vehicles but one of them (VRN2) has already calculation for both CAZes -
    // and it mustn't be touched by population for Fleet 1. So +2.
    // Fleet 4 has 3 vehicles with cache in 2 CAZes, and 2 vehicles witch cache in 1 CAZ.
    // Expected size of T_VEHICLE_CHARGEABILITY is Fleet 1 (10) + Fleet 2 (2)
    assertThat(allCachedCharges).hasSize(20);
    assertThatThereAreTwoEntriesForFleet2(allCachedCharges);
    assertThatThereAreEightEntriesForFleet3(allCachedCharges);
    assertCorrectFleet1VehiclesChargeabilityCache(allCachedCharges);
    assertThatAnyOfUpdatedVehiclesFromFleet1IsNowACarNotAVan();
  }

  @Test
  public void testChargeabilityCachePopulationForSingleVehicle() {
    mockVccsCleanAirZonesCall();
    mockVccsBulkComplianceCall(Collections.singleton(VRN_2), BIRMINGHAM_CAZ_ID, BATH_CAZ_ID,
        "vehicle-compliance-response.json", 200);
    chargeCalculationService.populateCacheForSingleVehicle(FLEET1_VEHICLE_2_ID, VRN_2);

    List<VehicleChargeability> allCachedCharges = newArrayList(
        vehicleChargeabilityRepository.findAll());

    // Fleet 1 had VRN 1 in both CAZEs, VRN 2 in one CAZ and VRN 3 in one CAZ. After charge calc for
    // VRN 2 it must have calc for it in both CAZes. So 5 entries for Fleet 1.
    // Fleet 2 has VRN 2 for both CAZes.
    // Fleet 3 has 5 VRNs with 8 entries in chargeable cache.
    assertThat(allCachedCharges).hasSize(15);
    assertThatThereAreTwoEntriesForFleet2(allCachedCharges);
    assertCorrectFleet1Vrn2ChargeabilityCache(allCachedCharges);
    assertThatAnyOfUpdatedVehiclesFromFleet1IsNowACarNotAVan();
  }

  private void assertCorrectFleet1VehiclesChargeabilityCache(
      List<VehicleChargeability> allCachedCharges) {
    assertThatFleet1VehicleHasChargeInCaz(FLEET1_VEHICLE_1_ID, 12.0f, BIRMINGHAM_CAZ_ID,
        allCachedCharges);
    assertThatFleet1VehicleHasChargeInCaz(FLEET1_VEHICLE_1_ID, 15.0f, BATH_CAZ_ID,
        allCachedCharges);
    assertThatFleet1VehicleHasChargeInCaz(FLEET1_VEHICLE_2_ID, 8.0f, BIRMINGHAM_CAZ_ID,
        allCachedCharges);
    assertThatFleet1VehicleHasChargeInCaz(FLEET1_VEHICLE_2_ID, 2.0f, BATH_CAZ_ID,
        allCachedCharges);
    assertThatFleet1VehicleHasChargeInCaz(FLEET1_VEHICLE_3_ID, 8.0f, BIRMINGHAM_CAZ_ID,
        allCachedCharges);
    assertThatFleet1VehicleHasChargeInCaz(FLEET1_VEHICLE_3_ID, 2.0f, BATH_CAZ_ID,
        allCachedCharges);
    assertThatFleet1VehicleHasChargeInCaz(FLEET1_VEHICLE_4_ID, 8.0f, BIRMINGHAM_CAZ_ID,
        allCachedCharges);
    assertThatFleet1VehicleHasChargeInCaz(FLEET1_VEHICLE_4_ID, 2.0f, BATH_CAZ_ID,
        allCachedCharges);
    assertThatFleet1VehicleHasChargeInCaz(FLEET1_VEHICLE_5_ID, 8.0f, BIRMINGHAM_CAZ_ID,
        allCachedCharges);
    assertThatFleet1VehicleHasChargeInCaz(FLEET1_VEHICLE_5_ID, 2.0f, BATH_CAZ_ID,
        allCachedCharges);
  }

  private void assertCorrectFleet1Vrn2ChargeabilityCache(
      List<VehicleChargeability> allCachedCharges) {
    assertThatFleet1VehicleHasChargeInCaz(FLEET1_VEHICLE_2_ID, 8.0f, BIRMINGHAM_CAZ_ID,
        allCachedCharges);
    assertThatFleet1VehicleHasChargeInCaz(FLEET1_VEHICLE_2_ID, 2.0f, BATH_CAZ_ID,
        allCachedCharges);
  }

  private void assertThatAnyOfUpdatedVehiclesFromFleet1IsNowACarNotAVan() {
    Page<AccountVehicle> fleet1Vehicles = accountVehicleRepository
        .findAllByAccountId(FLEET1_ID, Pageable.unpaged());
    AccountVehicle vehicle = fleet1Vehicles.get()
        .filter(accountVehicle -> accountVehicle.getVrn().equalsIgnoreCase(VRN_2)).findFirst()
        .get();
    assertThat(vehicle.getCazVehicleType()).isEqualTo("Car");
  }

  private void assertThatFleet1VehicleHasChargeInCaz(UUID accountVehicleId, float expectedCharge,
      UUID cazId, List<VehicleChargeability> allCachedCharges) {
    BigDecimal actualCharge = allCachedCharges.stream().filter(
        vehicleChargeability -> vehicleChargeability.getAccountVehicleId().equals(accountVehicleId))
        .filter(vehicleChargeability -> vehicleChargeability.getCazId().equals(cazId)).findFirst()
        .get().getCharge();
    assertThat(actualCharge).isEqualByComparingTo(BigDecimal.valueOf(expectedCharge));
  }

  private void assertThatThereAreTwoEntriesForFleet2(List<VehicleChargeability> allCachedCharges) {
    assertThat(allCachedCharges.stream().filter(
        vehicleChargeability -> vehicleChargeability.getAccountVehicleId()
            .equals(FLEET2_VEHICLE_2_ID))).hasSize(2);
  }

  private void assertThatThereAreEightEntriesForFleet3(List<VehicleChargeability> allCachedCharges) {
    assertThat(allCachedCharges.stream().filter(
        vehicleChargeability -> FLEET3_VEHICLE_IDS
            .contains(vehicleChargeability.getAccountVehicleId()))).hasSize(8);
  }
}
