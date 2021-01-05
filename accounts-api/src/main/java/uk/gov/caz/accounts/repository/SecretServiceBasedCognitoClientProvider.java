package uk.gov.caz.accounts.repository;

import static com.google.common.base.Preconditions.checkNotNull;

import com.amazonaws.services.secretsmanager.AWSSecretsManager;
import com.amazonaws.services.secretsmanager.model.GetSecretValueRequest;
import com.amazonaws.services.secretsmanager.model.GetSecretValueResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Base64;
import java.util.Collections;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;

@Slf4j
@AllArgsConstructor
public class SecretServiceBasedCognitoClientProvider implements CognitoClientProvider {

  private final String cognitoSecretsSource;
  private final AWSSecretsManager awsSecretsManager;
  private final ObjectMapper objectMapper;

  /**
   * Method that fetches credentials from secret store and initializes CognitoClient.
   */
  @Override
  public CognitoIdentityProviderClient getNewCognitoClient() {
    Map<String, String> getSecretsValue = getSecretsValue(cognitoSecretsSource);

    String awsAccessKeyId = checkNotNull(getSecretsValue.get("awsAccessKeyId"));
    String awsSecretAccessKey = checkNotNull(getSecretsValue.get("awsSecretAccessKey"));

    return CognitoIdentityProviderClient.builder()
        .credentialsProvider(new CustomAwsCredentialsProvider(awsAccessKeyId, awsSecretAccessKey))
        .region(Region.EU_WEST_2).build();
  }

  private Map<String, String> getSecretsValue(String secretName) {
    GetSecretValueResult getSecretValueResult = fetchSecretValue(secretName);

    String secretString = getSecretValueResult.getSecretString() != null
        ? getSecretValueResult.getSecretString()
        : new String(Base64.getDecoder().decode(getSecretValueResult.getSecretBinary()).array());

    try {
      return objectMapper.readValue(secretString, new TypeReference<Map<String, String>>() {});
    } catch (JsonProcessingException e) {
      log.error("Error while parsing AWS secrets:", e);
      return Collections.emptyMap();
    }
  }

  private GetSecretValueResult fetchSecretValue(String secretName) {
    GetSecretValueRequest getSecretValueRequest = new GetSecretValueRequest()
        .withSecretId(secretName);
    return awsSecretsManager.getSecretValue(getSecretValueRequest);
  }

  @RequiredArgsConstructor
  static class CustomAwsCredentialsProvider implements AwsCredentialsProvider {
    private final String awsAccessKeyId;
    private final String awsSecretAccessKey;

    @Override
    public AwsCredentials resolveCredentials() {
      return AwsBasicCredentials.create(awsAccessKeyId, awsSecretAccessKey);
    }
  }
}
