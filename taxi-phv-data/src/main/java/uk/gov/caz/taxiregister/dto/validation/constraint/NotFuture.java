package uk.gov.caz.taxiregister.dto.validation.constraint;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import javax.validation.Constraint;
import javax.validation.Payload;

/**
 * Custom constraint annotation for checking whether the date is not a future one.
 */
@Target({ PARAMETER })
@Retention(RUNTIME)
@Constraint(validatedBy = NotFutureDateValidator.class)
@Documented
public @interface NotFuture {

  /**
   * A message that will be returned when validation fails.
   */
  String message() default "Cannot process a future date";

  /**
   * Unused, but the presence is required by the framework.
   */
  Class<?>[] groups() default { };

  /**
   * Unused, but the presence is required by the framework.
   */
  Class<? extends Payload>[] payload() default { };
}
