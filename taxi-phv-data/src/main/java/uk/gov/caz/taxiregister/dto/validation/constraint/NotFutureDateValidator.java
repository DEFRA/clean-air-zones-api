package uk.gov.caz.taxiregister.dto.validation.constraint;

import java.time.LocalDate;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

/**
 * Custom validator that checks whether the passed date is today or from the past.
 */
public class NotFutureDateValidator implements ConstraintValidator<NotFuture, LocalDate> {

  /**
   * Checks if the passed {@code date} is a date from the past or equal to today.
   *
   * @param date An input date which will be validate.
   * @param constraintValidatorContext Validator context (unused).
   * @return true if {@code date} is null or not a future one.
   */
  @Override
  public boolean isValid(LocalDate date, ConstraintValidatorContext constraintValidatorContext) {
    if (date == null) {
      return true;
    }
    return !date.isAfter(LocalDate.now());
  }
}
