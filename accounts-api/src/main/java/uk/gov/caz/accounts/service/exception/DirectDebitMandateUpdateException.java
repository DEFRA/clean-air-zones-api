package uk.gov.caz.accounts.service.exception;

import java.util.List;
import lombok.Getter;
import uk.gov.caz.ApplicationRuntimeException;
import uk.gov.caz.accounts.dto.DirectDebitMandateUpdateError;

/**
 * An error to be thrown when validation of provided direct debit mandates data is invalid.
 */
public class DirectDebitMandateUpdateException extends ApplicationRuntimeException {

  @Getter
  private final List<DirectDebitMandateUpdateError> errors;

  public DirectDebitMandateUpdateException(List<DirectDebitMandateUpdateError> errors) {
    super("Direct debit mandates list update failed.");
    this.errors = errors;
  }
}
