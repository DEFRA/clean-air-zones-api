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
 * Class that represents incoming JSON payload for creation of User for existing Account.
 */
@Value
@Builder
public class UserForAccountCreationRequestDto {

  /**
   * Unique login credential.
   */
  @ApiModelProperty(value = "${swagger.model.descriptions.account.email}")
  String email;

  /**
   * User password used to login to the application.
   */
  @ApiModelProperty(value = "${swagger.model.descriptions.account.password}")
  String password;

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
  private static final Map<Function<UserForAccountCreationRequestDto, Boolean>, String> validators =
      MapPreservingOrderBuilder
          .<Function<UserForAccountCreationRequestDto, Boolean>, String>builder()
          .put(emailIsNotBlank(), "email cannot be null or empty.")
          .put(passwordIsNotBlank(), "password cannot be null or empty.")
          .put(verificationUrlIsNotBlank(), "verificationUrl cannot be null or empty.")
          .build();

  /**
   * Returns a lambda that verifies if 'email' is not null or empty.
   */
  private static Function<UserForAccountCreationRequestDto, Boolean> emailIsNotBlank() {
    return request -> !Strings.isNullOrEmpty(request.getEmail());
  }

  /**
   * Returns a lambda that verifies if 'password' is not null or empty.
   */
  private static Function<UserForAccountCreationRequestDto, Boolean> passwordIsNotBlank() {
    return request -> !Strings.isNullOrEmpty(request.getPassword());
  }

  /**
   * Returns a lambda that verifies if 'verificationUrl' is not null or empty.
   */
  private static Function<UserForAccountCreationRequestDto, Boolean> verificationUrlIsNotBlank() {
    return request -> !Strings.isNullOrEmpty(request.getVerificationUrl());
  }
}
