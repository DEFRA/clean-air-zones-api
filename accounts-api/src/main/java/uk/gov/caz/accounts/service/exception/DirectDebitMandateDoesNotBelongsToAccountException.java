package uk.gov.caz.accounts.service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import uk.gov.caz.ApplicationRuntimeException;

/**
 * Exception class which will be used to throw exception when DirectDebitMandate does not belongs to
 * provided accountId during DirectDebitMandate delete process.
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class DirectDebitMandateDoesNotBelongsToAccountException extends
    ApplicationRuntimeException {

  public DirectDebitMandateDoesNotBelongsToAccountException(String message) {
    super(message);
  }
}
