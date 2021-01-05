package uk.gov.caz.accounts.dto;

import java.net.URI;
import java.util.Map;
import java.util.function.Function;
import lombok.Builder;
import lombok.Value;
import org.hibernate.validator.internal.constraintvalidators.hv.EmailValidator;
import org.springframework.util.StringUtils;
import uk.gov.caz.accounts.controller.exception.InvalidRequestPayloadException;
import uk.gov.caz.accounts.util.MapPreservingOrderBuilder;
import uk.gov.caz.common.util.Strings;

/**
 * Object representing a payload for an endpoint that is responsible for changing the email of
 * a user.
 */
@Value
@Builder
public class InitiateEmailChangeRequest {

  private static final EmailValidator EMAIL_VALIDATOR = new EmailValidator();

  /**
   * Identifier of a user who wants to change his email address.
   */
  String accountUserId;

  /**
   * New email address which is to be set for this user.
   */
  String newEmail;

  /**
   * A root url of a link that will be generated for the user to confirm the change.
   */
  String confirmUrl;

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
  private static final Map<Function<InitiateEmailChangeRequest, Boolean>, String> validators =
      MapPreservingOrderBuilder
          .<Function<InitiateEmailChangeRequest, Boolean>, String>builder()
          .put(emailIsNotBlank(), "email cannot be null or empty.")
          .put(emailIsValid(), "email is not valid.")
          .put(accountUserIdIsNotBlank(), "accountUserId cannot be blank.")
          .put(accountUserIdIsValidUuid(), "accountUserId should be a valid UUID.")
          .put(confirmUrlIsNotBlank(), "confirmUrl cannot be null or empty.")
          .put(confirmUrlIsValidUri(), "confirmUrl should be a valid URL.")
          .build();

  /**
   * Returns a lambda that verifies if 'email' is not blank.
   */
  private static Function<InitiateEmailChangeRequest, Boolean> emailIsNotBlank() {
    return request -> StringUtils.hasText(request.getNewEmail());
  }

  /**
   * Returns a lambda that verifies if 'accountUserId' is not blank.
   */
  private static Function<InitiateEmailChangeRequest, Boolean> accountUserIdIsNotBlank() {
    return request -> StringUtils.hasText(request.getAccountUserId());
  }

  /**
   * Returns a lambda that verifies if 'confirmUrl' is not blank.
   */
  private static Function<InitiateEmailChangeRequest, Boolean> confirmUrlIsNotBlank() {
    return request -> StringUtils.hasText(request.getConfirmUrl());
  }

  /**
   * Returns a lambda that verifies if 'email' is valid.
   */
  private static Function<InitiateEmailChangeRequest, Boolean> emailIsValid() {
    return request -> EMAIL_VALIDATOR.isValid(request.getNewEmail(), null);
  }

  /**
   * Returns a lambda that verifies if 'accountUserId' is a valid UUID.
   */
  private static Function<InitiateEmailChangeRequest, Boolean> accountUserIdIsValidUuid() {
    return request -> Strings.isValidUuid(request.getAccountUserId());
  }

  /**
   * Returns a lambda that verifies if 'confirmUrl' is a valid URI.
   */
  private static Function<InitiateEmailChangeRequest, Boolean> confirmUrlIsValidUri() {
    return request -> isValidUri(request.getConfirmUrl());
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
