package uk.gov.caz.accounts;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import uk.gov.caz.accounts.repository.IdentityProvider;
import uk.gov.caz.accounts.repository.SecretServiceBasedCognitoClientProvider;

@Configuration
public class TestContext {

  @Bean
  @Primary
  public IdentityProvider stubbedIdentityProvider() {
    return new StubbedIdentityProvider();
  }

  @Bean
  @Primary
  public SecretServiceBasedCognitoClientProvider cognitoClientProvider() {
    return new SecretServiceBasedCognitoClientProvider(null, null, null) {
      @Override
      public CognitoIdentityProviderClient getNewCognitoClient() {
        return CognitoIdentityProviderClient.builder().region(Region.EU_WEST_2).build();
      }
    };
  }
}
