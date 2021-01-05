package uk.gov.caz.accounts.configuration;

import static uk.gov.caz.accounts.util.Strings.mask;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.caz.accounts.repository.CognitoIdentityProviderClient;
import uk.gov.caz.accounts.repository.IdentityProvider;

/**
 * Spring Configuration class for {@link IdentityProvider}.
 */
@Configuration
@Slf4j
public class RepositoriesConfiguration {

  public static final String AWS_ARN_DEFAULT_PREFIX = "arn:aws";

  /**
   * Creates and initializes {@link IdentityProvider}.
   *
   * @return A configured IdentityProvider.
   */
  @Bean
  public IdentityProvider identityProvider(@Value("${aws.cognito.user-pool-id}") String userPoolId,
      @Value("${aws.cognito.client-id}") String appClientId,
      @Value("${aws.cognito.client-secret}") String appClientSecret,
      CognitoIdentityProviderClient cognitoClient) {
    log.info("Creating IdentityProvider with user-pool-id: '{}', client-id: '{}', "
        + "client-secret: '{}'", mask(userPoolId), mask(appClientId), mask(appClientSecret));
    String normalisedUserPool = normaliseUserPool(userPoolId);
    return new IdentityProvider(normalisedUserPool, appClientId, appClientSecret, cognitoClient);
  }

  /**
   * Normalises user pool, stripping prefix if needed.
   * @param userPoolId to be normalised.
   * @return
   */
  private String normaliseUserPool(String userPoolId) {
    if (userPoolId.startsWith(AWS_ARN_DEFAULT_PREFIX)) {
      log.info("Using stripped form of cognito user pool.");
      String[] splitTextElements = userPoolId.split("/");
      if (splitTextElements.length < 2) {
        throw new RuntimeException("ARN is not expected, should contain a slash.");
      }
      return splitTextElements[1];
    } else {
      log.info("Using original form of cognito user pool id.");
      return userPoolId;
    }

  }
}
