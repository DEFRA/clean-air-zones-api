package uk.gov.caz.accounts.service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import uk.gov.caz.ApplicationRuntimeException;

/**
 * Exception which is going to be thrown when user will try to resend verification email in a moment
 * when he or she is not supposed to do that.
 */
@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class InvalidActiveVerificationCodesAmount extends ApplicationRuntimeException {

  public InvalidActiveVerificationCodesAmount() {
    super("Invalid amount of active verification codes");
  }
}
