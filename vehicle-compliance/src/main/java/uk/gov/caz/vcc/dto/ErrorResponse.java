package uk.gov.caz.vcc.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ErrorResponse {

  String vrn;
  String title;
  String detail;
  Integer status;

  public int getStatus() {
    return status;
  }
}
