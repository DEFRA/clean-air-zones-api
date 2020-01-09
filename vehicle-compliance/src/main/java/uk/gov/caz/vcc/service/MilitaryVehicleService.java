package uk.gov.caz.vcc.service;

import java.util.Optional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import uk.gov.caz.vcc.domain.MilitaryVehicle;
import uk.gov.caz.vcc.repository.ModRepository;

/**
 * Service layer for checking whether a vehicle is registered as a military vehicle.
 *
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MilitaryVehicleService {

  private final ModRepository militaryVehicleRepository;

  /**
   * Checks whether a vehicle has been registered as a military vehicle.
   * 
   * @param vrn the registration number of the vehicle to check.
   * @return boolean indicator of whether the vehicle has been registered as a military vehicle.
   */
  public boolean isMilitaryVehicle(String vrn) {
    log.info("Checking miltary vehicle status for vrn {}", vrn);
    // Note that the outcome of this check is not captured
    // to avoid this data being exposed via log files.
    return militaryVehicleRepository.existsByVrnIgnoreCase(vrn);
  }

  /**
   * Service layer method for retrieving a military vehicle by its VRN.
   * 
   * @param vrn the number plate of the vehicle to query.
   * @return an optional of a matched military vehicle.
   */
  public Optional<MilitaryVehicle> findByVrn(String vrn) {
    log.info("Attempting to retrieve military vehicle object for vrn {}", vrn);
    return Optional.ofNullable(militaryVehicleRepository.findByVrnIgnoreCase(vrn));
  }

}