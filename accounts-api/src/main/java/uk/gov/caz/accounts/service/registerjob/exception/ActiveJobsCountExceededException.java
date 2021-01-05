package uk.gov.caz.accounts.service.registerjob.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import uk.gov.caz.ApplicationRuntimeException;

/**
 * Exception that indicates that for a given account id there is at least one not finished register
 * job.
 */
@ResponseStatus(HttpStatus.NOT_ACCEPTABLE)
public class ActiveJobsCountExceededException extends ApplicationRuntimeException {

  public ActiveJobsCountExceededException() {
    super("Previous fleet vehicles register job has not finished yet");
  }
}