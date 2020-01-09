package uk.gov.caz.taxiregister.repository;

import com.google.common.annotations.VisibleForTesting;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestTemplate;
import uk.gov.caz.taxiregister.model.VrmSet;

/**
 * Abstracts API calls from National Taxi Register to Vehicle Compliance Checker.
 */
@Slf4j
@Repository
public class VehicleComplianceCheckerRepository {

  @VisibleForTesting
  public static final String VCCS_CACHE_INVALIDATION_URL_PATH = "/v1/cache-invalidations/licences";
  private final RestTemplate vccsRestTemplate;
  private final HttpHeaders httpHeaders;

  /**
   * Constructs new instance of {@link VehicleComplianceCheckerRepository} class.
   *
   * @param restTemplateBuilder {@link RestTemplateBuilder} that can be used to obtain instance
   *     of {@link RestTemplate}.
   * @param vccsRootUri Root Uri of API Gateway which manages Vehicle Compliance Checker
   *     service.
   */
  public VehicleComplianceCheckerRepository(RestTemplateBuilder restTemplateBuilder,
      @Value("${services.vehicle-compliance-checker.root-url}") String vccsRootUri) {
    this.vccsRestTemplate = restTemplateBuilder.rootUri(vccsRootUri).build();
    httpHeaders = new HttpHeaders();
    httpHeaders.setContentType(MediaType.APPLICATION_JSON);
  }

  /**
   * Calls Vehicle Compliance Checker API and tells it to purge all cache entries with keys matching
   * set of VRMs.
   *
   * @param vrmSet {@link java.util.Set} of {@link String} with VRMs as keys to purge from
   *     VCCS.
   */
  public void purgeCacheOfNtrData(VrmSet vrmSet) {
    log.info("Calling VCCS to purge cache of licences");

    if (vrmSet.getVrms().isEmpty()) {
      log.info(
          "Skipping purge of VCCS cache of licences as no licenses have changed.");
      return;
    }
 
    HttpEntity<VrmSet> request = new HttpEntity<>(vrmSet, httpHeaders);
    ResponseEntity<Void> response = vccsRestTemplate
        .postForEntity(VCCS_CACHE_INVALIDATION_URL_PATH, request, Void.class);
    log.info("Successfully purged VCCS cache of licences. Response: {}",
        response.getStatusCode());
  }
}
