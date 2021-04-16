package uk.gov.caz.vcc.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.util.Lists.newArrayList;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.caz.definitions.domain.Vehicle;
import uk.gov.caz.definitions.domain.VehicleType;
import uk.gov.caz.definitions.dto.VehicleDto;
import uk.gov.caz.vcc.domain.CalculationResult;
import uk.gov.caz.vcc.domain.service.VehicleIdentificationService;
import uk.gov.caz.vcc.dto.TaxiPhvLicenseInformationResponse;
import uk.gov.caz.vcc.repository.LicenseAndVehicleRepository;
import uk.gov.caz.vcc.repository.VehicleDetailsRepository;
import uk.gov.caz.vcc.util.VehicleApiAuthenticationUtility;

@ExtendWith(MockitoExtension.class)
class VehicleServiceTest {

  @Mock
  private NationalTaxiRegisterService nationalTaxiRegisterService;

  @Mock
  private VehicleDetailsRepository vehicleDetailsRepository;

  @Mock
  private VehicleIdentificationService vehicleIdentificationService;

  @Mock
  private ExemptionService exemptionService;

  @Mock
  private RetrofitService retrofitService;

  @Mock
  private MilitaryVehicleService militaryVehicleService;

  @Mock
  private GeneralWhitelistService generalWhitelistService;

  @Mock
  private LicenseAndVehicleRepository licenseAndVehicleRepository;

  @Mock
  private VehicleApiAuthenticationUtility vehicleApiAuthenticationUtility;

  @InjectMocks
  private VehicleService vehicleService;

  private Vehicle testVehicle;

  private final String someRegistrationNumber = "CU57ABC";

  @BeforeEach
  void init() {
    testVehicle = new Vehicle();
  }

  @Test
  void shouldReturnEarlyIfMod() {
    // given
    testVehicle.setRegistrationNumber(someRegistrationNumber);
    mockMilitaryVehicleStatus(true);

    // when
    VehicleDto vehicleDto = vehicleService.findVehicle(someRegistrationNumber).get();

    // then
    assertTrue(vehicleDto.isExempt());
    assertThat(vehicleDto.getRegistrationNumber()).isEqualTo(someRegistrationNumber);
  }

  @Test
  void shouldReturnEarlyIfOnGeneralPurposeWhitelist() {
    // given
    testVehicle.setRegistrationNumber(someRegistrationNumber);
    mockGeneralWhitelistStatus(true);

    // when
    VehicleDto vehicleDto = vehicleService.findVehicle(someRegistrationNumber).get();

    // then
    assertTrue(vehicleDto.isExempt());
    assertThat(vehicleDto.getRegistrationNumber()).isEqualTo(someRegistrationNumber);
  }
  
  @Test
  void shouldCallNtrIfNotExempt() {
    // given
    testVehicle.setRegistrationNumber(someRegistrationNumber);

    mockMilitaryVehicleStatus(false);
    mockGeneralWhitelistStatus(false);
    mockFullExemptionCheck(false);
    mockVehicleDetailsRepositoryReturningOk(testVehicle);

    // when
    VehicleDto vehicleDto = vehicleService.findVehicle(someRegistrationNumber).get();

    // then
    assertFalse(vehicleDto.isExempt());
    verify(nationalTaxiRegisterService, times(1)).getLicenseInformation(someRegistrationNumber);
    verify(vehicleIdentificationService, times(1)).setVehicleType(testVehicle);
  }

  @Test
  void shouldIdentifyVehicleIfNotExempt() {
    // given
    testVehicle.setRegistrationNumber(someRegistrationNumber);

    mockMilitaryVehicleStatus(false);
    mockGeneralWhitelistStatus(false);
    mockFullExemptionCheck(false);
    mockVehicleDetailsRepositoryReturningOk(testVehicle);

    // when
    VehicleDto vehicleDto = vehicleService.findVehicle(someRegistrationNumber).get();

    // then
    assertFalse(vehicleDto.isExempt());
    verify(nationalTaxiRegisterService, times(1)).getLicenseInformation(someRegistrationNumber);
    verify(vehicleIdentificationService, times(1)).setVehicleType(testVehicle);
  }

  @Test
  void shouldEnhanceVehicleWithTaxiStatusAndWheelchairAccessibleStatusIfVehicleFoundInNtr() {
    // given
    testVehicle.setVehicleType(VehicleType.PRIVATE_CAR);

    mockVehicleDetailsRepositoryReturningOk(testVehicle);
    mockFullExemptionCheck(false);
    mockActiveTaxiLicenceFoundOk(someRegistrationNumber, true);

    // when
    VehicleDto vehicle = vehicleService.findVehicle(someRegistrationNumber).get();

    // then
    assertThat(vehicle.isTaxiOrPhv()).isTrue();
    assertThat(vehicle.getLicensingAuthoritiesNames()).contains("la-1", "la-2");
  }

  @Test
  void shouldStripAllWhitespacesWhenSearchingForVehicle() {
    // given
    final String someRegistrationNumber = "CU 5 7ABC";
    String cleanVrn = "CU57ABC";

    testVehicle.setRegistrationNumber(cleanVrn);

    mockVehicleDetailsRepositoryReturningOk(testVehicle);
    mockTaxiNotFound(cleanVrn);
    mockFullExemptionCheck(false);

    // when
    VehicleDto vehicle = vehicleService.findVehicle(someRegistrationNumber).get();

    // then
    assertThat(vehicle.getRegistrationNumber()).isEqualTo(cleanVrn);
    assertThat(vehicle.getLicensingAuthoritiesNames())
        .hasSize(0)
        .isEmpty();
  }

  @Test
  void shouldSetDefaultTaxiStatusAndWheelchairAccesibleFlagsIfVehicleIsNotFoundInNtr() {
    // given
    testVehicle.setVehicleType(VehicleType.PRIVATE_CAR);

    mockVehicleDetailsRepositoryReturningOk(testVehicle);
    mockTaxiNotFound(someRegistrationNumber);
    mockFullExemptionCheck(false);

    // when
    VehicleDto vehicle = vehicleService
        .findVehicle(someRegistrationNumber).get();

    // then
    assertThat(vehicle.isTaxiOrPhv()).isFalse();
  }

  @Test
  void shouldSetDefaultWheelchairAccessibleFlagIfVehicleDoesntHaveWheelchairAccessibleFlagSet() {
    // given
    testVehicle.setVehicleType(VehicleType.PRIVATE_CAR);

    mockVehicleDetailsRepositoryReturningOk(testVehicle);
    mockInactiveTaxiLicenceFoundOk(someRegistrationNumber, null);
    mockFullExemptionCheck(false);

    // when
    VehicleDto vehicle = vehicleService
        .findVehicle(someRegistrationNumber)
        .get();

    // then
    assertFalse(vehicle.isTaxiOrPhv());
    assertThat(vehicle.getLicensingAuthoritiesNames())
        .hasSize(0)
        .isEmpty();
    assertNull(testVehicle.getIsWav());
  }

  @Test
  void shouldNotSetTaxiPhvIfNtrLicenceActiveButExpired() {
    // given
    testVehicle.setVehicleType(VehicleType.PRIVATE_CAR);

    mockVehicleDetailsRepositoryReturningOk(testVehicle);
    mockActiveExpiredTaxiLicenceFoundOk(someRegistrationNumber, null);
    mockFullExemptionCheck(false);

    // when
    VehicleDto vehicle = vehicleService
        .findVehicle(someRegistrationNumber)
        .get();

    // then
    assertFalse(vehicle.isTaxiOrPhv());
    assertThat(vehicle.getLicensingAuthoritiesNames()).hasSize(0).isEmpty();
  }

  @Test
  void shouldThrowIllegalArgumentExceptionIfVrnContainsOnlyWhitespaces() {
    // given
    String vrnWithWhitespacesOnly = "     ";

    // when
    Throwable throwable = catchThrowable(
        () -> vehicleService.findVehicle(vrnWithWhitespacesOnly));

    // then
    then(throwable).isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Registration number can not contain only whitespaces.");
  }

  @Test
  void shouldThrowNullPointerExceptionIfRegistrationNumberIsNull() {
    // given
    String nullRegistrationNumber = null;

    // when
    Throwable throwable = catchThrowable(
        () -> vehicleService.findVehicle(nullRegistrationNumber));

    // then
    then(throwable).isInstanceOf(NullPointerException.class)
        .hasMessage("Registration number can not be null");
  }

  @Test
  void shouldThrowIllegalArgumentExceptionIfRegistrationNumberIsEmpty() {
    // given
    String emptyRegistrationNumber = "";

    // when
    Throwable throwable = catchThrowable(
        () -> vehicleService.findVehicle(emptyRegistrationNumber));

    // then
    then(throwable).isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Registration number can not be empty");
  }

  @Test
  void shouldReturnDvlaDataWhenDataIsFound() {
    // given
    testVehicle.setRegistrationNumber(someRegistrationNumber);
    given(vehicleDetailsRepository.findByRegistrationNumber(someRegistrationNumber)).willReturn(Optional.of(testVehicle));

    // when
    Optional<Vehicle> vehicleDto = vehicleService.dvlaDataForVehicle(someRegistrationNumber);

    // then
    then(vehicleDto.isPresent()).isTrue();

  }

  @Test
  void shouldNotReturnDvlaDataWhenDataIsNotFound() {
    // given
    given(vehicleDetailsRepository.findByRegistrationNumber(someRegistrationNumber)).willReturn(Optional.empty());

    // when
    Optional<Vehicle> vehicleDto = vehicleService.dvlaDataForVehicle(someRegistrationNumber);

    // then
    then(vehicleDto.isPresent()).isFalse();

  }

  @Test
  void shouldThrowExceptionWhenWeTriedToQueryDvlaWithMissingVrn() {
    // given
    String emptyRegistrationNumber = "";

    // when
    Throwable throwable = catchThrowable(
        () -> vehicleService.findVehicle(emptyRegistrationNumber));

    // then
    then(throwable).isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Registration number can not be empty");

  }

  private void mockTaxiNotFound(String registrationNumber) {
    given(nationalTaxiRegisterService.getLicenseInformation(registrationNumber))
        .willReturn(Optional.empty());
  }

  private void mockActiveTaxiLicenceFoundOk(String registrationNumber,
      Boolean wheelchairAccessible) {
    TaxiPhvLicenseInformationResponse testLicence = TaxiPhvLicenseInformationResponse
        .builder()
        .active(true)
        .licensedStatusExpires(LocalDate.now().plusDays(1))
        .wheelchairAccessible(wheelchairAccessible)
        .licensingAuthoritiesNames(newArrayList("la-1", "la-2"))
        .build();

    given(nationalTaxiRegisterService.getLicenseInformation(registrationNumber))
        .willReturn(Optional.of(testLicence));
  }

  private void mockInactiveTaxiLicenceFoundOk(String registrationNumber,
      Boolean wheelchairAccessible) {
    TaxiPhvLicenseInformationResponse testLicence = TaxiPhvLicenseInformationResponse
        .builder()
        .active(false)
        .wheelchairAccessible(wheelchairAccessible)
        .build();

    given(nationalTaxiRegisterService.getLicenseInformation(registrationNumber))
        .willReturn(Optional.of(testLicence));
  }

  private void mockActiveExpiredTaxiLicenceFoundOk(String registrationNumber,
      Boolean wheelchairAccessible) {
    TaxiPhvLicenseInformationResponse testLicence = TaxiPhvLicenseInformationResponse
        .builder()
        .active(false)
        .licensedStatusExpires(LocalDate.now().minusDays(2))
        .wheelchairAccessible(wheelchairAccessible)
        .build();

    given(nationalTaxiRegisterService.getLicenseInformation(registrationNumber))
        .willReturn(Optional.of(testLicence));
  }

  private void mockVehicleDetailsRepositoryReturningOk(Vehicle testVehicle) {
    given(vehicleDetailsRepository.findByRegistrationNumber(Mockito.anyString()))
        .willReturn(Optional.of(testVehicle));
  }

  private void mockFullExemptionCheck(boolean isExempt) {
    CalculationResult result = new CalculationResult();
    result.setExempt(isExempt);

    given(exemptionService.updateCalculationResult(
        Mockito.any(Vehicle.class),
        Mockito.any(CalculationResult.class)))
        .willReturn(result);
  }

  private void mockGeneralWhitelistStatus(boolean isExempt) {
    given(generalWhitelistService.exemptOnGeneralWhitelist(Mockito.anyString()))
        .willReturn(isExempt);
  }

  private void mockMilitaryVehicleStatus(boolean isMilitary) {
    given(militaryVehicleService.isMilitaryVehicle(Mockito.anyString())).willReturn(isMilitary);
  }
}
