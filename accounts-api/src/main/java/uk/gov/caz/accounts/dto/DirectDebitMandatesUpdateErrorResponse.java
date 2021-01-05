package uk.gov.caz.accounts.dto;

import java.util.List;
import lombok.Builder;
import lombok.Value;

/**
 * An object representing response with validation errors which occurred while updating direct debit
 * mandates.
 */
@Value
@Builder
public class DirectDebitMandatesUpdateErrorResponse {

  List<DirectDebitMandateUpdateError> errors;

  /**
   * Public method creating response object from provided errors list.
   */
  public static DirectDebitMandatesUpdateErrorResponse from(
      List<DirectDebitMandateUpdateError> errors) {
    return DirectDebitMandatesUpdateErrorResponse.builder().errors(errors).build();
  }
}
