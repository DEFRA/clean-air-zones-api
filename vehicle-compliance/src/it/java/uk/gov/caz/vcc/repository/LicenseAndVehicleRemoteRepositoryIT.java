package uk.gov.caz.vcc.repository;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.common.net.MediaType;
import io.restassured.RestAssured;
import java.util.ArrayList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.caz.vcc.annotation.FullyRunningServerIntegrationTest;
import uk.gov.caz.vcc.domain.authentication.VehicleApiCredentialRotationManager;
import uk.gov.caz.vcc.domain.exceptions.ExternalServiceCallException;
import uk.gov.caz.vcc.dto.SingleDvlaVehicleData;
import uk.gov.caz.vcc.repository.LicenseAndVehicleRepository.NtrAndDvlaData;
import uk.gov.caz.vcc.util.MockServerTestIT;
import uk.gov.caz.vcc.util.VehicleApiAuthenticationUtility;

@FullyRunningServerIntegrationTest
@TestPropertySource(properties = {
    "dvla-api-endpoint=http://localhost:1080/dvla-api/details/",
    "dvla-authentication-endpoint=http://localhost:1080/dvla-api/",
    "dvla-api-username=test-username",
    "dvla-api-password=test-password",
    "redis.enabled=false",
    "services.remote-vehicle-data.use-remote-api=true"
})
public class LicenseAndVehicleRemoteRepositoryIT extends MockServerTestIT {

  // We need to mock calls to AWS secrets so that we can assert the returned values
  @Mock
  private VehicleApiCredentialRotationManager vehicleApiCredentialRotationManager;

  @Autowired
  private VehicleApiAuthenticationUtility vehicleApiAuthenticationUtility;

  @Autowired
  private LicenseAndVehicleRemoteRepository licenseAndVehicleRemoteRepository;

  @LocalServerPort
  int randomServerPort;

  @Value("${dvla-api-password}")
  private String dvlaApiPassword;

  @BeforeEach
  public void setup() {
    RestAssured.port = randomServerPort;
    RestAssured.baseURI = "http://localhost";

    ReflectionTestUtils.setField(vehicleApiAuthenticationUtility,
        "vehicleApiCredentialRotationManager", vehicleApiCredentialRotationManager);

    ReflectionTestUtils.setField(vehicleApiAuthenticationUtility,
        "dvlaApiPassword", dvlaApiPassword);
    ReflectionTestUtils.setField(vehicleApiAuthenticationUtility,
        "credentialRotationAttempted", false);
  }

  @AfterEach
  public void post() {
    mockServer.reset();
    vehicleApiAuthenticationUtility.evictAuthTokenCache();
  }

  @Nested
  class WhenFetchingSingleDvlaData {

    @Test
    public void failedAuthenticationTriggersRetry() {
      String vrn = "CAS300";

      mockDvlaVehicleDetailsEndpointFailure("old-token", vrn);
      mockDvlaVehicleDetailsEndpointSuccess("new-token", vrn);
      mockDvlaAuthEndpointSuccess("test-password");

      SingleDvlaVehicleData vehicle = licenseAndVehicleRemoteRepository
          .findDvlaVehicle(vrn, "old-token");

      assertFalse(vehicle.hasFailed());
    }

    @Test
    public void twiceFailedAuthenticationThrowsExternalServiceCallException() {
      String vrn = "CAS300";

      mockDvlaVehicleDetailsEndpointFailure("old-token", "CAS300");
      mockDvlaVehicleDetailsEndpointFailure("new-token", "CAS300");
      mockDvlaAuthEndpointSuccess("test-password");

      assertThrows(ExternalServiceCallException.class,
          () -> licenseAndVehicleRemoteRepository.findDvlaVehicle(vrn, "old-token"));
    }
  }

  /**
   * Test to assert that receiving a 401 response from the DVLA API triggers a call
   * to get a new auth token and retries the request.
   */
  @Test
  public void failedAuthenticationTriggersRetry() {
    ArrayList<String> vrns = new ArrayList<String> ();
    vrns.add("CAS300");
    vrns.add("CAS301");

    mockDvlaVehicleDetailsEndpointFailure("old-token", "CAS300");
    mockDvlaVehicleDetailsEndpointFailure("old-token", "CAS301");
    mockDvlaVehicleDetailsEndpointSuccess("new-token", "CAS300");
    mockDvlaVehicleDetailsEndpointSuccess("new-token", "CAS301");
    mockDvlaAuthEndpointSuccess("test-password");

    NtrAndDvlaData ntrAndDvlaData = licenseAndVehicleRemoteRepository
        .findLicenseAndVehicle(vrns, "old-token");

    assertFalse(
      ntrAndDvlaData.dvlaAsyncOps().stream()
        .anyMatch(dvlaOp -> dvlaOp.getHttpStatus() != HttpStatus.OK));
  }

  @Test
  public void failedPasswordOnDvlaAuthFetchesPasswordFromSecretsAndAsyncCallRetried() {
    ArrayList<String> vrns = new ArrayList<String> ();
    vrns.add("CAS300");
    vrns.add("CAS301");

    mockDvlaVehicleDetailsEndpointFailure("old-token", "CAS300");
    mockDvlaVehicleDetailsEndpointFailure("old-token", "CAS301");
    mockDvlaVehicleDetailsEndpointSuccess("new-token", "CAS300");
    mockDvlaVehicleDetailsEndpointSuccess("new-token", "CAS301");
    mockDvlaAuthEndpointFailure("test-password");
    mockAwsSecretsCall("dvla-api-password", "new-password");
    mockDvlaAuthEndpointSuccess("new-password");

    NtrAndDvlaData ntrAndDvlaData = licenseAndVehicleRemoteRepository
        .findLicenseAndVehicle(vrns, "old-token");

    assertFalse(
      ntrAndDvlaData.dvlaAsyncOps().stream()
        .anyMatch(dvlaOp -> dvlaOp.getHttpStatus() != HttpStatus.OK));
  }

  @Test
  public void twiceFailedAuthenticationThrowsExternalServiceCallException() {
    ArrayList<String> vrns = new ArrayList<String> ();
    vrns.add("CAS300");
    vrns.add("CAS301");

    mockDvlaVehicleDetailsEndpointFailure("old-token", "CAS300");
    mockDvlaVehicleDetailsEndpointFailure("old-token", "CAS301");
    mockDvlaVehicleDetailsEndpointFailure("new-token", "CAS300");
    mockDvlaVehicleDetailsEndpointFailure("new-token", "CAS301");
    mockDvlaAuthEndpointSuccess("test-password");

    assertThrows(ExternalServiceCallException.class,
        () -> licenseAndVehicleRemoteRepository
        .findLicenseAndVehicle(vrns, "old-token"));
  }

  private void mockDvlaVehicleDetailsEndpointFailure(String authToken, String vrn) {
    mockServer
        .when(HttpRequest.request()
          .withPath("/dvla-api/details/")
          .withMethod("POST")
          .withHeader("Authorization", authToken)
          .withBody("{\"registrationNumber\":\"" + vrn + "\"}"))
        .respond(HttpResponse.response()
          .withStatusCode(401));
  }

  private void mockDvlaVehicleDetailsEndpointSuccess(String authToken, String vrn) {
    mockServer
        .when(HttpRequest.request()
          .withPath("/dvla-api/details/")
          .withMethod("POST")
          .withHeader("Authorization", authToken)
          .withBody("{\"registrationNumber\":\"" + vrn + "\"}"))
        .respond(HttpResponse.response()
          .withStatusCode(200)
          .withBody("{\"registrationNumber\":\"" + vrn + "\"}", 
            MediaType.JSON_UTF_8));
  }

  private void mockDvlaAuthEndpointSuccess(String password) {
    mockServer
        .when(HttpRequest.request()
          .withPath("/dvla-api/authenticate")
          .withMethod("POST")
          .withBody("{\"userName\":\"test-username\",\"password\":\"" + password + "\"}"))
        .respond(HttpResponse.response()
          .withBody("{\"id-token\": \"new-token\"}",
            MediaType.JSON_UTF_8));
  }

  private void mockDvlaAuthEndpointFailure(String password) {
    mockServer
        .when(HttpRequest.request()
          .withPath("/dvla-api/authenticate")
          .withMethod("POST")
          .withBody("{\"userName\":\"test-username\",\"password\":\"" + password + "\"}"))
        .respond(HttpResponse.response()
          .withStatusCode(401));
  }

  private void mockAwsSecretsCall(String key, String returnValue) {
    Mockito.when(vehicleApiCredentialRotationManager.getRemoteAuthenticationSecretValue(key))
        .thenReturn(returnValue);
  }

}
