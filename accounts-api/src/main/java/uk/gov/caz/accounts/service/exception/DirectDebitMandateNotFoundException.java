package uk.gov.caz.accounts.service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import uk.gov.caz.ApplicationRuntimeException;

/**
 * Exception class which will be used to throw exception when not able to find Direct Debit Mandate
 * in DB.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class DirectDebitMandateNotFoundException extends ApplicationRuntimeException {

  public DirectDebitMandateNotFoundException(String message) {
    super(message);
  }
}
