package uk.gov.caz.accounts.dto;

import java.util.List;
import lombok.Builder;
import lombok.Value;

/**
 * Class that represents the JSON structure for response when getting list of DirectDebitMandates.
 */
@Value
@Builder
public class DirectDebitMandatesResponse {

  /**
   * The list of DirectDebitMandates associated with the account ID provided in the request.
   */
  List<DirectDebitMandateDto> directDebitMandates;

}
