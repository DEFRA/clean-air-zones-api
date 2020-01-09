package uk.gov.caz.vcc.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import uk.gov.caz.vcc.domain.RemoteVehicleAuthenticationResponse;
import uk.gov.caz.vcc.domain.authentication.VehicleApiCredentialRotationManager;
import uk.gov.caz.vcc.dto.RemoteDataNewApiKeyResponse;

@ExtendWith(MockitoExtension.class)
public class VehicleApiAuthenticationUtilityTest {

  private static final String HTTP_DVLA_NEW_API_KEY_ENDPOINT = "http://dvla/new-api-key";

  private static final String HTTP_DVLA_AUTHENTICATE_ENDPOINT = "http://dvla/authenticate";

  @Mock
  private RestTemplateBuilder restTemplateBuilder;

  @Mock
  private RestTemplate restTemplate;

  @Mock
  private VehicleApiCredentialRotationManager vehicleApiCredentialRotationManager;

  private VehicleApiAuthenticationUtility utility;

  @BeforeEach
  void init() {
    when(restTemplateBuilder.rootUri(null)).thenReturn(restTemplateBuilder);
    when(restTemplateBuilder.build()).thenReturn(restTemplate);
    utility = new VehicleApiAuthenticationUtility(restTemplateBuilder,
                    vehicleApiCredentialRotationManager);
    ReflectionTestUtils.setField(utility, "useRemoteApi", true);
    ReflectionTestUtils.setField(utility, "dvlaAuthenticationEndpoint", "http://dvla/");
    ReflectionTestUtils.setField(utility, "dvlaApiUsername", "dvlaApiUsername");
    ReflectionTestUtils.setField(utility, "dvlaApiPassword", "dvlaApiPassword");
  }

  @Test
  void whenAttemptIsMade_AuthenticationTokenIsReturned() {
    mockSuccessfulGetAuthenticationInvocation();
    utility.getAuthenticationToken();
    verify(restTemplate).postForObject(eq(HTTP_DVLA_AUTHENTICATE_ENDPOINT), any(HttpEntity.class), eq(RemoteVehicleAuthenticationResponse.class));
  }

  @Test
  void whenServiceIsNotAvailable_FailAttemptToGetAuthenticationToken() {
    mockFailedGetAuthenticationInvocation();
    assertThrows(HttpClientErrorException.class, () -> utility.getAuthenticationToken());
  }

  @Test
  void whenPasswordExpired_ReAttemptToGetToken() {
    ReflectionTestUtils.setField(utility, "credentialRotationAttempted", false);
    RemoteVehicleAuthenticationResponse mockedResponse = mockEventuallySuccessfulGetAuthenticationTokenInvocation();
    String token = utility.getAuthenticationToken();
    verify(vehicleApiCredentialRotationManager).getRemoteAuthenticationSecretValue("dvla-api-password");
    assertEquals(mockedResponse.idToken, token);
  }

  @Test
  void whenApiKeyExpired_ReAttemptToGetNewApiKey() {
    ReflectionTestUtils.setField(utility, "credentialRotationAttempted", false);
    
    mockSuccessfulGetAuthenticationInvocation();

    RemoteDataNewApiKeyResponse mockedResponse = mockEventuallySuccessfulRenewApiKeyInvocation();

    when(vehicleApiCredentialRotationManager.getRemoteAuthenticationSecretValue("dvla-api-key")).thenReturn("newApiKey");
    
    String token = utility.renewApiKey("jwtToken","oldApiKey");
    verify(vehicleApiCredentialRotationManager).getRemoteAuthenticationSecretValue("dvla-api-key");
    assertEquals(mockedResponse.getNewApiKey(), token);
  }

  private RemoteDataNewApiKeyResponse mockEventuallySuccessfulRenewApiKeyInvocation() {
    HttpClientErrorException ex = new HttpClientErrorException(HttpStatus.UNAUTHORIZED);
    RemoteDataNewApiKeyResponse mockedResponse = new RemoteDataNewApiKeyResponse();
    mockedResponse.setNewApiKey("new-api-key");

    when(restTemplate.postForObject(eq(HTTP_DVLA_NEW_API_KEY_ENDPOINT), any(HttpEntity.class), eq(RemoteDataNewApiKeyResponse.class)))
      .thenThrow(ex)
      .thenReturn(mockedResponse);
    return mockedResponse;
  }

  private RemoteVehicleAuthenticationResponse mockEventuallySuccessfulGetAuthenticationTokenInvocation() {
    HttpClientErrorException ex = new HttpClientErrorException(HttpStatus.UNAUTHORIZED);
    RemoteVehicleAuthenticationResponse mockedResponse = new RemoteVehicleAuthenticationResponse();
    mockedResponse.idToken = "authentication-token";
    when(restTemplate.postForObject(eq(HTTP_DVLA_AUTHENTICATE_ENDPOINT), any(HttpEntity.class), eq(RemoteVehicleAuthenticationResponse.class)))
      .thenThrow(ex)
      .thenReturn(mockedResponse);
    return mockedResponse;
  }

  private void mockSuccessfulGetAuthenticationInvocation() {
    RemoteVehicleAuthenticationResponse mockedAuthenticationResponse = new RemoteVehicleAuthenticationResponse();
    mockedAuthenticationResponse.idToken = "authentication-token";

    doReturn(mockedAuthenticationResponse)
      .when(restTemplate).postForObject(eq(HTTP_DVLA_AUTHENTICATE_ENDPOINT), any(HttpEntity.class), eq(RemoteVehicleAuthenticationResponse.class));
  }

  private void mockFailedGetAuthenticationInvocation() {
    HttpClientErrorException ex = new HttpClientErrorException(HttpStatus.SERVICE_UNAVAILABLE);
    when(restTemplate.postForObject(eq(HTTP_DVLA_AUTHENTICATE_ENDPOINT), any(HttpEntity.class), eq(RemoteVehicleAuthenticationResponse.class)))
      .thenThrow(ex);
  }
}