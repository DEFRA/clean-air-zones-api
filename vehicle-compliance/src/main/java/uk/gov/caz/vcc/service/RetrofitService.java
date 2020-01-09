package uk.gov.caz.vcc.service;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.caz.vcc.domain.RetrofittedVehicle;
import uk.gov.caz.vcc.repository.RetrofitRepository;

/**
 * Service layer for checking a vehicle Retrofit status.
 *
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RetrofitService {

  private final RetrofitRepository retrofitRepository;

  /**
   * Checks whether a vehicle has been registered as retrofitted.
   * 
   * @param vrn the registration number of the vehicle to check.
   * @return boolean indicator of whether the vehicle has been retrofitted.
   */
  public boolean isRetrofitted(String vrn) {
    log.info("Checking retrofit status for vrn {}", vrn);
    boolean retrofitStatus = retrofitRepository.existsByVrnIgnoreCase(vrn);
    log.info("Retrofit status identified as {} for vrn {}", retrofitStatus,
        vrn);
    return retrofitStatus;
  }
  
  /**
   * Service layer method for retrieving a retrofit vehicle by its VRN.
   * @param vrn the number plate of the vehicle to query.
   * @return an optional of a matched retrofitted vehicle.
   */
  public Optional<RetrofittedVehicle> findByVrn(String vrn) {
    log.info("Attempting to retrieve retrofit vehicle object for vrn {}", vrn);
    return Optional.ofNullable(retrofitRepository.findByVrnIgnoreCase(vrn));
  }

}