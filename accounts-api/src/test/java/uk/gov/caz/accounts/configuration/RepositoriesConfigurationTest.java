package uk.gov.caz.accounts.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.caz.accounts.configuration.RepositoriesConfiguration.AWS_ARN_DEFAULT_PREFIX;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import uk.gov.caz.accounts.repository.CredentialsRotatingCognitoIdentityProviderClient;
import uk.gov.caz.accounts.repository.IdentityProvider;

class RepositoriesConfigurationTest {

  private static final String VALID_CLIENT_ID = "clientId";
  private static final String VALID_CLIENT_APP_SECRET = "clientAppSecret";
  private RepositoriesConfiguration repositoriesConfiguration;
  private static final String VALID_USERPOOL_ID = "eu-west-2_" +
      RandomStringUtils.randomAlphanumeric(9);
  private static final String VALID_ARN =  AWS_ARN_DEFAULT_PREFIX +
      "aws:cognito-idp:eu-west-2:012345678901:userpool/" + VALID_USERPOOL_ID;
  private static final CredentialsRotatingCognitoIdentityProviderClient ANY_COGNITO_CLIENT =
      Mockito.mock(CredentialsRotatingCognitoIdentityProviderClient.class);


  @BeforeEach
  public void setup() {
    repositoriesConfiguration = new RepositoriesConfiguration();
  }

  @Test
  public void givenFullArnIdentityUserPoolIdValueIsStripped() {
    // when
    IdentityProvider identityProvider = repositoriesConfiguration
        .identityProvider(VALID_ARN, VALID_CLIENT_ID,
            VALID_CLIENT_APP_SECRET, ANY_COGNITO_CLIENT);

    // then
    assertThat(identityProvider.getUserPoolId()).isEqualTo(VALID_USERPOOL_ID);
  }

  @Test
  public void givenShortIdentityUserPoolIdValueIsNotStripped() {
    // when
    IdentityProvider identityProvider = repositoriesConfiguration
        .identityProvider(VALID_USERPOOL_ID, VALID_CLIENT_ID,
            VALID_CLIENT_APP_SECRET, ANY_COGNITO_CLIENT);

    // then
    assertThat(identityProvider.getUserPoolId()).isEqualTo(VALID_USERPOOL_ID);
  }

}