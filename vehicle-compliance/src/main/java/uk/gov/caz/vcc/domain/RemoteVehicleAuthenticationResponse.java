package uk.gov.caz.vcc.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response object representing an ID token returned by the remote vehicle
 * data API authentication endpoint.
 *
 */
public class RemoteVehicleAuthenticationResponse {

  @JsonProperty("id-token")
  public String idToken;

}
