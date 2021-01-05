package uk.gov.caz.accounts.dto;

import com.google.common.base.Strings;
import io.swagger.annotations.ApiModelProperty;
import java.util.Map;
import java.util.function.Function;
import lombok.Builder;
import lombok.Value;
import uk.gov.caz.accounts.controller.exception.InvalidRequestPayloadException;
import uk.gov.caz.accounts.util.MapPreservingOrderBuilder;

/**
 * Class that represents incoming JSON payload for email resend.
 */
@Value
@Builder
public class UserVerificationEmailResendRequest {

  /**
   * URL to which user will be redirected in order to verify email.
   */
  @ApiModelProperty(
      value = "${swagger.operations.accounts.create-user-for-account.verification-url}")
  String verificationUrl;

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
  private static final Map<Function<UserVerificationEmailResendRequest, Boolean>, String>
      validators =
      MapPreservingOrderBuilder
          .<Function<UserVerificationEmailResendRequest, Boolean>, String>builder()
          .put(verificationUrlIsNotBlank(), "verificationUrl cannot be null or empty.")
          .build();

  /**
   * Returns a lambda that verifies if 'verificationUrl' is not null or empty.
   */
  private static Function<UserVerificationEmailResendRequest, Boolean> verificationUrlIsNotBlank() {
    return request -> !Strings.isNullOrEmpty(request.getVerificationUrl());
  }

}
