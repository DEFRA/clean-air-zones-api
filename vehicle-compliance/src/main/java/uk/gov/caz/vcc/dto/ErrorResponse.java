package uk.gov.caz.vcc.dto;

import lombok.Builder;
import lombok.ToString;
import lombok.Value;

@Value
@Builder
public class ErrorResponse {

  @ToString.Exclude
  String vrn;
  String title;
  String detail;
  Integer status;

  /**
   * Method to return the status.
   * @return status of error response.
   */
  public int getStatus() {
    return status;
  }
}
