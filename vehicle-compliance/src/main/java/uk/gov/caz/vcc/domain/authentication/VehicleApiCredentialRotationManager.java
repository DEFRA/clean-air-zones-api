package uk.gov.caz.vcc.domain.authentication;

import com.amazonaws.services.secretsmanager.AWSSecretsManager;
import com.amazonaws.services.secretsmanager.model.GetSecretValueRequest;
import com.amazonaws.services.secretsmanager.model.GetSecretValueResult;
import com.amazonaws.services.secretsmanager.model.PutSecretValueRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.util.Base64;
import javax.annotation.Nullable;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

import uk.gov.caz.vcc.util.RandomPasswordGenerator;
import uk.gov.caz.vcc.util.VehicleApiAuthenticationUtility;


/**
 * Utility class for rotating credentials used for the remote Vehicle API.
 *
 */
@Slf4j
@Component
@RefreshScope
public class VehicleApiCredentialRotationManager {

  @Value("${aws.secret-name}")
  private String secretName;

  @Value("${aws.region}")
  private String region;

  @Value("${dvla-api-key}")
  private String dvlaApiKey;

  private final VehicleApiAuthenticationUtility vehicleApiAuthenticationUtility;
  private final VehicleApiCredentialRotationManagerBuilder credentialRotationManagerBuilder;

  private AWSSecretsManager client;

  /**
   * Public constructor for the VehicleApiCredentialRotationManager class.
   * 
   * @param remoteVehicleApiAuthenticationUtility Utility for handling
   *        communications with the external API.
   * @param awsSecretsManagerClientBuilderWrapper static class wrapper that can
   *        be mocked and passed from a test
   */
  public VehicleApiCredentialRotationManager(
      VehicleApiAuthenticationUtility remoteVehicleApiAuthenticationUtility,
      @Nullable VehicleApiCredentialRotationManagerBuilder awsSecretsManagerClientBuilderWrapper) {
    this.vehicleApiAuthenticationUtility =
        remoteVehicleApiAuthenticationUtility;

    if (awsSecretsManagerClientBuilderWrapper != null) {
      this.credentialRotationManagerBuilder =
          awsSecretsManagerClientBuilderWrapper;
    } else {
      this.credentialRotationManagerBuilder =
          VehicleApiCredentialRotationManagerBuilder.wrapper;
    }
  }

  /**
   * Default code snippet taken from AWS to fetch secrets values.
   * 
   * @return JSON String of existing secret values.
   */
  private String getSecretsValue() {

    client = this.credentialRotationManagerBuilder
        .buildAwsSecretsManagerClient(region);


    String secret;
    String decodedBinarySecret;

    GetSecretValueRequest getSecretValueRequest =
        new GetSecretValueRequest().withSecretId(secretName);
    GetSecretValueResult getSecretValueResult = null;

    getSecretValueResult = client.getSecretValue(getSecretValueRequest);

    // Decrypts secret using the associated KMS CMK.
    // Depending on whether the secret is a string or binary, one of these
    // fields will be populated.
    if (getSecretValueResult.getSecretString() != null) {
      secret = getSecretValueResult.getSecretString();
      return secret;
    } else {
      decodedBinarySecret = new String(Base64.getDecoder()
          .decode(getSecretValueResult.getSecretBinary()).array());

      return decodedBinarySecret;
    }
  }
  
  /**
   * Get secret value by a given key from external property source.
   * @param key the JSON object key to query
   * @return secret value for a given key.
   */
  public String getRemoteAuthenticationSecretValue(String key) {
    String rawSecretString = this.getSecretsValue();
    
    ObjectMapper objectMapper = new ObjectMapper();
    
    JsonNode jsonNode;

    try {
      jsonNode = objectMapper.readTree(rawSecretString);
      return jsonNode.get(key).asText();
    } catch (IOException e) {
      // if the existingSecrets are empty
      jsonNode = objectMapper.createObjectNode();
    }

    return jsonNode.toString();
  }

  /**
   * Method to update a single key/value pair in a JSON string.
   * 
   * @param existingSecrets JSON string to be updated
   * @param secretKey Key to be updated
   * @param secretValue Value to be updated
   * @return Updated JSON string
   * @throws IOException If there are no existing secrets to be appended to.
   */
  private String updateSecretString(String existingSecrets, String secretKey,
      String secretValue) {

    ObjectMapper objectMapper = new ObjectMapper();

    JsonNode jsonNode;

    try {
      jsonNode = objectMapper.readTree(existingSecrets);
    } catch (IOException e) {
      // if the existingSecrets are empty
      jsonNode = objectMapper.createObjectNode();
    }
    ((ObjectNode) jsonNode).put(secretKey, secretValue);

    return jsonNode.toString();
  }

  /**
   * Method to put a new secret value into secrets. This may be adding a new
   * key/value pair or updating an existing one.
   * 
   * @param updatedSecrets JSON string of secrets values to be put to secrets
   *        manager
   * @throws IOException If the existingSecrets JSON string is empty.
   */
  private void putUpdatedSecrets(String updatedSecrets) {
    client = this.credentialRotationManagerBuilder
        .buildAwsSecretsManagerClient(region);

    PutSecretValueRequest putSecretValueRequest = new PutSecretValueRequest()
        .withSecretId(secretName).withSecretString(updatedSecrets);

    client.putSecretValue(putSecretValueRequest);

    log.info("Successfully stored new API credential in secrets manager");
  }

  /**
   * Fetches existing secrets, generates a new JWT token, adds the new secret
   * value.
   * 
   */
  public void generateNewCredential(CredentialRotationRequest credentialType) {

    // 1. Establish AWS secrets connectivity

    String existingSecrets = getSecretsValue();
    String updatedSecrets;

    // 2. Generate new credential(s) and add to existing secrets

    if (credentialType.getCredentialRotationType()
        .equals(CredentialRotationType.API_KEY.toString())) {

      String token = vehicleApiAuthenticationUtility.getAuthenticationToken();

      String apiKey =
          vehicleApiAuthenticationUtility.renewApiKey(token, dvlaApiKey);
      
      // Note that in production, this key will not be logged due to error verbosity level
      log.info("Generated new API key: {}", apiKey);

      updatedSecrets =
          updateSecretString(existingSecrets, "dvla-api-key", apiKey);

    } else if (credentialType.getCredentialRotationType()
        .equals(CredentialRotationType.PASSWORD.toString())) {
      String newPassword = RandomPasswordGenerator.newRandomPassword();

      // Note that in production, this key will not be logged due to error verbosity level
      log.info("Generated new API password: {}", newPassword);

      vehicleApiAuthenticationUtility.changePassword(newPassword);
      updatedSecrets =
          updateSecretString(existingSecrets, "dvla-api-password", newPassword);
    } else {
      updatedSecrets = existingSecrets;
    }

    // 3. Put request to AWS secrets

    putUpdatedSecrets(updatedSecrets);

  }

}
