package uk.gov.caz.vcc.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.caz.vcc.service.VehicleEntrantsService.STATUSES_TO_CHARGE_VALIDITY_CODES;
import static uk.gov.caz.vcc.util.VccAssertions.assertThat;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.caz.async.rest.AsyncResponse;
import uk.gov.caz.vcc.domain.CalculationResult;
import uk.gov.caz.vcc.domain.ChargeValidity;
import uk.gov.caz.vcc.domain.CleanAirZoneEntrant;
import uk.gov.caz.vcc.domain.MilitaryVehicle;
import uk.gov.caz.vcc.domain.RetrofittedVehicle;
import uk.gov.caz.vcc.domain.TariffDetails;
import uk.gov.caz.vcc.domain.Vehicle;
import uk.gov.caz.vcc.domain.VehicleType;
import uk.gov.caz.vcc.domain.service.ChargeabilityService;
import uk.gov.caz.vcc.domain.service.ComplianceService;
import uk.gov.caz.vcc.domain.service.VehicleIdentificationService;
import uk.gov.caz.vcc.dto.PaymentStatus;
import uk.gov.caz.vcc.dto.PaymentStatusRequestDto;
import uk.gov.caz.vcc.dto.TaxiPhvLicenseInformationResponse;
import uk.gov.caz.vcc.dto.VehicleEntrantDto;
import uk.gov.caz.vcc.dto.VehicleEntrantsSaveRequestDto;
import uk.gov.caz.vcc.dto.VehicleResultDto;
import uk.gov.caz.vcc.repository.LicenseAndVehicleRemoteRepository;
import uk.gov.caz.vcc.repository.LicenseAndVehicleRepository.LicenseAndVehicleResponse;
import uk.gov.caz.vcc.repository.TariffDetailsRepository;
import uk.gov.caz.vcc.util.VehicleApiAuthenticationUtility;
import uk.gov.caz.vcc.util.VehicleResultDtoAssert;

@ExtendWith(MockitoExtension.class)
class VehicleEntrantsServiceTest {

  private static final UUID SOME_CLEAN_AIR_ZONE_ID = UUID.randomUUID();

  private static final String SOME_CORRELATION_ID = UUID.randomUUID().toString();

  private static final String SOME_VRN = "SW61BYD";

  private static final String SOME_DATE = "2017-10-01T155301Z";

  private static final String SOME_FAILURE_MESSAGE = "some message";

  @Mock
  private VehicleIdentificationService vehicleIdentificationService;

  @Mock
  private CazTariffService cazTariffService;

  @Mock
  private TariffDetailsRepository tariffDetailsRepository;

  @Mock
  private ChargeabilityService chargeabilityService;
  
  @Mock
  private ComplianceService complianceService;

  @Mock
  private ExemptionService exemptionService;

  @Mock
  private MilitaryVehicleService militaryVehicleService;

  @Mock
  private RetrofitService retrofitService;

  @Mock
  private LicenseAndVehicleRemoteRepository licenseAndVehicleRemoteRepository;

  @Mock
  private VehicleApiAuthenticationUtility remoteAuthenticationTokenGenerator;

  @Mock
  private PaymentsService paymentsService;

  @Mock
  private ChargeCalculationService chargeCalculationService;

  @InjectMocks
  private VehicleEntrantsService vehicleEntrantsService;

  private VehicleResultDto firstResult;

  @BeforeEach
  public void init() {
    ReflectionTestUtils.setField(vehicleEntrantsService, "paymentsEnabled", true);
  }

  @Test
  public void shouldMapDtoToModelObject() {
    // given
    UUID cazId = UUID.randomUUID();
    String vrn = "vrn123";
    String correlationId = UUID.randomUUID().toString();
    String time = "2017-10-01T155301Z";
    LocalDateTime localDateTime = LocalDateTime.parse(time,
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HHmmssX"));
    VehicleEntrantDto vehicleEntrantDto = new VehicleEntrantDto(vrn, time);
    VehicleEntrantsSaveRequestDto vehicleEntrantsSaveRequestDto = new VehicleEntrantsSaveRequestDto(
        cazId, correlationId, Collections.singletonList(vehicleEntrantDto));
    VehicleResultDto vehicleResultDto = VehicleResultDto.builder().vrn(vrn)
        .status("exempt").build();

    CleanAirZoneEntrant cleanAirZoneEntrant = new CleanAirZoneEntrant(cazId,
        correlationId, localDateTime);
    cleanAirZoneEntrant.setVrn(vrn);
    cleanAirZoneEntrant.setChargeValidityCode(new ChargeValidity("CVC02"));

    // when
    CleanAirZoneEntrant mappedEntrant = vehicleEntrantsService
        .toModel(vehicleEntrantsSaveRequestDto, vehicleResultDto);

    // then

    assertThat(mappedEntrant.getChargeValidityCode().getChargeValidityCode())
        .isEqualTo(cleanAirZoneEntrant.getChargeValidityCode()
            .getChargeValidityCode());
    assertThat(mappedEntrant.getCleanAirZoneId())
        .isEqualTo(cleanAirZoneEntrant.getCleanAirZoneId());
    assertThat(mappedEntrant.getCorrelationId())
        .isEqualTo(cleanAirZoneEntrant.getCorrelationId());
    assertThat(mappedEntrant.getVrn()).isEqualTo(cleanAirZoneEntrant.getVrn());
    assertThat(mappedEntrant.getEntrantTimestamp())
        .isEqualTo(cleanAirZoneEntrant.getEntrantTimestamp());
    assertThat(mappedEntrant.getInsertTimestamp())
        .isEqualTo(cleanAirZoneEntrant.getInsertTimestamp());
  }

  @Test
  public void shouldParseDate() {
    VehicleEntrantDto vehicleEntrantDto = new VehicleEntrantDto("vrn",
        "2017-10-01T155301Z");

    LocalDateTime localDateTime = vehicleEntrantsService
        .parseDate(vehicleEntrantDto);

    assertThat(localDateTime.getYear()).isEqualTo(2017);
    assertThat(localDateTime.getMonthValue()).isEqualTo(10);
    assertThat(localDateTime.getDayOfMonth()).isEqualTo(1);
    assertThat(localDateTime.getHour()).isEqualTo(15);
    assertThat(localDateTime.getMinute()).isEqualTo(53);
    assertThat(localDateTime.getSecond()).isEqualTo(01);
  }

  @Test
  public void shouldReturnVehicleResultsWithExemptStatusAndDiscountCodeFromMod() {
    // given
    CalculationResult result = prepareCalculationResult(true, false, false, 0);

    mockSuccess(VehicleType.PRIVATE_CAR, result);
    mockModService();

    // when
    List<VehicleResultDto> vehicleResultsResponse = vehicleEntrantsService
        .createVehicleResultsResponse(prepareVehicleEntrantsDto(),
            SOME_CLEAN_AIR_ZONE_ID, SOME_CORRELATION_ID);

    // then
    VehicleResultDto vehicleResultDto = vehicleResultsResponse.get(0);
    verifyVehicle(vehicleResultDto)
        .hasStatus("exempt")
        .hasExemptionCode("WDC001")
        .tariffCodeIsNull()
        .isTaxiOrPhv()
        .hasLicensingAuthority(prepareLicensingAuthoritiesNames());
    verify(complianceService, times(1)).updateCalculationResult(
        any(Vehicle.class), any(CalculationResult.class));
    verify(retrofitService, times(0)).findByVrn(anyString());
    verify(paymentsService, times(0))
        .registerVehicleEntryAndGetPaymentStatus(any(PaymentStatusRequestDto.class));
  }

  @Test
  public void shouldReturnVehicleResultsWithExemptStatusAndDiscountCodeFromRetrofit() {
    // given
    CalculationResult result = prepareCalculationResult(true, false, false, 0);

    mockSuccess(VehicleType.PRIVATE_CAR, result);
    mockRetrofitService();

    // when
    List<VehicleResultDto> vehicleResultsResponse = vehicleEntrantsService
        .createVehicleResultsResponse(prepareVehicleEntrantsDto(),
            SOME_CLEAN_AIR_ZONE_ID, SOME_CORRELATION_ID);

    // then
    VehicleResultDto vehicleResultDto = vehicleResultsResponse.get(0);
    verifyVehicle(vehicleResultDto)
        .hasStatus("exempt")
        .hasExemptionCode("WDC002")
        .tariffCodeIsNull()
        .isTaxiOrPhv()
        .hasLicensingAuthority(prepareLicensingAuthoritiesNames());
    verify(complianceService, times(1)).updateCalculationResult(
        any(Vehicle.class), any(CalculationResult.class));
    verify(paymentsService, times(0))
        .registerVehicleEntryAndGetPaymentStatus(any(PaymentStatusRequestDto.class));
  }

  @Test
  public void shouldReturnVehicleResultsWithCompliantStatusAndDiscountCodeNull() {
    // given
    CalculationResult result = prepareCalculationResult(false, true, true, 40);

    mockSuccess(VehicleType.PRIVATE_CAR, result);

    // when
    List<VehicleResultDto> vehicleResultsResponse = vehicleEntrantsService
        .createVehicleResultsResponse(prepareVehicleEntrantsDto(),
            SOME_CLEAN_AIR_ZONE_ID, SOME_CORRELATION_ID);

    // then
    VehicleResultDto vehicleResultDto = vehicleResultsResponse.get(0);
    verifyVehicle(vehicleResultDto)
        .hasStatus("compliant")
        .exemptionCodeIsNull()
        .tariffCodeIsNull()
        .isTaxiOrPhv()
        .hasLicensingAuthority(prepareLicensingAuthoritiesNames());
    verify(militaryVehicleService, times(0)).findByVrn(anyString());
    verify(retrofitService, times(0)).findByVrn(anyString());
    verify(paymentsService, times(0))
        .registerVehicleEntryAndGetPaymentStatus(any(PaymentStatusRequestDto.class));
  }

  @ParameterizedTest
  @MethodSource("vehicleTypesAndTariffCode")
  public void shouldReturnVehicleResultsWithNotCompliantNotPaidStatusDiscountCodeNullAndTariffCode(
      VehicleType vehicleType, String tariffCode) {
    // given
    CalculationResult result = prepareCalculationResult(false, false, false, 0);

    mockSuccess(vehicleType, result);

    // when
    List<VehicleResultDto> vehicleResultsResponse = vehicleEntrantsService
        .createVehicleResultsResponse(prepareVehicleEntrantsDto(),
            SOME_CLEAN_AIR_ZONE_ID, SOME_CORRELATION_ID);

    // then
    VehicleResultDto vehicleResultDto = vehicleResultsResponse.get(0);
    verifyVehicle(vehicleResultDto)
        .hasStatus("notCompliantNotPaid")
        .exemptionCodeIsNull()
        .hasTariffCode(tariffCode)
        .isTaxiOrPhv()
        .hasLicensingAuthority(prepareLicensingAuthoritiesNames());
    verify(militaryVehicleService, times(0)).findByVrn(anyString());
    verify(retrofitService, times(0)).findByVrn(anyString());
    verify(complianceService, times(1)).updateCalculationResult(
        any(Vehicle.class), any(CalculationResult.class));
    verify(paymentsService, times(0))
        .registerVehicleEntryAndGetPaymentStatus(any(PaymentStatusRequestDto.class));
  }

  @Test
  public void shouldReturnVehicleResultsWithOutInformationFromDvla() {
    // given
    stubAuthentication();

    given(licenseAndVehicleRemoteRepository.findLicenseAndVehicle(anyString(), anyString(),
        anyString()))
        .willReturn(LicenseAndVehicleResponse.builder()
            .licensInfo(SOME_VRN, AsyncResponse.failure(SOME_FAILURE_MESSAGE, HttpStatus.NOT_FOUND))
            .vehicle(SOME_VRN, AsyncResponse.failure(SOME_FAILURE_MESSAGE, HttpStatus.NOT_FOUND))
            .build());

    // when
    List<VehicleResultDto> vehicleResultsResponse = vehicleEntrantsService
        .createVehicleResultsResponse(prepareVehicleEntrantsDto(),
            SOME_CLEAN_AIR_ZONE_ID, SOME_CORRELATION_ID);

    // then
    VehicleResultDto vehicleResultDto = vehicleResultsResponse.get(0);
    assertThat(vehicleResultDto).hasVrn(SOME_VRN).colourIsNull().makeIsNull()
        .modelIsNull().typeApprovalIsNull().hasStatus("unrecognisedNotPaid")
        .exemptionCodeIsNull().tariffCodeIsNull().isNotTaxiOrPhv()
        .licensingAuthorityIsNull();
    verify(militaryVehicleService, times(0)).findByVrn(anyString());
    verify(retrofitService, times(0)).findByVrn(anyString());
    verify(exemptionService, times(0)).updateCalculationResult(
        any(Vehicle.class), any(CalculationResult.class));
    verify(complianceService, times(0)).updateCalculationResult(
        any(Vehicle.class), any(CalculationResult.class));
    verify(licenseAndVehicleRemoteRepository, times(1))
        .findLicenseAndVehicle(anyString(), anyString(), anyString());
    verify(chargeabilityService, times(0)).getCharge(
        any(Vehicle.class),  any(TariffDetails.class));
    verify(paymentsService, times(1))
        .registerVehicleEntryAndGetPaymentStatus(any(PaymentStatusRequestDto.class));
  }

  private void stubAuthentication() {
    given(remoteAuthenticationTokenGenerator.getAuthenticationToken())
        .willReturn(StringUtils.EMPTY);
  }

  @Test
  public void shouldReturnVehicleResultsWithOutTariffCode() {
    // given
    CalculationResult result = prepareCalculationResult(true, false, false, 0);

    mockLicenseAndVehicleResponse(VehicleType.PRIVATE_CAR);
    mockTariffDetails(Optional.empty());
    mockModService();
    mockExemptAndCompliantStatus(result);

    // when
    List<VehicleResultDto> vehicleResultsResponse = vehicleEntrantsService
        .createVehicleResultsResponse(prepareVehicleEntrantsDto(),
            SOME_CLEAN_AIR_ZONE_ID, SOME_CORRELATION_ID);

    // then
    VehicleResultDto vehicleResultDto = vehicleResultsResponse.get(0);
    verifyVehicle(vehicleResultDto)
        .hasStatus("exempt")
        .hasExemptionCode("WDC001")
        .tariffCodeIsNull()
        .isTaxiOrPhv()
        .hasLicensingAuthority(prepareLicensingAuthoritiesNames());
    verify(retrofitService, times(0)).findByVrn(anyString());
    verify(complianceService, times(1)).updateCalculationResult(
        any(Vehicle.class), any(CalculationResult.class));
    verify(chargeabilityService, times(0)).getCharge(
        any(Vehicle.class),  any(TariffDetails.class));
    verify(paymentsService, times(0))
        .registerVehicleEntryAndGetPaymentStatus(any(PaymentStatusRequestDto.class));
  }

  @Test
  public void shouldReturnTwoVehicleResultsOneFullAndOneWithOutInformationFromDvla() {
    // given
    CalculationResult result = prepareCalculationResult(true, false, false, 0);

    mockTariffDetails(Optional.of(prepareTariffDetails()));
    mockLicenseAndVehicleResponseFor2Vrns();
    mockRetrofitService();
    mockExemptAndCompliantStatus(result);
    mockChargeCalculation((float) 12.5);

    // when
    List<VehicleResultDto> vehicleResultsResponse = vehicleEntrantsService
        .createVehicleResultsResponse(
            Lists.newArrayList(new VehicleEntrantDto(SOME_VRN, SOME_DATE),
                new VehicleEntrantDto("MK16YZR", SOME_DATE)),
            SOME_CLEAN_AIR_ZONE_ID, SOME_CORRELATION_ID);

    // then
    VehicleResultDto firstResult = vehicleResultsResponse.get(0);
    verifyVehicle(firstResult)
        .hasVrn("SW61BYD")
        .hasStatus("exempt")
        .hasExemptionCode("WDC002")
        .tariffCodeIsNull()
        .isTaxiOrPhv()
        .hasLicensingAuthority(prepareLicensingAuthoritiesNames());
    VehicleResultDto secondResult = vehicleResultsResponse.get(1);
    assertThat(secondResult)
        .hasVrn("MK16YZR")
        .colourIsNull()
        .makeIsNull()
        .modelIsNull()
        .typeApprovalIsNull()
        .hasStatus("unrecognisedNotPaid")
        .exemptionCodeIsNull()
        .tariffCodeIsNull()
        .isNotTaxiOrPhv()
        .licensingAuthorityIsNull();
  }

  @Test
  public void shouldReturnVehicleResultsWithUnrecognisedNotPaid() {
    // given
    given(paymentsService.registerVehicleEntryAndGetPaymentStatus(any()))
        .willReturn(PaymentStatus.NOT_PAID);

    // when
    List<VehicleResultDto> vehicleResultsResponse = vehicleEntrantsService
        .createVehicleResultsResponse(
            Lists.newArrayList(new VehicleEntrantDto("SW61BYDBE", SOME_DATE)),
            SOME_CLEAN_AIR_ZONE_ID, SOME_CORRELATION_ID);

    // then
    firstResult = vehicleResultsResponse.get(0);

    shouldHaveStatus("unrecognisedNotPaid");
    shouldCallOnlyPaymentsService();
  }

  @Test
  public void shouldReturnVehicleResultsWithUnrecognisedPaid() {
    // given
    given(paymentsService.registerVehicleEntryAndGetPaymentStatus(any()))
        .willReturn(PaymentStatus.PAID);

    // when
    List<VehicleResultDto> vehicleResultsResponse = vehicleEntrantsService
        .createVehicleResultsResponse(
            Lists.newArrayList(new VehicleEntrantDto("SW61BYDBE", SOME_DATE)),
            SOME_CLEAN_AIR_ZONE_ID, SOME_CORRELATION_ID);

    // then
    firstResult = vehicleResultsResponse.get(0);

    shouldHaveStatus("unrecognisedPaid");
    shouldCallOnlyPaymentsService();
  }

  private void shouldHaveStatus(String status) {
    assertThat(firstResult)
        .hasVrn("SW61BYDBE")
        .hasStatus(status)
        .colourIsNull()
        .makeIsNull()
        .modelIsNull()
        .typeApprovalIsNull()
        .exemptionCodeIsNull()
        .tariffCodeIsNull()
        .isNotTaxiOrPhv()
        .licensingAuthorityIsNull();
  }

  private void shouldCallOnlyPaymentsService() {
    verify(paymentsService, times(1)).registerVehicleEntryAndGetPaymentStatus(any(
        PaymentStatusRequestDto.class));
    verify(militaryVehicleService, times(0)).findByVrn(anyString());
    verify(retrofitService, times(0)).findByVrn(anyString());
    verify(exemptionService, times(0)).updateCalculationResult(
        any(Vehicle.class), any(CalculationResult.class));
    verify(complianceService, times(0)).updateCalculationResult(
        any(Vehicle.class), any(CalculationResult.class));
    verify(chargeabilityService, times(0)).getCharge(
        any(Vehicle.class),  any(TariffDetails.class));
  }

  @Test
  public void shouldReturnVehicleResultsWithNotCompliantPaid() {
    // given
    mockSuccessWithNotCompliantPaidStatus();

    // when
    List<VehicleResultDto> vehicleResultsResponse = vehicleEntrantsService
        .createVehicleResultsResponse(prepareVehicleEntrantsDto(),
            SOME_CLEAN_AIR_ZONE_ID, SOME_CORRELATION_ID);

    // then
    VehicleResultDto vehicleResultDto = vehicleResultsResponse.get(0);
    verifyVehicle(vehicleResultDto)
        .hasStatus("notCompliantPaid")
        .exemptionCodeIsNull()
        .hasTariffCode("C0001-CAR")
        .isTaxiOrPhv()
        .hasLicensingAuthority(prepareLicensingAuthoritiesNames());
    verify(militaryVehicleService, times(0)).findByVrn(anyString());
    verify(retrofitService, times(0)).findByVrn(anyString());
    verify(complianceService, times(1)).updateCalculationResult(
        any(Vehicle.class), any(CalculationResult.class));
    verify(exemptionService, times(1)).updateCalculationResult(
        any(Vehicle.class), any(CalculationResult.class));
    verify(paymentsService, times(1))
        .registerVehicleEntryAndGetPaymentStatus(any(PaymentStatusRequestDto.class));
  }

  @Test
  public void shouldReturnEmptyList() {
    // given

    // when
    List<VehicleResultDto> vehicleResultsResponse = vehicleEntrantsService
        .createVehicleResultsResponse(Lists.emptyList(),
            SOME_CLEAN_AIR_ZONE_ID, SOME_CORRELATION_ID);

    // then
    assertThat(vehicleResultsResponse).hasSize(0);
  }

  @ParameterizedTest
  @ValueSource(strings = {"notCompliantPaid,CVC01",
      "notCompliantNotPaid,CVC01", "exempt,CVC02", "compliant,CVC03",
      "unrecognisedPaid,CVC04", "unrecognisedNotPaid,CVC04"})
  public void shouldMapVehicleEntrantStatusToValidityCode(String value) {
    String vehicleStatus = value.split(",")[0];
    String validityCode = value.split(",")[1];

    assertThat(STATUSES_TO_CHARGE_VALIDITY_CODES.get(vehicleStatus)
        .getChargeValidityCode()).isEqualTo(validityCode);
  }

  private void mockSuccessWithNotCompliantPaidStatus() {
    mockLicenseAndVehicleResponse(VehicleType.PRIVATE_CAR);
    mockTariffDetails(Optional.of(prepareTariffDetails()));
    mockExemptAndCompliantStatus(prepareCalculationResult(false, false, true, 40));
    mockChargeCalculation((float) 12.5);
    mockPaymentServiceWithStatusPaid();
  }

  private void mockSuccess(VehicleType vehicleType, CalculationResult result) {
    stubAuthentication();
    mockTariffDetails(Optional.of(prepareTariffDetails()));
    mockExemptAndCompliantStatus(result);
    mockLicenseAndVehicleResponse(vehicleType);
    mockChargeCalculation(result.getCharge());
  }

  private void mockExemptAndCompliantStatus(CalculationResult result) {
    mockIsExempt(result);
    mockIsCompliant(result);
  }

  private void mockLicenseAndVehicleResponse(VehicleType privateCar) {
    stubAuthentication();
    given(licenseAndVehicleRemoteRepository.findLicenseAndVehicle(anyString(), anyString(),
        anyString()))
        .willReturn(LicenseAndVehicleResponse.builder()
            .licensInfo(SOME_VRN, AsyncResponse.success(prepareLicenseInfoResponse(true,
                prepareLicensingAuthoritiesNames())))
            .vehicle(SOME_VRN, AsyncResponse.success(prepareVehicle(privateCar)))
            .build());

  }

  private void mockLicenseAndVehicleResponseFor2Vrns() {
    stubAuthentication();
    given(licenseAndVehicleRemoteRepository.findLicenseAndVehicle(anyString(), anyString(),
        anyString()))
        .willReturn(LicenseAndVehicleResponse.builder()
            .licensInfo(SOME_VRN, AsyncResponse.success(prepareLicenseInfoResponse(true,
                prepareLicensingAuthoritiesNames())))
            .licensInfo("MK16YZR",
                AsyncResponse.failure(SOME_FAILURE_MESSAGE, HttpStatus.NOT_FOUND))
            .vehicle(SOME_VRN, AsyncResponse.success(prepareVehicle(VehicleType.PRIVATE_CAR)))
            .vehicle("MK16YZR", AsyncResponse.failure(SOME_FAILURE_MESSAGE,
                HttpStatus.NOT_FOUND))
            .build());
  }

  private void mockIsExempt(CalculationResult result) {
    given(exemptionService.updateCalculationResult(any(Vehicle.class),
        any(CalculationResult.class))).willReturn(result);
  }

  private void mockIsCompliant(CalculationResult result) {
    given(complianceService.updateCalculationResult(any(Vehicle.class),
        any(CalculationResult.class))).willReturn(result);
  }

  private void mockChargeCalculation(float charge) {
    given(chargeabilityService.getCharge(
        any(Vehicle.class),  any(TariffDetails.class))).willReturn(charge);
  }

  private void mockTariffDetails(Optional<TariffDetails> tariffDetails) {
    given(tariffDetailsRepository.getTariffDetails(any(UUID.class)))
        .willReturn(tariffDetails);
  }

  private void mockRetrofitService() {
    given(retrofitService.findByVrn(anyString()))
        .willReturn(prepareRetrofittedVehicle());
  }

  private void mockModService() {
    given(militaryVehicleService.findByVrn(anyString()))
        .willReturn(prepareMilitaryVehicle());
  }

  private void mockPaymentServiceWithStatusPaid() {
    given(paymentsService.registerVehicleEntryAndGetPaymentStatus(any()))
        .willReturn(PaymentStatus.PAID);
  }

  private VehicleResultDtoAssert verifyVehicle(
      VehicleResultDto vehicleResultDto) {
    return assertThat(vehicleResultDto).hasVrn(SOME_VRN).hasColour("black")
        .hasMake("volkswagen").hasModel("golf").hasTypeApproval("m1");
  }

  private static Stream<Arguments> vehicleTypesAndTariffCode() {
    return Stream.of(Arguments.of(VehicleType.PRIVATE_CAR, "C0001-CAR"),
        Arguments.of(VehicleType.MINIBUS, "C0001-MINIBUS"),
        Arguments.of(VehicleType.SMALL_VAN, "C0001-SMALL VAN"),
        Arguments.of(VehicleType.MOTORCYCLE, "C0001-MOTORCYCLE"));
  }

  private List<VehicleEntrantDto> prepareVehicleEntrantsDto() {
    VehicleEntrantDto vehicleEntrantDto = new VehicleEntrantDto(SOME_VRN,
        SOME_DATE);
    return Lists.newArrayList(vehicleEntrantDto);
  }

  private Optional<RetrofittedVehicle> prepareRetrofittedVehicle() {
    RetrofittedVehicle retrofittedVehicle = new RetrofittedVehicle();
    retrofittedVehicle.setWhitelistDiscountCode("WDC002");
    return Optional.of(retrofittedVehicle);
  }

  private ArrayList<String> prepareLicensingAuthoritiesNames() {
    return Lists.newArrayList("la1", "la2");
  }

  private TaxiPhvLicenseInformationResponse prepareLicenseInfoResponse(boolean active,
      ArrayList<String> licensingAuthoritiesNames) {
    return TaxiPhvLicenseInformationResponse.builder()
        .licensingAuthoritiesNames(licensingAuthoritiesNames).active(active)
        .build();
  }

  private Optional<MilitaryVehicle> prepareMilitaryVehicle() {
    MilitaryVehicle militaryVehicle = new MilitaryVehicle();
    militaryVehicle.setWhitelistDiscountCode("WDC001");
    return Optional.of(militaryVehicle);
  }

  private TariffDetails prepareTariffDetails() {
    TariffDetails tariffDetails = new TariffDetails();
    tariffDetails.setChargeIdentifier("C0001");
    return tariffDetails;
  }

  private CalculationResult prepareCalculationResult(boolean exempt,
      boolean compliant, boolean chargeable, float charge) {
    CalculationResult result = new CalculationResult();
    result.setExempt(exempt);
    result.setCompliant(compliant);
    result.setChargeable(chargeable);
    result.setCharge(charge);
    result.setCazIdentifier(SOME_CLEAN_AIR_ZONE_ID);
    return result;
  }

  private Vehicle prepareVehicle(VehicleType vehicleType) {
    Vehicle vehicle = new Vehicle();
    vehicle.setVehicleType(vehicleType);
    vehicle.setRegistrationNumber(SOME_VRN);
    vehicle.setColour("black");
    vehicle.setMake("volkswagen");
    vehicle.setModel("golf");
    vehicle.setTypeApproval("m1");
    return vehicle;
  }
}