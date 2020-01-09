package uk.gov.caz.vcc.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RemoteVehicleAuthenticationResponse {

  @JsonProperty("id-token")
  public String idToken;

}
