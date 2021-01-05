package uk.gov.caz.accounts.dto;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import lombok.Builder;
import lombok.Value;
import org.springframework.util.StringUtils;
import uk.gov.caz.accounts.controller.exception.InvalidRequestPayloadException;

@Value
@Builder(toBuilder = true)
public class AccountVerificationRequestDto {

  String token;

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

  private static final Map<Function<AccountVerificationRequestDto, Boolean>, String> validators =
      ImmutableMap.of(
          tokenIsNotBlank(), "token cannot be blank.",
          tokenIsValidUuid(), "invalid format of the token."
      );

  /**
   * Returns a lambda that verifies if 'token' is not null or empty.
   */
  private static Function<AccountVerificationRequestDto, Boolean> tokenIsNotBlank() {
    return request -> StringUtils.hasText(request.getToken());
  }

  /**
   * Returns a lambda that verifies if 'token' is a valid UUID.
   */
  private static Function<AccountVerificationRequestDto, Boolean> tokenIsValidUuid() {
    return request -> isValidUuid(request.getToken());
  }

  /**
   * Helper method for validating token format.
   */
  private static boolean isValidUuid(String token) {
    try {
      // see https://bugs.java.com/bugdatabase/view_bug.do?bug_id=8159339
      return UUID.fromString(token).toString().equals(token);
    } catch (IllegalArgumentException e) {
      return false;
    }
  }

}
