package uk.gov.caz.accounts.repository.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import uk.gov.caz.ApplicationRuntimeException;

@ResponseStatus(value = HttpStatus.UNPROCESSABLE_ENTITY)
public class NotUniquePaymentProviderMandateIdException extends ApplicationRuntimeException {
  public NotUniquePaymentProviderMandateIdException(String message) {
    super(message);
  }
}
