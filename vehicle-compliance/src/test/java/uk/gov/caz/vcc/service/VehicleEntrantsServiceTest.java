package uk.gov.caz.vcc.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static uk.gov.caz.vcc.service.VehicleEntrantsService.STATUSES_TO_CHARGE_VALIDITY_CODES;
import static uk.gov.caz.vcc.util.VccAssertions.assertThat;

import com.google.common.collect.Sets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import uk.gov.caz.async.rest.AsyncCallException;
import uk.gov.caz.definitions.domain.Vehicle;
import uk.gov.caz.definitions.domain.VehicleType;
import uk.gov.caz.vcc.domain.CalculationResult;
import uk.gov.caz.vcc.domain.ChargeValidity;
import uk.gov.caz.vcc.domain.CleanAirZoneEntrant;
import uk.gov.caz.vcc.domain.GeneralWhitelistVehicle;
import uk.gov.caz.vcc.domain.RetrofittedVehicle;
import uk.gov.caz.vcc.domain.TariffDetails;
import uk.gov.caz.vcc.domain.exceptions.ExternalServiceCallException;
import uk.gov.caz.vcc.domain.exceptions.UnableToIdentifyVehicleComplianceException;
import uk.gov.caz.vcc.domain.service.ChargeabilityService;
import uk.gov.caz.vcc.domain.service.ComplianceService;
import uk.gov.caz.vcc.domain.service.VehicleIdentificationService;
import uk.gov.caz.vcc.dto.InitialVehicleResult;
import uk.gov.caz.vcc.dto.NtrAndDvlaVehicleData;
import uk.gov.caz.vcc.dto.PaymentMethod;
import uk.gov.caz.vcc.dto.PaymentStatus;
import uk.gov.caz.vcc.dto.PreFetchedDataResults;
import uk.gov.caz.vcc.dto.SingleDvlaVehicleData;
import uk.gov.caz.vcc.dto.SingleLicenceData;
import uk.gov.caz.vcc.dto.TaxiPhvLicenseInformationResponse;
import uk.gov.caz.vcc.dto.VehicleComplianceStatus;
import uk.gov.caz.vcc.dto.VehicleEntrantSaveDto;
import uk.gov.caz.vcc.dto.VehicleEntrantsSaveRequestDto;
import uk.gov.caz.vcc.dto.VehicleResultDto;
import uk.gov.caz.vcc.repository.TariffDetailsRepository;
import uk.gov.caz.vcc.util.VehicleResultDtoAssert;

@ExtendWith(MockitoExtension.class)
class VehicleEntrantsServiceTest {

  private static final UUID SOME_CLEAN_AIR_ZONE_ID = UUID.randomUUID();

  private static final String SOME_VRN = "SW61BYD";

  private static final String NON_UK_VRN = "SW61BYDBE";

  private static final String SOME_DATE = "2017-10-01T155301Z";

  private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter
      .ofPattern("yyyy-MM-dd'T'HHmmssX");

  private static final LocalDateTime SOME_DATETIME = LocalDateTime.parse(
      SOME_DATE, DATE_TIME_FORMATTER);

  private static final String SOME_FAILURE_MESSAGE = "some message";
  
  private static final String NATIONAL_EXEMPTION_CODE = "WDC001";

  @Mock
  private VehicleIdentificationService vehicleIdentificationService;

  @Mock
  private TariffDetailsRepository tariffDetailsRepository;

  @Mock
  private ChargeabilityService chargeabilityService;

  @Mock
  private ComplianceService complianceService;

  @Mock
  private ExemptionService exemptionService;

  @Mock
  private VehicleEntrantsPaymentsDataSupplier vehicleEntrantsPaymentsDataSupplier;

  @Mock
  private ChargeCalculationService chargeCalculationService;

  @Mock
  private ApplicationEventPublisher applicationEventPublisher;

  @Mock
  private MilitaryVehicleService militaryVehicleService;

  @Mock
  private GeneralWhitelistService generalWhitelistService;

  @Mock
  private RetrofitService retrofitVehicleService;

  @Mock
  private LicenseAndVehicleProvider licenseAndVehicleProvider;

  @InjectMocks
  private VehicleEntrantsService vehicleEntrantsService;

  private VehicleResultDto firstResult;

  private final String UNRECOGNISED_NOT_PAID = "unrecognisedNotPaid";
  private final String UNRECOGNISED_PAID = "unrecognisedPaid";

  private void mockPrefetchedDataResults() {
    List<GeneralWhitelistVehicle> matchedGeneralWhitelistVehicles = Lists.emptyList();
    List<RetrofittedVehicle> matchedRetrofittedVehicles = Lists.emptyList();
    Set<String> matchedMilitaryVrns = Collections.emptySet();

    PreFetchedDataResults prefetchedResults =
        new PreFetchedDataResults(matchedGeneralWhitelistVehicles, matchedRetrofittedVehicles,
            matchedMilitaryVrns);

    given(chargeCalculationService.computePrefetchedData(Mockito.anySet()))
        .willReturn(prefetchedResults);
  }
  
  @Nested
  class WhenExceptionHappensDuringComplianceCalculation {

    @Nested
    class AndEntranceHasNotBeenPaidYet {

      @Test
      public void itShouldReturnVehicleResultsWithUnrecognisedNotPaidStatus() {
        // given
        CalculationResult result = prepareCalculationResult(false, true, true, 40, false);
        mockIsExempt(result);
        mockLicenseAndVehicleResponse(VehicleType.PRIVATE_CAR);
        mockComplianceServiceException();
        mockPayments(SOME_CLEAN_AIR_ZONE_ID, PaymentStatus.NOT_PAID, PaymentMethod.NULL);
        mockPrefetchedDataResults();

        // when
        List<VehicleResultDto> vehicleResultsResponse =
            vehicleEntrantsService.createVehicleResultsResponse(
                prepareVehicleEntrantsDto(), SOME_CLEAN_AIR_ZONE_ID);
        // then
        VehicleResultDto vehicleResultDto = vehicleResultsResponse.get(0);
        assertThat(vehicleResultDto)
            .hasStatus(UNRECOGNISED_NOT_PAID)
            .hasPaymentMethod(null);
      }
    }

    @Nested
    class AndEntranceHasBeenPaid {

      @Test
      public void itShouldReturnVehicleResultsWithUnrecognisedPaidStatus() {
        // given
        CalculationResult result = prepareCalculationResult(false, true, true, 40, false);
        mockIsExempt(result);
        mockLicenseAndVehicleResponse(VehicleType.PRIVATE_CAR);
        mockComplianceServiceException();
        mockPayments(SOME_CLEAN_AIR_ZONE_ID, PaymentStatus.PAID, PaymentMethod.DIRECT_DEBIT);
        mockPrefetchedDataResults();

        // when
        List<VehicleResultDto> vehicleResultsResponse =
            vehicleEntrantsService.createVehicleResultsResponse(
                prepareVehicleEntrantsDto(), SOME_CLEAN_AIR_ZONE_ID);
        // then
        VehicleResultDto vehicleResultDto = vehicleResultsResponse.get(0);
        assertThat(vehicleResultDto)
            .hasStatus(UNRECOGNISED_PAID)
            .hasTariffCode("PAID_TARIFF_CODE")
            .hasPaymentMethod("direct_debit");
      }
    }
  }

  @Nested
  class WhenVehicleIsCompliantAndShouldNotPay {

    @Test
    public void shouldReturnVehicleResultsWithNotCompliantNotPaidStatusAndDiscountCodeNull() {
      // given
      CalculationResult result = prepareCalculationResult(false, true, true, 0, false);
      mockSuccess(VehicleType.PRIVATE_CAR, result);
      mockPrefetchedDataResults();

      // when
      List<VehicleResultDto> vehicleResultsResponse = vehicleEntrantsService
          .createVehicleResultsResponse(prepareVehicleEntrantsDto(),
              SOME_CLEAN_AIR_ZONE_ID);

      // then
      VehicleResultDto vehicleResultDto = vehicleResultsResponse.get(0);
      verifyVehicle(vehicleResultDto)
          .hasStatus("compliant")
          .exemptionCodeIsNull()
          .tariffCodeIsNull()
          .hasMake("volkswagen")
          .isTaxiOrPhv()
          .hasLicensingAuthority(prepareLicensingAuthoritiesNames());
      verify(vehicleEntrantsPaymentsDataSupplier)
          .processPaymentRelatedEntrants(argThat(List::isEmpty));
    }

    @Nested
    class AndRetrofitted {

      @Test
      public void shouldReturnVehicleResultsWithCompliantStatusAndDiscountCodeNull() {
        // given
        CalculationResult result = prepareCalculationResult(false, true, true, 0, true);
        mockSuccess(VehicleType.PRIVATE_CAR, result);
        mockPrefetchedDataResults();

        // when
        List<VehicleResultDto> vehicleResultsResponse = vehicleEntrantsService
            .createVehicleResultsResponse(prepareVehicleEntrantsDto(),
                SOME_CLEAN_AIR_ZONE_ID);

        // then
        VehicleResultDto vehicleResultDto = vehicleResultsResponse.get(0);
        verifyVehicle(vehicleResultDto)
            .hasStatus("compliant")
            .exemptionCodeIsNull()
            .tariffCodeIsNull()
            .isTaxiOrPhv()
            .hasLicensingAuthority(prepareLicensingAuthoritiesNames());
        verify(vehicleEntrantsPaymentsDataSupplier)
            .processPaymentRelatedEntrants(argThat(List::isEmpty));
      }

    }
  }

  @Nested
  class WhenVehicleIsNonCompliant {

    @ParameterizedTest
    @MethodSource("uk.gov.caz.vcc.service.VehicleEntrantsServiceTest#vehicleTypesAndTariffCode")
    public void shouldReturnVehicleResultsWithNotCompliantNotPaidStatusDiscountCodeNullAndTariffCode(
        VehicleType vehicleType, String tariffCode) {
      // given
      CalculationResult result = prepareCalculationResult(false, false, false, 40, false);
      mockPayments(SOME_CLEAN_AIR_ZONE_ID, PaymentStatus.NOT_PAID, PaymentMethod.NULL);
      mockPrefetchedDataResults();
      mockSuccess(vehicleType, result);

      // when
      List<VehicleResultDto> vehicleResultsResponse = vehicleEntrantsService
          .createVehicleResultsResponse(prepareVehicleEntrantsDto(),
              SOME_CLEAN_AIR_ZONE_ID);

      // then
      VehicleResultDto vehicleResultDto = vehicleResultsResponse.get(0);
      verifyVehicle(vehicleResultDto)
          .hasStatus("notCompliantNotPaid")
          .exemptionCodeIsNull()
          .hasTariffCode(tariffCode)
          .isTaxiOrPhv()
          .hasLicensingAuthority(prepareLicensingAuthoritiesNames());
      verify(complianceService, times(1)).updateCalculationResult(
          any(Vehicle.class), any(CalculationResult.class), anyBoolean(), anyBoolean());
      verify(vehicleEntrantsPaymentsDataSupplier, times(1))
          .processPaymentRelatedEntrants(anyList());
      verify(licenseAndVehicleProvider).prefetch(Sets.newHashSet(SOME_VRN));
    }

    @Test
    public void andChargeIsZeroThenShouldReturnCompliantStatus() {
      // given
      CalculationResult result = prepareCalculationResult(false, false, false, 0, false);
      mockSuccess(VehicleType.PRIVATE_CAR, result);
      mockPrefetchedDataResults();

      // when
      List<VehicleResultDto> vehicleResultsResponse = vehicleEntrantsService
          .createVehicleResultsResponse(prepareVehicleEntrantsDto(),
              SOME_CLEAN_AIR_ZONE_ID);

      // then
      VehicleResultDto vehicleResultDto = vehicleResultsResponse.get(0);
      verifyVehicle(vehicleResultDto)
          .hasStatus("compliant")
          .exemptionCodeIsNull()
          .tariffCodeIsNull()
          .isTaxiOrPhv()
          .hasLicensingAuthority(prepareLicensingAuthoritiesNames());
      verify(complianceService, times(1)).updateCalculationResult(
          any(Vehicle.class), any(CalculationResult.class), anyBoolean(), anyBoolean());
      verify(vehicleEntrantsPaymentsDataSupplier)
          .processPaymentRelatedEntrants(argThat(List::isEmpty));
    }

    @Test
    public void shouldReturnVehicleResultsWithNotCompliantPaid() {
      // given
      mockSuccessWithNotCompliantPaidStatus();
      mockPrefetchedDataResults();

      // when
      List<VehicleResultDto> vehicleResultsResponse = vehicleEntrantsService
          .createVehicleResultsResponse(prepareVehicleEntrantsDto(),
              SOME_CLEAN_AIR_ZONE_ID);

      // then
      VehicleResultDto vehicleResultDto = vehicleResultsResponse.get(0);
      verifyVehicle(vehicleResultDto)
          .hasStatus("notCompliantPaid")
          .exemptionCodeIsNull()
          .hasTariffCode("C0001-TAXI")
          .isTaxiOrPhv()
          .hasLicensingAuthority(prepareLicensingAuthoritiesNames());
      verify(complianceService, times(1)).updateCalculationResult(
          any(Vehicle.class), any(CalculationResult.class), anyBoolean(), anyBoolean());
      verify(exemptionService, times(1)).updateCalculationResult(
          any(Vehicle.class), any(CalculationResult.class), anyBoolean(), anyBoolean());
      verify(vehicleEntrantsPaymentsDataSupplier, times(1))
          .processPaymentRelatedEntrants(anyList());
    }
  }

  @Nested
  class WhenDvlaOrNtrIsMalfunctioning {

    @Test
    public void shouldReturnErrorWhenATimeoutIsEncountered() {
      // given
      given(licenseAndVehicleProvider.findLicenseAndVehicle(SOME_VRN)).willThrow(AsyncCallException.class);
      
      mockPrefetchedDataResults();
      
      Assertions.assertThrows(AsyncCallException.class, () -> {
        vehicleEntrantsService.createVehicleResultsResponse(
            prepareVehicleEntrantsDto(), SOME_CLEAN_AIR_ZONE_ID);
      });
    }
    
    @Test
    public void shouldReturnErrorWhenDvlaExperiencesAnError() {
      // given
      given(licenseAndVehicleProvider.findLicenseAndVehicle(SOME_VRN))
          .willReturn(new NtrAndDvlaVehicleData(
                  SingleDvlaVehicleData.failure(HttpStatus.SERVICE_UNAVAILABLE, SOME_FAILURE_MESSAGE),
                  SingleLicenceData.success(prepareLicenseInfoResponse(true, prepareLicensingAuthoritiesNames(),
                      LocalDate.now().minusDays(1)))
              )
          );
      
      mockPrefetchedDataResults();
      
      Assertions.assertThrows(ExternalServiceCallException.class, () -> {
        vehicleEntrantsService.createVehicleResultsResponse(
            prepareVehicleEntrantsDto(), SOME_CLEAN_AIR_ZONE_ID);
      });
    }
    
    @Test
    public void shouldReturnErrorWhenNtrCannotBeReached() {
      // given
      given(licenseAndVehicleProvider.findLicenseAndVehicle(SOME_VRN))
          .willReturn(new NtrAndDvlaVehicleData(
                  SingleDvlaVehicleData.success(prepareVehicle(VehicleType.PRIVATE_CAR)),
                  SingleLicenceData.failure(HttpStatus.SERVICE_UNAVAILABLE, SOME_FAILURE_MESSAGE)
              )
          );
      
      mockPrefetchedDataResults();
      
      Assertions.assertThrows(ExternalServiceCallException.class, () -> {
        vehicleEntrantsService.createVehicleResultsResponse(
            prepareVehicleEntrantsDto(), SOME_CLEAN_AIR_ZONE_ID);
      });
    }
    
  }

  @Nested
  class WhenVehicleIsExempt {

    @Nested
    class AndTariffCannotBeObtainedFromTariffService {

      @Test
      public void shouldReturnVehicleResultsWithOutTariffCode() {
        // given
        CalculationResult result = prepareCalculationResult(true, false, false, 0, false);
        mockLicenseAndVehicleResponse(VehicleType.PRIVATE_CAR);
        mockIsExempt(result);
        mockPrefetchedDataResults();

        // when
        List<VehicleResultDto> vehicleResultsResponse = vehicleEntrantsService
            .createVehicleResultsResponse(prepareVehicleEntrantsDto(),
                SOME_CLEAN_AIR_ZONE_ID);

        // then
        VehicleResultDto vehicleResultDto = vehicleResultsResponse.get(0);
        verifyVehicle(vehicleResultDto)
            .hasStatus("exempt")
            .hasExemptionCode("WDC001")
            .tariffCodeIsNull()
            .isTaxiOrPhv()
            .hasLicensingAuthority(prepareLicensingAuthoritiesNames());
        verifyNoInteractions(complianceService);
        verifyNoInteractions(chargeabilityService);
        verify(vehicleEntrantsPaymentsDataSupplier)
            .processPaymentRelatedEntrants(argThat(List::isEmpty));
      }
    }
  }

  @Nested
  class WhenVehicleHasActiveButExpiredTaxiLicense {

    @Test
    public void shouldReturnVehicleResultsWithOutTariffCode() {
      // given
      CalculationResult result = prepareCalculationResult(true, false, false, 0, false);
      mockExpiredLicenseAndVehicleResponse(VehicleType.PRIVATE_CAR);
      mockIsExempt(result);
      mockPrefetchedDataResults();

      // when
      List<VehicleResultDto> vehicleResultsResponse = vehicleEntrantsService
          .createVehicleResultsResponse(prepareVehicleEntrantsDto(),
              SOME_CLEAN_AIR_ZONE_ID);

      // then
      VehicleResultDto vehicleResultDto = vehicleResultsResponse.get(0);
      verifyVehicle(vehicleResultDto)
          .hasStatus("exempt")
          .hasExemptionCode("WDC001")
          .tariffCodeIsNull()
          .isNotTaxiOrPhv();
      verifyNoInteractions(complianceService);
      verifyNoInteractions(chargeabilityService);
      verify(vehicleEntrantsPaymentsDataSupplier)
          .processPaymentRelatedEntrants(argThat(List::isEmpty));
    }
  }

  @Nested
  class WhenVehicleHasNonUkVrn {

    @Test
    public void shouldReturnVehicleResultsWithUnrecognisedNotPaid() {
      // given
      mockPayments(SOME_CLEAN_AIR_ZONE_ID, PaymentStatus.NOT_PAID, PaymentMethod.NULL);
      mockPrefetchedDataResults();

      // when
      List<VehicleResultDto> vehicleResultsResponse = vehicleEntrantsService
          .createVehicleResultsResponse(
              Lists.newArrayList(new VehicleEntrantSaveDto(NON_UK_VRN, SOME_DATETIME)),
              SOME_CLEAN_AIR_ZONE_ID);

      // then
      firstResult = vehicleResultsResponse.get(0);
      shouldHaveStatusAndTariffCode("unrecognisedNotPaid", null);
      shouldHavePaymentMethod(null);
      shouldCallOnlyPaymentsService();
    }

    @Test
    public void shouldReturnVehicleResultsWithUnrecognisedPaid() {
      // given
      mockPayments(SOME_CLEAN_AIR_ZONE_ID, PaymentStatus.PAID, PaymentMethod.CARD);
      mockPrefetchedDataResults();

      // when
      List<VehicleResultDto> vehicleResultsResponse = vehicleEntrantsService
          .createVehicleResultsResponse(
              Lists.newArrayList(new VehicleEntrantSaveDto(NON_UK_VRN, SOME_DATETIME)),
              SOME_CLEAN_AIR_ZONE_ID);

      // then
      firstResult = vehicleResultsResponse.get(0);

      shouldHaveStatusAndTariffCode("unrecognisedPaid", "PAID_TARIFF_CODE");
      shouldHavePaymentMethod("card");
      shouldCallOnlyPaymentsService();
    }
  }

  @Nested
  class WhenVehicleIsOnGeneralPurposeWhitelist {

    @Nested
    class AndIsCategorizedAsOther {

      @Nested
      class AndHasUkVrnFormat {

        @Test
        public void itShouldReturnExemptStatusWithoutDvlaData() {
          // given
          given(militaryVehicleService.isMilitaryVrn(anyString(), any(PreFetchedDataResults.class)))
              .willReturn(false);
          given(generalWhitelistService
              .isOnGeneralPurposedWhitelistAndHasCategoryOther(anyString(), any()))
              .willReturn(true);
          mockGpwl("Other", false, true, false, SOME_VRN);
          given(licenseAndVehicleProvider.findLicenseAndVehicle(SOME_VRN))
              .willReturn(new NtrAndDvlaVehicleData(
                      SingleDvlaVehicleData.failure(HttpStatus.NOT_FOUND, SOME_FAILURE_MESSAGE),
                      SingleLicenceData.failure(HttpStatus.NOT_FOUND, SOME_FAILURE_MESSAGE)
                  )
              );

          // when
          List<VehicleResultDto> vehicleResultsResponse =
              vehicleEntrantsService.createVehicleResultsResponse(
                  prepareVehicleEntrantsDto(), SOME_CLEAN_AIR_ZONE_ID);

          // then
          VehicleResultDto vehicleResultDto = vehicleResultsResponse.get(0);
          assertThat(vehicleResultDto)
              .hasVrn(SOME_VRN)
              .hasStatus("exempt")
              .isNotTaxiOrPhv()
              .hasExemptionCode(NATIONAL_EXEMPTION_CODE);
        }
      }

      @Nested
      class AndHasNonUkVrnFormat {

        @Test
        public void itShouldReturnExemptStatusWithoutDvlaData() {
          // given
          given(militaryVehicleService.isMilitaryVrn(anyString(), any(PreFetchedDataResults.class)))
              .willReturn(false);
          given(generalWhitelistService
              .isOnGeneralPurposedWhitelistAndHasCategoryOther(anyString(), any()))
              .willReturn(true);
          given(generalWhitelistService
              .isOnGeneralPurposeWhitelistAndHasCategoryNonUk(anyString(), any()))
              .willReturn(false);
          mockGpwl("Other", false, true, false, NON_UK_VRN);

          // when
          List<VehicleResultDto> vehicleResultsResponse =
              vehicleEntrantsService.createVehicleResultsResponse(
                  prepareVehicleEntrantsDtoWithNonUkVrn(), SOME_CLEAN_AIR_ZONE_ID);

          // then
          VehicleResultDto vehicleResultDto = vehicleResultsResponse.get(0);
          assertThat(vehicleResultDto)
              .hasVrn(NON_UK_VRN)
              .hasStatus("exempt")
              .isNotTaxiOrPhv()
              .hasExemptionCode(NATIONAL_EXEMPTION_CODE);
        }
      }
    }

    @Nested
    class AndIsCategorizedAsNonUkVehicle {

      @Nested
      class AndIsOnMod {

        @Test
        public void itShouldCallDvlaAndPaymentsAndReturnExemptStatus() {
          // given - note the list returned for filtered MOD VRNs included the same as that submitted
          given(
              militaryVehicleService.isMilitaryVrn(eq(SOME_VRN), any(PreFetchedDataResults.class)))
              .willReturn(true);

          mockGpwl("Non-UK Vehicle", true, false, true, SOME_VRN);
          mockLicenseAndVehicleResponse(VehicleType.PRIVATE_CAR);

          // when
          List<VehicleResultDto> vehicleResultsResponse =
              vehicleEntrantsService.createVehicleResultsResponse(
                  prepareVehicleEntrantsDto(), SOME_CLEAN_AIR_ZONE_ID);

          // then
          VehicleResultDto vehicleResultDto = vehicleResultsResponse.get(0);
          verifyVehicle(vehicleResultDto)
              .hasStatus("exempt")
              .hasExemptionCode("WDC001")
              .hasMake("volkswagen")
              .tariffCodeIsNull();
        }
      }

      @Nested
      class AndIsNotOnMod {

        @Test
        public void itShouldNotCallDvlaNorPaymentsAndReturnCompliantStatus() {
          // given
          mockGpwl("Non-UK Vehicle", true, false, true, SOME_VRN);

          // when
          List<VehicleResultDto> vehicleResultsResponse =
              vehicleEntrantsService.createVehicleResultsResponse(
                  prepareVehicleEntrantsDto(), SOME_CLEAN_AIR_ZONE_ID);

          // then
          thenHasNoDvlaDataAndPaymentsWereNotCalledAndHasStatus(vehicleResultsResponse,
              "compliant");
        }
      }
    }

    @Nested
    class AndIsCategorizedAsProblematicVrn {

      @Test
      public void itShouldNotCallDvlaNorPaymentsAndReturnExemptStatus() {
        // given
        mockGpwlWithProblematicVrn("Problematic VRN", false, true, false, SOME_VRN);

        // when
        List<VehicleResultDto> vehicleResultsResponse =
            vehicleEntrantsService.createVehicleResultsResponse(
                prepareVehicleEntrantsDto(), SOME_CLEAN_AIR_ZONE_ID);

        // then
        thenHasNoDvlaDataAndPaymentsWereNotCalledAndHasStatus(vehicleResultsResponse, "exempt");
      }
    }

    private void mockGpwl(String category, boolean compliant, boolean exempt, boolean nonUk,
        String vrn) {
      List<GeneralWhitelistVehicle> mockedResult = prepareMockGpwlResults(category, compliant,
          exempt, vrn);


      given(generalWhitelistService
          .isOnGeneralPurposeWhitelistAndHasCategoryNonUk(vrn,
              mockedResult)).willReturn(nonUk);
    }

    private void mockGpwlWithProblematicVrn(String category, boolean compliant, boolean exempt, boolean nonUk,
        String vrn) {
      List<GeneralWhitelistVehicle> mockedResult = prepareMockGpwlResults(category, compliant,
          exempt, vrn);

      given(generalWhitelistService
          .isOnGeneralPurposeWhitelistAndHasCategoryProblematicVrn(SOME_VRN, mockedResult))
          .willReturn(true);

      given(generalWhitelistService
          .isOnGeneralPurposeWhitelistAndHasCategoryNonUk(vrn,
              mockedResult)).willReturn(nonUk);

    }

    private List<GeneralWhitelistVehicle> prepareMockGpwlResults(String category, boolean compliant,
        boolean exempt, String vrn) {
      GeneralWhitelistVehicle generalWhitelistVehicle = GeneralWhitelistVehicle.builder()
          .category(category).compliant(compliant)
          .exempt(exempt).vrn(vrn).build();

      List<GeneralWhitelistVehicle> matchedGeneralWhitelistVehicles = Lists
          .newArrayList(generalWhitelistVehicle);
      List<RetrofittedVehicle> matchedRetrofittedVehicles = Lists.emptyList();
      Set<String> matchedMilitaryVrns = Collections.emptySet();

      PreFetchedDataResults prefetchedResults =
          new PreFetchedDataResults(matchedGeneralWhitelistVehicles, matchedRetrofittedVehicles,
              matchedMilitaryVrns);

      given(chargeCalculationService.computePrefetchedData(Mockito.anySet()))
          .willReturn(prefetchedResults);

      return Lists.newArrayList(generalWhitelistVehicle);
    }

    private void thenHasNoDvlaDataAndPaymentsWereNotCalledAndHasStatus(
        List<VehicleResultDto> vehicleResultsResponse, String status) {
      VehicleResultDto vehicleResultDto = vehicleResultsResponse.get(0);
      assertThat(vehicleResultDto)
          .hasStatus(status)
          .colourIsNull()
          .modelIsNull()
          .typeApprovalIsNull()
          .makeIsNull()
          .hasExemptionCode(status.equals("exempt") ? "WDC001" : null)
          .hasPaymentMethod(null);

      verify(vehicleEntrantsPaymentsDataSupplier)
          .processPaymentRelatedEntrants(argThat(List::isEmpty));
    }

    // A comment: all other statuses are currently handled by vehicle being either "compliant" or
    // "exempt". The rules can be found in CategoryType.java in Whitelist-API project. And of course
    // in VehicleEntrantsService itself.
  }

  @Nested
  class EdgeCasesAndStrangeInput {

    @Test
    public void shouldReturnEmptyList() {
      // given

      // when
      List<VehicleResultDto> vehicleResultsResponse = vehicleEntrantsService
          .createVehicleResultsResponse(Lists.emptyList(),
              SOME_CLEAN_AIR_ZONE_ID);

      // then
      assertThat(vehicleResultsResponse).hasSize(0);
    }
  }

  @Nested
  class HelperMethods {

    @Test
    public void shouldMapDtoToModelObject() {
      // given
      UUID cazId = UUID.randomUUID();
      String vrn = "vrn123";
      String correlationId = UUID.randomUUID().toString();
      String time = "2017-10-01T155301Z";
      LocalDateTime localDateTime = LocalDateTime.parse(time,
          DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HHmmssX"));
      VehicleEntrantSaveDto vehicleEntrantSaveDto = new VehicleEntrantSaveDto(vrn, localDateTime);
      VehicleEntrantsSaveRequestDto vehicleEntrantsSaveRequestDto = new VehicleEntrantsSaveRequestDto(
          cazId, correlationId, Collections.singletonList(vehicleEntrantSaveDto));
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
  }

  private void mockSuccessWithNotCompliantPaidStatus() {
    mockLicenseAndVehicleResponse(VehicleType.PRIVATE_CAR);
    mockTariffDetails(Optional.of(prepareTariffDetails()));
    mockExemptAndCompliantStatus(prepareCalculationResult(false, false, true, 40, false));
    mockChargeCalculation((float) 12.5);
    mockPaymentServiceWithStatusPaid();
  }

  private void mockSuccess(VehicleType vehicleType, CalculationResult result) {
    mockTariffDetails(Optional.of(prepareTariffDetails()));
    mockExemptAndCompliantStatus(result);
    mockLicenseAndVehicleResponse(vehicleType);
    mockChargeCalculation(result.getCharge());
  }

  private void mockExemptAndCompliantStatus(CalculationResult result) {
    mockIsExempt(result);
    mockIsCompliant(result);
  }

  private void mockLicenseAndVehicleResponse(VehicleType vehicleType) {
    given(licenseAndVehicleProvider.findLicenseAndVehicle(SOME_VRN))
        .willReturn(new NtrAndDvlaVehicleData(
                SingleDvlaVehicleData.success(prepareVehicle(vehicleType)),
                SingleLicenceData
                    .success(prepareLicenseInfoResponse(true, prepareLicensingAuthoritiesNames(),
                        LocalDate.now().plusDays(1)))
            )
        );
  }

  private void mockExpiredLicenseAndVehicleResponse(VehicleType vehicleType) {
    given(licenseAndVehicleProvider.findLicenseAndVehicle(SOME_VRN))
        .willReturn(new NtrAndDvlaVehicleData(
                SingleDvlaVehicleData.success(prepareVehicle(vehicleType)),
                SingleLicenceData
                    .success(prepareLicenseInfoResponse(true, prepareLicensingAuthoritiesNames(),
                        LocalDate.now().minusDays(1)))
            )
        );
  }

  private void mockLicenseAndVehicleResponseFor2Vrns() {
    given(licenseAndVehicleProvider.findLicenseAndVehicle(SOME_VRN))
        .willReturn(new NtrAndDvlaVehicleData(
                SingleDvlaVehicleData.success(prepareVehicle(VehicleType.PRIVATE_CAR)),
                SingleLicenceData
                    .success(prepareLicenseInfoResponse(true, prepareLicensingAuthoritiesNames(),
                        LocalDate.now().plusDays(1)))
            )
        )
        .willReturn(new NtrAndDvlaVehicleData(
                SingleDvlaVehicleData.failure(HttpStatus.NOT_FOUND, SOME_FAILURE_MESSAGE),
                SingleLicenceData
                    .success(prepareLicenseInfoResponse(true, prepareLicensingAuthoritiesNames(),
                        LocalDate.now().plusDays(1)))
            )
        );
  }

  private void mockIsExempt(CalculationResult result) {
    given(exemptionService.updateCalculationResult(any(Vehicle.class),
        any(CalculationResult.class), anyBoolean(), anyBoolean())).willReturn(result);
  }

  private void mockIsCompliant(CalculationResult result) {
    given(complianceService.updateCalculationResult(any(Vehicle.class),
        any(CalculationResult.class), anyBoolean(), anyBoolean())).willReturn(result);
  }

  private void mockComplianceServiceException() {
    given(complianceService.updateCalculationResult(any(Vehicle.class),
        any(CalculationResult.class), anyBoolean(), anyBoolean())).willThrow(new UnableToIdentifyVehicleComplianceException("Unable to determine compliance"));
  }

  private void mockChargeCalculation(float charge) {
    given(chargeabilityService.getCharge(
        any(Vehicle.class), any(TariffDetails.class))).willReturn(charge);
  }

  private void mockTariffDetails(Optional<TariffDetails> tariffDetails) {
    given(tariffDetailsRepository.getTariffDetails(any(UUID.class)))
        .willReturn(tariffDetails);
  }

  private void mockPaymentServiceWithStatusPaid() {
    mockPayments(SOME_CLEAN_AIR_ZONE_ID, PaymentStatus.PAID, PaymentMethod.CARD);
  }

  private void mockPayments(UUID cleanAirZoneId, PaymentStatus paymentStatus,
      PaymentMethod paymentMethod) {
    given(vehicleEntrantsPaymentsDataSupplier.processPaymentRelatedEntrants(any())).willAnswer(
        (Answer<List<InitialVehicleResult>>) invocationOnMock -> {
          List<InitialVehicleResult> argument = invocationOnMock.getArgument(0);
          return argument.stream()
              .map(e -> {
                VehicleResultDto result = e.getResult();
                VehicleComplianceStatus complianceStatus = e.getComplianceStatusProvider()
                    .apply(paymentStatus);
                return e.toBuilder()
                    .cleanAirZoneId(cleanAirZoneId)
                    .result(
                        result.toBuilder()
                            .status(complianceStatus.getStatus())
                            .paymentMethod(paymentMethod.toDtoString())
                            .tariffCode(mockTariffCode(complianceStatus, e.getTariffCodeBuilder(),
                                e.getResult()))
                            .build())
                    .build();
              })
              .collect(Collectors.toList());
        });
  }

  private String mockTariffCode(VehicleComplianceStatus complianceStatus,
      Function<VehicleComplianceStatus, String> tariffCodeBuilder, VehicleResultDto result) {

    if (complianceStatus.equals(VehicleComplianceStatus.UNRECOGNISED_PAID)) {
      return "PAID_TARIFF_CODE";
    } else {
      return tariffCodeBuilder == null
          ? result.getTariffCode()
          : tariffCodeBuilder.apply(complianceStatus);
    }
  }

  private VehicleResultDtoAssert verifyVehicle(
      VehicleResultDto vehicleResultDto) {
    return assertThat(vehicleResultDto).hasVrn(SOME_VRN).hasColour("black")
        .hasMake("volkswagen").hasModel("golf").hasTypeApproval("m1");
  }

  private static Stream<Arguments> vehicleTypesAndTariffCode() {
    return Stream.of(Arguments.of(VehicleType.PRIVATE_CAR, "C0001-TAXI"),
        Arguments.of(VehicleType.MINIBUS, "C0001-TAXI"),
        Arguments.of(VehicleType.VAN, "C0001-TAXI"),
        Arguments.of(VehicleType.MOTORCYCLE, "C0001-TAXI"));
  }

  private List<VehicleEntrantSaveDto> prepareVehicleEntrantsDto() {
    VehicleEntrantSaveDto vehicleEntrantSaveDto = new VehicleEntrantSaveDto(SOME_VRN,
        SOME_DATETIME);
    return Lists.newArrayList(vehicleEntrantSaveDto);
  }

  private List<VehicleEntrantSaveDto> prepareVehicleEntrantsDtoWithNonUkVrn() {
    VehicleEntrantSaveDto vehicleEntrantSaveDto = new VehicleEntrantSaveDto(NON_UK_VRN,
        SOME_DATETIME);
    return Lists.newArrayList(vehicleEntrantSaveDto);
  }

  private ArrayList<String> prepareLicensingAuthoritiesNames() {
    return Lists.newArrayList("la1", "la2");
  }

  private TaxiPhvLicenseInformationResponse prepareLicenseInfoResponse(boolean active,
      ArrayList<String> licensingAuthoritiesNames, LocalDate licensedStatusExpires) {
    return TaxiPhvLicenseInformationResponse.builder()
        .licensingAuthoritiesNames(licensingAuthoritiesNames)
        .active(active)
        .licensedStatusExpires(licensedStatusExpires)
        .build();
  }

  private TariffDetails prepareTariffDetails() {
    TariffDetails tariffDetails = new TariffDetails();
    tariffDetails.setChargeIdentifier("C0001");
    return tariffDetails;
  }

  private CalculationResult prepareCalculationResult(boolean exempt,
      boolean compliant, boolean chargeable, float charge, boolean isRetrofitted) {
    CalculationResult result = new CalculationResult();
    result.setExempt(exempt);
    result.setCompliant(compliant);
    result.setChargeable(chargeable);
    result.setCharge(charge);
    result.setCazIdentifier(SOME_CLEAN_AIR_ZONE_ID);
    result.setIsRetrofitted(isRetrofitted);
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

  private void shouldHaveStatusAndTariffCode(String status, String tariffCode) {
    assertThat(firstResult)
        .hasVrn("SW61BYDBE")
        .hasStatus(status)
        .colourIsNull()
        .makeIsNull()
        .modelIsNull()
        .typeApprovalIsNull()
        .exemptionCodeIsNull()
        .hasTariffCode(tariffCode)
        .isNotTaxiOrPhv()
        .licensingAuthorityIsNull();
  }

  private void shouldHavePaymentMethod(String expectedPaymentMethod) {
    assertThat(firstResult).hasPaymentMethod(expectedPaymentMethod);
  }

  private void shouldCallOnlyPaymentsService() {
    verify(vehicleEntrantsPaymentsDataSupplier, times(1)).processPaymentRelatedEntrants(anyList());
    verify(exemptionService, times(0)).updateCalculationResult(
        any(Vehicle.class), any(CalculationResult.class));
    verify(complianceService, times(0)).updateCalculationResult(
        any(Vehicle.class), any(CalculationResult.class));
    verify(chargeabilityService, times(0)).getCharge(
        any(Vehicle.class), any(TariffDetails.class));
  }
}