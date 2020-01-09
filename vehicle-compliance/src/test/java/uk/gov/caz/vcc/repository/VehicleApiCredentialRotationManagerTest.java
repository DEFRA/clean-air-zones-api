package uk.gov.caz.vcc.repository;

import static org.mockito.Mockito.times;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import com.amazonaws.services.secretsmanager.AWSSecretsManager;
import com.amazonaws.services.secretsmanager.model.GetSecretValueRequest;
import com.amazonaws.services.secretsmanager.model.GetSecretValueResult;
import com.amazonaws.services.secretsmanager.model.PutSecretValueRequest;
import uk.gov.caz.vcc.domain.authentication.CredentialRotationRequest;
import uk.gov.caz.vcc.domain.authentication.CredentialRotationType;
import uk.gov.caz.vcc.domain.authentication.VehicleApiCredentialRotationManager;
import uk.gov.caz.vcc.domain.authentication.VehicleApiCredentialRotationManagerBuilder;
import uk.gov.caz.vcc.util.RandomPasswordGenerator;
import uk.gov.caz.vcc.util.VehicleApiAuthenticationUtility;

@ExtendWith(MockitoExtension.class)
public class VehicleApiCredentialRotationManagerTest {

  VehicleApiCredentialRotationManager awsSecretsWrapper;

  @Mock
  VehicleApiAuthenticationUtility remoteVehicleApiAuthenticationUtility;

  @Mock
  RandomPasswordGenerator randomPasswordGenerator;

  @Mock
  AWSSecretsManager client;

  @Mock
  VehicleApiCredentialRotationManagerBuilder awsSecretsManagerClientBuilderWrapper;

  @Mock
  GetSecretValueResult getSecretValueResult;

  String token;
  String apiKey;

  @BeforeEach
  void init() {
    this.token = "tokenTest";
    this.apiKey = "apiKeyTest";
    this.awsSecretsWrapper =
        new VehicleApiCredentialRotationManager(this.remoteVehicleApiAuthenticationUtility,
            this.awsSecretsManagerClientBuilderWrapper);
    Mockito
        .when(this.awsSecretsManagerClientBuilderWrapper
            .buildAwsSecretsManagerClient(ArgumentMatchers.<String>isNull()))
        .thenReturn(client);

    Mockito
        .when(this.client
            .getSecretValue(Mockito.any(GetSecretValueRequest.class)))
        .thenReturn(getSecretValueResult);
    Mockito.when(getSecretValueResult.getSecretString())
        .thenReturn("{\"testKey\":\"testValue\"}");

    Mockito
        .when(this.client
            .putSecretValue(Mockito.any(PutSecretValueRequest.class)))
        .thenReturn(null);
  }

  @Test
  void canGenerateNewApiKey() throws IOException {
    Mockito.when(remoteVehicleApiAuthenticationUtility.getAuthenticationToken())
        .thenReturn(token);

    Mockito.when(remoteVehicleApiAuthenticationUtility.renewApiKey(Mockito.anyString(),
        ArgumentMatchers.<String>isNull())).thenReturn(apiKey);

    CredentialRotationRequest request =  CredentialRotationRequest.builder()
        .credentialRotationType(CredentialRotationType.API_KEY.toString()).build();
    
    awsSecretsWrapper.generateNewCredential(request);

    // get secret checks
    Mockito.verify(awsSecretsManagerClientBuilderWrapper, times(2))
        .buildAwsSecretsManagerClient(ArgumentMatchers.<String>isNull());
    Mockito.verify(client, times(1))
        .getSecretValue(Mockito.any(GetSecretValueRequest.class));
    Mockito.verify(getSecretValueResult, times(2)).getSecretString();

    Mockito.verify(remoteVehicleApiAuthenticationUtility, times(1))
        .getAuthenticationToken();
    Mockito.verify(remoteVehicleApiAuthenticationUtility, times(1))
        .renewApiKey(Mockito.anyString(), ArgumentMatchers.<String>isNull());

    // put secret checks
    Mockito.verify(client, times(1))
        .putSecretValue(Mockito.any(PutSecretValueRequest.class));
  }

  @Test
  void canGenerateNewPassword() {
    CredentialRotationRequest request =  CredentialRotationRequest.builder()
        .credentialRotationType(CredentialRotationType.PASSWORD.toString()).build();
    
    awsSecretsWrapper.generateNewCredential(request);

    // get secret checks
    Mockito.verify(awsSecretsManagerClientBuilderWrapper, times(2))
        .buildAwsSecretsManagerClient(ArgumentMatchers.<String>isNull());
    Mockito.verify(client, times(1))
        .getSecretValue(Mockito.any(GetSecretValueRequest.class));
    Mockito.verify(getSecretValueResult, times(2)).getSecretString();

    Mockito.verify(remoteVehicleApiAuthenticationUtility, times(1))
        .changePassword(Mockito.anyString());

    // put secret checks
    Mockito.verify(client, times(1))
        .putSecretValue(Mockito.any(PutSecretValueRequest.class));
  }

}
