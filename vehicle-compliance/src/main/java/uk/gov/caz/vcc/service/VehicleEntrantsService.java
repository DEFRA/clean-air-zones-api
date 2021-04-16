package uk.gov.caz.vcc.service;

import static com.google.common.collect.Lists.newArrayList;
import static uk.gov.caz.vcc.dto.VehicleComplianceStatus.COMPLIANT;
import static uk.gov.caz.vcc.dto.VehicleComplianceStatus.NOT_COMPLIANT_NOT_PAID;
import static uk.gov.caz.vcc.dto.VehicleComplianceStatus.NOT_COMPLIANT_PAID;
import static uk.gov.caz.vcc.dto.VehicleComplianceStatus.UNRECOGNISED_NOT_PAID;
import static uk.gov.caz.vcc.dto.VehicleComplianceStatus.UNRECOGNISED_PAID;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import uk.gov.caz.correlationid.MdcCorrelationIdInjector;
import uk.gov.caz.definitions.domain.Vehicle;
import uk.gov.caz.definitions.domain.VehicleType;
import uk.gov.caz.definitions.exceptions.UnrecognisedFuelTypeException;
import uk.gov.caz.vcc.domain.CalculationResult;
import uk.gov.caz.vcc.domain.ChargeValidity;
import uk.gov.caz.vcc.domain.CleanAirZoneEntrant;
import uk.gov.caz.vcc.domain.TariffDetails;
import uk.gov.caz.vcc.domain.events.VehicleEntrantListPersistedEvent;
import uk.gov.caz.vcc.domain.exceptions.ExternalServiceCallException;
import uk.gov.caz.vcc.domain.exceptions.UnableToIdentifyVehicleComplianceException;
import uk.gov.caz.vcc.domain.service.ChargeabilityService;
import uk.gov.caz.vcc.domain.service.ComplianceService;
import uk.gov.caz.vcc.domain.service.UkVrnTestingService;
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
import uk.gov.caz.vcc.dto.VehicleEntrantReportingRequest;
import uk.gov.caz.vcc.dto.VehicleEntrantSaveDto;
import uk.gov.caz.vcc.dto.VehicleEntrantsSaveRequestDto;
import uk.gov.caz.vcc.dto.VehicleResultDto;
import uk.gov.caz.vcc.repository.CleanAirZoneEntrantRepository;
import uk.gov.caz.vcc.repository.TariffDetailsRepository;
import uk.gov.caz.vcc.util.Sha2Hasher;

/**
 * Service that combines VehicleEntrantsSaveRequestDto with calculation results and persists data to
 * the DB.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleEntrantsService {

  private static final String NATIONAL_EXEMPTION_CODE = "WDC001";
  private static final String NULL_EXEMPTION_CODE = null;
  static DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;

  private final CleanAirZoneEntrantRepository cleanAirZoneEntrantRepository;
  private final TariffDetailsRepository tariffDetailsRepository;
  private final ChargeabilityService chargeabilityService;
  private final ComplianceService complianceService;
  private final ExemptionService exemptionService;
  private final VehicleIdentificationService vehicleIdentificationService;
  private final MilitaryVehicleService militaryVehicleService;
  private final VehicleEntrantsPaymentsDataSupplier vehicleEntrantsPaymentsDataProvider;
  private final ApplicationEventPublisher applicationEventPublisher;
  private final GeneralWhitelistService generalWhitelistService;
  private final RetrofitService retrofitService;
  private final LicenseAndVehicleProvider licenseAndVehicleProvider;
  private final ChargeCalculationService chargeCalculationService;

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

  /**
   * Main method that handles business logic related to saving Vehicle Entrants and preparing result
   * with entrance, compliance and payment details.
   *
   * @param vehicleEntrantsSaveRequestDto payload from the API
   * @return list of {@link VehicleResultDto}
   */
  @Transactional
  public List<VehicleResultDto> save(VehicleEntrantsSaveRequestDto vehicleEntrantsSaveRequestDto) {
    List<VehicleResultDto> vehicleResultsResponse = createVehicleResultsResponse(
        vehicleEntrantsSaveRequestDto.getVehicleEntrants(),
        vehicleEntrantsSaveRequestDto.getCazId());

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
      VehicleResultDto vehicleResultDto) {

    VehicleEntrantSaveDto vehicleEntrantSaveDto = vehicleEntrantsSaveRequestDto
        .getVehicleEntrants().stream()
        .filter(e -> e.getVrn().equals(vehicleResultDto.getVrn())).findFirst()
        .orElseThrow(() -> new RuntimeException(
            "Cannot fetch vehicle from request when constucting DTO object for vehicle entrant"));

    CleanAirZoneEntrant cleanAirZoneEntrant = new CleanAirZoneEntrant(
        vehicleEntrantsSaveRequestDto.getCazId(),
        vehicleEntrantsSaveRequestDto.getCorrelationId(),
        vehicleEntrantSaveDto.getTimestamp()
    );

    cleanAirZoneEntrant.setVrn(vehicleResultDto.getVrn());
    cleanAirZoneEntrant.setChargeValidityCode(getChargeValidity(vehicleResultDto));
    cleanAirZoneEntrant.setEntrantPaymentId(vehicleResultDto.getEntrantPaymentId());

    return cleanAirZoneEntrant;
  }

  /*
   * Maps vehicle status to corresponding Charge Validity code from DB.
   */
  private ChargeValidity getChargeValidity(VehicleResultDto vehicleResultDto) {
    String status = vehicleResultDto.getStatus();

    return Optional
        .ofNullable(STATUSES_TO_CHARGE_VALIDITY_CODES.get(status))
        .orElseThrow(
            () -> new RuntimeException("Cannot fetch validity code for status: " + status));
  }

  /**
   * Method for preparing response for ANPR.
   *
   * @param vehicleEntrants list of vehicle entrants
   * @param cleanAirZoneId clean air zone id
   * @return {@link List} of {@link VehicleResultDto}
   */
  @VisibleForTesting
  public List<VehicleResultDto> createVehicleResultsResponse(
      List<VehicleEntrantSaveDto> vehicleEntrants, UUID cleanAirZoneId) {
    // Business rules.
    // MOD  = Ministry of Defense aka Military vehicle (White or Green) - list of vehicles which
    //        should not be charged
    // GPWL = General Purpose Whitelist - list of vehicles that should not be charged
    // GPWL Category = see model.CategoryType enum of JAQU-CAZ-Whitelist-API project
    //
    // When should we call DVLA to obtain vehicle details and fill them in the response?
    // What about informing Payments about vehicle entrant?
    // Operation                  | Should call DVLA | Should call Payments | Exempt | Compliant
    // Vehicle is in Retrofit DB  | YES              | NO                   | NO     | YES
    // Vehicle is in MOD DB       | YES              | NO                   | YES    | NO
    // Exempt by tax class        | YES              | NO                   | YES    | NO
    // Exempt by being historic   | YES              | NO                   | YES    | NO
    // Exempt by marked disabled  | YES              | NO                   | YES    | NO
    // GPWL - "Early Adopter"     | YES              | NO                   | NO     | YES
    // GPWL - "Exemption"         | YES              | NO                   | YES    | NO
    // GPWL - "Other"             | YES              | NO                   | YES    | NO
    // GPWL - "Non UK Vehicle"    | NO               | NO                   | NO     | YES
    // GPWL - "Problematic VRN"   | NO               | NO                   | YES    | NO
    // VRN is Non UK              | NO               | YES                  | NO     | NO
    // Any other case             | YES              | YES                  | MAYBE  | MAYBE

    Set<String> submittedVrns = extractVrns(vehicleEntrants);
    PreFetchedDataResults preFetchedDataWrapper =
        chargeCalculationService.computePrefetchedData(submittedVrns);

    List<InitialVehicleResult> vehicleResultsThatDoNotNeedDvla = getResultsThatDoNotNeedDvla(
        vehicleEntrants, cleanAirZoneId, preFetchedDataWrapper);

    List<VehicleEntrantSaveDto> toBeProcessedUsingDvla = getRemainingThatNeedToCallDvla(
        vehicleEntrants, vehicleResultsThatDoNotNeedDvla);

    // At this point we have a list of VehicleEntrantDto objects (VRNs) for which we know
    // that we will need to call NTR and DVLA. We can then optimize by doing bulk
    // queries and prefetching (caching) NTR and DVLA responses.
    // The implementation provided by LicenceAndVehicleProvider focuses only on getting this data
    // and can be easily updated without touching business logic.
    licenseAndVehicleProvider.prefetch(extractVrns(toBeProcessedUsingDvla));

    List<InitialVehicleResult> vehicleResultsThatNeedDvla = getResultsThatNeedDvla(
        cleanAirZoneId, preFetchedDataWrapper, toBeProcessedUsingDvla);

    ArrayList<InitialVehicleResult> allVehicleResults = combine(vehicleResultsThatDoNotNeedDvla,
        vehicleResultsThatNeedDvla);

    List<InitialVehicleResult> toBeProcessedByPayments = allVehicleResults.stream()
        .filter(InitialVehicleResult::shouldFetchPaymentDetails)
        .collect(Collectors.toList());
    List<InitialVehicleResult> processedByPayments = vehicleEntrantsPaymentsDataProvider
        .processPaymentRelatedEntrants(toBeProcessedByPayments);
    List<InitialVehicleResult> notProcessedByPayments = allVehicleResults.stream()
        .filter(initialResult -> !initialResult.shouldFetchPaymentDetails())
        .collect(Collectors.toList());

    publishVehicleEntrantPersistedEvents(processedByPayments, notProcessedByPayments);

    List<VehicleResultDto> results = Stream
        .concat(processedByPayments.stream(), notProcessedByPayments.stream())
        .map(InitialVehicleResult::getResult)
        .collect(Collectors.toList());
    sortResultsToMatchIncomingEventsOrder(vehicleEntrants, results);
    return results;
  }

  /**
   * Traverses incoming vehicle entrants and process those that do not need DVLA and NTR data, so
   * those which for example are MOD, Retrofitted or on non-uk VRN list.
   */
  private List<InitialVehicleResult> getResultsThatDoNotNeedDvla(
      List<VehicleEntrantSaveDto> vehicleEntrants, UUID cleanAirZoneId,
      PreFetchedDataResults preFetchedDataWrapper) {
    List<InitialVehicleResult> vehicleResultsThatDoNotNeedDvla = vehicleEntrants.stream()
        // Calculate compliance result for API response payload
        .map(
            vehicleEntrant -> populateVehicleResultsThatDoNotUseDvla(vehicleEntrant, cleanAirZoneId,
                preFetchedDataWrapper))
        .collect(Collectors.toList());
    return removeNulls(vehicleResultsThatDoNotNeedDvla);
  }

  /**
   * Removes nulls from the list.
   */
  private List<InitialVehicleResult> removeNulls(List<InitialVehicleResult> vehicleResultList) {
    return vehicleResultList.stream().filter(Objects::nonNull).collect(Collectors.toList());
  }

  /**
   * Given full list of vehicle entrants to process and results of events that were already
   * processed because they did not need to call DVLA and NTR, returns list of vehicle entrants that
   * still need to be processed.
   */
  private List<VehicleEntrantSaveDto> getRemainingThatNeedToCallDvla(
      List<VehicleEntrantSaveDto> vehicleEntrants,
      List<InitialVehicleResult> vehicleResultsThatDoNotNeedDvla) {
    List<VehicleEntrantSaveDto> toBeProcessedUsingDvla = vehicleEntrants.stream()
        .filter(vehicleEntrantDto -> vehicleResultsThatDoNotNeedDvla.stream()
            .noneMatch(vehicleResult -> vehicleResult.getResult().getVrn()
                .equalsIgnoreCase(vehicleEntrantDto.getVrn()))).collect(Collectors.toList());
    return toBeProcessedUsingDvla;
  }

  /**
   * Processes vehicle entrants events that need to call DVLA and NTR.
   */
  private List<InitialVehicleResult> getResultsThatNeedDvla(UUID cleanAirZoneId,
      PreFetchedDataResults preFetchedDataWrapper,
      List<VehicleEntrantSaveDto> toBeProcessedUsingDvla) {
    List<InitialVehicleResult> vehicleResultsUsingDvla = toBeProcessedUsingDvla.stream()
        // Calculate compliance result for API response payload
        .map(
            vehicleEntrant -> populateVehicleResultsThatNeedDvla(vehicleEntrant, cleanAirZoneId,
                preFetchedDataWrapper, licenseAndVehicleProvider))
        .collect(Collectors.toList());
    return vehicleResultsUsingDvla;
  }

  /**
   * Combines two lists into one.
   */
  private ArrayList<InitialVehicleResult> combine(
      List<InitialVehicleResult> vehicleResultsThatDoNotNeedDvla,
      List<InitialVehicleResult> vehicleResultsThatNeedDvla) {
    ArrayList<InitialVehicleResult> vehicleResults = newArrayList(
        Iterables.concat(vehicleResultsThatDoNotNeedDvla, vehicleResultsThatNeedDvla));
    return vehicleResults;
  }

  /**
   * Sorts result list in the same order as input vehicle entrant events, by VRN.
   */
  private void sortResultsToMatchIncomingEventsOrder(List<VehicleEntrantSaveDto> vehicleEntrants,
      List<VehicleResultDto> results) {
    results.sort(Comparator.comparing(item -> vehicleEntrants.indexOf(
        vehicleEntrants.stream().filter(ve -> ve.getVrn().equalsIgnoreCase(item.getVrn()))
            .findFirst().get())));
  }

  private void publishVehicleEntrantPersistedEvents(List<InitialVehicleResult> processedByPayments,
      List<InitialVehicleResult> notProcessedByPayments) {
    List<VehicleEntrantReportingRequest> reportingRequests =
        new ArrayList<VehicleEntrantReportingRequest>();
    Stream.concat(processedByPayments.stream(), notProcessedByPayments.stream())
        .map(e -> e.getReportingRequestBuilder() == null
            ? e.getReportingRequest()
            : e.getReportingRequestBuilder().apply(e.getResult()))
        .forEach(reportingRequests::add);
    VehicleEntrantListPersistedEvent vehicleEntrants =
        new VehicleEntrantListPersistedEvent(this, reportingRequests);
    applicationEventPublisher.publishEvent(vehicleEntrants);
  }

  private Set<String> extractVrns(List<VehicleEntrantSaveDto> vehicleEntrants) {
    return vehicleEntrants.stream()
        .map(VehicleEntrantSaveDto::getVrn)
        .collect(Collectors.toSet());
  }

  /**
   * Processes vehicle entrant event that do not need to call DVLA and NTR.
   */
  private InitialVehicleResult populateVehicleResultsThatDoNotUseDvla(
      VehicleEntrantSaveDto vehicleEntrant,
      UUID cleanAirZoneId, PreFetchedDataResults preFetchedDataResults) {
    String vrn = vehicleEntrant.getVrn();

    boolean isOnGpwAndHasNonUkCategory =
        generalWhitelistService.isOnGeneralPurposeWhitelistAndHasCategoryNonUk(
            vrn, preFetchedDataResults.getMatchedGeneralWhitelistVehicles());

    boolean isOnGpwAndHasOtherCategory = generalWhitelistService
        .isOnGeneralPurposedWhitelistAndHasCategoryOther(vrn,
            preFetchedDataResults.getMatchedGeneralWhitelistVehicles());

    boolean vrnIsNotRecognisedUkFormat = vrnIsNotRecognisedUkFormat(vrn);

    boolean isMilitaryVrn = militaryVehicleService.isMilitaryVrn(vrn, preFetchedDataResults);

    if (vrnIsNotRecognisedUkFormat && isOnGpwAndHasNonUkCategory) {
      // Do not call DVLA, Vehicle is Compliant on GPW
      return buildResultForNonUkVehicleWithCompliantStatus(vehicleEntrant, cleanAirZoneId);
    }

    if (vrnIsNotRecognisedUkFormat && isOnGpwAndHasOtherCategory) {
      // Do not call DVLA. Vehicle is Diplomatic Vehicle
      return buildResultForOtherCategoryExemptVehicle(cleanAirZoneId, vehicleEntrant);
    }

    if (vrnIsNotRecognisedUkFormat && !isMilitaryVrn) {
      // Do not call DVLA but inform Payments. Vehicle is Unrecognised
      return buildResultWithUnrecognisedStatusAndWithPaymentDetails(cleanAirZoneId, vehicleEntrant);
    }

    // If is on GPWL and has a problematic VRN we can be sure that calling DVLA is a waste and
    // we should early yield "Exempt" here.
    if (generalWhitelistService
        .isOnGeneralPurposeWhitelistAndHasCategoryProblematicVrn(vrn,
            preFetchedDataResults.getMatchedGeneralWhitelistVehicles())) {
      // Do not call DVLA and do not inform Payments. Vehicle is "Exempt"
      log.info("Vehicle on GPWL and is Problematic. Preparing response to ANPR");
      return buildResultWithoutDvlaDataAndWithoutPaymentDetails(vrn,
          VehicleComplianceStatus.EXEMPT, NATIONAL_EXEMPTION_CODE, cleanAirZoneId,
          vehicleEntrant.getTimestamp(), "Problematic VRN", false);
    }

    // If is on GPWL and is categorized as "Non-UK Vehicle" we can be sure that calling DVLA is
    // a waste because there is no data. Such vehicle is generally marked as "Compliant"
    // however if vehicle is also on MOD list it must be returned as "Exempt" with all data that
    // we can find in DVLA. Exempt has higher priority than "Compliant". Hence here, if is on MOD
    // we skip the if and allow to process it in buildDvlaResponse method.
    if (!isMilitaryVrn && isOnGpwAndHasNonUkCategory) {
      // Do not call DVLA and do not inform Payments. Vehicle is "Compliant"
      log.info("Vehicle on GPWL and is Non-UK. Preparing response to ANPR");
      return buildResultWithoutDvlaDataAndWithoutPaymentDetails(vrn,
          VehicleComplianceStatus.COMPLIANT, NULL_EXEMPTION_CODE, cleanAirZoneId,
          vehicleEntrant.getTimestamp(), "Non-UK Vehicle", true);
    }

    return null;
  }

  /**
   * Main service method for producing a response to a single vehicle entrant request entry.
   *
   * @param vehicleEntrant a Vehicle submission
   * @param cleanAirZoneId the Clean Air Zone a vehicle has been captured in
   * @param preFetchedDataResults a list of matched GPW vehicles from the batch in the
   *     submission
   * @param licenseAndVehicleProvider Provides data from DVLA and NTR
   * @return a vehicle entrant result for a single vehicle
   */
  private InitialVehicleResult populateVehicleResultsThatNeedDvla(
      VehicleEntrantSaveDto vehicleEntrant,
      UUID cleanAirZoneId, PreFetchedDataResults preFetchedDataResults,
      LicenseAndVehicleProvider licenseAndVehicleProvider) {
    return buildDvlaResponse(vehicleEntrant, cleanAirZoneId,
        preFetchedDataResults, licenseAndVehicleProvider);
  }

  /**
   * Tests whether VRN is not conforming to UK plates specification.
   */
  private boolean vrnIsNotRecognisedUkFormat(String vrn) {
    return !UkVrnTestingService.isPotentialUkVrn(vrn);
  }

  /**
   * Builds {@link VehicleResultDto} object with unrecognised vehicle status.
   */
  private InitialVehicleResult buildResultWithUnrecognisedStatusAndWithPaymentDetails(
      UUID cleanAirZoneId, VehicleEntrantSaveDto vehicleEntrant) {
    log.info("Vehicle with an unrecognised number plate format found when preparing "
        + "response for ANPR.");
    return buildResultWithUnrecognisedStatus(vehicleEntrant, cleanAirZoneId, true);
  }

  /**
   * Builds {@link VehicleResultDto} object with compliant vehicle status.
   */
  private InitialVehicleResult buildResultForNonUkVehicleWithCompliantStatus(
      VehicleEntrantSaveDto vehicleEntrant, UUID cleanAirZoneId) {
    log.info("Vehicle deemed Non-UK by General Purpose Whitelist found when preparing "
        + "response for ANPR.");
    return buildResultWithCompliantStatus(vehicleEntrant, cleanAirZoneId, true);
  }

  /**
   * Method for preparing response for ANPR.
   *
   * @param vehicleEntrant list of vehicle entrants
   * @param cleanAirZoneId clean air zone id
   * @param licenseAndVehicleProvider an instance of {@link LicenseAndVehicleProvider}
   * @return {@link List} of {@link VehicleResultDto}
   */
  private InitialVehicleResult buildDvlaResponse(VehicleEntrantSaveDto vehicleEntrant,
      UUID cleanAirZoneId,
      PreFetchedDataResults preFetchedDataResults,
      LicenseAndVehicleProvider licenseAndVehicleProvider) {
    try {
      String vrn = vehicleEntrant.getVrn();
      boolean isMilitaryVrn = militaryVehicleService.isMilitaryVrn(vrn, preFetchedDataResults);
      boolean isOnGpwAndHasOtherCategory = generalWhitelistService
          .isOnGeneralPurposedWhitelistAndHasCategoryOther(vrn,
              preFetchedDataResults.getMatchedGeneralWhitelistVehicles());

      NtrAndDvlaVehicleData licenseAndVehicle = licenseAndVehicleProvider
          .findLicenseAndVehicle(vrn);

      SingleDvlaVehicleData dvlaVehicle = licenseAndVehicle.getDvlaVehicleData();
      
      if (dvlaVehicle.hasFailed() && isOnGpwAndHasOtherCategory) {
        // Diplomatic vehicle
        return buildResultForOtherCategoryExemptVehicle(cleanAirZoneId, vehicleEntrant);
      }

      if (dvlaVehicle.hasFailed() && isMilitaryVrn) {
        log.info("DVLA data are not available and vehicle is in MOD");
        return buildResultWithExemptStatus(vehicleEntrant, cleanAirZoneId);
      }

      // If calling of the vehicle API has failed due to any other reason than being
      // not found (valid under REST principles), then re-raise exception
      if (dvlaVehicle.hasFailed()
          && dvlaVehicle.getHttpStatus() != HttpStatus.NOT_FOUND) {
        log.info("AsyncOperation to DVLA has failed. DVLA error: {}, code {}",
            dvlaVehicle.getErrorMessage(), dvlaVehicle.getHttpStatus());
        throw new ExternalServiceCallException();
      }

      // If the API has responded but returned a status of not found
      // mark vehicle as unidentified
      if (dvlaVehicle.getHttpStatus() == HttpStatus.NOT_FOUND) {
        return buildResultWithUnrecognisedStatus(vehicleEntrant, cleanAirZoneId,
            false);
      }

      SingleLicenceData ntrVehicleData = licenseAndVehicle.getNtrVehicleData();

      // If calling of the NTR API has failed due to any other reason than being
      // not found (valid under REST principles), then re-raise exception
      if (ntrVehicleData.hasFailed()
          && ntrVehicleData.getHttpStatus() != HttpStatus.NOT_FOUND) {
        log.info("AsyncOperation to NTR has failed. NTR error: {}, code {}",
            ntrVehicleData.getErrorMessage(), ntrVehicleData.getHttpStatus());
        throw new ExternalServiceCallException();
      }

      Vehicle vehicle = setVehicleTypeAndTaxiOrPhv(dvlaVehicle.getVehicle(),
          getTaxiLicenseStatus(ntrVehicleData));

      checkTaxiStatus(ntrVehicleData, vehicle);

      if (isMilitaryVrn) {
        return buildResultWithExemptStatusAndDvlaData(vehicleEntrant, vehicle, cleanAirZoneId);
      }

      CalculationResult calculationResult;
      try {
        calculationResult = prepareCalculationResult(cleanAirZoneId, vehicle,
            preFetchedDataResults);
      } catch (UnableToIdentifyVehicleComplianceException | UnrecognisedFuelTypeException ex) {
        // We are unable to calculate compliance (lack of crucial data from DVLA mostly).
        // We don't need to process tariffs, charges but want to check if entrance has been
        // paid and return as many details as possible (DVLA present fields).
        return buildResultWithUnrecognisedStatus(vehicleEntrant, cleanAirZoneId, vehicle,
            ntrVehicleData, vehicleEntrant.getTimestamp());
      }

      if (!calculationResult.isExempt()) {
        Optional<TariffDetails> tariff = getTariffDetails(cleanAirZoneId);

        if (!tariff.isPresent()) {
          log.info(
              "Tariff not found when preparing response for ANPR for provided VRN, "
                  + "and cleanAirZoneId {}", cleanAirZoneId);
          return buildResultWithoutTariffCode(
              vehicleEntrant, cleanAirZoneId, vehicle,
              calculationResult, ntrVehicleData);
        }

        TariffDetails tariffDetails = tariff.get();

        setCharge(vehicle, calculationResult, tariffDetails);

        return buildResultForNonExemptVehicle(cleanAirZoneId, vehicle,
            tariffDetails.getChargeIdentifier(), ntrVehicleData, calculationResult, vehicleEntrant);
      } else {
        return buildResultForExemptVehicle(vehicleEntrant.getVrn(), cleanAirZoneId, vehicle,
            ntrVehicleData, vehicleEntrant.getTimestamp());
      }
    } catch (Exception ex) {
      throw ex;
    }
  }

  /**
   * Builds {@link VehicleResultDto} for Diplomatic Vehicle which has no DVLA data, only exemption
   * data.
   */
  private InitialVehicleResult buildResultForOtherCategoryExemptVehicle(UUID cleanAirZoneId,
      VehicleEntrantSaveDto vehicleEntrant) {
    String vrn = vehicleEntrant.getVrn();
    VehicleResultDto result = VehicleResultDto.builder()
        .vrn(vrn)
        .status(VehicleComplianceStatus.EXEMPT.getStatus())
        .isTaxiOrPhv(false)
        .exemptionCode(NATIONAL_EXEMPTION_CODE)
        .build();

    VehicleEntrantReportingRequest event = VehicleEntrantReportingRequest.builder()
        .correlationId(MdcCorrelationIdInjector.getCurrentValue())
        .cleanAirZoneId(cleanAirZoneId)
        .vrnHash(Sha2Hasher.sha256Hash(vehicleEntrant.getVrn()))
        .hour(parseEntrantTimestamp(vehicleEntrant.getTimestamp()))
        .chargeValidityCode(getChargeValidity(result).getChargeValidityCode())
        .exemptionReason("Other")
        .build();

    return InitialVehicleResult.builder()
        .result(result)
        .shouldFetchPaymentDetails(false)
        .reportingRequest(event)
        .build();
  }

  /**
   * Builds {@link VehicleResultDto} with status Exempt when vehicle is in MOD and DVLA data are not
   * available.
   */
  private InitialVehicleResult buildResultWithExemptStatus(VehicleEntrantSaveDto vehicleEntrant,
      UUID cleanAirZoneId) {
    String vrn = vehicleEntrant.getVrn();
    VehicleResultDto result = VehicleResultDto.builder()
        .vrn(vrn)
        .status(VehicleComplianceStatus.EXEMPT.getStatus())
        .isTaxiOrPhv(false)
        .exemptionCode(NATIONAL_EXEMPTION_CODE)
        .build();

    VehicleEntrantReportingRequest event = VehicleEntrantReportingRequest.builder()
        .correlationId(MdcCorrelationIdInjector.getCurrentValue())
        .cleanAirZoneId(cleanAirZoneId)
        .vrnHash(Sha2Hasher.sha256Hash(vehicleEntrant.getVrn()))
        .hour(parseEntrantTimestamp(vehicleEntrant.getTimestamp()))
        .chargeValidityCode(getChargeValidity(result).getChargeValidityCode())
        .exemptionReason("Other")
        .nonStandardUkPlateFormat(false).build();

    return InitialVehicleResult.builder()
        .result(result)
        .shouldFetchPaymentDetails(false)
        .reportingRequest(event)
        .build();
  }

  /**
   * Builds {@link VehicleResultDto} with status Exempt when vehicle is in MOD and DVLA data are
   * available.
   */
  private InitialVehicleResult buildResultWithExemptStatusAndDvlaData(
      VehicleEntrantSaveDto vehicleEntrant, Vehicle vehicle, UUID cleanAirZoneId) {
    String vrn = vehicleEntrant.getVrn();
    VehicleResultDto result = VehicleResultDto.builder()
        .vrn(vrn)
        .make(vehicle.getMake())
        .model(vehicle.getModel())
        .colour(vehicle.getColour())
        .typeApproval(vehicle.getTypeApproval())
        .status(VehicleComplianceStatus.EXEMPT.getStatus())
        .isTaxiOrPhv(false)
        .exemptionCode(NATIONAL_EXEMPTION_CODE)
        .build();

    VehicleEntrantReportingRequest event = VehicleEntrantReportingRequest.builder()
        .correlationId(MdcCorrelationIdInjector.getCurrentValue())
        .cleanAirZoneId(cleanAirZoneId)
        .vrnHash(Sha2Hasher.sha256Hash(vehicleEntrant.getVrn()))
        .hour(parseEntrantTimestamp(vehicleEntrant.getTimestamp()))
        .fuelType(vehicle.getFuelType())
        .model(vehicle.getModel())
        .make(vehicle.getMake())
        .colour(vehicle.getColour())
        .typeApproval(vehicle.getTypeApproval())
        .chargeValidityCode(getChargeValidity(result).getChargeValidityCode())
        .vehicleType(vehicle.getVehicleType())
        .exemptionReason("Other")
        .nonStandardUkPlateFormat(false).build();

    return InitialVehicleResult.builder()
        .result(result)
        .shouldFetchPaymentDetails(false)
        .reportingRequest(event)
        .build();
  }

  /**
   * Method to create {@link CalculationResult}.
   *
   * @param cleanAirZoneId Clean Air Zone identifier
   * @param vehicle from dvla
   * @return {@link CalculationResult}
   */
  private CalculationResult prepareCalculationResult(UUID cleanAirZoneId,
      Vehicle vehicle, PreFetchedDataResults preFetchedDataResults) {
    CalculationResult calculationResult =
        updateExempt(vehicle, preFetchedDataResults);
    calculationResult.setCazIdentifier(cleanAirZoneId);
    if (!calculationResult.isExempt()) {
      calculationResult =
          updateCompliant(vehicle, calculationResult, preFetchedDataResults);
    }
    calculationResult.setCharge(0);
    return calculationResult;
  }

  private InitialVehicleResult buildResultForExemptVehicle(String vrn, UUID cleanAirZoneId,
      Vehicle vehicle, SingleLicenceData ntrData, LocalDateTime entrantTimestamp) {
    VehicleResultDto result = VehicleResultDto.builder()
        .vrn(vrn)
        .make(vehicle.getMake())
        .model(vehicle.getModel())
        .colour(vehicle.getColour())
        .typeApproval(vehicle.getTypeApproval())
        .status(VehicleComplianceStatus.EXEMPT.getStatus())
        .paymentMethod(PaymentMethod.NULL.toDtoString())
        .exemptionCode(VehicleEntrantsService.NATIONAL_EXEMPTION_CODE)
        .tariffCode(prepareTariffCode(vehicle.getVehicleType(),
            null, VehicleComplianceStatus.EXEMPT))
        .isTaxiOrPhv(getTaxiLicenseStatus(ntrData))
        .licensingAuthority(getLicensingAuthority(ntrData))
        .entrantPaymentId(null)
        .build();

    VehicleEntrantReportingRequest event = VehicleEntrantReportingRequest.builder()
        .correlationId(MdcCorrelationIdInjector.getCurrentValue())
        .cleanAirZoneId(cleanAirZoneId)
        .vrnHash(Sha2Hasher.sha256Hash(vrn))
        .hour(parseEntrantTimestamp(entrantTimestamp))
        .fuelType(vehicle.getFuelType())
        .model(vehicle.getModel())
        .make(vehicle.getMake())
        .colour(vehicle.getColour())
        .typeApproval(vehicle.getTypeApproval())
        .chargeValidityCode(getChargeValidity(result).getChargeValidityCode())
        .vehicleType(vehicle.getVehicleType())
        .exemptionReason(exemptionService.identifyExemptionReason(vehicle))
        .taxiPhvDescription(getTaxiPhvDescription(ntrData))
        .licensingAuthorities(getLicensingAuthority(ntrData))
        .nonStandardUkPlateFormat(false).build();

    return InitialVehicleResult.builder()
        .result(result)
        .shouldFetchPaymentDetails(false)
        .reportingRequest(event)
        .build();
  }

  /**
   * Method to create full result.
   */
  private InitialVehicleResult buildResultForNonExemptVehicle(UUID cleanAirZoneId,
      Vehicle vehicle, String chargeIdentifier, SingleLicenceData ntrData,
      CalculationResult calculationResult, VehicleEntrantSaveDto vehicleEntrant) {
    if (calculationResult.getCompliant() || calculationResult.getIsRetrofitted()) {
      calculationResult.setCharge(0);
    }

    if (shouldNotBeCharged(calculationResult)) {
      VehicleComplianceStatus complianceStatus = VehicleComplianceStatus.COMPLIANT;
      VehicleResultDto result = VehicleResultDto.builder()
          .vrn(vehicleEntrant.getVrn())
          .make(vehicle.getMake())
          .model(vehicle.getModel())
          .colour(vehicle.getColour())
          .typeApproval(vehicle.getTypeApproval())
          .status(complianceStatus.getStatus())
          .paymentMethod(PaymentMethod.NULL.toDtoString())
          .exemptionCode(VehicleEntrantsService.NULL_EXEMPTION_CODE)
          .tariffCode(prepareTariffCode(vehicle.getVehicleType(),
              chargeIdentifier, complianceStatus))
          .isTaxiOrPhv(getTaxiLicenseStatus(ntrData))
          .licensingAuthority(getLicensingAuthority(ntrData))
          .entrantPaymentId(null)
          .build();

      VehicleEntrantReportingRequest event = VehicleEntrantReportingRequest.builder()
          .correlationId(MdcCorrelationIdInjector.getCurrentValue())
          .cleanAirZoneId(cleanAirZoneId)
          .vrnHash(Sha2Hasher.sha256Hash(vehicleEntrant.getVrn()))
          .hour(parseEntrantTimestamp(vehicleEntrant.getTimestamp()))
          .fuelType(vehicle.getFuelType())
          .model(vehicle.getModel())
          .make(vehicle.getMake())
          .colour(vehicle.getColour())
          .typeApproval(vehicle.getTypeApproval())
          .chargeValidityCode(getChargeValidity(result).getChargeValidityCode())
          .vehicleType(vehicle.getVehicleType())
          .exemptionReason(exemptionService.identifyExemptionReason(vehicle))
          .taxiPhvDescription(getTaxiPhvDescription(ntrData))
          .licensingAuthorities(getLicensingAuthority(ntrData))
          .nonStandardUkPlateFormat(false).build();

      return InitialVehicleResult.builder()
          .result(result)
          .shouldFetchPaymentDetails(false)
          .reportingRequest(event)
          .build();
    }

    // assertion: should be charged
    VehicleResultDto result = VehicleResultDto.builder()
        .vrn(vehicleEntrant.getVrn())
        .make(vehicle.getMake())
        .model(vehicle.getModel())
        .colour(vehicle.getColour())
        .typeApproval(vehicle.getTypeApproval())
        .exemptionCode(VehicleEntrantsService.NULL_EXEMPTION_CODE)
        .isTaxiOrPhv(getTaxiLicenseStatus(ntrData))
        .licensingAuthority(getLicensingAuthority(ntrData))
        .build();

    Function<VehicleResultDto, VehicleEntrantReportingRequest> eventBuilder = finalResult -> {
      return VehicleEntrantReportingRequest.builder()
          .correlationId(MdcCorrelationIdInjector.getCurrentValue())
          .cleanAirZoneId(cleanAirZoneId)
          .vrnHash(Sha2Hasher.sha256Hash(vehicleEntrant.getVrn()))
          .hour(parseEntrantTimestamp(vehicleEntrant.getTimestamp()))
          .fuelType(vehicle.getFuelType())
          .model(vehicle.getModel())
          .make(vehicle.getMake())
          .colour(vehicle.getColour())
          .typeApproval(vehicle.getTypeApproval())
          .chargeValidityCode(getChargeValidity(finalResult).getChargeValidityCode())
          .vehicleType(vehicle.getVehicleType())
          .exemptionReason(exemptionService.identifyExemptionReason(vehicle))
          .taxiPhvDescription(getTaxiPhvDescription(ntrData))
          .licensingAuthorities(getLicensingAuthority(ntrData))
          .nonStandardUkPlateFormat(false).build();
    };

    return InitialVehicleResult.builder()
        .complianceStatusProvider(paymentStatus -> paymentStatus == PaymentStatus.PAID
            ? NOT_COMPLIANT_PAID
            : NOT_COMPLIANT_NOT_PAID)
        .paymentMethodProvider(entrantPaymentMethodAndStatusSupplier ->
            entrantPaymentMethodAndStatusSupplier.getPaymentStatus() == PaymentStatus.PAID
                ? entrantPaymentMethodAndStatusSupplier.getPaymentMethod()
                : PaymentMethod.NULL)
        .tariffCodeBuilder(complianceStatus ->
            prepareTariffCode(vehicle.getVehicleType(), chargeIdentifier, complianceStatus))
        .reportingRequestBuilder(eventBuilder)
        .result(result)
        .shouldFetchPaymentDetails(true)
        .cleanAirZoneId(cleanAirZoneId)
        .cazEntryTimestamp(vehicleEntrant.getTimestamp())
        .build();
  }

  /**
   * Builds {@link VehicleResultDto} when no DVLA data is available and as well there is no payment
   * details.
   */
  private InitialVehicleResult buildResultWithoutDvlaDataAndWithoutPaymentDetails(String vrn,
      VehicleComplianceStatus status, String exemptionCode, UUID cleanAirZoneId,
      LocalDateTime timestamp,
      String exemptionReason, boolean nonUkVehicle) {
    VehicleResultDto result = VehicleResultDto.builder()
        .vrn(vrn)
        .status(status.getStatus())
        .paymentMethod(PaymentMethod.NULL.toDtoString())
        .exemptionCode(exemptionCode)
        .isTaxiOrPhv(false)
        .build();

    VehicleEntrantReportingRequest event = VehicleEntrantReportingRequest.builder()
        .correlationId(MdcCorrelationIdInjector.getCurrentValue())
        .cleanAirZoneId(cleanAirZoneId)
        .vrnHash(Sha2Hasher.sha256Hash(vrn))
        .hour(parseEntrantTimestamp(timestamp))
        .chargeValidityCode(getChargeValidity(result).getChargeValidityCode())
        .exemptionReason(exemptionReason)
        .nonStandardUkPlateFormat(nonUkVehicle).build();

    return InitialVehicleResult.builder()
        .result(result)
        .shouldFetchPaymentDetails(false)
        .reportingRequest(event)
        .build();
  }

  /**
   * Method to create result when vrn is not recognized by DVLA.
   *
   * @param vehicleEntrant single request from ANPR
   * @param cleanAirZoneId clean air zone
   * @return result with vrn and status
   */
  private InitialVehicleResult buildResultWithUnrecognisedStatus(
      VehicleEntrantSaveDto vehicleEntrant, UUID cleanAirZoneId, boolean nonUkVehicle) {

    VehicleResultDto result = VehicleResultDto.builder()
        .vrn(vehicleEntrant.getVrn())
        .build();

    Function<VehicleResultDto, VehicleEntrantReportingRequest> eventBuilder = finalResult -> {
      return VehicleEntrantReportingRequest.builder()
          .correlationId(MdcCorrelationIdInjector.getCurrentValue())
          .cleanAirZoneId(cleanAirZoneId)
          .vrnHash(Sha2Hasher.sha256Hash(vehicleEntrant.getVrn()))
          .hour(parseEntrantTimestamp(vehicleEntrant.getTimestamp()))
          .chargeValidityCode(getChargeValidity(finalResult).getChargeValidityCode())
          .nonStandardUkPlateFormat(nonUkVehicle).build();
    };

    return InitialVehicleResult.builder()
        .complianceStatusProvider(paymentStatus -> paymentStatus == PaymentStatus.PAID
            ? UNRECOGNISED_PAID
            : UNRECOGNISED_NOT_PAID)
        .paymentMethodProvider(entrantPaymentMethodAndStatusSupplier ->
            entrantPaymentMethodAndStatusSupplier.getPaymentStatus() == PaymentStatus.PAID
                ? entrantPaymentMethodAndStatusSupplier.getPaymentMethod()
                : PaymentMethod.NULL)
        .reportingRequestBuilder(eventBuilder)
        .result(result)
        .shouldFetchPaymentDetails(true)
        .cleanAirZoneId(cleanAirZoneId)
        .cazEntryTimestamp(vehicleEntrant.getTimestamp())
        .build();
  }

  /**
   * Method to create result when we are unable to calculate compliance and charge but have some of
   * DVLA data.
   */
  private InitialVehicleResult buildResultWithUnrecognisedStatus(
      VehicleEntrantSaveDto vehicleEntrant, UUID cleanAirZoneId, Vehicle vehicle,
      SingleLicenceData ntrData, LocalDateTime entrantTimestamp) {

    VehicleResultDto result = VehicleResultDto.builder()
        .vrn(vehicleEntrant.getVrn())
        .make(vehicle.getMake())
        .model(vehicle.getModel())
        .colour(vehicle.getColour())
        .typeApproval(vehicle.getTypeApproval())
        .isTaxiOrPhv(getTaxiLicenseStatus(ntrData))
        .licensingAuthority(getLicensingAuthority(ntrData))
        .build();

    Function<VehicleResultDto, VehicleEntrantReportingRequest> eventBuilder = finalResult -> {
      return VehicleEntrantReportingRequest.builder()
          .correlationId(MdcCorrelationIdInjector.getCurrentValue())
          .cleanAirZoneId(cleanAirZoneId)
          .vrnHash(Sha2Hasher.sha256Hash(vehicleEntrant.getVrn()))
          .hour(parseEntrantTimestamp(entrantTimestamp))
          .fuelType(vehicle.getFuelType())
          .chargeValidityCode(getChargeValidity(finalResult).getChargeValidityCode())
          .vehicleType(vehicle.getVehicleType())
          .exemptionReason(exemptionService.identifyExemptionReason(vehicle))
          .taxiPhvDescription(getTaxiPhvDescription(ntrData))
          .licensingAuthorities(getLicensingAuthority(ntrData))
          .nonStandardUkPlateFormat(false).build();
    };

    return InitialVehicleResult.builder()
        .complianceStatusProvider(paymentStatus -> paymentStatus == PaymentStatus.PAID
            ? UNRECOGNISED_PAID
            : UNRECOGNISED_NOT_PAID
        )
        .paymentMethodProvider(entrantPaymentMethodAndStatusSupplier ->
            entrantPaymentMethodAndStatusSupplier.getPaymentStatus() == PaymentStatus.PAID
                ? entrantPaymentMethodAndStatusSupplier.getPaymentMethod()
                : PaymentMethod.NULL
        )
        .reportingRequestBuilder(eventBuilder)
        .result(result)
        .shouldFetchPaymentDetails(true)
        .cleanAirZoneId(cleanAirZoneId)
        .cazEntryTimestamp(vehicleEntrant.getTimestamp())
        .build();
  }

  /**
   * Method to create result when vrn is not uk but vehicle is in whitelisting with category Non
   * UK.
   *
   * @param vehicleEntrant single request from ANPR
   * @return result with vrn and status
   */
  private InitialVehicleResult buildResultWithCompliantStatus(VehicleEntrantSaveDto vehicleEntrant,
      UUID cleanAirZoneId, boolean nonUkVehicle) {
    VehicleResultDto result = VehicleResultDto.builder()
        .vrn(vehicleEntrant.getVrn())
        .status(COMPLIANT.getStatus())
        .build();

    VehicleEntrantReportingRequest event = VehicleEntrantReportingRequest.builder()
        .correlationId(MdcCorrelationIdInjector.getCurrentValue())
        .cleanAirZoneId(cleanAirZoneId)
        .vrnHash(Sha2Hasher.sha256Hash(vehicleEntrant.getVrn()))
        .hour(parseEntrantTimestamp(vehicleEntrant.getTimestamp()))
        .chargeValidityCode(getChargeValidity(result).getChargeValidityCode())
        .exemptionReason("Non-UK Vehicle")
        .nonStandardUkPlateFormat(nonUkVehicle).build();

    return InitialVehicleResult.builder()
        .result(result)
        .reportingRequest(event)
        .shouldFetchPaymentDetails(false)
        .build();
  }

  /**
   * Method to create result without tariff code.
   *
   * @param vehicleEntrant single request from ANPR
   * @param cleanAirZoneId clean air zone
   * @param vehicle information from DVLA
   * @param calculationResult information about exempt, compliant, chargeable
   * @param ntrData response from NTR
   * @return result with out tariff code
   */
  private InitialVehicleResult buildResultWithoutTariffCode(
      VehicleEntrantSaveDto vehicleEntrant, UUID cleanAirZoneId,
      Vehicle vehicle, CalculationResult calculationResult,
      SingleLicenceData ntrData) {
    // assertion: calculationResult.isExempt() is false

    if (calculationResult.getCompliant() || calculationResult.getIsRetrofitted()) {
      calculationResult.setCharge(0);
    }

    if (shouldNotBeCharged(calculationResult)) {
      VehicleResultDto result = VehicleResultDto.builder()
          .vrn(vehicleEntrant.getVrn())
          .make(vehicle.getMake())
          .model(vehicle.getModel())
          .colour(vehicle.getColour())
          .typeApproval(vehicle.getTypeApproval())
          .status(VehicleComplianceStatus.COMPLIANT.getStatus())
          .paymentMethod(PaymentMethod.NULL.toDtoString())
          .exemptionCode(null)
          .isTaxiOrPhv(vehicle.getIsTaxiOrPhv())
          .tariffCode(null)
          .licensingAuthority(getLicensingAuthority(ntrData))
          .entrantPaymentId(null)
          .build();

      String exemptionReason = calculationResult.isExempt()
          ? exemptionService.identifyExemptionReason(vehicle) : null;

      VehicleEntrantReportingRequest event = VehicleEntrantReportingRequest.builder()
          .correlationId(MdcCorrelationIdInjector.getCurrentValue())
          .cleanAirZoneId(cleanAirZoneId)
          .vrnHash(Sha2Hasher.sha256Hash(vehicleEntrant.getVrn()))
          .hour(parseEntrantTimestamp(vehicleEntrant.getTimestamp()))
          .fuelType(vehicle.getFuelType())
          .model(vehicle.getModel())
          .make(vehicle.getMake())
          .colour(vehicle.getColour())
          .typeApproval(vehicle.getTypeApproval())
          .chargeValidityCode(getChargeValidity(result).getChargeValidityCode())
          .vehicleType(vehicle.getVehicleType())
          .exemptionReason(exemptionReason)
          .taxiPhvDescription(getTaxiPhvDescription(ntrData))
          .licensingAuthorities(getLicensingAuthority(ntrData))
          .nonStandardUkPlateFormat(false).build();

      return InitialVehicleResult.builder()
          .result(result)
          .shouldFetchPaymentDetails(false)
          .reportingRequest(event)
          .build();
    }
    // vehicle should be charged

    VehicleResultDto result = VehicleResultDto.builder()
        .vrn(vehicleEntrant.getVrn())
        .make(vehicle.getMake())
        .model(vehicle.getModel())
        .colour(vehicle.getColour())
        .typeApproval(vehicle.getTypeApproval())
        .exemptionCode(null)
        .isTaxiOrPhv(vehicle.getIsTaxiOrPhv())
        .tariffCode(null)
        .licensingAuthority(getLicensingAuthority(ntrData))
        .build();

    Function<VehicleResultDto, VehicleEntrantReportingRequest> eventBuilder = finalResult -> {
      String exemptionReason = calculationResult.isExempt()
          ? exemptionService.identifyExemptionReason(vehicle)
          : null;
      return VehicleEntrantReportingRequest.builder()
          .correlationId(MdcCorrelationIdInjector.getCurrentValue())
          .cleanAirZoneId(cleanAirZoneId)
          .vrnHash(Sha2Hasher.sha256Hash(vehicleEntrant.getVrn()))
          .hour(parseEntrantTimestamp(vehicleEntrant.getTimestamp()))
          .fuelType(vehicle.getFuelType())
          .model(vehicle.getModel())
          .make(vehicle.getMake())
          .colour(vehicle.getColour())
          .typeApproval(vehicle.getTypeApproval())
          .chargeValidityCode(getChargeValidity(finalResult).getChargeValidityCode())
          .vehicleType(vehicle.getVehicleType())
          .exemptionReason(exemptionReason)
          .taxiPhvDescription(getTaxiPhvDescription(ntrData))
          .licensingAuthorities(getLicensingAuthority(ntrData))
          .nonStandardUkPlateFormat(false).build();

    };

    return InitialVehicleResult.builder()
        .complianceStatusProvider(paymentStatus -> paymentStatus == PaymentStatus.PAID
            ? NOT_COMPLIANT_PAID
            : NOT_COMPLIANT_NOT_PAID
        )
        .paymentMethodProvider(entrantPaymentMethodAndStatusSupplier ->
            entrantPaymentMethodAndStatusSupplier.getPaymentStatus() == PaymentStatus.PAID
                ? entrantPaymentMethodAndStatusSupplier.getPaymentMethod()
                : PaymentMethod.NULL
        )
        .reportingRequestBuilder(eventBuilder)
        .result(result)
        .shouldFetchPaymentDetails(true)
        .cleanAirZoneId(cleanAirZoneId)
        .cazEntryTimestamp(vehicleEntrant.getTimestamp())
        .build();
  }

  private String getTaxiPhvDescription(SingleLicenceData ntrData) {
    return ntrData.getLicence()
        .map(TaxiPhvLicenseInformationResponse::getDescription)
        .orElse(null);
  }

  /**
   * Helper method for updating a Vehicle to include taxi details.
   *
   * @param licenceData Precomputed NTR data details.
   * @param vehicle Vehicle whose details are to be updated.
   */
  private void checkTaxiStatus(SingleLicenceData licenceData, Vehicle vehicle) {
    // assertion: licenceData succeeded, i.e. licenceData.hasFailed() is false
    Optional<TaxiPhvLicenseInformationResponse> licenceInfo = licenceData.getLicence();
    boolean hasActiveLicence = licenceInfo
        .map(TaxiPhvLicenseInformationResponse::isActiveAndNotExpired)
        .orElse(Boolean.FALSE);
    if (hasActiveLicence) {
      // We do not set the VehicleType to TAXI_OR_PHV here as we need to retain the original
      // VehicleType for the compliance check.
      vehicle.setIsTaxiOrPhv(true);
      vehicle.setIsWav(
          // we could use just `licenceInfo.get().getWheelchairAccessible()`, but sonar complains
          licenceInfo.filter(TaxiPhvLicenseInformationResponse::isActiveAndNotExpired)
              .map(TaxiPhvLicenseInformationResponse::getWheelchairAccessible)
              .orElse(null)
      );
    } else {
      vehicle.setIsTaxiOrPhv(false);
    }
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
    if (vehicle.getIsTaxiOrPhv()) {
      vehicle.setVehicleType(VehicleType.TAXI_OR_PHV);
    }
    float charge = chargeabilityService.getCharge(vehicle, tariffDetails);
    calculationResult.setCharge(charge);
  }

  /**
   * Helper method to set vehicleType.
   */
  private Vehicle setVehicleTypeAndTaxiOrPhv(Vehicle vehicle, boolean isTaxi) {
    vehicleIdentificationService.setVehicleType(vehicle);
    vehicle.setIsTaxiOrPhv(isTaxi);
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
      CalculationResult calculationResult, PreFetchedDataResults preFetchedDataResults) {
    boolean isCompliantOnGeneralWhitelist =
        generalWhitelistService.isCompliantOnGeneralPurposeWhitelist(
            vehicle.getRegistrationNumber(),
            preFetchedDataResults.getMatchedGeneralWhitelistVehicles());

    boolean isRetrofitVehicle =
        retrofitService.isRetrofitVehicle(vehicle.getRegistrationNumber(),
            preFetchedDataResults.getMatchedRetrofittedVehicles());

    return complianceService.updateCalculationResult(vehicle, calculationResult,
        isCompliantOnGeneralWhitelist, isRetrofitVehicle);
  }

  /**
   * Helper method to check if the vehicle is exempt.
   */
  private CalculationResult updateExempt(Vehicle vehicle,
      PreFetchedDataResults preFetchedDataResults) {
    boolean isMilitary = militaryVehicleService
        .isMilitaryVrn(vehicle.getRegistrationNumber(), preFetchedDataResults);

    boolean isExemptOnGeneralWhitelist = generalWhitelistService
        .isExemptOnGeneralPurposeWhitelist(vehicle.getRegistrationNumber(),
            preFetchedDataResults.getMatchedGeneralWhitelistVehicles());

    return exemptionService.updateCalculationResult(vehicle,
        new CalculationResult(), isMilitary, isExemptOnGeneralWhitelist);
  }

  /**
   * tariffCode - A code that uniquely identifies the tariff that determines the charge that the
   * vehicle is liable to pay. Will always be provided when status is equal to: (notCompliantPaid,
   * notCompliantNotPaid, unrecognisedPaid, unrecognisedNotPaid). A null value will be provided
   * otherwise.
   */
  private static String prepareTariffCode(VehicleType vehicleType, String chargeIdentifier,
      VehicleComplianceStatus status) {
    // TODO At the moment there is not possible to create tariffCode for unrecognisedPaid
    //  and unrecognisedNotPaid status

    if (status == NOT_COMPLIANT_NOT_PAID
        || status == NOT_COMPLIANT_PAID) {
      return formatTariffCode(vehicleType.toString(), chargeIdentifier);
    }
    return null;
  }

  /**
   * Method to properly format tariff code.
   *
   * @param vehicleType {@link VehicleType}
   * @param chargeIdentifier charge identifier
   * @return properly formatted tariff code
   */
  static String formatTariffCode(String vehicleType, String chargeIdentifier) {
    return chargeIdentifier + "-" + vehicleType.toUpperCase();
  }

  /**
   * Method to check if vehicle should not be charged.
   */
  private boolean shouldNotBeCharged(CalculationResult calculationResult) {
    return !shouldBeCharged(calculationResult);
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

  private boolean getTaxiLicenseStatus(SingleLicenceData licensesInformation) {
    return licensesInformation.getLicence()
        .map(TaxiPhvLicenseInformationResponse::isActiveAndNotExpired)
        .orElse(Boolean.FALSE);
  }

  private List<String> getLicensingAuthority(SingleLicenceData licence) {
    return licence.getLicence()
        .filter(TaxiPhvLicenseInformationResponse::isActiveAndNotExpired)
        .map(TaxiPhvLicenseInformationResponse::getLicensingAuthoritiesNames)
        .orElse(null);
  }

  private static String parseEntrantTimestamp(LocalDateTime entrantTimestamp) {
    LocalDateTime hour = entrantTimestamp.withMinute(0).withSecond(0).withNano(0);
    return hour.format(DateTimeFormatter.ISO_DATE_TIME);
  }
}