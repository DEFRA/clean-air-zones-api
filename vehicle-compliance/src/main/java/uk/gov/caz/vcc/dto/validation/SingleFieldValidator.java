package uk.gov.caz.vcc.dto.validation;

import java.util.Optional;

/**
 * Interface to be implemented by specialized field validators.
 */
public interface SingleFieldValidator<T> {
  Optional<ValidationError> validate(String vrn, T field);
}
