package uk.gov.caz.accounts.service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import uk.gov.caz.ApplicationRuntimeException;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class PageOutOfBoundsException extends ApplicationRuntimeException {

  private static final long serialVersionUID = 257253865103469992L;

  public PageOutOfBoundsException() {
    super("The page number provided is out of bounds.");
  }

}
