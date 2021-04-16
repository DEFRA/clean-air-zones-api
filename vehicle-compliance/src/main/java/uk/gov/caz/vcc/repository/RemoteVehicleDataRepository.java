package uk.gov.caz.vcc.repository;

import com.google.common.annotations.VisibleForTesting;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Repository;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpClientErrorException.NotFound;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import uk.gov.caz.definitions.domain.Vehicle;
import uk.gov.caz.vcc.domain.authentication.VehicleApiCredentialRotationManager;
import uk.gov.caz.vcc.domain.exceptions.ExternalServiceCallException;
import uk.gov.caz.vcc.dto.RemoteVehicleDataRequest;
import uk.gov.caz.vcc.util.VehicleApiAuthenticationUtility;

@Slf4j
@Repository
@ConditionalOnProperty(value = "services.remote-vehicle-data.use-remote-api", havingValue = "true",
    matchIfMissing = false)
public class RemoteVehicleDataRepository implements VehicleDetailsRepository {

  @Value("${dvla-api-endpoint}")
  private String dvlaApiEndpoint;

  @Value("${dvla-api-key}")
  private String dvlaApiKey;

  @VisibleForTesting
  private boolean credentialRotationAttempted;

  @VisibleForTesting
  private final RestTemplate remoteVehicleDataRestTemplate;

  @VisibleForTesting
  private final VehicleApiAuthenticationUtility remoteAuthenticationTokenGenerator;

  @VisibleForTesting
  private final VehicleApiCredentialRotationManager vehicleApiCredentialRotationManager;

  /**
   * Public constructor. Builds two rest templates from authentication and data endpoints for remote
   * vehicle data repository.
   */
  public RemoteVehicleDataRepository(RestTemplateBuilder restTemplateBuilder,
      VehicleApiAuthenticationUtility remoteAuthenticationTokenGenerator,
      VehicleApiCredentialRotationManager vehicleApiCredentialRotationManager) {
    log.info("Using remote API as a vehicle details repository");
    this.remoteVehicleDataRestTemplate = restTemplateBuilder.rootUri(dvlaApiEndpoint).build();
    this.remoteAuthenticationTokenGenerator = remoteAuthenticationTokenGenerator;
    this.vehicleApiCredentialRotationManager = vehicleApiCredentialRotationManager;
  }

  /**
   * Make and handle DVLA API request log and throw errors when DVLA API fails.
   *
   * @param registrationNumber Vehicle registration number to find
   * @return A vehicle - may be empty
   **/
  @Override
  public Optional<Vehicle> findByRegistrationNumber(String registrationNumber) {
    try {
      return requestWithRegistrationNumber(registrationNumber);
    } catch (ExternalServiceCallException e) {
      log.error(
          "DVLA API not responsive to call with provided vrn raised error with message: {}",
          e.getMessage());
      throw e;
    }
  }

  /*
   * Private method that handles the call to the DVLA API
   */
  private Optional<Vehicle> requestWithRegistrationNumber(String registrationNumber) {
    UUID correlationId = UUID.randomUUID();

    log.info("Calling DVLA to fetch vehicle details using Correlation ID: '{}'", correlationId);

    try {
      MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
      String authenticationToken = remoteAuthenticationTokenGenerator.getAuthenticationToken();

      headers.add("x-api-key", dvlaApiKey);
      headers.add(HttpHeaders.AUTHORIZATION, authenticationToken);
      headers.set("X-Correlation-Id", correlationId.toString());
      headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

      remoteVehicleDataRestTemplate.getMessageConverters()
          .add(new MappingJackson2HttpMessageConverter());

      RemoteVehicleDataRequest requestBody =
          RemoteVehicleDataRequest.builder().registrationNumber(registrationNumber).build();

      HttpEntity<RemoteVehicleDataRequest> request = new HttpEntity<>(requestBody, headers);

      Vehicle response =
          remoteVehicleDataRestTemplate.postForObject(dvlaApiEndpoint, request, Vehicle.class);

      return Optional.ofNullable(response);

    } catch (NotFound e) { // 404 - API healthy but vehicle not found
      log.warn(" License info provided VRN not found in DVLA");
      return Optional.empty();

    } catch (HttpClientErrorException e) { // 4XX
      HttpStatus statusCode = e.getStatusCode();

      if (
          (statusCode.equals(HttpStatus.UNAUTHORIZED) || statusCode.equals(HttpStatus.FORBIDDEN))
              && !credentialRotationAttempted) {
        log.warn(
            "Authentication exception encountered when connecting to remote vehicle API: {}",
            e.getMessage());
        apiKeyCredentialRotationManager();
        return requestWithRegistrationNumber(registrationNumber);

      } else {
        throw new ExternalServiceCallException(e);

      }
    } catch (HttpServerErrorException e) { // 5XX
      throw new ExternalServiceCallException(e);

    }
  }

  private void apiKeyCredentialRotationManager() {
    // Fetch new API key in event of credential rotation and
    // set local variable (noting this may be a hot lambda).
    this.dvlaApiKey =
        vehicleApiCredentialRotationManager.getRemoteAuthenticationSecretValue("dvla-api-key");

    // Evict prior cached token
    remoteAuthenticationTokenGenerator.evictAuthTokenCache();

    // Set credential rotation attempt marker to true to prevent infinite loop
    this.credentialRotationAttempted = true;
  }
}
