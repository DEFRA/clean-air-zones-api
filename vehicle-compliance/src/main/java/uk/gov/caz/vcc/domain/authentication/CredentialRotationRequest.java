package uk.gov.caz.vcc.domain.authentication;

import lombok.Builder;
import lombok.Value;

/**
 * Request wrapper for rotating an API key or password on the remote
 * vehicle API.
 *
 */
@Value
@Builder
public class CredentialRotationRequest {
  
  String credentialRotationType;
  
}
