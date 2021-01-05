package uk.gov.caz.accounts.dto;

import io.swagger.annotations.ApiModelProperty;
import java.net.URI;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import lombok.Builder;
import lombok.Value;
import org.hibernate.validator.internal.constraintvalidators.hv.EmailValidator;
import org.springframework.util.StringUtils;
import uk.gov.caz.accounts.controller.exception.InvalidRequestPayloadException;
import uk.gov.caz.accounts.util.MapPreservingOrderBuilder;
import uk.gov.caz.common.util.Strings;

/**
 * Class that represents incoming JSON payload for an endpoint that creates and invites a user.
 */
@Value
@Builder
public class CreateAndInviteUserRequestDto {

  public static final int NAME_MAX_LENGTH = 255;

  private static final EmailValidator EMAIL_VALIDATOR = new EmailValidator();

  /**
   * Email of the invited user.
   */
  @ApiModelProperty(value = "${swagger.model.descriptions.create-and-invite-user-for-account."
      + "email}")
  String email;

  /**
   * Username of the invited user.
   */
  @ApiModelProperty(value = "${swagger.model.descriptions.create-and-invite-user-for-account."
      + "name}")
  String name;

  /**
   * Internal identifier of the inviting user.
   */
  @ApiModelProperty(value = "${swagger.model.descriptions.create-and-invite-user-for-account."
      + "is-administered-by}")
  String isAdministeredBy;

  /**
   * URL to which user will be redirected in order to verify email.
   */
  @ApiModelProperty(
      value = "${swagger.operations.accounts.create-and-invite-user-for-account.verification-url}")
  String verificationUrl;

  /**
   * List of permissions that needs to added for given user.
   */
  @ApiModelProperty(value = "${swagger.model.descriptions.users.permissions}")
  Set<String> permissions;

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
   * Private field with validation rules.
   */
  private static final Map<Function<CreateAndInviteUserRequestDto, Boolean>, String> validators =
      MapPreservingOrderBuilder
          .<Function<CreateAndInviteUserRequestDto, Boolean>, String>builder()
          .put(emailIsNotBlank(), "email cannot be null or empty.")
          .put(emailIsValid(), "email is not valid.")
          .put(nameIsNotBlank(), "name cannot be null or empty.")
          .put(nameIsNotTooLong(), "name is too long.")
          .put(isAdministeredByIsNotBlank(), "isAdministeredBy cannot be null or empty.")
          .put(isAdministeredByIsValidUuid(), "isAdministeredBy should be a valid UUID value.")
          .put(verificationUrlIsNotBlank(), "verificationUrl cannot be null or empty.")
          .put(verificationUrlIsValid(), "verificationUrl is not valid.")
          .build();

  /**
   * Returns a lambda that verifies if 'name' is not blank.
   */
  private static Function<CreateAndInviteUserRequestDto, Boolean> nameIsNotBlank() {
    return request -> StringUtils.hasText(request.getName());
  }

  /**
   * Returns a lambda that verifies if 'name' is not too long.
   */
  private static Function<CreateAndInviteUserRequestDto, Boolean> nameIsNotTooLong() {
    return request -> request.getName().length() <= NAME_MAX_LENGTH;
  }

  /**
   * Returns a lambda that verifies if 'isAdministeredBy' is not blank.
   */
  private static Function<CreateAndInviteUserRequestDto, Boolean> isAdministeredByIsNotBlank() {
    return request -> StringUtils.hasText(request.getIsAdministeredBy());
  }

  /**
   * Returns a lambda that verifies if 'isAdministeredBy' is a valid UUID identifier.
   */
  private static Function<CreateAndInviteUserRequestDto, Boolean> isAdministeredByIsValidUuid() {
    return request -> Strings.isValidUuid(request.getIsAdministeredBy());
  }

  /**
   * Returns a lambda that verifies if 'email' is not blank.
   */
  private static Function<CreateAndInviteUserRequestDto, Boolean> emailIsNotBlank() {
    return request -> StringUtils.hasText(request.getEmail());
  }

  /**
   * Returns a lambda that verifies if 'email' is valid.
   */
  private static Function<CreateAndInviteUserRequestDto, Boolean> emailIsValid() {
    return request -> EMAIL_VALIDATOR.isValid(request.getEmail(), null);
  }

  /**
   * Returns a lambda that verifies if 'verificationUrl' is not blank.
   */
  private static Function<CreateAndInviteUserRequestDto, Boolean> verificationUrlIsNotBlank() {
    return request -> StringUtils.hasText(request.getVerificationUrl());
  }

  /**
   * Returns a lambda that verifies if 'verificationUrl' is valid.
   */
  private static Function<CreateAndInviteUserRequestDto, Boolean> verificationUrlIsValid() {
    return request -> isValidUri(request.getVerificationUrl());
  }

  /**
   * Helper method for validating URI format.
   */
  private static boolean isValidUri(String uri) {
    try {
      URI.create(uri);
      return true;
    } catch (IllegalArgumentException e) {
      return false;
    }
  }

}
