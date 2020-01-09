
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
import uk.gov.caz.vcc.domain.exceptions.ExternalServiceCallException;
import uk.gov.caz.vcc.domain.Vehicle;
import uk.gov.caz.vcc.domain.authentication.VehicleApiCredentialRotationManager;
import uk.gov.caz.vcc.dto.RemoteVehicleDataRequest;
import uk.gov.caz.vcc.util.VehicleApiAuthenticationUtility;

@Slf4j
@Repository
@ConditionalOnProperty(value = "services.remote-vehicle-data.use-remote-api",
    havingValue = "true", matchIfMissing = false)
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
   * Public constructor. Builds two rest templates from authentication and data
   * endpoints for remote vehicle data repository.
   * 
   */
  public RemoteVehicleDataRepository(RestTemplateBuilder restTemplateBuilder,
      VehicleApiAuthenticationUtility remoteAuthenticationTokenGenerator,
      VehicleApiCredentialRotationManager vehicleApiCredentialRotationManager) {
    this.remoteVehicleDataRestTemplate =
        restTemplateBuilder.rootUri(dvlaApiEndpoint).build();
    this.remoteAuthenticationTokenGenerator = remoteAuthenticationTokenGenerator;
    this.vehicleApiCredentialRotationManager = vehicleApiCredentialRotationManager;
  }

  @Override
  public Optional<Vehicle> findByRegistrationNumber(String registrationNumber) {
    UUID correlationId = UUID.randomUUID();

    log.info(
        "Calling remote API to fetch vehicle details for"
            + " '{}' registration number with correlation Id: '{}'",
        registrationNumber, correlationId);

    try {
      MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
      String authenticationToken = remoteAuthenticationTokenGenerator.getAuthenticationToken();
      
      headers.add("x-api-key", dvlaApiKey);
      headers.add(HttpHeaders.AUTHORIZATION, authenticationToken);
      headers.set("X-Correlation-Id", correlationId.toString());
      headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

      remoteVehicleDataRestTemplate.getMessageConverters()
          .add(new MappingJackson2HttpMessageConverter());

      RemoteVehicleDataRequest requestBody = RemoteVehicleDataRequest.builder()
          .registrationNumber(registrationNumber).build();

      HttpEntity<RemoteVehicleDataRequest> request =
          new HttpEntity<>(requestBody, headers);

      Vehicle response = remoteVehicleDataRestTemplate
          .postForObject(dvlaApiEndpoint, request, Vehicle.class);

      return Optional.ofNullable(response);
    } catch (NotFound e) {
      log.warn(" License info for {} not found in DVLA", registrationNumber);
      return Optional.empty();
    } catch (HttpServerErrorException e) {
      log.error("Cannot call DVLA for vrn: [{}], response code: {}, message: {}",
          registrationNumber, e.getRawStatusCode(), e.getMessage());
      return Optional.empty();
    } catch (HttpClientErrorException e) {
      HttpStatus statusCode = e.getStatusCode();
      
      if ((statusCode.equals(HttpStatus.UNAUTHORIZED) || statusCode.equals(HttpStatus.FORBIDDEN))
          && !credentialRotationAttempted) {
        log.error("Authentication exception encountered when connecting to remote vehicle API");
        log.error(e.getMessage());
        
        // Fetch new API key in event of credential rotation and 
        // set local variable (noting this may be a hot lambda).
        this.dvlaApiKey = vehicleApiCredentialRotationManager
            .getRemoteAuthenticationSecretValue("dvla-api-key");
        
        // Evict prior cached token
        remoteAuthenticationTokenGenerator.evictAuthTokenCache();
        
        // Set credential rotation attempt marker to true to prevent infinite loop
        this.credentialRotationAttempted = true;
        
        // Recurse with new API key value
        return this.findByRegistrationNumber(registrationNumber);
      } else {
        throw new ExternalServiceCallException(e);
      }
    } catch (Exception e) {
      log.error("Cannot call DVLA for vrn: [{}], message: {}",
          registrationNumber, e.getMessage());
      return Optional.empty();
    }
  }

}
