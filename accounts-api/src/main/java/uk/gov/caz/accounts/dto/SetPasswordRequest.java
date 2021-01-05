package uk.gov.caz.accounts.dto;

import static java.util.Objects.isNull;

import com.google.common.base.Strings;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import lombok.Builder;
import lombok.Value;
import uk.gov.caz.accounts.controller.exception.InvalidRequestPayloadException;
import uk.gov.caz.accounts.util.MapPreservingOrderBuilder;

/**
 * Class that represents incoming JSON payload for set password endpoint.
 */
@Value
@Builder
public class SetPasswordRequest {

  /**
   * Password reset token.
   */
  UUID token;

  /**
   * New password.
   */
  String password;

  /**
   * Private method with validation rules.
   */
  private static final Map<Function<SetPasswordRequest, Boolean>, String> validators =
      MapPreservingOrderBuilder.<Function<SetPasswordRequest, Boolean>, String>builder()
          .put(tokenIsNotNull(), "Token cannot be null.")
          .put(passwordIsNotNullOrEmpty(), "Password cannot be null or empty.")
          .build();

  /**
   * Public method that validates given object and throws exceptions if validation doesn't pass.
   */
  public void validate() {
    validators.forEach((validator, message) -> {
      Boolean isValid = validator.apply(this);
      if (!isValid) {
        throw new InvalidRequestPayloadException(message);
      }
    });
  }

  /**
   * Returns a lambda that verifies if 'token' is not null.
   */
  private static Function<SetPasswordRequest, Boolean> tokenIsNotNull() {
    return request -> !isNull(request.getToken());
  }

  /**
   * Returns a lambda that verifies if 'password' is not null or empty.
   */
  private static Function<SetPasswordRequest, Boolean> passwordIsNotNullOrEmpty() {
    return request -> !Strings.isNullOrEmpty(request.getPassword());
  }
}
