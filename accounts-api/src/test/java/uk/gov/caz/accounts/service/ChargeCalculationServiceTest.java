package uk.gov.caz.accounts.service;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.sql.Timestamp;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import okhttp3.MediaType;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import retrofit2.Response;
import uk.gov.caz.accounts.model.AccountVehicleBare;
import uk.gov.caz.accounts.model.VehiclesToCalculateChargeability;
import uk.gov.caz.accounts.repository.AccountVehicleRepository;
import uk.gov.caz.accounts.repository.VccsRepository;
import uk.gov.caz.accounts.repository.VehicleChargeabilityRepository;
import uk.gov.caz.accounts.service.ChargeCalculationService.CachePopulationResult;
import uk.gov.caz.definitions.dto.CleanAirZoneDto;
import uk.gov.caz.definitions.dto.CleanAirZonesDto;
import uk.gov.caz.definitions.dto.ComplianceOutcomeDto;
import uk.gov.caz.definitions.dto.ComplianceResultsDto;

@ExtendWith(MockitoExtension.class)
public class ChargeCalculationServiceTest {

  private final static String VRN1 = "VRN1";
  private final static UUID ACCOUNT_VEHICLE_1_ID = UUID.randomUUID();
  private final static String VRN2 = "VRN2";
  private final static UUID ACCOUNT_VEHICLE_2_ID = UUID.randomUUID();
  private final static String VRN3 = "VRN3";
  private final static UUID ACCOUNT_VEHICLE_3_ID = UUID.randomUUID();
  private final static String VRN4 = "VRN4";
  private final static UUID ACCOUNT_VEHICLE_4_ID = UUID.randomUUID();
  private final static String TARIFF_1 = "Tarrif 1";
  private final static String TARIFF_2 = "Tarrif 2";

  private final static UUID ACCOUNT_ID = UUID.randomUUID();
  private final static UUID CAZ_1_ID = UUID.randomUUID();
  private final static UUID CAZ_2_ID = UUID.randomUUID();
  private final static int BATCH_LIMIT_OF_VEHICLES_TO_PROCESS = 3;
  private final static int BATCH_ALL_VEHICLES_TO_PROCESS = 5;
  private final static int PROCESS_ALL_VEHICLES_WITHOUT_BATCHING = 0;
  private final static int CHARGEABILITY_CACHE_REFRESH_DAYS = 2;
  private final static int VCCS_BULK_SIZE = 2;

  @Mock
  private VehicleChargeabilityRepository vehicleChargeabilityRepository;

  @Mock
  private AccountVehicleRepository accountVehicleRepository;

  @Mock
  private VccsRepository vccsRepository;

  private ChargeCalculationService chargeCalculationService;

  @BeforeEach
  public void setup() {
    chargeCalculationService = new ChargeCalculationService(vehicleChargeabilityRepository,
        accountVehicleRepository, vccsRepository, VCCS_BULK_SIZE);
    mockClearAirZones();
  }

  @Nested
  class PopulateCache {

    @BeforeEach
    public void initAccountVehiclesToProcess() {
      mockAllAccountVehiclesToProcess();
    }

    @Test
    public void testWhenEverythingIsCorrectAndNotAllVehiclesCanBeFinishedInOneCalculation() {
      // given
      mockVccsBulkComplianceCheckWithChargeAndVehicleType(12.0f, "car");

      // when
      CachePopulationResult cachePopulationResult = chargeCalculationService
          .populateCache(ACCOUNT_ID, BATCH_LIMIT_OF_VEHICLES_TO_PROCESS);

      // then
      assertThatResultIs(cachePopulationResult,
          CachePopulationResult.PROCESSED_BATCH_BUT_STILL_NOT_FINISHED);
      assertThatCountOfDeletedCacheEntriesWas(3);
      assertThatCountOfVehicleTypesUpdatedWas(3);
      assertThatChargeabilityCacheWasUpdatedInTwoBatchesAndFinalBatchHadSize(2);
    }

    @Test
    public void testWhenEverythingIsCorrectAndAllVehiclesCanBeFinishedInOneCalculation() {
      // given
      mockVccsBulkComplianceCheckWithChargeAndVehicleType(12.0f, "car");

      // when
      CachePopulationResult cachePopulationResult = chargeCalculationService
          .populateCache(ACCOUNT_ID, BATCH_ALL_VEHICLES_TO_PROCESS);

      // then
      assertThatResultIs(cachePopulationResult, CachePopulationResult.ALL_RECORDS_CACHED);
      assertThatCountOfDeletedCacheEntriesWas(4);
      assertThatCountOfVehicleTypesUpdatedWas(4);
      assertThatChargeabilityCacheWasUpdatedInTwoBatchesAndFinalBatchHadSize(4);
    }

    @Test
    public void testWhenEverythingIsCorrectAndAndVccsReturnsVehicleWithNoTypeAndWithChargeAsZero() {
      // given
      mockVccsBulkComplianceCheckWithChargeAndVehicleType(0.0f, null);

      // when
      CachePopulationResult cachePopulationResult = chargeCalculationService
          .populateCache(ACCOUNT_ID, PROCESS_ALL_VEHICLES_WITHOUT_BATCHING);

      //then
      assertThatResultIs(cachePopulationResult, CachePopulationResult.ALL_RECORDS_CACHED);
      assertThatCountOfDeletedCacheEntriesWas(4);
      verify(accountVehicleRepository, never())
          .updateVehicleType(any(UUID.class), anyString());
      assertThatChargeabilityCacheWasUpdatedInTwoBatchesAndFinalBatchHadSize(4);
    }

    @Test
    public void testCachePopulateWhenVccsErrorsOnOneBulkCheck() {
      // given
      mockVccsBulkComplianceCheckToReturnErrorOnSecondBatch();

      // when
      CachePopulationResult cachePopulationResult = chargeCalculationService
          .populateCache(ACCOUNT_ID, PROCESS_ALL_VEHICLES_WITHOUT_BATCHING);

      assertThat(cachePopulationResult)
          .isEqualTo(CachePopulationResult.EXTERNAL_SERVICE_CALL_EXCEPTION);
    }

    @Test
    public void testWhenAllVehiclesHaveCachePopulated() {
      // given
      Mockito.reset(vehicleChargeabilityRepository);
      given(vehicleChargeabilityRepository
          .findAllForFleetThatDoNotHaveChargeabilityCacheInEachCaz(ACCOUNT_ID, 2))
          .willReturn(new VehiclesToCalculateChargeability(emptySet()));

      // when
      CachePopulationResult cachePopulationResult = chargeCalculationService
          .populateCache(ACCOUNT_ID, PROCESS_ALL_VEHICLES_WITHOUT_BATCHING);

      //then
      assertThatResultIs(cachePopulationResult, CachePopulationResult.ALL_RECORDS_CACHED);
      verify(vehicleChargeabilityRepository, never()).deleteFromVehicleChargeability(anySet());
      verify(accountVehicleRepository, never())
          .updateVehicleType(any(UUID.class), anyString());
      verify(accountVehicleRepository, never()).saveAll(anyIterable());
    }

    private void mockAllAccountVehiclesToProcess() {
      AccountVehicleBare vehicle1 = AccountVehicleBare.from(ACCOUNT_VEHICLE_1_ID.toString(), VRN1);
      AccountVehicleBare vehicle2 = AccountVehicleBare.from(ACCOUNT_VEHICLE_2_ID.toString(), VRN2);
      AccountVehicleBare vehicle3 = AccountVehicleBare.from(ACCOUNT_VEHICLE_3_ID.toString(), VRN3);
      AccountVehicleBare vehicle4 = AccountVehicleBare.from(ACCOUNT_VEHICLE_4_ID.toString(), VRN4);
      Set<AccountVehicleBare> setOfVehiclesToProcess = newHashSet(vehicle1, vehicle2, vehicle3,
          vehicle4);
      VehiclesToCalculateChargeability allVehiclesToProcess = new VehiclesToCalculateChargeability(
          setOfVehiclesToProcess);
      lenient().when(vehicleChargeabilityRepository
          .findAllForFleetThatDoNotHaveChargeabilityCacheInEachCaz(ACCOUNT_ID, 2))
          .thenReturn(allVehiclesToProcess);
    }
  }

  @Nested
  class PopulateCacheForSingleVehicle {

    @Test
    public void testWhenOnlySingleVehicleNeedsToHaveCachePopulated() {
      // given
      mockVccsBulkComplianceCheckWithChargeAndVehicleType(12.0f, "car");

      // when
      chargeCalculationService.populateCacheForSingleVehicle(ACCOUNT_VEHICLE_1_ID, VRN1);

      // then
      assertThatCountOfDeletedCacheEntriesWas(1);
      assertThatCountOfVehicleTypesUpdatedWas(1);
      assertThatChargeabilityCacheWasUpdatedInOneBatchAndThisBatchHadSize(2);
    }
  }

  @Nested
  class RefreshCache {

    @BeforeEach
    public void initAccountVehiclesToProcess() {
      mockAllVehiclesToProcess();
    }

    @Test
    public void testWhenEverythingIsCorrectAndNotAllVehiclesCanBeFinishedInOneCalculation() {
      // given
      mockVccsBulkComplianceCheckWithChargeAndVehicleType(12.0f, "car");

      // when
      CachePopulationResult cachePopulationResult = chargeCalculationService
          .refreshCache(BATCH_LIMIT_OF_VEHICLES_TO_PROCESS, CHARGEABILITY_CACHE_REFRESH_DAYS);

      // then
      assertThatResultIs(cachePopulationResult,
          CachePopulationResult.PROCESSED_BATCH_BUT_STILL_NOT_FINISHED);
      assertThatCountOfDeletedCacheEntriesWas(3);
      assertThatCountOfVehicleTypesUpdatedWas(3);
      assertThatChargeabilityCacheWasUpdatedInTwoBatchesAndFinalBatchHadSize(2);
    }

    @Test
    public void testWhenEverythingIsCorrectAndAllVehiclesCanBeFinishedInOneCalculation() {
      // given
      mockVccsBulkComplianceCheckWithChargeAndVehicleType(12.0f, "car");

      // when
      CachePopulationResult cachePopulationResult = chargeCalculationService
          .refreshCache(BATCH_ALL_VEHICLES_TO_PROCESS, CHARGEABILITY_CACHE_REFRESH_DAYS);

      // then
      assertThatResultIs(cachePopulationResult, CachePopulationResult.ALL_RECORDS_CACHED);
      assertThatCountOfDeletedCacheEntriesWas(4);
      assertThatCountOfVehicleTypesUpdatedWas(4);
      assertThatChargeabilityCacheWasUpdatedInTwoBatchesAndFinalBatchHadSize(4);
    }

    @Test
    public void testWhenEverythingIsCorrectAndAndVccsReturnsVehicleWithNoTypeAndWithChargeAsZero() {
      // given
      mockVccsBulkComplianceCheckWithChargeAndVehicleType(0.0f, null);

      // when
      CachePopulationResult cachePopulationResult = chargeCalculationService
          .refreshCache(PROCESS_ALL_VEHICLES_WITHOUT_BATCHING, CHARGEABILITY_CACHE_REFRESH_DAYS);

      //then
      assertThatResultIs(cachePopulationResult, CachePopulationResult.ALL_RECORDS_CACHED);
      assertThatCountOfDeletedCacheEntriesWas(4);
      verify(accountVehicleRepository, never())
          .updateVehicleType(any(UUID.class), anyString());
      assertThatChargeabilityCacheWasUpdatedInTwoBatchesAndFinalBatchHadSize(4);
    }

    @Test
    public void testCachePopulateWhenVccsErrorsOnOneBulkCheck() {
      // given
      mockVccsBulkComplianceCheckToReturnErrorOnSecondBatch();

      // when
      CachePopulationResult cachePopulationResult = chargeCalculationService
          .refreshCache(PROCESS_ALL_VEHICLES_WITHOUT_BATCHING, CHARGEABILITY_CACHE_REFRESH_DAYS);

      assertThat(cachePopulationResult)
          .isEqualTo(CachePopulationResult.EXTERNAL_SERVICE_CALL_EXCEPTION);
    }

    @Test
    public void testWhenAllVehiclesHaveNotExpiredCachePopulated() {
      // given
      Mockito.reset(vehicleChargeabilityRepository);
      given(vehicleChargeabilityRepository
          .findAllThatDoNotHaveChargeabilityCacheInEachCazOrExpired(anyInt(), any(Timestamp.class)))
          .willReturn(new VehiclesToCalculateChargeability(emptySet()));

      // when
      CachePopulationResult cachePopulationResult = chargeCalculationService
          .refreshCache(PROCESS_ALL_VEHICLES_WITHOUT_BATCHING, CHARGEABILITY_CACHE_REFRESH_DAYS);

      //then
      assertThatResultIs(cachePopulationResult, CachePopulationResult.ALL_RECORDS_CACHED);
      verify(vehicleChargeabilityRepository, never()).deleteFromVehicleChargeability(anySet());
      verify(accountVehicleRepository, never())
          .updateVehicleType(any(UUID.class), anyString());
      verify(accountVehicleRepository, never()).saveAll(anyIterable());
    }

    private void mockAllVehiclesToProcess() {
      AccountVehicleBare vehicle1 = AccountVehicleBare.from(ACCOUNT_VEHICLE_1_ID.toString(), VRN1);
      AccountVehicleBare vehicle2 = AccountVehicleBare.from(ACCOUNT_VEHICLE_2_ID.toString(), VRN2);
      AccountVehicleBare vehicle3 = AccountVehicleBare.from(ACCOUNT_VEHICLE_3_ID.toString(), VRN3);
      AccountVehicleBare vehicle4 = AccountVehicleBare.from(ACCOUNT_VEHICLE_4_ID.toString(), VRN4);
      Set<AccountVehicleBare> setOfVehiclesToProcess = newHashSet(vehicle1, vehicle2, vehicle3,
          vehicle4);
      VehiclesToCalculateChargeability allVehiclesToProcess = new VehiclesToCalculateChargeability(
          setOfVehiclesToProcess);
      lenient().when(vehicleChargeabilityRepository
          .findAllThatDoNotHaveChargeabilityCacheInEachCazOrExpired(anyInt(), any(Timestamp.class)))
          .thenReturn(allVehiclesToProcess);
    }
  }

  private void mockClearAirZones() {
    CleanAirZoneDto caz1 = CleanAirZoneDto.builder()
        .cleanAirZoneId(CAZ_1_ID)
        .build();
    CleanAirZoneDto caz2 = CleanAirZoneDto.builder()
        .cleanAirZoneId(CAZ_2_ID)
        .build();
    CleanAirZonesDto cleanAirZonesDto = CleanAirZonesDto.builder()
        .cleanAirZones(newArrayList(caz1, caz2))
        .build();
    Response<CleanAirZonesDto> cazResponse = Response.success(cleanAirZonesDto);
    lenient().when(vccsRepository.findCleanAirZonesSync()).thenReturn(cazResponse);
  }

  private void mockVccsBulkComplianceCheckWithChargeAndVehicleType(float charge,
      String vehicleType) {
    given(vccsRepository.findComplianceInBulkSync(anySet())).willAnswer(
        (Answer<Response>) invocationOnMock -> {
          Set<String> vrns = invocationOnMock.getArgument(0);
          List<ComplianceResultsDto> compliance = newArrayList();
          for (String vrn : vrns) {
            ComplianceOutcomeDto firstVrnFirstCazCompliance = ComplianceOutcomeDto.builder()
                .charge(10.0f)
                .cleanAirZoneId(CAZ_1_ID)
                .tariffCode(TARIFF_1)
                .build();
            ComplianceOutcomeDto firstVrnSecondCazCompliance = ComplianceOutcomeDto.builder()
                .charge(charge)
                .cleanAirZoneId(CAZ_2_ID)
                .tariffCode(TARIFF_2)
                .build();

            ComplianceResultsDto firstVrnCompliance = ComplianceResultsDto.builder()
                .vehicleType(vehicleType)
                .registrationNumber(vrn)
                .isRetrofitted(false)
                .isExempt(false)
                .complianceOutcomes(
                    newArrayList(firstVrnFirstCazCompliance, firstVrnSecondCazCompliance))
                .build();
            compliance.add(firstVrnCompliance);
          }

          return Response.success(compliance);
        });
  }

  private void mockVccsBulkComplianceCheckToReturnErrorOnSecondBatch() {
    AtomicInteger count = new AtomicInteger();
    given(vccsRepository.findComplianceInBulkSync(anySet())).willAnswer(
        (Answer<Response>) invocationOnMock -> {
          count.getAndIncrement();
          if (count.get() == 1) {
            Set<String> vrns = invocationOnMock.getArgument(0);
            List<ComplianceResultsDto> compliance = newArrayList();
            for (String vrn : vrns) {
              ComplianceOutcomeDto firstVrnFirstCazCompliance = ComplianceOutcomeDto.builder()
                  .charge(10.0f)
                  .cleanAirZoneId(CAZ_1_ID)
                  .tariffCode(TARIFF_1)
                  .build();
              ComplianceOutcomeDto firstVrnSecondCazCompliance = ComplianceOutcomeDto.builder()
                  .charge(12.0f)
                  .cleanAirZoneId(CAZ_2_ID)
                  .tariffCode(TARIFF_2)
                  .build();

              ComplianceResultsDto firstVrnCompliance = ComplianceResultsDto.builder()
                  .vehicleType("Vehicle-" + vrn)
                  .registrationNumber(vrn)
                  .isRetrofitted(false)
                  .isExempt(false)
                  .complianceOutcomes(
                      newArrayList(firstVrnFirstCazCompliance, firstVrnSecondCazCompliance))
                  .build();
              compliance.add(firstVrnCompliance);
            }

            return Response.success(compliance);
          }
          return Response.error(503, ResponseBody.create(MediaType.parse("text/plain"), "Error"));
        });
  }

  private void assertThatChargeabilityCacheWasUpdatedInTwoBatchesAndFinalBatchHadSize(
      int expectedBatchSize) {
    ArgumentCaptor<List> cacheCalculations = ArgumentCaptor.forClass(List.class);
    verify(vehicleChargeabilityRepository, times(2)).saveAll(cacheCalculations.capture());
    assertThat(cacheCalculations.getAllValues()).hasSize(2);
    assertThat(cacheCalculations.getAllValues().get(0)).hasSize(4);
    assertThat(cacheCalculations.getAllValues().get(1)).hasSize(expectedBatchSize);
  }

  private void assertThatChargeabilityCacheWasUpdatedInOneBatchAndThisBatchHadSize(
      int expectedBatchSize) {
    ArgumentCaptor<List> cacheCalculations = ArgumentCaptor.forClass(List.class);
    verify(vehicleChargeabilityRepository, times(1)).saveAll(cacheCalculations.capture());
    assertThat(cacheCalculations.getValue()).hasSize(expectedBatchSize);
  }

  private void assertThatCountOfVehicleTypesUpdatedWas(int expectedCount) {
    verify(accountVehicleRepository, times(expectedCount))
        .updateVehicleType(any(UUID.class), anyString());
  }

  private void assertThatResultIs(CachePopulationResult cachePopulationResult,
      CachePopulationResult processedBatchButStillNotFinished) {
    assertThat(cachePopulationResult)
        .isEqualTo(processedBatchButStillNotFinished);
  }

  private void assertThatCountOfDeletedCacheEntriesWas(int expectedCount) {
    ArgumentCaptor<Set<UUID>> subsetOfVehiclesCaptor = ArgumentCaptor.forClass(Set.class);
    verify(vehicleChargeabilityRepository)
        .deleteFromVehicleChargeability(subsetOfVehiclesCaptor.capture());
    assertThat(subsetOfVehiclesCaptor.getValue()).hasSize(expectedCount);
  }
}