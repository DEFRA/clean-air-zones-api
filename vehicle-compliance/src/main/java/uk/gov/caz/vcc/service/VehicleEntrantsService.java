package uk.gov.caz.vcc.service;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import uk.gov.caz.async.rest.AsyncResponse;
import uk.gov.caz.vcc.domain.CalculationResult;
import uk.gov.caz.vcc.domain.ChargeValidity;
import uk.gov.caz.vcc.domain.CleanAirZoneEntrant;
import uk.gov.caz.vcc.domain.MilitaryVehicle;
import uk.gov.caz.vcc.domain.RetrofittedVehicle;
import uk.gov.caz.vcc.domain.TariffDetails;
import uk.gov.caz.vcc.domain.Vehicle;
import uk.gov.caz.vcc.domain.VehicleType;
import uk.gov.caz.vcc.domain.exceptions.ExternalServiceCallException;
import uk.gov.caz.vcc.domain.service.ChargeabilityService;
import uk.gov.caz.vcc.domain.service.ComplianceService;
import uk.gov.caz.vcc.domain.service.UkVrnTestingService;
import uk.gov.caz.vcc.domain.service.VehicleIdentificationService;
import uk.gov.caz.vcc.dto.PaymentStatus;
import uk.gov.caz.vcc.dto.PaymentStatusRequestDto;
import uk.gov.caz.vcc.dto.TaxiPhvLicenseInformationResponse;
import uk.gov.caz.vcc.dto.VehicleComplianceStatus;
import uk.gov.caz.vcc.dto.VehicleEntrantDto;
import uk.gov.caz.vcc.dto.VehicleEntrantsSaveRequestDto;
import uk.gov.caz.vcc.dto.VehicleResultDto;
import uk.gov.caz.vcc.repository.CleanAirZoneEntrantRepository;
import uk.gov.caz.vcc.repository.LicenseAndVehicleRepository;
import uk.gov.caz.vcc.repository.LicenseAndVehicleRepository.LicenseAndVehicleResponse;
import uk.gov.caz.vcc.repository.TariffDetailsRepository;
import uk.gov.caz.vcc.util.VehicleApiAuthenticationUtility;

/**
 * Service that saves combines VehicleEntrantsSaveRequestDto with calculation results and persists
 * data to the DB.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleEntrantsService {

  private final CleanAirZoneEntrantRepository cleanAirZoneEntrantRepository;

  private final TariffDetailsRepository tariffDetailsRepository;
  
  private final ChargeabilityService chargeabilityService;

  private final ComplianceService complianceService;

  private final ExemptionService exemptionService;

  private final VehicleIdentificationService vehicleIdentificationService;

  private final RetrofitService retrofitService;

  private final MilitaryVehicleService militaryVehicleService;

  private final PaymentsService paymentsService;

  private final LicenseAndVehicleRepository licenseAndVehicleRepository;

  private final VehicleApiAuthenticationUtility remoteAuthenticationTokenGenerator;

  @VisibleForTesting
  public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter
      .ofPattern("yyyy-MM-dd'T'HHmmssX");

  @VisibleForTesting
  static final ImmutableMap<String, ChargeValidity> STATUSES_TO_CHARGE_VALIDITY_CODES = ImmutableMap
      .<String, ChargeValidity>builder()
      .put("notCompliantPaid", new ChargeValidity("CVC01"))
      .put("notCompliantNotPaid", new ChargeValidity("CVC01"))
      .put("exempt", new ChargeValidity("CVC02"))
      .put("compliant", new ChargeValidity("CVC03"))
      .put("unrecognisedPaid", new ChargeValidity("CVC04"))
      .put("unrecognisedNotPaid", new ChargeValidity("CVC04"))
      .build();

  @Value("${services.payments.enabled}")
  private boolean paymentsEnabled;

  /**
   * Main method that handles business logic related to saving Vehicle Entrants and calculating
   * charge.
   *
   * @param vehicleEntrantsSaveRequestDto payload from the API
   * @return list of VehicleResultDto
   */
  @Transactional
  public List<VehicleResultDto> save(
      VehicleEntrantsSaveRequestDto vehicleEntrantsSaveRequestDto) {

    List<VehicleResultDto> vehicleResultsResponse = createVehicleResultsResponse(
        vehicleEntrantsSaveRequestDto.getVehicleEntrants(),
        vehicleEntrantsSaveRequestDto.getCazId(),
        vehicleEntrantsSaveRequestDto.getCorrelationId());

    saveVehicleEntrantsToDb(vehicleEntrantsSaveRequestDto, vehicleResultsResponse);

    return vehicleResultsResponse;
  }

  /*
   * Saves vehicle entrants to the DB.
   */
  private void saveVehicleEntrantsToDb(VehicleEntrantsSaveRequestDto vehicleEntrantsSaveRequestDto,
      List<VehicleResultDto> vehicleResultsResponse) {
    List<CleanAirZoneEntrant> cleanAirZoneEntrants = vehicleResultsResponse
        .stream()
        .map(e -> toModel(vehicleEntrantsSaveRequestDto, e))
        .collect(Collectors.toList());

    cleanAirZoneEntrantRepository.saveAll(cleanAirZoneEntrants);
  }

  /**
   * Method that maps DTO passed by upper layers to model class understandable by JPA.
   */
  @VisibleForTesting
  CleanAirZoneEntrant toModel(VehicleEntrantsSaveRequestDto vehicleEntrantsSaveRequestDto,
      VehicleResultDto vehicleEntrantDto) {

    VehicleEntrantDto vehicleEntrant = vehicleEntrantsSaveRequestDto.getVehicleEntrants()
        .stream()
        .filter(e -> e.getVrn().equals(vehicleEntrantDto.getVrn()))
        .findFirst()
        .orElseThrow(() -> new RuntimeException(
            "Cannot fetch vehicle from request, vrn: " + vehicleEntrantDto.getVrn()));

    CleanAirZoneEntrant cleanAirZoneEntrant = new CleanAirZoneEntrant(
        vehicleEntrantsSaveRequestDto.getCazId(),
        vehicleEntrantsSaveRequestDto.getCorrelationId(),
        parseDate(vehicleEntrant)
    );

    cleanAirZoneEntrant.setVrn(vehicleEntrantDto.getVrn());
    cleanAirZoneEntrant.setChargeValidityCode(getChargeValidity(vehicleEntrantDto));

    return cleanAirZoneEntrant;
  }

  /*
   * Maps vehicle status to corresponding Charge Validity code from DB.
   */
  private ChargeValidity getChargeValidity(VehicleResultDto vehicleEntrantDto) {
    String status = vehicleEntrantDto.getStatus();

    return Optional
        .ofNullable(STATUSES_TO_CHARGE_VALIDITY_CODES.get(status))
        .orElseThrow(
            () -> new RuntimeException("Cannot fetch validity code for status: " + status));
  }

  /**
   * Method that parses date that is sent as string by the API caller.
   */
  @VisibleForTesting
  LocalDateTime parseDate(VehicleEntrantDto vehicleEntrantDto) {
    return LocalDateTime.parse(vehicleEntrantDto.getTimestamp(), DATE_TIME_FORMATTER);
  }

  /**
   * Method for preparing response for ANPR.
   *
   * @param vehicleEntrants list of vehicle entrants
   * @param cleanAirZoneId clean air zone id
   * @param correlationId
   * @return {@link List} of {@link VehicleResultDto}
   */
  @VisibleForTesting
  public List<VehicleResultDto> createVehicleResultsResponse(
      List<VehicleEntrantDto> vehicleEntrants,
      UUID cleanAirZoneId, String correlationId) {
    List<VehicleResultDto> vehicleResults = Lists.newArrayList();
    String authenticationToken = remoteAuthenticationTokenGenerator.getAuthenticationToken();

    for (VehicleEntrantDto vehicleEntrant : vehicleEntrants) {
      String vrn = vehicleEntrant.getVrn();

      // If military yield early exemption
      if (militaryVehicleService.isMilitaryVehicle(vrn)) {
        VehicleResultDto result = VehicleResultDto.builder()
            .vrn(vrn)
            .status(VehicleComplianceStatus.EXEMPT.getStatus())
            .isTaxiOrPhv(false)
            .exemptionCode(getModDiscountCode(vehicleEntrant))
            .build();
        vehicleResults.add(result);
        continue;
      }

      // If entrant is non-uk, should call only payments for payment status.
      if (!UkVrnTestingService.isPotentialUkVrn(vrn)) {
        log.info("Vehicle deemed non-UK found when preparing response for ANPR for vrn {}", vrn);
        VehicleResultDto result = buildResultWithUnrecognisedStatus(vehicleEntrant, cleanAirZoneId);
        vehicleResults.add(result);
        continue;
      }

      LicenseAndVehicleResponse licenseAndVehicle = licenseAndVehicleRepository
          .findLicenseAndVehicle(vrn, authenticationToken, correlationId);

      AsyncResponse<Vehicle> vehicleAsyncResponse = licenseAndVehicle.getVehicles().get(vrn);
      validateResponse(vehicleAsyncResponse);
      if (vehicleAsyncResponse.hasError()) {
        log.info("Vehicle not found when preparing response for ANPR for vrn {}", vrn);
        VehicleResultDto result = buildResultWithUnrecognisedStatus(vehicleEntrant, cleanAirZoneId);
        vehicleResults.add(result);
        continue;
      }

      AsyncResponse<TaxiPhvLicenseInformationResponse> licenseInfo = licenseAndVehicle
          .getLicensInfos().get(vrn);
      validateResponse(licenseInfo);

      Vehicle vehicle = setVehicleTypeAndTaxiOrPhv(vehicleAsyncResponse.getBody(), licenseInfo);

      CalculationResult calculationResult = prepareCalculationResult(cleanAirZoneId, vehicle);

      String exemptionCode = prepareExemptionCode(vehicleEntrant, calculationResult);

      Optional<TariffDetails> tariff = getTariffDetails(cleanAirZoneId);

      if (!tariff.isPresent()) {
        log.info(
            "Tariff not found when preparing response for ANPR for vrn {}, and cleanAirZoneId {}",
            vrn, cleanAirZoneId);
        VehicleResultDto result = buildResultWithoutTariffCode(vehicleEntrant, cleanAirZoneId,
            vehicle, calculationResult, exemptionCode, licenseInfo);
        vehicleResults.add(result);
        continue;
      }

      TariffDetails tariffDetails = tariff.get();

      setCharge(vehicle, calculationResult, tariffDetails);

      VehicleComplianceStatus status = prepareVehicleCompliantStatus(calculationResult,
          vehicleEntrant, cleanAirZoneId);

      VehicleResultDto result = buildResult(vehicleEntrant, vehicle, exemptionCode,
          tariffDetails, status, licenseInfo);
      vehicleResults.add(result);
    }
    return vehicleResults;
  }

  /**
   * Helper method to validate response.
   *
   * @param vehicleAsyncResponse {@link AsyncResponse}
   */
  private void validateResponse(AsyncResponse<?> vehicleAsyncResponse) {
    if (vehicleAsyncResponse.hasError()) {
      mapStatusCode(vehicleAsyncResponse);
    }
  }

  /**
   * Helper method to properly act on status code.
   *
   * @param vehicleAsyncResponse {@link AsyncResponse}
   */
  private void mapStatusCode(AsyncResponse<?> vehicleAsyncResponse) {
    if (vehicleAsyncResponse.getCode() == HttpStatus.NOT_FOUND) {
      log.info("Requested object not found");
    } else {
      log.info(
          "AsyncResponse has error {} with code {}", 
          vehicleAsyncResponse.getBody(), 
          vehicleAsyncResponse.getCode());
      throw new ExternalServiceCallException();
    }
  }

  /**
   * Method to map {@link VehicleEntrantDto} to {@link PaymentStatusRequestDto}.
   *
   * @param cleanAirZoneId The identifiers of the clean air zones
   * @param vehicleEntrant Wrapper class for single Vehicle Entrant request object
   * @return {@link PaymentStatusRequestDto}
   */
  private PaymentStatusRequestDto mapToPaymentStatusRequest(UUID cleanAirZoneId,
      VehicleEntrantDto vehicleEntrant) {
    return PaymentStatusRequestDto.builder()
        .vrn(vehicleEntrant.getVrn())
        .cleanZoneId(cleanAirZoneId)
        .cazEntryTimestamp(parseDate(vehicleEntrant))
        .build();
  }

  /**
   * Method to create {@link CalculationResult}.
   *
   * @param cleanAirZoneId Clean Air Zone identifier
   * @param vehicle from dvla
   * @return {@link CalculationResult}
   */
  private CalculationResult prepareCalculationResult(UUID cleanAirZoneId, Vehicle vehicle) {
    CalculationResult calculationResult = updateExempt(vehicle);
    calculationResult.setCazIdentifier(cleanAirZoneId);
    calculationResult = updateCompliant(vehicle, calculationResult);
    calculationResult.setCharge(0);
    return calculationResult;
  }

  /**
   * Method to create full result.
   *
   * @param vehicleEntrant single request
   * @param vehicle information from DVLA
   * @param exemptionCode code from mod or retrofit repository
   * @param tariffDetails information from tariff service
   * @param status Vehicle Compliant Status
   * @param licenseInfo information from NTR
   * @return full result
   */
  private VehicleResultDto buildResult(VehicleEntrantDto vehicleEntrant, Vehicle vehicle,
      String exemptionCode, TariffDetails tariffDetails, VehicleComplianceStatus status,
      AsyncResponse<TaxiPhvLicenseInformationResponse> licenseInfo) {
    return VehicleResultDto.builder()
        .vrn(vehicleEntrant.getVrn())
        .make(vehicle.getMake())
        .model(vehicle.getModel())
        .colour(vehicle.getColour())
        .typeApproval(vehicle.getTypeApproval())
        .status(status.getStatus())
        .exemptionCode(exemptionCode)
        .tariffCode(prepareTariffCode(vehicle.getVehicleType(),
            tariffDetails.getChargeIdentifier(), status))
        .isTaxiOrPhv(getTaxiLicenseStatus(licenseInfo))
        .licensingAuthority(getLicensingAuthority(licenseInfo))
        .build();
  }

  /**
   * Method to create result when vrn is not recognized by DVLA.
   *
   * @param vehicleEntrant single request from ANPR
   * @param cleanAirZoneId clean air zone
   * @return result with vrn and status
   */
  private VehicleResultDto buildResultWithUnrecognisedStatus(
      VehicleEntrantDto vehicleEntrant, UUID cleanAirZoneId) {
    PaymentStatus paymentStatus = registerVehicleEntryAndGetPaymentStatus(cleanAirZoneId,
        vehicleEntrant);
    VehicleComplianceStatus status = isPaymentStatusPaid(paymentStatus)
        ? VehicleComplianceStatus.UNRECOGNISED_PAID : VehicleComplianceStatus.UNRECOGNISED_NOT_PAID;
    return VehicleResultDto.builder()
        .vrn(vehicleEntrant.getVrn())
        .status(status.getStatus())
        .build();
  }

  /**
   * Method to create result with out tariff code.
   *
   * @param vehicleEntrant single request from ANPR
   * @param cleanAirZoneId clean air zone
   * @param vehicle information from DVLA
   * @param calculationResult information about exempt, compliant, chargeable
   * @param exemptionCode code from mod or retrofit repository
   * @return result with out tariff code
   */
  private VehicleResultDto buildResultWithoutTariffCode(
      VehicleEntrantDto vehicleEntrant, UUID cleanAirZoneId,
      Vehicle vehicle, CalculationResult calculationResult, String exemptionCode,
      AsyncResponse<TaxiPhvLicenseInformationResponse> licenseInfo) {
    VehicleComplianceStatus status = prepareVehicleCompliantStatus(calculationResult,
        vehicleEntrant, cleanAirZoneId);
    return VehicleResultDto.builder()
        .vrn(vehicleEntrant.getVrn())
        .make(vehicle.getMake())
        .model(vehicle.getModel())
        .colour(vehicle.getColour())
        .typeApproval(vehicle.getTypeApproval())
        .status(status.getStatus())
        .exemptionCode(exemptionCode)
        .isTaxiOrPhv(vehicle.getIsTaxiOrPhv())
        .tariffCode(null)
        .licensingAuthority(getLicensingAuthority(licenseInfo))
        .build();
  }

  /**
   * Helper method to set charge.
   *
   * @param vehicle {@link Vehicle} vehicle with information from DVLA
   * @param calculationResult {@link CalculationResult}
   * @param tariffDetails {@link TariffDetails} tariff information from tariff service
   */
  private void setCharge(Vehicle vehicle, CalculationResult calculationResult,
      TariffDetails tariffDetails) {
    float charge = chargeabilityService.getCharge(vehicle, tariffDetails);
    calculationResult.setCharge(charge);
  }

  /**
   * Helper method to set vehicleType.
   */
  private Vehicle setVehicleTypeAndTaxiOrPhv(Vehicle vehicle,
      AsyncResponse<TaxiPhvLicenseInformationResponse> licenseInfo) {
    vehicleIdentificationService.setVehicleType(vehicle);
    vehicle.setIsTaxiOrPhv(getTaxiLicenseStatus(licenseInfo));
    return vehicle;
  }

  /**
   * Helper method to get tariff details.
   */
  private Optional<TariffDetails> getTariffDetails(UUID cleanAirZoneId) {
    return tariffDetailsRepository.getTariffDetails(cleanAirZoneId);
  }

  /**
   * Helper method to check if the vehicle is compliant.
   */
  private CalculationResult updateCompliant(Vehicle vehicle,
      CalculationResult calculationResult) {
    // checking compliant status
    return complianceService.updateCalculationResult(vehicle, calculationResult);
  }

  /**
   * Helper method to check if the vehicle is exempt.
   */
  private CalculationResult updateExempt(Vehicle vehicle) {
    // checking exempt status
    return exemptionService.updateCalculationResult(vehicle, new CalculationResult());
  }

  /**
   * tariffCode - A code that uniquely identifies the tariff that determines the charge that the
   * vehicle is liable to pay. Will always be provided when status is equal to: (notCompliantPaid,
   * notCompliantNotPaid, unrecognisedPaid, unrecognisedNotPaid). A null value will be provided
   * otherwise.
   */
  private String prepareTariffCode(VehicleType vehicleType, String chargeIdentifier,
      VehicleComplianceStatus status) {
    // TODO At the moment there is not possible to create tariffCode for unrecognisedPaid
    //  and unrecognisedNotPaid status

    if (status == VehicleComplianceStatus.NOT_COMPLIANT_NOT_PAID
        || status == VehicleComplianceStatus.NOT_COMPLIANT_PAID) {
      return chargeIdentifier + "-" + vehicleType.toString().toUpperCase();
    }
    return null;
  }

  /**
   * ExemptionCode - A code that uniquely identifies the national exemption on which the exempt
   * status is based. Will always be provided when status is equal to ‘exempt’. A null value will be
   * provided otherwise.
   *
   * @param vehicleEntrant single request dto
   * @param calculationResult when exempt is true then return discount code, null otherwise
   * @return whitelistDiscountCode
   */
  private String prepareExemptionCode(VehicleEntrantDto vehicleEntrant,
      CalculationResult calculationResult) {
    if (!calculationResult.getExempt()) {
      return null;
    }
    String modDiscountCode = getModDiscountCode(vehicleEntrant);
    if (!Strings.isNullOrEmpty(modDiscountCode)) {
      return modDiscountCode;
    }
    return getRetrofitDiscount(vehicleEntrant);
  }

  /**
   * Helper method to get mod discount code.
   */
  private String getModDiscountCode(VehicleEntrantDto vehicleEntrant) {
    return militaryVehicleService.findByVrn(vehicleEntrant.getVrn())
        .map(MilitaryVehicle::getWhitelistDiscountCode)
        .orElse(null);
  }

  /**
   * Helper method to get retrofit discount code.
   */
  private String getRetrofitDiscount(VehicleEntrantDto vehicleEntrant) {
    return retrofitService.findByVrn(vehicleEntrant.getVrn())
        .map(RetrofittedVehicle::getWhitelistDiscountCode)
        .orElse(null);
  }

  /**
   * Returns one of the following values: • exempt – The VRN is registered to a vehicle that meets
   * one or more national exemption criteria: Compliant – The VRN is registered to a vehicle that is
   * compliant with the CAZ framework. NotCompliantPaid – The vehicle is eligible to be charged and
   * has paid the charge in advance. NotCompliantNotPaid – The vehicle is eligible to be charged and
   * has not yet paid the charge in advance. UnrecognisedPaid – The VRN is not recognised by DVLA
   * but has paid the charge in advance. An unrecognised vehicle may indicate: (1) the VRN
   * corresponds to a non-UK vehicle; or, (2) the VRN has been incorrectly recognised by ANPR
   * infrastructure. UnrecognisedNotPaid – The VRN is not recognised by DVLA and has not yet paid
   * the charge. An unrecognised vehicle may indicate: (1) the VRN corresponds to a non-UK vehicle;
   * or, (2) the VRN has been incorrectly recognised by ANPR infrastructure.
   *
   * @param calculationResult - contains updateExempt, updateCompliant and isChargeable
   * @param vehicleEntrant {@link VehicleEntrantDto}
   * @param cleanAirZone clean air zone id
   * @return {@link VehicleComplianceStatus}
   */
  private VehicleComplianceStatus prepareVehicleCompliantStatus(CalculationResult calculationResult,
      VehicleEntrantDto vehicleEntrant, UUID cleanAirZone) {
    if (calculationResult.getExempt()) {
      return VehicleComplianceStatus.EXEMPT;
    }
    if (calculationResult.getCompliant()) {
      return VehicleComplianceStatus.COMPLIANT;
    }
    if (shouldBeCharged(calculationResult)) {
      PaymentStatus paymentStatus = registerVehicleEntryAndGetPaymentStatus(cleanAirZone,
          vehicleEntrant);
      return isPaymentStatusPaid(paymentStatus) ? VehicleComplianceStatus.NOT_COMPLIANT_PAID
          : VehicleComplianceStatus.NOT_COMPLIANT_NOT_PAID;
    }
    return VehicleComplianceStatus.NOT_COMPLIANT_NOT_PAID;
  }

  /**
   * Method to check if vehicle should be charged.
   *
   * @param calculationResult {@link CalculationResult}
   * @return true if vehicle should be charged false otherwise
   */
  private boolean shouldBeCharged(CalculationResult calculationResult) {
    return calculationResult.getCharge() != 0;
  }

  /**
   * Method to check if payment status should be PAID.
   *
   * @param paymentStatus {@link PaymentStatus}
   * @return true if payment status is PAID false otherwise
   */
  private boolean isPaymentStatusPaid(PaymentStatus paymentStatus) {
    return paymentStatus == PaymentStatus.PAID;
  }

  /**
   * Helper method to get payment status from PSR.
   *
   * @param cleanAirZoneId clean air zone
   * @param vehicleEntrant {@link VehicleEntrantDto}
   * @return {@link PaymentStatus}
   */
  private PaymentStatus registerVehicleEntryAndGetPaymentStatus(UUID cleanAirZoneId,
      VehicleEntrantDto vehicleEntrant) {
    PaymentStatusRequestDto paymentStatusRequest = mapToPaymentStatusRequest(cleanAirZoneId,
        vehicleEntrant);
    if (paymentsEnabled) {
      return paymentsService.registerVehicleEntryAndGetPaymentStatus(paymentStatusRequest);
    }
    return PaymentStatus.NOT_PAID;
  }

  /**
   * Helper method to check if a vehicle is currently operating as a taxi.
   *
   * @param licenseInfoResponse the response from NTR.
   * @return A boolean indicator of whether the vehicle is currently operating as a taxi.
   */
  private boolean getTaxiLicenseStatus(
      AsyncResponse<TaxiPhvLicenseInformationResponse> licenseInfoResponse) {
    return !licenseInfoResponse.hasError() && licenseInfoResponse.getBody().isActive();
  }

  /**
   * Helper method to check licensing authorities names when isTaxiOrPhv is true.
   *
   * @param licenseInfoResponse the response from NTR.
   * @return A {@link List} of {@link String}
   */
  private List<String> getLicensingAuthority(
      AsyncResponse<TaxiPhvLicenseInformationResponse> licenseInfoResponse) {
    return licenseInfoResponse.hasError() ? null
        : getActiveLicensingAuthorities(licenseInfoResponse.getBody());
  }

  /**
   * Helper method to extract active license authorities names.
   *
   * @param informationResponse the response from NTR.
   * @return A {@link List} of {@link String}
   */
  private List<String> getActiveLicensingAuthorities(
      TaxiPhvLicenseInformationResponse informationResponse) {
    return informationResponse.isActive() ? informationResponse.getLicensingAuthoritiesNames()
        : null;
  }
}