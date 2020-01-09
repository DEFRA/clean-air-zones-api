package uk.gov.caz.vcc.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * Simple request wrapper for authenticating against the DVLA API.
 *
 */
@Builder
@Getter
public class RemoteVehicleAuthenticationRequest {

  private String userName;
  private String password;

}
