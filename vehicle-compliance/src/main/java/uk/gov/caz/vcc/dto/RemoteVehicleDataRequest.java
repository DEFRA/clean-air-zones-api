package uk.gov.caz.vcc.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * Simple request wrapper for fetching remote vehicle data from an API.
 *
 */
@Builder
@Getter
public class RemoteVehicleDataRequest {

  private String registrationNumber;

}
