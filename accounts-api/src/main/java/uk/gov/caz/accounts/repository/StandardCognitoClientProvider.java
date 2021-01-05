package uk.gov.caz.accounts.repository;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;

public class StandardCognitoClientProvider implements CognitoClientProvider {
  @Override
  public CognitoIdentityProviderClient getNewCognitoClient() {
    return CognitoIdentityProviderClient.builder().region(Region.EU_WEST_2).build();
  }
}