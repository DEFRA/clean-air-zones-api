package uk.gov.caz.vcc.domain.authentication;

/**
 * Enumerable for credential types to rotate.
 *
 */
public enum CredentialRotationType {
  API_KEY("api-key"), 
  PASSWORD("password");
  
  private final String text;
  
  private CredentialRotationType(String text) {
    this.text = text;
  }
  
  @Override
  public String toString() {
    return this.text;
  }
}
