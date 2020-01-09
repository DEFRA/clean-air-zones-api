package uk.gov.caz.vcc.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class RemoteDataNewApiKeyResponse {

  private String message;
  private String newApiKey;

}
