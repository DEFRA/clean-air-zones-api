package uk.gov.caz.vcc.dto;

import com.opencsv.bean.CsvBindByPosition;

import lombok.Builder;
import lombok.Getter;

/**
 * Simple request wrapper for fetching remote vehicle data from an API.
 */
@Builder
@Getter
public class RemoteVehicleDataRequest {

  @CsvBindByPosition(position = 0)
  private String registrationNumber;

  public RemoteVehicleDataRequest(String registrationNumber) {
    this.registrationNumber = registrationNumber;
  }

  public RemoteVehicleDataRequest() {
  }
}
