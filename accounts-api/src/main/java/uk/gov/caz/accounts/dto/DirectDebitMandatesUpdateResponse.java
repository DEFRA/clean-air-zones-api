package uk.gov.caz.accounts.dto;

import lombok.Value;

/**
 * A successful response for update of direct debit mandates.
 */
@Value
public class DirectDebitMandatesUpdateResponse {

  String message = "Direct debit mandates updated successfully";
}
