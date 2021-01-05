package uk.gov.caz.accounts.repository.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import uk.gov.caz.ApplicationRuntimeException;

@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public class IdentityProviderUnavailableException extends ApplicationRuntimeException {

  public IdentityProviderUnavailableException() {
    super("External Service Failure");
  }

  public IdentityProviderUnavailableException(String message) {
    super(message);
  }
}
