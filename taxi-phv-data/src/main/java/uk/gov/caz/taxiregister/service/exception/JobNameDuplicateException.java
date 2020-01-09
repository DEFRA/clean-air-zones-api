package uk.gov.caz.taxiregister.service.exception;

import org.springframework.dao.DuplicateKeyException;

public class JobNameDuplicateException extends DuplicateKeyException {

  public JobNameDuplicateException(String msg) {
    super(msg);
  }
}