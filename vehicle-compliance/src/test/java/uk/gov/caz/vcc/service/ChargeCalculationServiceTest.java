package uk.gov.caz.vcc.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import javassist.NotFoundException;
import org.apache.commons.lang3.RandomStringUtils;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.OngoingStubbing;
import uk.gov.caz.definitions.domain.Vehicle;
import uk.gov.caz.definitions.domain.VehicleType;
import uk.gov.caz.definitions.dto.CleanAirZoneDto;
import uk.gov.caz.definitions.dto.CleanAirZonesDto;
import uk.gov.caz.definitions.dto.ComplianceOutcomeDto;
import uk.gov.caz.definitions.dto.ComplianceResultsDto;
import uk.gov.caz.vcc.domain.CalculationResult;
import uk.gov.caz.vcc.domain.TariffDetails;
import uk.gov.caz.vcc.domain.VehicleTypeCharge;
import uk.gov.caz.vcc.domain.exceptions.ExternalResourceNotFoundException;
import uk.gov.caz.vcc.domain.service.ChargeabilityService;
import uk.gov.caz.vcc.domain.service.ComplianceService;
import uk.gov.caz.vcc.domain.service.VehicleIdentificationService;
import uk.gov.caz.vcc.dto.TaxiPhvLicenseInformationResponse;
import uk.gov.caz.vcc.repository.TariffDetailsRepository;
import uk.gov.caz.vcc.repository.VehicleDetailsRepository;

@ExtendWith(MockitoExtension.class)
public class ChargeCalculationServiceTest {

  @Mock
  private VehicleIdentificationService identificationService;

  @Mock
  private VehicleService vehicleService;

  @Mock
  private ChargeabilityService chargeabilityService;

  @Mock
  private ExemptionService exemptionService;

  @Mock
  private ComplianceService complianceService;

  @Mock
  private CazTariffService cazTariffService;

  @Mock
  private TariffDetailsRepository tariffDetailsRepository;

  @Mock
  private VehicleDetailsRepository vehicleDetailsRepository;

  @Mock
  private NationalTaxiRegisterService nationalTaxiRegisterService;

  @Mock
  private RetrofitService retrofitService;

  @Mock
  private MilitaryVehicleService militaryVehicleService;

  @Mock
  private GeneralWhitelistService generalWhitelistService;

  @Mock
  private LicenseAndVehicleProvider licenseAndVehicleProvider;

  @InjectMocks
  private ChargeCalculationService chargeCalculationService;

  private ArrayList<UUID> cleanAirZoneIds;
  private CalculationResult result;
  private Vehicle vehicle;

  @BeforeEach
  public void init() {
    cleanAirZoneIds = new ArrayList<>();

    UUID birminghamIdentifier = UUID.randomUUID();

    cleanAirZoneIds.add(birminghamIdentifier);

    result = new CalculationResult();
    vehicle = new Vehicle();
    vehicle.setRegistrationNumber("TEST");
  }

  // ==================== //
  // Begin helper methods //
  // ==================== //

  private OngoingStubbing<Optional<Vehicle>> stubVehicleRepository() {
    return when(vehicleDetailsRepository
        .findByRegistrationNumber(vehicle.getRegistrationNumber()))
        .thenReturn(Optional.of(vehicle));
  }

  private OngoingStubbing<CalculationResult> stubExemptions(boolean isExempt) {
    return when(exemptionService.updateCalculationResult(
        Mockito.any(Vehicle.class), Mockito.any(CalculationResult.class)))
        .thenReturn(new CalculationResult() {
          {
            setExempt(isExempt);
          }
        });
  }

  private void stubIdentification(VehicleType type) {
    doAnswer((v) -> {
      ((Vehicle) v.getArgument(0)).setVehicleType(type);
      return null;
    }).when(identificationService).setVehicleType(ArgumentMatchers.eq(vehicle));
  }

  private OngoingStubbing<Optional<TaxiPhvLicenseInformationResponse>> stubTaxiRegister(
      boolean isTaxiOrPhv) {
    if (isTaxiOrPhv) {
      return when(nationalTaxiRegisterService
          .getLicenseInformation(vehicle.getRegistrationNumber()))
          .thenReturn(Optional.of(TaxiPhvLicenseInformationResponse.builder()
              .active(true)
              .licensedStatusExpires(LocalDate.now().plusDays(1))
              .wheelchairAccessible(false).build()));

    } else {
      return when(nationalTaxiRegisterService
          .getLicenseInformation(vehicle.getRegistrationNumber()))
          .thenReturn(Optional.empty());
    }
  }

  private OngoingStubbing<Optional<TaxiPhvLicenseInformationResponse>> stubActiveExpiredTaxiRegister() {
    return when(nationalTaxiRegisterService
        .getLicenseInformation(vehicle.getRegistrationNumber()))
        .thenReturn(Optional.of(TaxiPhvLicenseInformationResponse.builder()
            .active(true)
            .licensedStatusExpires(LocalDate.now().minusDays(2))
            .wheelchairAccessible(false).build()));
  }

  private void stubTariffRepository(String operatorName) {
    TariffDetails tariffDetails = new TariffDetails();
    UUID cazId = cleanAirZoneIds.get(0);
    tariffDetails.setCazId(cazId);
    tariffDetails.setName("Test CAZ");
    tariffDetails.setChargesMotorcycles(false);
    tariffDetails.setRates(
        new ArrayList<VehicleTypeCharge>(Arrays.asList(new VehicleTypeCharge() {
          /**
           *
           */
          private static final long serialVersionUID = 1L;

          {
            setVehicleType(VehicleType.PRIVATE_CAR);
            setCharge((float) 3.142);
          }
        })));

    when(
        tariffDetailsRepository.getTariffDetails(Mockito.any(UUID.class)))
        .thenReturn(Optional.of(tariffDetails));

    CleanAirZoneDto zone = CleanAirZoneDto.builder()
        .cleanAirZoneId(cazId)
        .operatorName(operatorName)
        .build();
    CleanAirZonesDto cazDto = CleanAirZonesDto.builder().cleanAirZones(
        Lists.newArrayList(zone)).build();
    when(cazTariffService.getCleanAirZoneSelectionListings())
        .thenReturn(cazDto);
  }

  private OngoingStubbing<CalculationResult> stubComplianceCheck(
      boolean compliant) {

    result.setCompliant(compliant);

    return when(complianceService.updateCalculationResult(
        ArgumentMatchers.any(Vehicle.class),
        ArgumentMatchers.any(CalculationResult.class))).thenReturn(result);
  }

  private OngoingStubbing<Float> stubChargeabilityService(float charge) {
    return when(
        chargeabilityService.getCharge(ArgumentMatchers.any(Vehicle.class),
            ArgumentMatchers.any(TariffDetails.class))).thenReturn(charge);
  }

  // ================== //
  // End helper methods //
  // ================== //

  @Test
  void vehicleRepositoryCalled() throws NotFoundException {
    stubVehicleRepository();
    stubExemptions(true);

    chargeCalculationService.checkVrnAgainstCaz(vehicle.getRegistrationNumber(),
        cleanAirZoneIds);

    verify(vehicleDetailsRepository, times(1))
        .findByRegistrationNumber(vehicle.getRegistrationNumber());
  }

  @Test
  void exceptionThrownIfVehicleNotFound() throws NotFoundException {
    when(vehicleDetailsRepository
        .findByRegistrationNumber(vehicle.getRegistrationNumber()))
        .thenReturn(Optional.empty());

    assertThrows(ExternalResourceNotFoundException.class, () -> {
      chargeCalculationService.checkVrnAgainstCaz(
          vehicle.getRegistrationNumber(), cleanAirZoneIds);
    });
  }

  @Test
  void identificationServiceCalled() throws NotFoundException {
    stubVehicleRepository();
    stubExemptions(true);
    stubIdentification(VehicleType.PRIVATE_CAR);

    chargeCalculationService.checkVrnAgainstCaz(vehicle.getRegistrationNumber(),
        cleanAirZoneIds);

    verify(identificationService, times(1)).setVehicleType(vehicle);
  }

  @Test
  void returnCompliantForRetrofitted() throws NotFoundException {
    stubVehicleRepository();
    stubExemptions(false);
    stubTariffRepository(RandomStringUtils.randomAlphabetic(5));
    stubComplianceCheck(true);
    Mockito.when(retrofitService.isRetrofitted(Mockito.anyString()))
        .thenReturn(true);
    stubIdentification(VehicleType.PRIVATE_CAR);

    ComplianceResultsDto complianceDto = chargeCalculationService
        .checkVrnAgainstCaz(vehicle.getRegistrationNumber(), cleanAirZoneIds);

    verify(nationalTaxiRegisterService, times(1))
        .getLicenseInformation(Mockito.any());
    assertTrue(complianceDto.getIsRetrofitted());
    assertEquals(complianceDto.getComplianceOutcomes().get(0).getCharge(), 0.0,
        0);
  }

  @Test
  void returnEarlyForExempt() throws NotFoundException {
    stubVehicleRepository();
    stubExemptions(true);
    stubIdentification(VehicleType.PRIVATE_CAR);

    ComplianceResultsDto complianceDto = chargeCalculationService
        .checkVrnAgainstCaz(vehicle.getRegistrationNumber(), cleanAirZoneIds);

    assertEquals(new ArrayList<>(), complianceDto.getComplianceOutcomes());
    verify(nationalTaxiRegisterService, times(0))
        .getLicenseInformation(Mockito.any());
  }

  @Test
  void retrofittedTrueIfOnWhitelist() throws NotFoundException {
    stubVehicleRepository();
    stubExemptions(true);
    Mockito.when(retrofitService.isRetrofitted(Mockito.anyString()))
        .thenReturn(true);
    stubIdentification(VehicleType.PRIVATE_CAR);

    ComplianceResultsDto complianceDto = chargeCalculationService
        .checkVrnAgainstCaz(vehicle.getRegistrationNumber(), cleanAirZoneIds);

    assertTrue(complianceDto.getIsRetrofitted());
    assertTrue(complianceDto.getIsExempt());
  }

  @Test
  void retrofittedFalseIfNotOnWhitelist() throws NotFoundException {
    stubVehicleRepository();
    stubExemptions(true);
    stubIdentification(VehicleType.PRIVATE_CAR);

    ComplianceResultsDto complianceDto = chargeCalculationService
        .checkVrnAgainstCaz(vehicle.getRegistrationNumber(), cleanAirZoneIds);

    assertFalse(complianceDto.getIsRetrofitted());
  }

  @Test
  void exemptTrueIfExempt() throws NotFoundException {
    stubVehicleRepository();
    stubExemptions(true);
    stubIdentification(VehicleType.PRIVATE_CAR);

    ComplianceResultsDto complianceDto = chargeCalculationService
        .checkVrnAgainstCaz(vehicle.getRegistrationNumber(), cleanAirZoneIds);

    assertFalse(complianceDto.getIsRetrofitted());
    assertTrue(complianceDto.getIsExempt());
  }

  @Test
  void exemptFalseIfNotExempt() throws NotFoundException {
    stubVehicleRepository();
    stubExemptions(false);
    stubIdentification(VehicleType.PRIVATE_CAR);
    stubTaxiRegister(false);
    stubTariffRepository(RandomStringUtils.randomAlphabetic(5));
    stubComplianceCheck(false);
    stubChargeabilityService((float) 3.142);

    ComplianceResultsDto complianceDto = chargeCalculationService
        .checkVrnAgainstCaz(vehicle.getRegistrationNumber(), cleanAirZoneIds);

    assertFalse(complianceDto.getIsRetrofitted());
    assertFalse(complianceDto.getIsExempt());
  }

  @Test
  void taxiRegisterCalledIfNotExempt() throws NotFoundException {
    stubVehicleRepository();
    stubExemptions(false);
    stubIdentification(VehicleType.PRIVATE_CAR);
    stubTaxiRegister(true);
    stubTariffRepository(RandomStringUtils.randomAlphabetic(5));
    stubComplianceCheck(true);

    chargeCalculationService.checkVrnAgainstCaz(vehicle.getRegistrationNumber(),
        cleanAirZoneIds);

    verify(nationalTaxiRegisterService, times(1))
        .getLicenseInformation(vehicle.getRegistrationNumber());
  }

  @Test
  void vehicleTypeTaxiIfOnTaxiRegister() throws NotFoundException {
    stubVehicleRepository();
    stubExemptions(false);
    stubIdentification(VehicleType.PRIVATE_CAR);
    stubTaxiRegister(true);
    stubTariffRepository(RandomStringUtils.randomAlphabetic(5));
    stubComplianceCheck(false);
    stubChargeabilityService((float) 3.142);

    chargeCalculationService.checkVrnAgainstCaz(vehicle.getRegistrationNumber(),
        cleanAirZoneIds);

    assertEquals(VehicleType.TAXI_OR_PHV, vehicle.getVehicleType());
    assertTrue(vehicle.getIsTaxiOrPhv());
  }

  @Test
  void vehicleTypeTaxiIfOnExpiredLicenseTaxiRegister() throws NotFoundException {
    stubVehicleRepository();
    stubExemptions(false);
    stubIdentification(VehicleType.PRIVATE_CAR);
    stubActiveExpiredTaxiRegister();
    stubTariffRepository(RandomStringUtils.randomAlphabetic(5));
    stubComplianceCheck(false);
    stubChargeabilityService((float) 3.142);

    chargeCalculationService.checkVrnAgainstCaz(vehicle.getRegistrationNumber(),
        cleanAirZoneIds);

    assertEquals(VehicleType.PRIVATE_CAR, vehicle.getVehicleType());
    assertFalse(vehicle.getIsTaxiOrPhv());
  }

  @Test
  void unchangedVehicleTypeIfNotOnTaxiRegister() throws NotFoundException {
    stubVehicleRepository();
    stubExemptions(false);
    stubIdentification(VehicleType.PRIVATE_CAR);
    stubTaxiRegister(false);
    stubTariffRepository(RandomStringUtils.randomAlphabetic(5));
    stubComplianceCheck(true);

    chargeCalculationService.checkVrnAgainstCaz(vehicle.getRegistrationNumber(),
        cleanAirZoneIds);

    assertEquals(VehicleType.PRIVATE_CAR, vehicle.getVehicleType());
    assertFalse(vehicle.getIsTaxiOrPhv());
  }

  @Test
  void oneComplianceOutcomePerCazId() throws NotFoundException {
    stubVehicleRepository();
    stubExemptions(false);
    stubIdentification(VehicleType.PRIVATE_CAR);
    stubTaxiRegister(false);
    stubTariffRepository(RandomStringUtils.randomAlphabetic(5));
    stubComplianceCheck(true);

    ComplianceResultsDto firstComplianceDto = chargeCalculationService
        .checkVrnAgainstCaz(vehicle.getRegistrationNumber(), cleanAirZoneIds);

    assertEquals(1, firstComplianceDto.getComplianceOutcomes().size());

    // Repeat test with a second CAZ
    cleanAirZoneIds.add(UUID.randomUUID());

    ComplianceResultsDto secondComplianceDto = chargeCalculationService
        .checkVrnAgainstCaz(vehicle.getRegistrationNumber(), cleanAirZoneIds);

    assertEquals(2, secondComplianceDto.getComplianceOutcomes().size());
  }

  @Test
  void canYieldChargeabilityOutcomeWithVehicleType() throws NotFoundException {
    stubVehicleRepository();
    stubExemptions(false);
    stubIdentification(VehicleType.PRIVATE_CAR);
    stubTaxiRegister(false);
    stubTariffRepository(RandomStringUtils.randomAlphabetic(5));
    stubComplianceCheck(true);

    ComplianceResultsDto complianceDto = chargeCalculationService
        .checkVrnAgainstCaz(vehicle.getRegistrationNumber(), cleanAirZoneIds);

    assertEquals(complianceDto.getVehicleType(),
        VehicleType.PRIVATE_CAR.toString());
  }

  @Test
  void canYieldChargeabilityOutcomeWithoutVehicleType()
      throws NotFoundException {
    stubVehicleRepository();
    stubExemptions(false);
    stubTaxiRegister(false);
    stubTariffRepository(RandomStringUtils.randomAlphabetic(5));
    stubComplianceCheck(true);

    ComplianceResultsDto complianceDto = chargeCalculationService
        .checkVrnAgainstCaz(vehicle.getRegistrationNumber(), cleanAirZoneIds);

    assertNull(complianceDto.getVehicleType());
  }

  @Test
  void tariffRepositoryCalled() throws NotFoundException {
    stubVehicleRepository();
    stubExemptions(false);
    stubIdentification(VehicleType.PRIVATE_CAR);
    stubTaxiRegister(false);
    stubTariffRepository(RandomStringUtils.randomAlphabetic(5));
    stubComplianceCheck(true);

    chargeCalculationService.checkVrnAgainstCaz(vehicle.getRegistrationNumber(),
        cleanAirZoneIds);

    verify(tariffDetailsRepository, times(1))
        .getTariffDetails(Mockito.any(UUID.class));
  }

  @Test
  void exceptionThrownIfTariffNotFound() throws NotFoundException {
    stubVehicleRepository();
    stubExemptions(false);
    stubIdentification(VehicleType.PRIVATE_CAR);
    stubTaxiRegister(false);

    // N.B. - getTariffDetails throws an checked NotFound Exception, but is
    // not declared in the method signature. This means Mockito cannot throw
    // that Exception.
    // Instead return an Optional for now, which the chargeCalculationService
    // throws.
    when(tariffDetailsRepository.getTariffDetails(Mockito.any(UUID.class)))
        .thenReturn(Optional.empty());

    assertThrows(ExternalResourceNotFoundException.class, () -> {
      chargeCalculationService.checkVrnAgainstCaz(
          vehicle.getRegistrationNumber(), cleanAirZoneIds);
    });
  }

  @Test
  void findsCorrectChargeFromvehicleChargesList() throws NotFoundException {
    stubVehicleRepository();
    stubExemptions(false);
    stubIdentification(VehicleType.PRIVATE_CAR);
    stubTaxiRegister(false);
    stubTariffRepository(RandomStringUtils.randomAlphabetic(5));
    stubComplianceCheck(false);
    stubChargeabilityService((float) 3.142);

    ComplianceResultsDto complianceDto = chargeCalculationService
        .checkVrnAgainstCaz(vehicle.getRegistrationNumber(), cleanAirZoneIds);

    assertEquals((float) 3.142,
        complianceDto.getComplianceOutcomes().get(0).getCharge(), 0.001);
  }

  @Test
  void checkForChargeableIfNotComliant() throws NotFoundException {
    stubVehicleRepository();
    stubExemptions(false);
    stubIdentification(VehicleType.PRIVATE_CAR);
    stubTaxiRegister(false);
    stubTariffRepository(RandomStringUtils.randomAlphabetic(5));
    stubComplianceCheck(false);

    chargeCalculationService.checkVrnAgainstCaz(vehicle.getRegistrationNumber(),
        cleanAirZoneIds);

    verify(chargeabilityService, times(1)).getCharge(
        ArgumentMatchers.eq(vehicle),
        ArgumentMatchers.any(TariffDetails.class));
  }

  @Test
  void zeroChargeIfNotChargeable() throws NotFoundException {
    stubVehicleRepository();
    stubExemptions(false);
    stubIdentification(VehicleType.PRIVATE_CAR);
    stubTaxiRegister(false);
    stubTariffRepository(RandomStringUtils.randomAlphabetic(5));
    stubComplianceCheck(false);
    stubChargeabilityService((float) 0);

    ComplianceResultsDto complianceDto = chargeCalculationService
        .checkVrnAgainstCaz(vehicle.getRegistrationNumber(), cleanAirZoneIds);

    for (ComplianceOutcomeDto result : complianceDto.getComplianceOutcomes()) {
      assertEquals((float) 0.0, result.getCharge(), 0.001);
      assertNotNull(result.getTariffCode());
    }
  }

  @Test
  void zeroChargeIfCompliant() throws NotFoundException {
    stubVehicleRepository();
    stubExemptions(false);
    stubIdentification(VehicleType.PRIVATE_CAR);
    stubTaxiRegister(false);
    stubTariffRepository(RandomStringUtils.randomAlphabetic(5));
    stubComplianceCheck(true);

    ComplianceResultsDto complianceDto = chargeCalculationService
        .checkVrnAgainstCaz(vehicle.getRegistrationNumber(), cleanAirZoneIds);

    for (ComplianceOutcomeDto result : complianceDto.getComplianceOutcomes()) {
      assertEquals((float) 0.0, result.getCharge(), 0.001);
      assertNull(result.getTariffCode());
    }
  }

  @Test
  void applyChargeIfChargeableAndNonCompliant() throws NotFoundException {
    stubVehicleRepository();
    stubExemptions(false);
    stubIdentification(VehicleType.PRIVATE_CAR);
    stubTaxiRegister(false);
    stubTariffRepository(RandomStringUtils.randomAlphabetic(5));
    stubComplianceCheck(false);
    stubChargeabilityService((float) 3.142);

    ComplianceResultsDto complianceDto = chargeCalculationService
        .checkVrnAgainstCaz(vehicle.getRegistrationNumber(), cleanAirZoneIds);

    for (ComplianceOutcomeDto result : complianceDto.getComplianceOutcomes()) {
      assertEquals((float) 3.142, result.getCharge(), 0.001);
      assertNotNull(result.getTariffCode());
    }
  }

  @Test
  void taxiOrPhvTrueIfParamTrueEvenThoughVrnNotInNtr() {
    stubVehicleRepository();
    stubExemptions(false);
    stubIdentification(VehicleType.PRIVATE_CAR);
    stubTaxiRegister(false);
    stubTariffRepository(RandomStringUtils.randomAlphabetic(5));
    stubComplianceCheck(false);
    stubChargeabilityService((float) 3.142);

    ComplianceResultsDto complianceDto = chargeCalculationService
        .checkVrnAgainstCaz(vehicle.getRegistrationNumber(), cleanAirZoneIds);

    for (ComplianceOutcomeDto result : complianceDto.getComplianceOutcomes()) {
      assertEquals((float) 3.142, result.getCharge(), 0.001);
      assertNotNull(result.getTariffCode());
    }
  }

  @Test
  void shouldReturnOperatorName() {
    String operatorName = RandomStringUtils.randomAlphabetic(10);
    stubVehicleRepository();
    stubExemptions(false);
    stubIdentification(VehicleType.PRIVATE_CAR);
    stubTaxiRegister(false);
    stubTariffRepository(operatorName);
    stubComplianceCheck(false);
    stubChargeabilityService((float) 3.142);

    ComplianceResultsDto complianceDto = chargeCalculationService
        .checkVrnAgainstCaz(vehicle.getRegistrationNumber(), cleanAirZoneIds);

    String actualOperatorName = complianceDto.getComplianceOutcomes().stream().findFirst().get()
        .getOperatorName();
    assertThat(actualOperatorName).isEqualTo(operatorName);
  }

  @Nested
  class PhgvDiscountAvailability {

    @Nested
    class WhenVehicleTypeIsEligibleForDiscount {

      @Nested
      class AndMatchesPhgvTaxClass {

        @Nested
        class AndBodyTypeIsEqualToLivestockCarrier {

          @Test
          public void shouldReturnTrueWhenHgv() {
            stubPhgvDiscountAvailabilityRelatedVehicleData("private hgv",
                "livestock carrier", VehicleType.HGV);

            ComplianceResultsDto complianceDto = chargeCalculationService
                .checkVrnAgainstCaz(vehicle.getRegistrationNumber(),
                    cleanAirZoneIds);

            assertThat(complianceDto.isPhgvDiscountAvailable()).isTrue();
          }

          @Test
          public void shouldReturnTrueWhenBus() {
            stubPhgvDiscountAvailabilityRelatedVehicleData("private hgv",
                "livestock carrier", VehicleType.BUS);

            ComplianceResultsDto complianceDto = chargeCalculationService
                .checkVrnAgainstCaz(vehicle.getRegistrationNumber(),
                    cleanAirZoneIds);

            assertThat(complianceDto.isPhgvDiscountAvailable()).isTrue();
          }
        }

        @Nested
        class AndBodyTypeIsEqualToMotorHomeCaravan {

          @Test
          public void shouldReturnTrueWhenHgv() {
            stubPhgvDiscountAvailabilityRelatedVehicleData("private hgv",
                "motor home/caravan", VehicleType.HGV);

            ComplianceResultsDto complianceDto = chargeCalculationService
                .checkVrnAgainstCaz(vehicle.getRegistrationNumber(),
                    cleanAirZoneIds);

            assertThat(complianceDto.isPhgvDiscountAvailable()).isTrue();
          }

          @Test
          public void shouldReturnTrueWhenBus() {
            stubPhgvDiscountAvailabilityRelatedVehicleData("private hgv",
                "motor home/caravan", VehicleType.BUS);

            ComplianceResultsDto complianceDto = chargeCalculationService
                .checkVrnAgainstCaz(vehicle.getRegistrationNumber(),
                    cleanAirZoneIds);

            assertThat(complianceDto.isPhgvDiscountAvailable()).isTrue();
          }
        }

        class AndBodyTypeIsNotEqualToLivestockCarrierOrMotorhomeCaravan {

          @Test
          public void shouldReturnFalse() {
            stubPhgvDiscountAvailabilityRelatedVehicleData("private hgv",
                "non-applicable-body-type", VehicleType.HGV);

            ComplianceResultsDto complianceDto = chargeCalculationService
                .checkVrnAgainstCaz(vehicle.getRegistrationNumber(),
                    cleanAirZoneIds);

            assertThat(complianceDto.isPhgvDiscountAvailable()).isFalse();
          }
        }

      }

      class AndDoesntMatchPhgvTaxClass {

        @Test
        public void shouldReturnFalseForLivestockCarrier() {
          stubPhgvDiscountAvailabilityRelatedVehicleData("public hgv", "livestock carrier",
              VehicleType.HGV);

          ComplianceResultsDto complianceDto = chargeCalculationService.checkVrnAgainstCaz(
              vehicle.getRegistrationNumber(),
              cleanAirZoneIds
          );

          assertThat(complianceDto.isPhgvDiscountAvailable()).isFalse();
        }

        @Test
        public void shouldReturnFalseForMotorhomeOrCaravan() {
          stubPhgvDiscountAvailabilityRelatedVehicleData("public hgv", "motor home/caravan",
              VehicleType.HGV);

          ComplianceResultsDto complianceDto = chargeCalculationService.checkVrnAgainstCaz(
              vehicle.getRegistrationNumber(),
              cleanAirZoneIds
          );

          assertThat(complianceDto.isPhgvDiscountAvailable()).isFalse();
        }
      }

    }

    @Nested
    class WhenVehicleTypeIsNotEligibleForDiscount {

      @Nested
      class AndBodyTypeIsEqualToLivestockCarrier {

        @Test
        public void shouldReturnFalseWhenPrivateCar() {
          stubPhgvDiscountAvailabilityRelatedVehicleData("private hgv",
              "livestock carrier", VehicleType.PRIVATE_CAR);

          ComplianceResultsDto complianceDto = chargeCalculationService
              .checkVrnAgainstCaz(vehicle.getRegistrationNumber(),
                  cleanAirZoneIds);

          assertThat(complianceDto.isPhgvDiscountAvailable()).isFalse();
        }

        @Test
        public void shouldReturnFalseWhenVan() {
          stubPhgvDiscountAvailabilityRelatedVehicleData("private hgv",
              "livestock carrier", VehicleType.VAN);

          ComplianceResultsDto complianceDto = chargeCalculationService
              .checkVrnAgainstCaz(vehicle.getRegistrationNumber(),
                  cleanAirZoneIds);

          assertThat(complianceDto.isPhgvDiscountAvailable()).isFalse();
        }

        @Test
        public void shouldReturnFalseWhenAgricultural() {
          stubPhgvDiscountAvailabilityRelatedVehicleData("private hgv",
              "livestock carrier", VehicleType.AGRICULTURAL);

          ComplianceResultsDto complianceDto = chargeCalculationService
              .checkVrnAgainstCaz(vehicle.getRegistrationNumber(),
                  cleanAirZoneIds);

          assertThat(complianceDto.isPhgvDiscountAvailable()).isFalse();
        }
      }

      @Nested
      class AndBodyTypeIsEqualToMotorHomeCaravan {

        @Test
        public void shouldReturnFalse() {
          stubPhgvDiscountAvailabilityRelatedVehicleData("public hgv",
              "motor home/caravan", VehicleType.PRIVATE_CAR);

          ComplianceResultsDto complianceDto = chargeCalculationService
              .checkVrnAgainstCaz(vehicle.getRegistrationNumber(),
                  cleanAirZoneIds);

          assertThat(complianceDto.isPhgvDiscountAvailable()).isFalse();
        }
      }

    }

    private void stubPhgvDiscountAvailabilityRelatedVehicleData(
        String private_hgv, String bodyType, VehicleType vehicleType) {
      stubVehicleRepository();
      stubVehicleTaxClassAndBodyTypeTo(private_hgv, bodyType);
      stubExemptions(false);
      stubIdentification(vehicleType);
      stubTaxiRegister(false);
      stubTariffRepository(RandomStringUtils.randomAlphabetic(5));
      stubComplianceCheck(false);
      stubChargeabilityService((float) 3.142);
    }

    private void stubVehicleTaxClassAndBodyTypeTo(String taxClass,
        String bodyType) {
      vehicle.setTaxClass(taxClass);
      vehicle.setBodyType(bodyType);
    }
  }
}
