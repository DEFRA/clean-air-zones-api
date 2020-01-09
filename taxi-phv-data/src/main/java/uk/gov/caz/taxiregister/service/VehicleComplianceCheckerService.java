package uk.gov.caz.taxiregister.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import uk.gov.caz.taxiregister.model.VrmSet;
import uk.gov.caz.taxiregister.repository.VehicleComplianceCheckerRepository;

/**
 * Service that abstracts operations that National Taxi Register can ask from Vehicle Compliance
 * Checker. Will use REST calls to VCCS APIs.
 */
@Service
@Slf4j
public class VehicleComplianceCheckerService {

  private final VehicleComplianceCheckerRepository vehicleComplianceCheckerRepository;
  private final boolean cacheEvictionOfNtrDataEnabled;

  /**
   * Constructs new instance of {@link VehicleComplianceCheckerService} class.
   *
   * @param vehicleComplianceCheckerRepository Instance of
   *     {@link VehicleComplianceCheckerRepository} that abstracts low level REST calls.
   * @param cacheEvictionOfNtrDataEnabled A boolean that enables (if true) or disables (if
   *     false) notification from NTR
   */
  public VehicleComplianceCheckerService(
      VehicleComplianceCheckerRepository vehicleComplianceCheckerRepository,
      @Value("${services.vehicle-compliance-checker.cache-eviction-of-ntr-data-enabled}")
          boolean cacheEvictionOfNtrDataEnabled) {
    this.vehicleComplianceCheckerRepository = vehicleComplianceCheckerRepository;
    this.cacheEvictionOfNtrDataEnabled = cacheEvictionOfNtrDataEnabled;
  }

  /**
   * If configuration flag 'cache-eviction-of-ntr-data-enabled' is set to true it will call Vehicle
   * Compliance Checker API and tell it to purge all cache entries with keys matching set of VRMs.
   * If flag is false method does nothing.
   *
   * @param vrmSet {@link java.util.Set} of {@link String} with VRMs as keys to purge from
   *     VCCS.
   */
  public void purgeCacheOfNtrData(VrmSet vrmSet) {
    if (!cacheEvictionOfNtrDataEnabled) {
      log.info("Eviction of NTR data from VCCS cache is disabled. "
          + "Skipping call to 'purgeCacheOfNtrData'");
      return;
    }

    try {
      vehicleComplianceCheckerRepository.purgeCacheOfNtrData(vrmSet);
    } catch (RestClientException restClientException) {
      log.error("Error when calling VCCS to purge cache of licences", restClientException);
    }
  }
}
