package uk.gov.caz.vcc.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import uk.gov.caz.vcc.domain.exceptions.ExternalServiceCallException;
import uk.gov.caz.vcc.domain.RemoteVehicleAuthenticationResponse;
import uk.gov.caz.definitions.domain.Vehicle;
import uk.gov.caz.vcc.domain.authentication.VehicleApiCredentialRotationManager;
import uk.gov.caz.vcc.dto.RemoteDataNewApiKeyResponse;
import uk.gov.caz.vcc.util.VehicleApiAuthenticationUtility;

public class RemoteVehicleDataRepositoryTest {
  
  @Mock
  private RestTemplate remoteVehicleAuthenticationRestTemplate;

  @Mock
  private RestTemplateBuilder remoteVehicleAuthenticationRestTemplateBuilder;

  @Mock
  private RestTemplate remoteVehicleDataRestTemplate;
  
  @Mock
  private RestTemplateBuilder remoteVehicleDataRestTemplateBuilder;
  
  @Mock
  private VehicleApiCredentialRotationManager vehicleApiCredentialRotationManager;
  
  private VehicleApiAuthenticationUtility remoteVehicleApiAuthenticationUtility;

  private RemoteVehicleDataRepository remoteVehicleDataRepository;

  public static final String REMOTE_API_DATA_URL = "remote-api-data-url";
  public static final String REMOTE_API_AUTH_URL = "remote-api-auth-url";
  
  
  @BeforeEach
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    
    Mockito.when(remoteVehicleAuthenticationRestTemplateBuilder.rootUri(null))
        .thenReturn(remoteVehicleAuthenticationRestTemplateBuilder);

    Mockito
        .when(remoteVehicleAuthenticationRestTemplateBuilder.build())
        .thenReturn(remoteVehicleAuthenticationRestTemplate);

    Mockito.when(remoteVehicleDataRestTemplateBuilder.rootUri(REMOTE_API_DATA_URL))
    .thenReturn(remoteVehicleDataRestTemplateBuilder);
    
    Mockito.when(remoteVehicleDataRestTemplateBuilder.build())
    .thenReturn(remoteVehicleDataRestTemplate);    
    
    remoteVehicleApiAuthenticationUtility = 
        new VehicleApiAuthenticationUtility(remoteVehicleAuthenticationRestTemplateBuilder, vehicleApiCredentialRotationManager);
   
    remoteVehicleDataRepository =
        new RemoteVehicleDataRepository(remoteVehicleAuthenticationRestTemplateBuilder, remoteVehicleApiAuthenticationUtility, vehicleApiCredentialRotationManager);

    ReflectionTestUtils.setField(remoteVehicleApiAuthenticationUtility, "dvlaAuthenticationEndpoint", REMOTE_API_AUTH_URL);
    ReflectionTestUtils.setField(remoteVehicleApiAuthenticationUtility, "remoteVehicleAuthenticationRestTemplate", remoteVehicleAuthenticationRestTemplate);
    
    ReflectionTestUtils.setField(remoteVehicleDataRepository, "dvlaApiEndpoint", REMOTE_API_DATA_URL);
    ReflectionTestUtils.setField(remoteVehicleDataRepository, "remoteVehicleDataRestTemplate", remoteVehicleDataRestTemplate);
    
  }

  @Test
  public void testCanGetVehicleDetails() {
    String registrationNumber = "CAS310";

    // authentication mocks
    RemoteVehicleAuthenticationResponse remoteVehicleAuthenticationResponse =
        new RemoteVehicleAuthenticationResponse();
    remoteVehicleAuthenticationResponse.idToken = "test";
    when(remoteVehicleAuthenticationRestTemplate.postForObject(
            anyString(), any(),
            eq(RemoteVehicleAuthenticationResponse.class)))
        .thenReturn(remoteVehicleAuthenticationResponse);

    // data access mocks
    Vehicle vehicle = new Vehicle();
    vehicle.setRegistrationNumber(registrationNumber);

    Mockito.when(remoteVehicleDataRestTemplate.postForObject(
        ArgumentMatchers.matches(REMOTE_API_DATA_URL), 
        ArgumentMatchers.any(),
        ArgumentMatchers.eq(Vehicle.class))).thenReturn(vehicle);

    Optional<Vehicle> vehicleOpt = remoteVehicleDataRepository
        .findByRegistrationNumber(registrationNumber);
    assertThat(vehicleOpt).isPresent().hasValue(vehicle);
  }

  @Test
  public void testEmptyVehicleResponse() {
    String registrationNumber = "CAS310";

    // authentication mocks
    RemoteVehicleAuthenticationResponse remoteVehicleAuthenticationResponse =
        new RemoteVehicleAuthenticationResponse();
    remoteVehicleAuthenticationResponse.idToken = "test";
    when(remoteVehicleAuthenticationRestTemplate.postForObject(
            anyString(), any(),
            eq(RemoteVehicleAuthenticationResponse.class)))
        .thenReturn(remoteVehicleAuthenticationResponse);

    // data access mocks
    when(remoteVehicleAuthenticationRestTemplate.postForObject(
        ArgumentMatchers.<String>isNull(), any(),
        eq(Vehicle.class))).thenReturn(null);

    Optional<Vehicle> vehicleOpt = remoteVehicleDataRepository
        .findByRegistrationNumber(registrationNumber);
    assertThat(vehicleOpt).isNotPresent();
  }

  @Test
  public void testCanCatch503Error() {
    when(remoteVehicleDataRestTemplate.postForObject(
        ArgumentMatchers.matches(REMOTE_API_DATA_URL),
        any(),
        eq(Vehicle.class)))
        .thenThrow(new HttpServerErrorException(HttpStatus.SERVICE_UNAVAILABLE));

    assertThrows(ExternalServiceCallException.class,
        () -> remoteVehicleDataRepository.findByRegistrationNumber("CAS310"));
  }

  @Test
  public void shouldThrowExternalServiceCallExceptionIfHttpClientErrorExceptionWasThrown() {
    when(remoteVehicleDataRestTemplate.postForObject(
            ArgumentMatchers.matches(REMOTE_API_DATA_URL), 
            any(),
            eq(Vehicle.class)))
        .thenThrow(new HttpClientErrorException(HttpStatus.SERVICE_UNAVAILABLE));

    assertThrows(ExternalServiceCallException.class,
        () -> remoteVehicleDataRepository.findByRegistrationNumber("CAS310"));
  }

  @Test
  public void testApiKey() {
    String key = "key";
    RemoteDataNewApiKeyResponse remoteDataNewApiKeyResponse = new RemoteDataNewApiKeyResponse(
        "message", key);

    RemoteVehicleAuthenticationResponse testResponse = new RemoteVehicleAuthenticationResponse();
    testResponse.idToken = "test";
    
    when(remoteVehicleAuthenticationRestTemplate.postForObject(
            anyString(), any(),
            eq(RemoteDataNewApiKeyResponse.class)))
        .thenReturn(
            remoteDataNewApiKeyResponse);
    
    when(remoteVehicleAuthenticationRestTemplate.postForObject(
        anyString(), any(),
        eq(RemoteVehicleAuthenticationResponse.class)))
    .thenReturn(testResponse);
    
    String newKey =
        remoteVehicleApiAuthenticationUtility.renewApiKey("token", "oldKey");
    assertThat(newKey).isEqualTo(key);
  }

  @Test
  public void shouldThrowExceptionIfUnknownExceptionWasThrown() {
    when(remoteVehicleDataRestTemplate.postForObject(
        ArgumentMatchers.matches(REMOTE_API_DATA_URL),
        any(),
        eq(Vehicle.class)))
        .thenThrow(new IllegalArgumentException());

    assertThrows(IllegalArgumentException.class,
        () -> remoteVehicleDataRepository.findByRegistrationNumber("CAS310"));
  }

  @Test
  public void testApiKeyNullPointer() {

    RemoteDataNewApiKeyResponse remoteDataNewApiKeyResponse = null;

    when(remoteVehicleAuthenticationRestTemplate.postForObject(
        anyString(), any(),
        eq(RemoteDataNewApiKeyResponse.class)))
        .thenReturn(
            remoteDataNewApiKeyResponse);
    assertThrows(NullPointerException.class,
        () -> remoteVehicleApiAuthenticationUtility.renewApiKey("token", "oldKey"));

  }

  @Test
  public void shouldChangePassword() {

    String newPassword = "new password";

    RemoteVehicleAuthenticationResponse testResponse = new RemoteVehicleAuthenticationResponse();
    testResponse.idToken = "test";
    
    when(remoteVehicleAuthenticationRestTemplate.postForObject(
            anyString(), any(),
            eq(String.class)))
        .thenReturn(
            "");

     when(remoteVehicleAuthenticationRestTemplate.postForObject(
            anyString(), any(),
            eq(RemoteVehicleAuthenticationResponse.class)))
        .thenReturn(testResponse);
        
    String response = remoteVehicleApiAuthenticationUtility.changePassword(newPassword);

    assertThat(response).isEmpty();
  }
  
  @Test
  public void credentialUpdateAttemptMadeInAuthForbiddenFailureConditions() {

    RemoteVehicleAuthenticationResponse testResponse = new RemoteVehicleAuthenticationResponse();
    testResponse.idToken = "test";
    
    Mockito
    .when(remoteVehicleAuthenticationRestTemplate.postForObject(
        ArgumentMatchers.matches(REMOTE_API_AUTH_URL), ArgumentMatchers.any(),
        eq(RemoteVehicleAuthenticationResponse.class)))
    .thenReturn(testResponse);

    HttpClientErrorException exception = new HttpClientErrorException(HttpStatus.UNAUTHORIZED);
    
     Mockito
        .when(remoteVehicleDataRestTemplate.postForObject(
            ArgumentMatchers.matches(REMOTE_API_DATA_URL), 
            ArgumentMatchers.any(),
            ArgumentMatchers.any()))
        .thenThrow(exception)
        .thenReturn(new Vehicle());
     
     remoteVehicleDataRepository.findByRegistrationNumber("CAS310");
     
     // Assert credential refresh was made
     Mockito.verify(vehicleApiCredentialRotationManager, times(1))
       .getRemoteAuthenticationSecretValue(ArgumentMatchers.anyString());
     
     // Check a second call was made using the revised credentials
     Mockito.verify(remoteVehicleDataRestTemplate, times(2))
     .postForObject(
         ArgumentMatchers.matches(REMOTE_API_DATA_URL), 
         ArgumentMatchers.any(),
         ArgumentMatchers.any());
  }
}
