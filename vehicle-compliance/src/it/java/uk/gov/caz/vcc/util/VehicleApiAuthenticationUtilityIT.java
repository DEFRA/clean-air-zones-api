package uk.gov.caz.vcc.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.common.net.MediaType;
import io.restassured.RestAssured;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import uk.gov.caz.vcc.annotation.FullyRunningServerIntegrationTest;
import uk.gov.caz.vcc.domain.authentication.VehicleApiCredentialRotationManager;

@FullyRunningServerIntegrationTest
@TestPropertySource(properties = {
    "dvla-api-endpoint=http://localhost:1080/dvla-api/details/",
    "dvla-authentication-endpoint=http://localhost:1080/dvla-api/",
    "dvla-api-username=test-username",
    "dvla-api-password=test-password",
    "redis.enabled=false",
    "services.remote-vehicle-data.use-remote-api=true"
})
public class VehicleApiAuthenticationUtilityIT extends MockServerTestIT {

  // We need to mock calls to AWS secrets so that we can assert the returned values
  @Mock
  private VehicleApiCredentialRotationManager vehicleApiCredentialRotationManager;

  @Autowired
  private VehicleApiAuthenticationUtility vehicleApiAuthenticationUtility;

  @LocalServerPort
  int randomServerPort;

  @Value("${dvla-api-password}")
  private String dvlaApiPassword;

  @BeforeEach
  public void setup() throws InterruptedException {
    RestAssured.port = randomServerPort;
    RestAssured.baseURI = "http://localhost";

    ReflectionTestUtils.setField(vehicleApiAuthenticationUtility,
        "vehicleApiCredentialRotationManager", vehicleApiCredentialRotationManager);

    ReflectionTestUtils.setField(vehicleApiAuthenticationUtility,
        "dvlaApiPassword", dvlaApiPassword);
    ReflectionTestUtils.setField(vehicleApiAuthenticationUtility,
        "credentialRotationAttempted", false);
    someSlackTimeToLetResetPropagate();
  }

  @AfterEach
  public void post() {
    mockServer.reset();
    vehicleApiAuthenticationUtility.evictAuthTokenCache();
  }

  @Test
  public void getAuthTokenReturnsTokenOn200Response() {
    mockDvlaAuthEndpointSuccess("test-password");

    String token = vehicleApiAuthenticationUtility.getAuthenticationToken();

    assertEquals("test-token", token);
  }

  @Test
  public void unauthorizedResponseTriggersPasswordCredentialRollover() {
    mockDvlaAuthEndpointFailure("test-password");
    mockDvlaAuthEndpointSuccess("new-test-password");
    mockAwsSecretsCall("dvla-api-password", "new-test-password");

    String token = vehicleApiAuthenticationUtility.getAuthenticationToken();

    assertEquals("test-token", token);
  }

  @Test
  public void errorThrownIfRetryAlreadyAttempted() {
    mockDvlaAuthEndpointFailure("test-password");
    mockDvlaAuthEndpointFailure("new-test-password");
    mockAwsSecretsCall("dvla-api-password", "new-test-password");

    assertThrows(HttpClientErrorException.class,
        () -> vehicleApiAuthenticationUtility.getAuthenticationToken());
  }

  private void mockDvlaAuthEndpointSuccess(String password) {
    mockServer
        .when(HttpRequest.request()
            .withPath("/dvla-api/authenticate")
            .withMethod("POST")
            .withBody("{\"userName\":\"test-username\",\"password\":\"" + password + "\"}"))
        .respond(HttpResponse.response()
            .withBody("{\"id-token\": \"test-token\"}",
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

  private void someSlackTimeToLetResetPropagate() throws InterruptedException {
    Thread.sleep(1500);
  }
}
