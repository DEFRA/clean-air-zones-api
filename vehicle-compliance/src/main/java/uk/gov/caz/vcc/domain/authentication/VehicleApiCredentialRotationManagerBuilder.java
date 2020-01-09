package uk.gov.caz.vcc.domain.authentication;

import com.amazonaws.services.secretsmanager.AWSSecretsManager;
import com.amazonaws.services.secretsmanager.AWSSecretsManagerClientBuilder;

/**
 * Builder class for a AWSSecretsManagerClientBuilder instance.
 *
 */
public class VehicleApiCredentialRotationManagerBuilder {

  public static final VehicleApiCredentialRotationManagerBuilder wrapper =
      new VehicleApiCredentialRotationManagerBuilder();

  /**
   * Builder method for an AWS Secrets manager client.
   * @param region the aws region to target.
   * @return a region configured AWS Secrets manager client.
   */
  public AWSSecretsManager buildAwsSecretsManagerClient(String region) {
    return AWSSecretsManagerClientBuilder.standard().withRegion(region).build();
  }
}
