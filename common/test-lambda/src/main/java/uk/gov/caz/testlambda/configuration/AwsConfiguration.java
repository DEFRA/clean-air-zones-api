package uk.gov.caz.testlambda.configuration;

import uk.gov.caz.testlambda.util.AwsHelpers;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;

@Configuration
@Slf4j
public class AwsConfiguration {

  @Bean
  @Profile("!integration-tests & !localstack")
  public CognitoIdentityProviderClient cognitoClient() {
    if (AwsHelpers.areWeRunningLocallyUsingSam()) {
      log.info("Running Lambda locally using SAM Local");
    }

    logAwsVariables();

    return CognitoIdentityProviderClient.create();
  }

  private void logAwsVariables() {
    String awsAccessKeyId = AwsHelpers.getAwsAccessKeyFromEnvVar();
    String awsRegion = AwsHelpers.getAwsRegionFromEnvVar();
    String awsProfile = AwsHelpers.getAwsProfileFromEnvVar();

    log.info("IAM env credentials: Access Key Id is '{}'; AWS Region is '{}'; AWS profile is '{}'",
        awsAccessKeyId,
        awsRegion,
        awsProfile);
  }
}