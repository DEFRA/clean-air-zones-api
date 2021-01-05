package uk.gov.caz.accounts.dto;

import com.google.common.base.Strings;
import java.util.Map;
import java.util.function.Function;
import lombok.Builder;
import lombok.Value;
import uk.gov.caz.accounts.controller.exception.InvalidRequestPayloadException;
import uk.gov.caz.accounts.util.MapPreservingOrderBuilder;

/**
 * Class that represents the JSON structure for the incoming request when confirming the email
 * change.
 */
@Value
@Builder
public class ConfirmEmailChangeRequest {

  /**
   * Token used to change the users email.
   */
  String emailChangeVerificationToken;

  /**
   * New password which is going to be set after the email change.
   */
  String password;

  /**
   * Public method that validates given object and throws exceptions if validation doesn't pass.
   */
  public void validate() {
    validators.forEach((validator, message) -> {
      boolean isValid = validator.apply(this);
      if (!isValid) {
        throw new InvalidRequestPayloadException(message);
      }
    });
  }

  /**
   * Private method with validation rules.
   */
  private static final Map<Function<ConfirmEmailChangeRequest, Boolean>, String> validators =
      MapPreservingOrderBuilder.<Function<ConfirmEmailChangeRequest, Boolean>, String>builder()
          .put(tokenIsNotNull(), "emailChangeVerificationToken cannot be null.")
          .put(passwordIsNotBlank(), "password cannot be null or empty.")
          .build();

  /**
   * Returns lambda that verifies if 'emailChangeVerificationToken' is not null.
   */
  private static Function<ConfirmEmailChangeRequest, Boolean> tokenIsNotNull() {
    return request -> !Strings.isNullOrEmpty(request.getEmailChangeVerificationToken());
  }

  /**
   * Returns lambda that verified is 'password' is not blank.
   */
  private static Function<ConfirmEmailChangeRequest, Boolean> passwordIsNotBlank() {
    return request -> !Strings.isNullOrEmpty(request.password);
  }
}
