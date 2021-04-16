package uk.gov.caz.vcc.service;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.caz.vcc.domain.RetrofittedVehicle;
import uk.gov.caz.vcc.repository.RetrofitRepository;

/**
 * Service layer for checking a vehicle Retrofit status.
 *
 */
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
    return retrofitRepository.existsByVrnIgnoreCase(vrn);
  }

  /**
   * Service layer method for retrieving a retrofit vehicle by its VRN.
   * 
   * @param vrn the number plate of the vehicle to query.
   * @return an optional of a matched retrofitted vehicle.
   */
  public Optional<RetrofittedVehicle> findByVrn(String vrn) {
    return Optional.ofNullable(retrofitRepository.findByVrnIgnoreCase(vrn));
  }
  
  /**
   * Service layer method for retrieving multiple retrofit vehicles by a list of VRN.
   * 
   * @param vrns the number plates of the vehicle to query.
   * @return an optional of a matched retrofitted vehicle.
   */
  public List<RetrofittedVehicle> findByVrns(Set<String> vrns) {
    return retrofitRepository.findRetrofitVehicleByVrns(vrns);
  }
  
  public boolean isRetrofitVehicle(String vrn,
      List<RetrofittedVehicle> matchedRetrofitVehicles) {
    return matchedRetrofitVehicles.stream()
        .anyMatch(retrofitVehicle -> retrofitVehicle.getVrn().equals(vrn));
  }

  @RequiredArgsConstructor
  @Getter
  public static class RetrofitQueryResponse {
    private final List<String> retrofittedVehicleVrns;
    private final List<String> unProcessedVrns;
  }

}
