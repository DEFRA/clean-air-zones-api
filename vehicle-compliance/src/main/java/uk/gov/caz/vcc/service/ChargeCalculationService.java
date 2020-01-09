package uk.gov.caz.vcc.service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javassist.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.caz.vcc.domain.CalculationResult;
import uk.gov.caz.vcc.domain.TariffDetails;
import uk.gov.caz.vcc.domain.Vehicle;
import uk.gov.caz.vcc.domain.VehicleType;
import uk.gov.caz.vcc.domain.exceptions.ExternalResourceNotFoundException;
import uk.gov.caz.vcc.domain.service.ChargeabilityService;
import uk.gov.caz.vcc.domain.service.ComplianceService;
import uk.gov.caz.vcc.domain.service.VehicleIdentificationService;
import uk.gov.caz.vcc.dto.ComplianceOutcomeDto;
import uk.gov.caz.vcc.dto.ComplianceResultsDto;
import uk.gov.caz.vcc.dto.TaxiPhvLicenseInformationResponse;
import uk.gov.caz.vcc.repository.TariffDetailsRepository;
import uk.gov.caz.vcc.repository.VehicleDetailsRepository;

/**
 * Facade Application service to coordinate the various Domain services that determine the charge
 * for a given vehicle.
 */
@Service
@RequiredArgsConstructor
public class ChargeCalculationService {

  private final VehicleIdentificationService vehicleIdentificationService;

  private final VehicleDetailsRepository vehicleDetailsRepository;

  private final TariffDetailsRepository tariffDetailsRepository;

  private final ExemptionService exemptionService;

  private final ComplianceService complianceService;
  
  private final ChargeabilityService chargeabilityService;

  private final RetrofitService retrofitService;

  private final NationalTaxiRegisterService nationalTaxiRegisterService;

  /**
   * Public method for checking the compliance of a vehicle against a list of clean air zones.
   *
   * @param vrn The vehicle's registration number.
   * @param cleanAirZoneIds The identifiers of the clean air zones to be queried against for
   *     compliance checking purposes.
   * @return A response detailing the compliance of a vehicle in the requested clean air zones.
   * @throws NotFoundException if DVLA has no details for this VRN.
   */
  public ComplianceResultsDto checkVrnAgainstCaz(String vrn,
      List<UUID> cleanAirZoneIds) {

    List<ComplianceOutcomeDto> calculationDtoResults = new ArrayList<>();

    Vehicle vehicle = findVehicleOrThrow(vrn);
    vehicleIdentificationService.setVehicleType(vehicle);

    boolean isRetrofitted = retrofitService.isRetrofitted(vehicle.getRegistrationNumber());
    boolean isExempt = exemptionService
        .updateCalculationResult(vehicle, new CalculationResult()).getExempt();

    if (isRetrofitted || isExempt) {
      return ComplianceResultsDto.builder()
          .registrationNumber(vrn)
          .isRetrofitted(isRetrofitted)
          .isExempt(isExempt)
          .complianceOutcomes(calculationDtoResults)
          .build();
    }

    addTaxiStatus(vehicle);

    calculationDtoResults = cleanAirZoneIds.stream()
        .map(id -> calculateComplianceOutcome(vehicle, id))
        .collect(Collectors.toList());

    return ComplianceResultsDto.builder()
        .registrationNumber(vrn)
        .isRetrofitted(isRetrofitted)
        .isExempt(isExempt)
        .complianceOutcomes(calculationDtoResults)
        .build();
  }

  /**
   * Private method to calculate the compliance (charge) outcome for a given Vehicle in a given
   * CAZ.
   *
   * @param vehicle Vehicle to be checked against the CAZ
   * @param cleanAirZoneId Id of the CAZ to be checked
   * @return ComplianceOutcomeDto Serializable representation of the compliance outcome
   * @throws NotFoundException If a tariff cannot be found for the given Id
   */
  private ComplianceOutcomeDto calculateComplianceOutcome(Vehicle vehicle,
      UUID cleanAirZoneId) {

    CalculationResult result = new CalculationResult();
    result.setCazIdentifier(cleanAirZoneId);

    TariffDetails tariff = findTariffOrThrow(cleanAirZoneId);

    complianceService.updateCalculationResult(vehicle, result);

    if (!result.getCompliant()) {
      result.setCharge(chargeabilityService.getCharge(vehicle, tariff));
    } else {
      result.setCharge(0);
    }

    return ComplianceOutcomeDto.builder()
        .cleanAirZoneId(cleanAirZoneId)
        .name(tariff.getName())
        .charge(result.getCharge())
        .informationUrls(tariff.getInformationUrls())
        .build();
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
            "Vehicle with VRN: '" + registrationNumber
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
  private void addTaxiStatus(Vehicle vehicle) {
    boolean taxiLicenseStatus = nationalTaxiRegisterService
        .getLicenseInformation(vehicle.getRegistrationNumber())
        .map(TaxiPhvLicenseInformationResponse::isActive)
        .orElse(false);

    if (taxiLicenseStatus) {
      vehicle.setIsTaxiOrPhv(true);
      vehicle.setVehicleType(VehicleType.TAXI_OR_PHV);
    } else {
      vehicle.setIsTaxiOrPhv(false);
    }
  }

}