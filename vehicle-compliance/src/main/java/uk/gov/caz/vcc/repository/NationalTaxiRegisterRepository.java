package uk.gov.caz.vcc.repository;

import java.util.Optional;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.HttpClientErrorException.NotFound;
import org.springframework.web.client.RestTemplate;

import uk.gov.caz.vcc.domain.exceptions.ExternalServiceCallException;
import uk.gov.caz.vcc.dto.TaxiPhvLicenseInformationResponse;

@Slf4j
@Repository
public class NationalTaxiRegisterRepository {

  private final RestTemplate nationalTaxiRegisterRestTemplate;

  /**
   * Default constructor.
   */
  public NationalTaxiRegisterRepository(RestTemplateBuilder restTemplateBuilder,
      @Value("${services.national-taxi-register.root-url}") String nationalTaxiRegisterRootUri) {
    this.nationalTaxiRegisterRestTemplate =
        restTemplateBuilder.rootUri(nationalTaxiRegisterRootUri).build();
  }

  /**
   * Get license info for a vehicle from the National Taxi Register.
   *
   * @param vrn The vehicle registration number.
   * @return A LicenseInfoResponse (optional).
   */
  @Cacheable(value = "licenseInfo", key = "#vrn", cacheManager = "licenseInfoCacheManager")
  public Optional<TaxiPhvLicenseInformationResponse> getLicenseInfo(String vrn) {
    log.info("Calling NTR to get license info for '{}' VRN", vrn);
    try {
      ResponseEntity<TaxiPhvLicenseInformationResponse> responseEntity =
          nationalTaxiRegisterRestTemplate.getForEntity(
              "/v1/vehicles/{vrn}/licence-info",
              TaxiPhvLicenseInformationResponse.class, vrn);

      Optional<TaxiPhvLicenseInformationResponse> taxiPhvOptional =
          Optional.ofNullable(responseEntity.getBody());

      log.info("Taxi or PHV status was {} for vrn {}", taxiPhvOptional.isPresent(), vrn);

      return taxiPhvOptional;
    } catch (NotFound e) {
      log.warn("Vrn {} not found in the taxi/PHV database", vrn);
      return Optional.empty();
    } catch (Exception e) {
      log.error("Cannot fetch licence info from NTR for VRN {}", vrn);
      throw new ExternalServiceCallException(e);
    }
  }

  /**
   * Method for evicting a cached licenseInfo for given vrn.
   */
  @CacheEvict(value = "licenseInfo", allEntries = true, cacheManager = "licenseInfoCacheManager")
  public void cacheEvictLicenseInfo() {
    log.debug("Evicting cached license info for taxi VRNs in response to an upload being made.");
  }
}
