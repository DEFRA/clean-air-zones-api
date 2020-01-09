package uk.gov.caz.vcc.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * Simple request wrapper for changing password for remote vehicle data
 * repository.
 *
 */
@Builder
@Getter
public class RemoteVehicleDataNewPasswordRequest {

  private String userName;
  private String password;
  private String newPassword;

}
