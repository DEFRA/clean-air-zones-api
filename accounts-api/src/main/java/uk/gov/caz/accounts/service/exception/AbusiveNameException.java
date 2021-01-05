package uk.gov.caz.accounts.service.exception;

import lombok.EqualsAndHashCode;
import lombok.Value;
import uk.gov.caz.accounts.model.ProhibitedTermType;

/**
 * An exception that is thrown when an improper language for the account's name is detected.
 */
@Value
@EqualsAndHashCode(callSuper = false)
public class AbusiveNameException extends RuntimeException {
  ProhibitedTermType type;

  public AbusiveNameException(ProhibitedTermType type) {
    super("Improper language detected, type: " + type);
    this.type = type;
  }
}
