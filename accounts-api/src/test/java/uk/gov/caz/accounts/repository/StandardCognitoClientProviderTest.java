package uk.gov.caz.accounts.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;

class StandardCognitoClientProviderTest {

  private StandardCognitoClientProvider standardCognitoClientProvider
      = new StandardCognitoClientProvider();

  @Test
  public void shouldProvideBaseCognitoClient() {
    assertThat(standardCognitoClientProvider.getNewCognitoClient())
        .isInstanceOf(CognitoIdentityProviderClient.class);
  }
}