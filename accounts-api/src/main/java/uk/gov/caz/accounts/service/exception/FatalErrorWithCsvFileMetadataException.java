package uk.gov.caz.accounts.service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import uk.gov.caz.ApplicationRuntimeException;

/**
 * An exception thrown when there is an error upon extracting metadata from the csv file.
 */
@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class FatalErrorWithCsvFileMetadataException extends ApplicationRuntimeException {

  public FatalErrorWithCsvFileMetadataException(String message) {
    super(message);
  }
}
