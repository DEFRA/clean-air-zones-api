package uk.gov.caz.accounts.repository;

import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;

public interface CognitoClientProvider {
  CognitoIdentityProviderClient getNewCognitoClient();
}
