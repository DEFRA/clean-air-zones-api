package uk.gov.caz.accounts.repository;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.amazonaws.services.secretsmanager.AWSSecretsManager;
import com.amazonaws.services.secretsmanager.model.GetSecretValueRequest;
import com.amazonaws.services.secretsmanager.model.GetSecretValueResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.ByteBuffer;
import java.util.Base64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import uk.gov.caz.accounts.repository.SecretServiceBasedCognitoClientProvider.CustomAwsCredentialsProvider;

@ExtendWith(MockitoExtension.class)
class SecretServiceBasedCognitoClientProviderTest {

  public static final String SECRET = "secret";

  @Mock
  private AWSSecretsManager awsSecretsManager;

  private ObjectMapper objectMapper = new ObjectMapper();

  private SecretServiceBasedCognitoClientProvider cognitoClientProvider;

  @BeforeEach
  public void setUp() {
    cognitoClientProvider = new SecretServiceBasedCognitoClientProvider(
        SECRET,
        awsSecretsManager,
        objectMapper
    );
  }

  @Test
  public void shouldBuildCognitoIdentityProviderClientFromSecrets() throws Exception {
    //given
    GetSecretValueRequest getSecretValueRequest = new GetSecretValueRequest()
        .withSecretId(SECRET);
    String jsonSecret =
        "{\"awsAccessKeyId\":\"keyIDValue\", \"awsSecretAccessKey\":\"accessKeyValue\"}";
    when(awsSecretsManager.getSecretValue(getSecretValueRequest)).thenReturn(
        new GetSecretValueResult().withSecretString(
            jsonSecret)
    );

    //when
    CognitoIdentityProviderClient newCognitoClient = cognitoClientProvider.getNewCognitoClient();

    //then
    assertThat(newCognitoClient).isNotNull();
  }

  @Test
  public void shouldBuildCognitoIdentityProviderClientFromSecretsBase64() throws Exception {
    //given
    GetSecretValueRequest getSecretValueRequest = new GetSecretValueRequest()
        .withSecretId(SECRET);
    String jsonSecret =
        "{\"awsAccessKeyId\":\"keyIDValue\", \"awsSecretAccessKey\":\"accessKeyValue\"}";
    when(awsSecretsManager.getSecretValue(getSecretValueRequest)).thenReturn(
        new GetSecretValueResult().withSecretBinary(
            ByteBuffer.wrap(
                Base64.getEncoder().encode(jsonSecret.getBytes())
            ))
    );

    //when
    CognitoIdentityProviderClient newCognitoClient = cognitoClientProvider.getNewCognitoClient();

    //then
    assertThat(newCognitoClient).isNotNull();
  }

  @Test
  public void shouldThrowExceptionIfObjectMapperThrowsException() throws Exception {
    ObjectMapper objectMapper = mock(ObjectMapper.class);

    cognitoClientProvider = new SecretServiceBasedCognitoClientProvider(
        SECRET,
        awsSecretsManager,
        objectMapper
    );

    GetSecretValueRequest getSecretValueRequest = new GetSecretValueRequest()
        .withSecretId(SECRET);
    String jsonSecret =
        "{\"awsAccessKeyId\":\"keyIDValue\", \"awsSecretAccessKey\":\"accessKeyValue\"}";
    when(awsSecretsManager.getSecretValue(getSecretValueRequest)).thenReturn(
        new GetSecretValueResult().withSecretBinary(
            ByteBuffer.wrap(
                Base64.getEncoder().encode(jsonSecret.getBytes())
            ))
    );
    when(objectMapper.readValue(any(String.class), any(TypeReference.class)))
        .thenThrow(mock(JsonProcessingException.class));

    //then
    assertThrows(RuntimeException.class, () -> cognitoClientProvider.getNewCognitoClient());
  }

  @Test
  public void shouldCreateAwsCredentials() {
    //given
    String awsAccessKeyId = "1";
    String awsSecretAccessKey = "2";

    CustomAwsCredentialsProvider customAwsCredentialsProvider = new CustomAwsCredentialsProvider(
        awsAccessKeyId, awsSecretAccessKey);

    //when
    AwsCredentials awsCredentials = customAwsCredentialsProvider.resolveCredentials();

    //then
    assertThat(awsCredentials).isEqualTo(
        AwsBasicCredentials.create(awsAccessKeyId, awsSecretAccessKey));
  }
}