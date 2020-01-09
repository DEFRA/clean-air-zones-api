package uk.gov.caz.vcc.service;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.emptyList;

import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import uk.gov.caz.vcc.domain.CalculationResult;
import uk.gov.caz.vcc.domain.Vehicle;
import uk.gov.caz.vcc.domain.service.VehicleIdentificationService;
import uk.gov.caz.vcc.dto.TaxiPhvLicenseInformationResponse;
import uk.gov.caz.vcc.dto.VehicleDto;
import uk.gov.caz.vcc.repository.VehicleDetailsRepository;

/**
 * Application service to grab vehicle details and cast to a DTO for the /details endpoint.
 *
 * @author informed
 */
@Slf4j
@Service
public class VehicleService {

  private static final TaxiPhvLicenseInformationResponse DEFAULT_LICENSE_INFO =
      TaxiPhvLicenseInformationResponse.builder()
          .active(false)
          .wheelchairAccessible(null)
          .licensingAuthoritiesNames(emptyList())
          .build();

  private final NationalTaxiRegisterService nationalTaxiRegisterService;
  private final VehicleDetailsRepository vehicleDetailsRepository;
  private final VehicleIdentificationService vehicleIdentificationService;
  private final RetrofitService retrofitService;
  private final MilitaryVehicleService militaryVehicleService;
  private final ExemptionService exemptionService;

  /**
   * Public constructor for the VehicleService.
   *
   * @param nationalTaxiRegisterService service to retrieve taxi details.
   * @param vehicleDetailsRepository repository implementation for fetching remotely stored
   *     vehicle details.
   * @param vehicleIdentificationService service to identify a vehicle, given other data
   * @param retrofitService service for querying whether a vehicle has been retrofitted.
   * @param militaryVehicleService service for querying whether a vehicle is classified as
   *     military.
   * @param exemptionService service to determine exemption status for a given vehicle.
   */
  public VehicleService(NationalTaxiRegisterService nationalTaxiRegisterService,
      VehicleDetailsRepository vehicleDetailsRepository,
      VehicleIdentificationService vehicleIdentificationService,
      RetrofitService retrofitService,
      MilitaryVehicleService militaryVehicleService,
      ExemptionService exemptionService) {
    this.nationalTaxiRegisterService = nationalTaxiRegisterService;
    this.vehicleDetailsRepository = vehicleDetailsRepository;
    this.vehicleIdentificationService = vehicleIdentificationService;
    this.retrofitService = retrofitService;
    this.militaryVehicleService = militaryVehicleService;
    this.exemptionService = exemptionService;
  }

  /**
   * Public method to grab vehicle details, check for exemption and cast to a DTO.
   *
   * @param vrn RegistrationNumber of the vehicle whose details are to be found
   * @return VehicleDto containing vehicle details and isExempt
   */
  public Optional<VehicleDto> findVehicle(String vrn) {
    String strippedVrn = StringUtils.trimAllWhitespace(vrn);
    validateVrnFormat(vrn, strippedVrn);
    return this.findVehicleDetails(strippedVrn);
  }

  /**
   * Method to expose cache eviction for vehicle details.
   */
  @CacheEvict(value = { "vehicles" }, allEntries = true)
  public void cacheEvictVehicles() {
    log.info("Evicted vehicle details cache");
  }

  private void validateVrnFormat(String vrn, String strippedVrn) {
    checkNotNull(vrn, "Registration number can not be null");
    checkArgument(!vrn.isEmpty(), "Registration number can not be empty");
    checkArgument(!strippedVrn.isEmpty(),
        "Registration number can not contain only whitespaces.");
  }

  /**
   * Method for retrieving vehicle details for consumption by the compliance checker online service.
   * In the event that a vehicle has been retrofitted or is deemed a military vehicle this will
   * yield an early return as querying remote sources is not necessary.
   *
   * @param vrn the VRN of the vehicle to fetch details for.
   * @return an object detailing the particulars of a vehicle.
   */
  private Optional<VehicleDto> findVehicleDetails(String vrn) {

    // Yield immediate return if a vehicle is retrofitted
    if (retrofitService.isRetrofitted(vrn)) {
      return Optional.of(VehicleDto.builder().isExempt(true).build());
    }

    // Yield immediate return if a vehicle is military
    if (militaryVehicleService.isMilitaryVehicle(vrn)) {
      return Optional.of(VehicleDto.builder().isExempt(true).build());
    }

    Optional<Vehicle> vehicle = vehicleDetailsRepository.findByRegistrationNumber(vrn);

    // Yield without performing redundant checks for taxi status or vehicle type if exempt.
    if (vehicle.isPresent() && isVehicleExempt(vehicle.get())) {
      return vehicle
          .map(e -> VehicleDto.fromVehicle(e, true));
    } else {
      return vehicle
          .map(this::enhanceWithVehicleType)
          .map(e -> enhanceWithTaxiPhvStatus(vrn, e))
          .map(e -> VehicleDto.fromVehicle(e, false));
    }
  }
  
  /**
   * A method for determining if a vehicle is exempt.
   * 
   * @param vehicle Vehicle whose exemption is to be determined.
   * @return true if exempt, else false
   */
  private boolean isVehicleExempt(Vehicle vehicle) {
    return exemptionService.updateCalculationResult(
      vehicle, new CalculationResult())
      .getExempt();
  }
  
  /**
   * A method for enhancing a vehicle definition with a CAZ vehicle type classification.
   *
   * @param vehicle the vehicle to be assigned a CAZ classification.
   * @return an updated Vehicle definition with a CAZ classification assigned.
   */
  private Vehicle enhanceWithVehicleType(Vehicle vehicle) {
    vehicleIdentificationService.setVehicleType(vehicle);
    return vehicle;
  }

  /**
   * A method for enhancing a vehicle definition with a taxi or PHV status.
   *
   * @param vehicle the vehicle to be assigned a taxi or PHV status.
   * @return an updated Vehicle definition with a taxi or PHV status assigned.
   */
  private Vehicle enhanceWithTaxiPhvStatus(String vrn, Vehicle vehicle) {
    TaxiPhvLicenseInformationResponse taxiPhvStatus = nationalTaxiRegisterService
        .getLicenseInformation(vrn)
        .orElse(DEFAULT_LICENSE_INFO);
    List<String> licensingAuthoritiesNames = Optional
        .ofNullable(taxiPhvStatus.getLicensingAuthoritiesNames())
        .orElse(emptyList());
    vehicle.setIsTaxiOrPhv(taxiPhvStatus.isActive());
    vehicle.setIsWav(taxiPhvStatus.getWheelchairAccessible());
    vehicle.setLicensingAuthoritiesNames(licensingAuthoritiesNames);
    return vehicle;
  }

}
