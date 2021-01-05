package uk.gov.caz.accounts.dto;

import static java.util.Objects.isNull;

import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import lombok.Builder;
import lombok.Value;
import uk.gov.caz.accounts.controller.exception.InvalidRequestPayloadException;
import uk.gov.caz.accounts.util.MapPreservingOrderBuilder;

@Value
@Builder
public class ValidateTokenRequest {

  /**
   * Password reset token.
   */
  UUID token;

  /**
   * Private method with validation rules.
   */
  private static final Map<Function<ValidateTokenRequest, Boolean>, String> validators =
      MapPreservingOrderBuilder.<Function<ValidateTokenRequest, Boolean>, String>builder()
          .put(tokenIsNotNull(), "Token cannot be null.")
          .build();

  /**
   * Public method that validates given object and throws exceptions if validation doesn't pass.
   */
  public ValidateTokenRequest validate() {
    validators.forEach((validator, message) -> {
      Boolean isValid = validator.apply(this);
      if (!isValid) {
        throw new InvalidRequestPayloadException(message);
      }
    });
    return this;
  }

  /**
   * Returns a lambda that verifies if 'token' is not null.
   */
  private static Function<ValidateTokenRequest, Boolean> tokenIsNotNull() {
    return request -> !isNull(request.getToken());
  }

}
