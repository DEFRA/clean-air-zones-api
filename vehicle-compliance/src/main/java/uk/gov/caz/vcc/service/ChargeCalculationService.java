package uk.gov.caz.vcc.service;

import static uk.gov.caz.vcc.service.VehicleEntrantsService.formatTariffCode;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.opencsv.bean.CsvBindAndJoinByPosition;
import com.opencsv.bean.CsvBindByPosition;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MultiValuedMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.caz.common.util.Strings;
import uk.gov.caz.definitions.domain.Vehicle;
import uk.gov.caz.definitions.domain.VehicleType;
import uk.gov.caz.definitions.dto.CleanAirZoneDto;
import uk.gov.caz.definitions.dto.CleanAirZonesDto;
import uk.gov.caz.definitions.dto.ComplianceOutcomeDto;
import uk.gov.caz.definitions.dto.ComplianceResultsDto;
import uk.gov.caz.definitions.dto.InformationUrlsDto;
import uk.gov.caz.vcc.domain.CalculationResult;
import uk.gov.caz.vcc.domain.GeneralWhitelistVehicle;
import uk.gov.caz.vcc.domain.RetrofittedVehicle;
import uk.gov.caz.vcc.domain.TariffDetails;
import uk.gov.caz.vcc.domain.exceptions.ExternalResourceNotFoundException;
import uk.gov.caz.vcc.domain.exceptions.ExternalServiceCallException;
import uk.gov.caz.vcc.domain.service.ChargeabilityService;
import uk.gov.caz.vcc.domain.service.ComplianceService;
import uk.gov.caz.vcc.domain.service.VehicleIdentificationService;
import uk.gov.caz.vcc.domain.service.bulkchecker.BulkCheckerUtility;
import uk.gov.caz.vcc.dto.NtrAndDvlaVehicleData;
import uk.gov.caz.vcc.dto.PreFetchedDataResults;
import uk.gov.caz.vcc.dto.SingleDvlaVehicleData;
import uk.gov.caz.vcc.dto.SingleLicenceData;
import uk.gov.caz.vcc.dto.TaxiPhvLicenseInformationResponse;
import uk.gov.caz.vcc.repository.TariffDetailsRepository;
import uk.gov.caz.vcc.repository.VehicleDetailsRepository;

/**
 * Facade Application service to coordinate the various Domain services that
 * determine the charge for a given vehicle.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChargeCalculationService {

  private static final String PHGV_APPLICABLE_BODY_TYPE_1 =
      "MOTOR HOME/CARAVAN";
  private static final String PHGV_APPLICABLE_BODY_TYPE_2 = "LIVESTOCK CARRIER";
  private static final String PHGV_APPLICABLE_TAX_CLASS = "PRIVATE HGV";

  private final VehicleIdentificationService vehicleIdentificationService;
  private final VehicleDetailsRepository vehicleDetailsRepository;
  private final TariffDetailsRepository tariffDetailsRepository;
  private final ExemptionService exemptionService;
  private final ComplianceService complianceService;
  private final ChargeabilityService chargeabilityService;
  private final GeneralWhitelistService generalWhitelistService;
  private final RetrofitService retrofitService;
  private final MilitaryVehicleService militaryVehicleService;
  private final NationalTaxiRegisterService nationalTaxiRegisterService;
  private final CazTariffService tariffService;
  private final LicenseAndVehicleProvider licenseAndVehicleProvider;

  @Value("${services.bulk-checker-batch-size:30}")
  private int bulkCheckerBatchSize;

  @Value("${services.bulk-checker-wait-interval:500}")
  private int waitIntervalMs;

  @VisibleForTesting
  static final String exemptionNote = "Exempt from charges.";

  @VisibleForTesting
  static final String vehicleNotFoundNote = "Vehicle details not found.";

  @VisibleForTesting
  static final String vehicleDetailsIncompleteNote =
      "Vehicle details incomplete.";

  /**
   * Public method for checking the compliance of a vehicle against a list of
   * clean air zones.
   *
   * @param vrn The vehicle's registration number.
   * @param cleanAirZoneIds The identifiers of the clean air zones to be queried
   *        against for compliance checking purposes.
   * @return A response detailing the compliance of a vehicle in the requested
   *         clean air zones.
   * @throws NotFoundException if DVLA has no details for this VRN.
   */
  public ComplianceResultsDto checkVrnAgainstCaz(String vrn,
      List<UUID> cleanAirZoneIds) {

    List<ComplianceOutcomeDto> calculationDtoResults = new ArrayList<>();

    Vehicle vehicle = findVehicleOrThrow(vrn);

    vehicleIdentificationService.setVehicleType(vehicle);

    boolean isRetrofitted =
        retrofitService.isRetrofitted(vehicle.getRegistrationNumber());
    boolean isExempt = exemptionService
        .updateCalculationResult(vehicle, new CalculationResult()).isExempt();

    if (isExempt) {
      return buildComplianceResultsDto(vrn, vehicle, isRetrofitted, isExempt,
          calculationDtoResults, Optional.empty());
    }

    checkTaxiStatus(vehicle);

    calculationDtoResults = cleanAirZoneIds.stream()
        .map(id -> calculateComplianceOutcome(vehicle, id))
        .collect(Collectors.toList());

    return buildComplianceResultsDto(vrn, vehicle, isRetrofitted, isExempt,
        calculationDtoResults, Optional.empty());
  }

  /**
   * Private helper to construct a compliance result data transfer object used
   * for CSV outputs.
   *
   * @return a constructed compliance result data transfer object.
   */
  private ComplianceResultsDto buildComplianceResultsDto(String vrn,
      Vehicle vehicle, Boolean retrofitted, Boolean exempt,
      List<ComplianceOutcomeDto> complianceOutcomes, Optional<String> note) {
    return ComplianceResultsDto.builder().registrationNumber(vrn)
        .vehicleType(getStringifiedVehicleType(vehicle))
        .isRetrofitted(retrofitted).isExempt(exempt)
        .complianceOutcomes(complianceOutcomes).note(note.orElse(""))
        .phgvDiscountAvailable(computePhgvDiscountAvailabilityFor(vehicle))
        .build();
  }

  /**
   * Private method to calculate the compliance (charge) outcome for a given
   * Vehicle in a given CAZ.
   *
   * @param vehicle Vehicle to be checked against the CAZ
   * @param cleanAirZoneId Id of the CAZ to be checked
   * @return ComplianceOutcomeDto Serializable representation of the compliance
   *         outcome
   * @throws NotFoundException If a tariff cannot be found for the given Id
   */
  private ComplianceOutcomeDto calculateComplianceOutcome(Vehicle vehicle,
      UUID cleanAirZoneId) {

    CalculationResult result = new CalculationResult();
    result.setCazIdentifier(cleanAirZoneId);
    TariffDetails tariff = findTariffOrThrow(cleanAirZoneId);

    if (vehicle.getVehicleType() != null
        && vehicle.getVehicleType().equals(VehicleType.TAXI_OR_PHV)) {
      vehicleIdentificationService.setVehicleType(vehicle);
    }
    result = complianceService.updateCalculationResult(vehicle, result);
    String tariffCode = null;

    if (result.getCompliant()) {
      result.setCharge(0);
    } else {

      result = complianceService.updateCalculationResult(vehicle, result);

      if (result.getCompliant()) {
        result.setCharge(0);
      } else {

        if (vehicle.getIsTaxiOrPhv()) {
          // We now set the VehicleType to TAXI_OR_PHV to get the correct
          // charge.
          vehicle.setVehicleType(VehicleType.TAXI_OR_PHV);
        }

        result.setCharge(chargeabilityService.getCharge(vehicle, tariff));
        tariffCode = formatTariffCode(String.valueOf(vehicle.getVehicleType()),
            tariff.getChargeIdentifier());
      }
    }
    String operatorName = getOperatorNameFromCaz(cleanAirZoneId);
    return buildComplianceOutcome(cleanAirZoneId, tariff.getName(),
        result.getCharge(), tariffCode, tariff.getInformationUrls(),
        operatorName);
  }


  private String getOperatorNameFromCaz(UUID cleanAirZoneId) {
    CleanAirZonesDto cleanAirZoneSelectionListings =
        tariffService.getCleanAirZoneSelectionListings();
    String operatorName =
        cleanAirZoneSelectionListings.getCleanAirZones().stream()
            .filter(cleanAirZoneDto -> cleanAirZoneDto.getCleanAirZoneId()
                .equals(cleanAirZoneId))
            .findFirst().map(CleanAirZoneDto::getOperatorName)
            .orElseGet(() -> null);
    return operatorName;
  }

  /**
   * Helper method to get details of vehicle from DVLA.
   *
   * @param registrationNumber the vehicle registration number.
   * @return An instance of {@link Vehicle} that holds DVLA vehicle details.
   */
  private Vehicle findVehicleOrThrow(String registrationNumber) {
    return vehicleDetailsRepository.findByRegistrationNumber(registrationNumber)
        .orElseThrow(() -> new ExternalResourceNotFoundException(
            "Vehicle with VRN: '" + Strings.mask(registrationNumber)
                + "' not found when calculating compliance outcome"));
  }

  /**
   * Helper method to find a Tariff or, if one cannot be found, throw an error.
   *
   * @param cleanAirZoneId id to be searched in the tariff repository
   * @return Tariff with details populated from the tariff repository
   */
  private TariffDetails findTariffOrThrow(UUID cleanAirZoneId) {
    return tariffDetailsRepository.getTariffDetails(cleanAirZoneId)
        .orElseThrow(() -> new ExternalResourceNotFoundException(
            "Tariff not found for caz id: " + cleanAirZoneId.toString()));
  }

  /**
   * Helper method for updating a Vehicle to include taxi details.
   *
   * @param vehicle Vehicle whose details are to be updated.
   */
  private void checkTaxiStatus(Vehicle vehicle) {
    Optional<TaxiPhvLicenseInformationResponse> taxiLicense =
        nationalTaxiRegisterService
            .getLicenseInformation(vehicle.getRegistrationNumber());

    if (taxiLicense.isPresent() && taxiLicense.get().isActiveAndNotExpired()) {
      // We do not set the VehicleType to TAXI_OR_PHV here as we need to retain
      // the original
      // VehicleType for the compliance check.
      vehicle.setIsTaxiOrPhv(true);
      vehicle.setIsWav(taxiLicense.get().getWheelchairAccessible());
    } else {
      vehicle.setIsTaxiOrPhv(false);
    }
  }

  /**
   * Private helper to construct a compliance outcome data transfer object.
   *
   * @param cleanAirZoneId the identifier of a clean air zone.
   * @param tariffName the full name of a tariff.
   * @param charge the charge a vehicle is subject to.
   * @param tariffCode the tariff code for the applied tariff rates.
   * @param informationUrls informational URLs containing links to LA published
   *        materials.
   * @return a constructed compliance outcome data transfer object.
   */
  private ComplianceOutcomeDto buildComplianceOutcome(UUID cleanAirZoneId,
      String tariffName, float charge, String tariffCode,
      InformationUrlsDto informationUrls, String operatorName) {
    return ComplianceOutcomeDto.builder().cleanAirZoneId(cleanAirZoneId)
        .name(tariffName).charge(charge).tariffCode(tariffCode)
        .informationUrls(informationUrls).operatorName(operatorName).build();
  }

  // ========================== //
  // Begin Bulk Checker methods //
  // ========================== //

  /**
   * Takes a list of VRNs and calculates their compliance, wrapping the result
   * in an instance of {@link CsvOutput}.
   *
   * @param cleanAirZones a list of {@link CleanAirZoneDto}
   * @throws InterruptedException if the main thread is interrupted
   */
  public List<CsvOutput> getComplianceCheckAsCsv(List<String> vrns,
      List<CleanAirZoneDto> cleanAirZones) throws InterruptedException {
    log.info(String.format("Received %s vrns in CSV bulk check input file",
        vrns.size()));
    log.info("Successfully retrieved clean air zone ids");

    List<ComplianceResultsDto> complianceOutcomes =
        new ArrayList<ComplianceResultsDto>();

    List<List<String>> batchedVrns =
        Lists.partition(vrns, bulkCheckerBatchSize);

    for (List<String> vrnBatch : batchedVrns) {
      log.info(String.format("Processing batch of %s vrns", vrnBatch.size()));
      complianceOutcomes.addAll(bulkComplianceCheck(vrnBatch, null));
      log.info(String.format("Waiting for %s ms before processing next batch",
          waitIntervalMs));
      Thread.sleep(waitIntervalMs);
    }

    log.info("All batched vrns processed");
    log.info(String.format("Final result size to be written to output %s",
        complianceOutcomes.size()));
    log.info("Writing output to CSV");

    return BulkCheckerUtility.toCsvOutput(complianceOutcomes, cleanAirZones);
  }

  /**
   * Calculate charges applicable to vehicles if they enter CAZ.
   *
   * @param vrns list of vrns
   * @param zones a list of clean air zones to perform a bulk check against.
   *        Note that if this list is null or empty, all clean air zones will be
   *        queried.
   * @return List of {@link ChargeCalculationService.CsvOutput}
   * @throws InterruptedException if the main thread is interrupted
   */
  public List<ComplianceResultsDto> bulkComplianceCheck(List<String> vrns,
      List<UUID> zones) throws InterruptedException {

    // Convert list of VRNs to set for method compatibility
    Set<String> vrnSet = Sets.newHashSet(vrns);

    // Pre-fetch GPW, Retrofit and MOD data prior to processing (as this is
    // inspected for all vehicles)
    PreFetchedDataResults preFetchedDataResults = computePrefetchedData(vrnSet);
    
    log.info("Fetching NTR and Remote Vehicle data responses as part of a bulk check");
    
    // Create the vehicle and taxi license information provider
    licenseAndVehicleProvider.prefetch(vrns);
    
    log.info("Successfully fetched NTR and Remote Vehicle data responses");
    
    // Create placeholders for vehicle data and taxi license responses
    Map<String, Vehicle> vehicles = new HashMap<String, Vehicle>();
    Map<String, TaxiPhvLicenseInformationResponse> vrnToTaxiPhvLicenseInfoMapper =
        new HashMap<String, TaxiPhvLicenseInformationResponse>();

    List<String> unableToProcessedVrns = new ArrayList<String>();

    log.info("Iterating NTR and Remote Vehicle data responses");
    
    // Iterate the received VRNs populating their data, to ensure the vehicle
    // type is always present in outputs
    for (String vrn : vrnSet) {
      NtrAndDvlaVehicleData licenseAndVehicle =
          licenseAndVehicleProvider.findLicenseAndVehicle(vrn);

      SingleDvlaVehicleData dvlaVehicle =
          licenseAndVehicle.getDvlaVehicleData();
      Vehicle processedVehicle = dvlaVehicle.getVehicle();

      if (dvlaVehicle.hasFailed()) {
        unableToProcessedVrns.add(vrn);
      } else {
        vehicleIdentificationService.setVehicleType(processedVehicle);
        vehicles.put(vrn, processedVehicle);
      }

      SingleLicenceData ntrVehicleData = licenseAndVehicle.getNtrVehicleData();

      Optional<TaxiPhvLicenseInformationResponse> optionalLicence =
          ntrVehicleData.getLicence();
      if (optionalLicence.isPresent()) {
        vrnToTaxiPhvLicenseInfoMapper.put(vrn, optionalLicence.get());
      }
    }

    // Exempt all GPW and MOD vehicles based on pre-fetched data results
    Set<String> exemptVehicleVrns = new HashSet<String>();

    // Inspect pre-fetched data to identify exempt vehicles
    List<String> gwlExemptedVrns = preFetchedDataResults
        .getMatchedGeneralWhitelistVehicles().stream()
        .filter(gpwVehicle -> gpwVehicle.isExempt())
        .map(gpwVehicle -> gpwVehicle.getVrn()).collect(Collectors.toList());

    exemptVehicleVrns.addAll(gwlExemptedVrns);
    exemptVehicleVrns.addAll(preFetchedDataResults.getMatchedMilitaryVrns());

    // Add all vehicles deemed exempt by their fuel types etc.
    exemptVehicleVrns.addAll(vehicles.entrySet().stream()
        .filter(entry -> exemptionService.isVehicleExempted(entry.getValue()))
        .map(Entry::getKey).collect(Collectors.toSet()));

    // Create placeholder for compliance result outputs
    List<ComplianceResultsDto> complianceResults = new ArrayList<>();

    // Filter vehicle information that will yield a non-chargeable result ahead
    // of
    // inspecting particulars (e.g. fuel types). Note that taxis are excluded as
    // they can be deemed
    // chargeable regardless of compliance
    List<String> gwlCompliantVrns = preFetchedDataResults
        .getMatchedGeneralWhitelistVehicles().stream()
        .filter(gpwVehicle -> gpwVehicle.isCompliant()
            && !vrnToTaxiPhvLicenseInfoMapper.containsKey(gpwVehicle.getVrn()))
        .map(gpwVehicle -> gpwVehicle.getVrn()).collect(Collectors.toList());

    // Get list of tariffs details for each Clean Air Zone
    Map<UUID, TariffDetails> tariffDetailsByCaz = getCleanAirZoneTariffMap();

    List<UUID> cleanAirZoneIds;

    // If zones have been supplied (as an optional parameter) filter down to
    // just the requested clean air zones
    if (zones != null && !zones.isEmpty()) {
      cleanAirZoneIds = zones;
    } else {
      cleanAirZoneIds = Lists.newArrayList(tariffDetailsByCaz.keySet());
    }

    // Populate exempt and compliant results based on pre-fetched data
    complianceResults.addAll(buildNoChargeableResponseFor(exemptVehicleVrns,
        vehicles, cleanAirZoneIds, true));
    complianceResults.addAll(buildNoChargeableResponseFor(gwlCompliantVrns,
        vehicles, cleanAirZoneIds, false));

    // Limit down the number of vehicles that need full chargeability checks
    // executing
    List<Vehicle> vehiclesThatRequireChargeAssessment =
        vehicles.entrySet().stream()
            .filter(entry -> !gwlExemptedVrns.contains(entry.getKey())
                && !gwlCompliantVrns.contains(entry.getKey())
                && !preFetchedDataResults.getMatchedMilitaryVrns()
                    .contains(entry.getKey())
                && !exemptVehicleVrns.contains(entry.getKey())
                && !unableToProcessedVrns.contains(entry.getKey()))
            .map(Entry::getValue).collect(Collectors.toList());

    // Populate full list of compliance results based on those which need
    // assessed
    Map<String, List<CalculationResult>> chargeabilityCheckResult =
        complianceService.bulkComplianceCheck(
            vehiclesThatRequireChargeAssessment, cleanAirZoneIds,
            preFetchedDataResults);

    // build response for unprocessed vrns
    complianceResults.addAll(buildNullResponseFor(unableToProcessedVrns.stream()
        .filter(
            vrn -> !preFetchedDataResults.getMatchedMilitaryVrns().contains(vrn)
                && !exemptVehicleVrns.contains(vrn))
        .collect(Collectors.toList()), vehicles));
    Map<UUID, String> cazIdOperatorNameMap = tariffService
        .getCleanAirZoneSelectionListings().getCleanAirZones().stream()
        .collect(Collectors.toMap(CleanAirZoneDto::getCleanAirZoneId,
            CleanAirZoneDto::getOperatorName));
    complianceResults.addAll(vehiclesThatRequireChargeAssessment.stream()
        .map(buildBulkComplianceResponse(vrnToTaxiPhvLicenseInfoMapper,
            tariffDetailsByCaz, chargeabilityCheckResult, preFetchedDataResults,
            cazIdOperatorNameMap))
        .collect(Collectors.toList()));

    return complianceResults;

  }

  /**
   * Build bulk compliance check response for vehicles whose compliance are
   * undetermined.
   *
   * @param vrnEntries List of vrns.
   * @param vehicles Mapper that map vrn to vehicle.
   */
  private List<ComplianceResultsDto> buildNullResponseFor(List<String> vrns,
      Map<String, Vehicle> vehicles) {
    return vrns.stream()
        .map(vrn -> buildComplianceResultsDto(vrn, vehicles.get(vrn), false,
            false, Collections.emptyList(), Optional.of(vehicleNotFoundNote)))
        .collect(Collectors.toList());
  }

  /**
   * Private helper method to retrieve a map of clean air zone tariffs.
   *
   * @return a map of clean air zone ids, paired with their respective tariffs.
   */
  private Map<UUID, TariffDetails> getCleanAirZoneTariffMap() {
    // Get tariffs details for each Clean Air Zone
    Map<UUID, TariffDetails> tariffDetailsByCaz =
        new HashMap<UUID, TariffDetails>();
    CleanAirZonesDto cleanAirZones =
        tariffService.getCleanAirZoneSelectionListings();
    for (CleanAirZoneDto cleanAirZone : cleanAirZones.getCleanAirZones()) {
      Optional<TariffDetails> tariffDetails = tariffService
          .getTariffDetailsForGivenCazId(cleanAirZone.getCleanAirZoneId());
      if (tariffDetails.isPresent()) {
        tariffDetailsByCaz.put(cleanAirZone.getCleanAirZoneId(),
            tariffDetails.get());
      }
    }
    return tariffDetailsByCaz;
  }

  /**
   * Method to pre-fetch register data for a set of VRNs covering retrofit,
   * General Purpose Whitelist and MOD status.
   * 
   * @param submittedVrns the series of VRNs to be queried.
   * @return a pre-fetched data wrapper containing vehicle register results.
   */
  public PreFetchedDataResults computePrefetchedData(
      Set<String> submittedVrns) {
    log.info(
        "Starting to compute prefetched data from MOD | GPW + Retrofit in parallel");

    CompletableFuture<List<List>> gpwAndRetrofitFuture =
        CompletableFuture.supplyAsync(() -> Lists.newArrayList(
            generalWhitelistService.findByVrns(submittedVrns),
            retrofitService.findByVrns(submittedVrns)));
    CompletableFuture<Set<String>> modFuture = CompletableFuture.supplyAsync(
        () -> militaryVehicleService.filterMilitaryVrnsFromList(submittedVrns));

    return CompletableFuture.allOf(gpwAndRetrofitFuture, modFuture)
        .thenApply(result -> {
          try {
            Set<String> matchedMilitaryVrns = modFuture.get();
            List<List> gpwAndRetrofitLists = gpwAndRetrofitFuture.get();
            List<GeneralWhitelistVehicle> matchedGpwVrns =
                gpwAndRetrofitLists.get(0);
            List<RetrofittedVehicle> matchedRetrofitVrns =
                gpwAndRetrofitLists.get(1);
            log.info(
                "MOD | GPW + Retrofit calls succeeded with {} vehicles in MOD, "
                    + "{} in GPW and "
                    + "{} in Retrofit",
                matchedMilitaryVrns.size(), matchedGpwVrns.size(),
                matchedRetrofitVrns.size());
            return new PreFetchedDataResults(matchedGpwVrns,
                matchedRetrofitVrns, matchedMilitaryVrns);
          } catch (InterruptedException | ExecutionException e) {
            log.error(
                "Exception while trying to run MOD | GPW + Retrofit calls in parallel");
            log.error(e.getMessage());
            throw new ExternalServiceCallException(e);
          }
        }).join();
  }

  /**
   * Checks whether the passed {@code vehicle} is a private heavy goods vehicle.
   * The vehicle is considered a private heavy goods vehicle if its tax class is
   * equal to 'PRIVATE HGV' and body type to either 'MOTOR HOME/CARAVAN' or
   * 'LIVESTOCK CARRIER'
   */
  private boolean computePhgvDiscountAvailabilityFor(
      @Nullable Vehicle vehicle) {
    return !Objects.isNull(vehicle) && matchesPhgvTaxClass(vehicle)
        && matchesPhgvBodyType(vehicle)
        && vehicleIsEligibleForBathPhgvDiscount(vehicle);
  }

  /**
   * Returns true if 'body type' is equal to either 'MOTOR HOME/CARAVAN' or
   * 'LIVESTOCK CARRIER', false otherwise.
   */
  private boolean matchesPhgvBodyType(Vehicle vehicle) {
    return PHGV_APPLICABLE_BODY_TYPE_1.equalsIgnoreCase(vehicle.getBodyType())
        || PHGV_APPLICABLE_BODY_TYPE_2.equalsIgnoreCase(vehicle.getBodyType());
  }

  /**
   * Returns true if the vehicle's tax class is equal to 'PRIVATE HGV', false
   * otherwise.
   */
  private boolean matchesPhgvTaxClass(Vehicle vehicle) {
    return PHGV_APPLICABLE_TAX_CLASS.equalsIgnoreCase(vehicle.getTaxClass());
  }

  /**
   * Returns true if a vehicle is eligible for the Bath PHGV discount offering.
   */
  private boolean vehicleIsEligibleForBathPhgvDiscount(Vehicle vehicle) {
    return vehicle.getVehicleType() == VehicleType.HGV
        || vehicle.getVehicleType() == VehicleType.BUS;
  }

  /**
   * Gets vehicle's type if present, {@code null} otherwise.
   */
  private String getStringifiedVehicleType(@Nullable Vehicle vehicle) {
    return Objects.isNull(vehicle) || Objects.isNull(vehicle.getVehicleType())
        ? null
        : vehicle.getVehicleType().toString();
  }


  /**
   * Build bulk checker response from compliance check result.
   *
   * @param vrnToTaxiPhvLicenseInfoMapper Mapper that map vrn to Taxi/Phv
   *        Information
   * @param tariffDetailsByCaz Tariff details used to calculate vehicle charge
   * @param complianceCheckResult Compliance check result
   * @param preFetchedDataResults Prefetched data results
   * @param cazIdOperatorNameMap map of names and UUID of Clean Air Zones
   */
  private Function<? super Vehicle, ? extends ComplianceResultsDto> buildBulkComplianceResponse(
      Map<String, TaxiPhvLicenseInformationResponse> vrnToTaxiPhvLicenseInfoMapper,
      Map<UUID, TariffDetails> tariffDetailsByCaz,
      Map<String, List<CalculationResult>> complianceCheckResult,
      PreFetchedDataResults preFetchedDataResults,
      Map<UUID, String> cazIdOperatorNameMap) {
    return v -> {
      List<ComplianceOutcomeDto> complianceOutcomes = new ArrayList<>();
      String vrn = v.getRegistrationNumber();
      String vehicleType = v.getVehicleType() == null ? null
          : String.valueOf(v.getVehicleType());
      complianceCheckResult.get(vrn).forEach(calculationResult -> {
        UUID cleanAirZoneId = calculationResult.getCazIdentifier();
        float charge = 0;
        TariffDetails tariffDetails = tariffDetailsByCaz.get(cleanAirZoneId);
        String tariffCode = vehicleType != null
            ? formatTariffCode(vehicleType, tariffDetails.getChargeIdentifier())
            : null;

        if (calculationResult.getCompliant() != null) {
          TaxiPhvLicenseInformationResponse taxiPhvLicenseInformationResponse =
              vrnToTaxiPhvLicenseInfoMapper.get(vrn);

          if (taxiPhvLicenseInformationResponse != null) {
            v.setIsTaxiOrPhv(true);
            if (v.getVehicleType().equals(VehicleType.TAXI_OR_PHV)) {
              vehicleIdentificationService.setVehicleType(v);
            }
            v.setIsWav(
                taxiPhvLicenseInformationResponse.getWheelchairAccessible());

            boolean gpwCompliant = generalWhitelistService
                .isCompliantOnGeneralPurposeWhitelist(v.getRegistrationNumber(),
                    preFetchedDataResults.getMatchedGeneralWhitelistVehicles());
            boolean retroFit =
                retrofitService.isRetrofitVehicle(v.getRegistrationNumber(),
                    preFetchedDataResults.getMatchedRetrofittedVehicles());

            calculationResult = complianceService.updateCalculationResult(v,
                calculationResult, gpwCompliant, retroFit);
            v.setVehicleType(VehicleType.TAXI_OR_PHV);
            tariffCode =
                formatTariffCode(String.valueOf(VehicleType.TAXI_OR_PHV),
                    tariffDetails.getChargeIdentifier());
          } else {
            v.setIsTaxiOrPhv(false);
          }

          if (!calculationResult.getCompliant()) {
            charge = chargeabilityService.getCharge(v, tariffDetails);
          }
          String operatorName = cazIdOperatorNameMap.get(cleanAirZoneId);
          complianceOutcomes
              .add(buildComplianceOutcome(calculationResult.getCazIdentifier(),
                  tariffDetails.getName(), charge, tariffCode,
                  tariffDetails.getInformationUrls(), operatorName));
        } else {
          // unable to determine vehicle compliance
          charge = -1;
        }
      });
      boolean unableToDetermineCompliance =
          complianceCheckResult.get(vrn).stream().anyMatch(
              calculationResult -> calculationResult.getCompliant() == null);
      boolean isRetrofitted = complianceCheckResult.get(vrn).stream()
          .anyMatch(calculationResult -> calculationResult.getIsRetrofitted());
      return buildComplianceResultsDto(v.getRegistrationNumber(), v,
          isRetrofitted, false, complianceOutcomes,
          unableToDetermineCompliance
              ? Optional.of(vehicleDetailsIncompleteNote)
              : Optional.empty());
    };
  }

  /**
   * Build bulk compliance check response for charge free vehicles.
   *
   * @param vrns List of exempted vrns.
   * @param vehicles Mapper that map vrn to vehicle.
   */
  private List<ComplianceResultsDto> buildNoChargeableResponseFor(
      Collection<String> vrns, Map<String, Vehicle> vehicles,
      List<UUID> cleanAirZoneIds, boolean exempt) {
    if (vrns.isEmpty()) {
      return Collections.emptyList();
    }

    List<ComplianceOutcomeDto> outcomes = cleanAirZoneIds.stream()
        .map(c -> buildComplianceOutcome(c, null, 0, null, null, null))
        .collect(Collectors.toList());

    Optional<String> note;

    if (exempt) {
      note = Optional.of(exemptionNote);
    } else {
      note = Optional.empty();
    }

    return vrns.stream().map(vrn -> buildComplianceResultsDto(vrn,
        vehicles.get(vrn), false, exempt, outcomes, note))
        .collect(Collectors.toList());
  }

  @Getter
  @Setter
  public static class CsvOutput {

    @CsvBindByPosition(position = 0)
    private String vrn;

    @CsvBindByPosition(position = 1)
    private String vehicleType;

    @CsvBindAndJoinByPosition(position = "2-", elementType = String.class)
    private MultiValuedMap<Integer, String> charges;
  }
}
