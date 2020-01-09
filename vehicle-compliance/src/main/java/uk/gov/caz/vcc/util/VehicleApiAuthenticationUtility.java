package uk.gov.caz.vcc.util;

import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import uk.gov.caz.vcc.domain.RemoteVehicleAuthenticationResponse;
import uk.gov.caz.vcc.domain.authentication.VehicleApiCredentialRotationManager;
import uk.gov.caz.vcc.domain.exceptions.ExternalServiceCallException;
import uk.gov.caz.vcc.dto.RemoteDataNewApiKeyResponse;
import uk.gov.caz.vcc.dto.RemoteVehicleAuthenticationRequest;
import uk.gov.caz.vcc.dto.RemoteVehicleDataNewPasswordRequest;

@Slf4j
@Component
public class VehicleApiAuthenticationUtility {

  @Value("${dvla-api-username}")
  private String dvlaApiUsername;

  @Value("${dvla-api-password}")
  private String dvlaApiPassword;

  @Value("${dvla-authentication-endpoint}")
  private String dvlaAuthenticationEndpoint;

  @Value("${services.remote-vehicle-data.use-remote-api}")
  private boolean useRemoteApi;

  private boolean credentialRotationAttempted;

  private final RestTemplate remoteVehicleAuthenticationRestTemplate;
  private final VehicleApiCredentialRotationManager vehicleApiCredentialRotationManager;

  /**
   * Utility class for retrieving an API authentication token from the remote
   * vehicle data endpoint.
   */
  public VehicleApiAuthenticationUtility(
      RestTemplateBuilder restTemplateBuilder,
      VehicleApiCredentialRotationManager vehicleApiCredentialRotationManager) {
    this.remoteVehicleAuthenticationRestTemplate =
        restTemplateBuilder.rootUri(dvlaAuthenticationEndpoint).build();
    this.vehicleApiCredentialRotationManager =
        vehicleApiCredentialRotationManager;
  }

  /**
   * Method to get JWT for Secure Token Service authentication.
   *
   * @return JWT token for authenticating against remote data source.
   */
  @Cacheable(value = "authToken", cacheManager = "authTokenCacheManager")
  public String getAuthenticationToken() {
    if (!useRemoteApi) {
      log.info("Remote dvla call turn off, authentication omitted");
      return StringUtils.EMPTY;
    }

    log.info("Fetching remote API authentication token");

    MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
    headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

    RemoteVehicleAuthenticationRequest requestBody =
        RemoteVehicleAuthenticationRequest.builder().userName(dvlaApiUsername)
            .password(dvlaApiPassword).build();

    HttpEntity<RemoteVehicleAuthenticationRequest> request =
        new HttpEntity<>(requestBody, headers);

    try {
      RemoteVehicleAuthenticationResponse response =
          remoteVehicleAuthenticationRestTemplate.postForObject(
              dvlaAuthenticationEndpoint + "authenticate", request,
              RemoteVehicleAuthenticationResponse.class);
      
      log.info("Remote API authentication token successfully fetched");
      
      return response.idToken;
    } catch (HttpClientErrorException e) {
      HttpStatus statusCode = e.getStatusCode();

      if ((statusCode.equals(HttpStatus.UNAUTHORIZED)
          || statusCode.equals(HttpStatus.FORBIDDEN))
          && !credentialRotationAttempted) {
        // Fetch new password in event of credential rotation and
        // refresh local variable (noting this may be a hot lambda).
        this.dvlaApiPassword = vehicleApiCredentialRotationManager
            .getRemoteAuthenticationSecretValue("dvla-api-password");
        
        this.credentialRotationAttempted = true;
        
        // Evict cached tokens
        this.evictAuthTokenCache();
        
        return this.getAuthenticationToken();
      } else {
        throw e;
      }
    }
  }
  
  /**
   * Evicts oAuth tokens cached in redis. 
   * Used only in scenarios of credential rotation taking place.
   */
  @CacheEvict(value = "authToken", allEntries = true, cacheManager = "authTokenCacheManager")
  public void evictAuthTokenCache() {
    log.debug("Evicting all authToken entries.");
  }

  /**
   * Use authentication endpoint to change a password.
   * 
   * @param newPassword the new value of the password
   * @return an empty string if the request is successful
   */
  public String changePassword(String newPassword) {

    MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
    headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

    RemoteVehicleDataNewPasswordRequest requestBody =
        RemoteVehicleDataNewPasswordRequest.builder().userName(dvlaApiUsername)
            .password(dvlaApiPassword).newPassword(newPassword).build();
    HttpEntity<RemoteVehicleDataNewPasswordRequest> request =
        new HttpEntity<>(requestBody, headers);
    log.info("Password successfully updated");
    
    String response = remoteVehicleAuthenticationRestTemplate.postForObject(
        dvlaAuthenticationEndpoint + "password", request, String.class);
    
    // Evict old cached auth token
    this.evictAuthTokenCache();
    this.dvlaApiPassword = newPassword;
    
    // Fetch new auth token to ensure new password is working
    this.getAuthenticationToken();

    return response;
  }

  /**
   * Use authentication endpoint to generate a new API key.
   * 
   * @param jwtToken a freshly generated JWT token to authenticate endpoint
   *        caller
   * @param oldApiKey the previous API key
   * @return
   */
  public String renewApiKey(String jwtToken, String oldApiKey) {

    try {
      MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
      headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
      headers.add("x-api-key", oldApiKey);
      headers.add(HttpHeaders.AUTHORIZATION, jwtToken);
  
      log.info("Attempting rotation with API key: {}", oldApiKey);
      
      HttpEntity<?> request = new HttpEntity<>(headers);
  
      RemoteDataNewApiKeyResponse response =
          remoteVehicleAuthenticationRestTemplate.postForObject(
              dvlaAuthenticationEndpoint + "new-api-key", request,
              RemoteDataNewApiKeyResponse.class);
      log.info("API key successfully updated");
      log.info("New value: {}", response.getNewApiKey());
      Preconditions.checkNotNull(response, "Receive null response");
  
      // Force update of cached auth token
      this.getAuthenticationToken();
  
      return response.getNewApiKey();
    } catch (HttpClientErrorException e) {
      HttpStatus statusCode = e.getStatusCode();
      
      if ((statusCode.equals(HttpStatus.UNAUTHORIZED) || statusCode.equals(HttpStatus.FORBIDDEN))
          && !credentialRotationAttempted) {
        log.error("Authentication exception encountered when attempting to renew API key");
        log.error(e.getMessage());
        
        log.info("Refreshing API key before making next attempt");
        
        // Fetch new API key in event of credential rotation and 
        // set local variable (noting this may be a hot lambda).
        String refreshedApiKey = vehicleApiCredentialRotationManager
            .getRemoteAuthenticationSecretValue("dvla-api-key");
        
        log.info("Refreshed API key value prior to reattempted update: {}", refreshedApiKey);
        
        // Set credential rotation attempt marker to true to prevent infinite loop
        this.credentialRotationAttempted = true;
        
        // Update auth token prior to next attempt
        String newJwtToken = this.getAuthenticationToken();
        
        log.info("Latest JWT token: {}", newJwtToken);
        
        // Recurse with new API key value
        return this.renewApiKey(newJwtToken, refreshedApiKey);
      } else {
        log.error(e.getMessage());
        throw new ExternalServiceCallException(e);
      }
    }
  }
}
