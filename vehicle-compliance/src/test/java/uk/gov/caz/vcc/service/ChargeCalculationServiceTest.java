package uk.gov.caz.vcc.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.OngoingStubbing;

import javassist.NotFoundException;
import uk.gov.caz.vcc.domain.exceptions.ExternalResourceNotFoundException;
import uk.gov.caz.vcc.domain.CalculationResult;
import uk.gov.caz.vcc.domain.TariffDetails;
import uk.gov.caz.vcc.domain.Vehicle;
import uk.gov.caz.vcc.domain.VehicleType;
import uk.gov.caz.vcc.domain.VehicleTypeCharge;
import uk.gov.caz.vcc.domain.service.ChargeabilityService;
import uk.gov.caz.vcc.domain.service.ComplianceService;
import uk.gov.caz.vcc.domain.service.VehicleIdentificationService;
import uk.gov.caz.vcc.dto.ComplianceOutcomeDto;
import uk.gov.caz.vcc.dto.ComplianceResultsDto;
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
              .thenReturn(Optional.of(TaxiPhvLicenseInformationResponse.builder().active(true)
                  .wheelchairAccessible(false).build()));

    } else {
      return when(nationalTaxiRegisterService
          .getLicenseInformation(vehicle.getRegistrationNumber()))
              .thenReturn(Optional.empty());
    }
  }

  private OngoingStubbing<Optional<TariffDetails>> stubTariffRepository() {
    TariffDetails tariffDetails = new TariffDetails();

    tariffDetails.setCazId(UUID.randomUUID());
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

    return when(
        tariffDetailsRepository.getTariffDetails(Mockito.any(UUID.class)))
            .thenReturn(Optional.of(tariffDetails));
  }

  private OngoingStubbing<CalculationResult> stubComplianceCheck(
      boolean compliant) {

    result.setCompliant(compliant);

    return when(complianceService.updateCalculationResult(
        ArgumentMatchers.any(Vehicle.class),
        ArgumentMatchers.any(CalculationResult.class))).thenReturn(result);
  }
  
  private OngoingStubbing<Float> stubChargeabilityService(float charge) {
    return when(chargeabilityService.getCharge(
        ArgumentMatchers.any(Vehicle.class),
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
      chargeCalculationService
          .checkVrnAgainstCaz(vehicle.getRegistrationNumber(), cleanAirZoneIds);
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
  void returnEarlyForRetrofitted() throws NotFoundException {
    stubVehicleRepository();
    stubExemptions(true);
    stubIdentification(VehicleType.PRIVATE_CAR);

    ComplianceResultsDto complianceDto = chargeCalculationService
        .checkVrnAgainstCaz(vehicle.getRegistrationNumber(), cleanAirZoneIds);

    assertEquals(new ArrayList<>(), complianceDto.getComplianceOutcomes());
    verify(nationalTaxiRegisterService, times(0)).getLicenseInformation(Mockito.any());
  }

  @Test
  void returnEarlyForExempt() throws NotFoundException {
    stubVehicleRepository();
    stubExemptions(true);
    stubIdentification(VehicleType.PRIVATE_CAR);

    ComplianceResultsDto complianceDto = chargeCalculationService
        .checkVrnAgainstCaz(vehicle.getRegistrationNumber(), cleanAirZoneIds);

    assertEquals(new ArrayList<>(), complianceDto.getComplianceOutcomes());
    verify(nationalTaxiRegisterService, times(0)).getLicenseInformation(Mockito.any());
  }

  @Test
  void retrofittedTrueIfOnWhitelist() throws NotFoundException {
    stubVehicleRepository();
    stubExemptions(true);
    Mockito.when(retrofitService.isRetrofitted(Mockito.anyString())).thenReturn(true);
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
    stubTariffRepository();
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
    stubTariffRepository();
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
    stubTariffRepository();
    stubComplianceCheck(false);
    stubChargeabilityService((float) 3.142);

    chargeCalculationService.checkVrnAgainstCaz(vehicle.getRegistrationNumber(),
        cleanAirZoneIds);

    assertEquals(VehicleType.TAXI_OR_PHV, vehicle.getVehicleType());
    assertTrue(vehicle.getIsTaxiOrPhv());
  }

  @Test
  void unchangedVehicleTypeIfNotOnTaxiRegister() throws NotFoundException {
    stubVehicleRepository();
    stubExemptions(false);
    stubIdentification(VehicleType.PRIVATE_CAR);
    stubTaxiRegister(false);
    stubTariffRepository();
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
    stubTariffRepository();
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
  void tariffRepostoryCalled() throws NotFoundException {
    stubVehicleRepository();
    stubExemptions(false);
    stubIdentification(VehicleType.PRIVATE_CAR);
    stubTaxiRegister(false);
    stubTariffRepository();
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
      chargeCalculationService
          .checkVrnAgainstCaz(vehicle.getRegistrationNumber(), cleanAirZoneIds);
    });
  }

  @Test
  void findsCorrectChargeFromvehicleChargesList() throws NotFoundException {
    stubVehicleRepository();
    stubExemptions(false);
    stubIdentification(VehicleType.PRIVATE_CAR);
    stubTaxiRegister(false);
    stubTariffRepository();
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
    stubTariffRepository();
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
    stubTariffRepository();
    stubComplianceCheck(false);
    stubChargeabilityService((float) 0);

    ComplianceResultsDto complianceDto = chargeCalculationService
        .checkVrnAgainstCaz(vehicle.getRegistrationNumber(), cleanAirZoneIds);

    for (ComplianceOutcomeDto result : complianceDto.getComplianceOutcomes()) {
      assertEquals((float) 0.0, result.getCharge(), 0.001);
    }
  }

  @Test
  void zeroChargeIfCompliant() throws NotFoundException {
    stubVehicleRepository();
    stubExemptions(false);
    stubIdentification(VehicleType.PRIVATE_CAR);
    stubTaxiRegister(false);
    stubTariffRepository();
    stubComplianceCheck(true);

    ComplianceResultsDto complianceDto = chargeCalculationService
        .checkVrnAgainstCaz(vehicle.getRegistrationNumber(), cleanAirZoneIds);

    for (ComplianceOutcomeDto result : complianceDto.getComplianceOutcomes()) {
      assertEquals((float) 0.0, result.getCharge(), 0.001);
    }
  }

  @Test
  void applyChargeIfChargeableAndNonCompliant() throws NotFoundException {
    stubVehicleRepository();
    stubExemptions(false);
    stubIdentification(VehicleType.PRIVATE_CAR);
    stubTaxiRegister(false);
    stubTariffRepository();
    stubComplianceCheck(false);
    stubChargeabilityService((float) 3.142);

    ComplianceResultsDto complianceDto = chargeCalculationService
        .checkVrnAgainstCaz(vehicle.getRegistrationNumber(), cleanAirZoneIds);

    for (ComplianceOutcomeDto result : complianceDto.getComplianceOutcomes()) {
      assertEquals((float) 3.142, result.getCharge(), 0.001);
    }
  }
}
