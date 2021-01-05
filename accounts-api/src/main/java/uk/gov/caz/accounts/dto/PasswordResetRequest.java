package uk.gov.caz.accounts.dto;

import static org.apache.logging.log4j.util.Strings.isNotBlank;

import io.swagger.annotations.ApiModelProperty;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.Builder;
import lombok.Value;
import org.hibernate.validator.internal.constraintvalidators.hv.EmailValidator;
import uk.gov.caz.accounts.controller.exception.InvalidRequestPayloadException;
import uk.gov.caz.accounts.util.MapPreservingOrderBuilder;

@Value
@Builder
public class PasswordResetRequest {

  private static final EmailValidator EMAIL_VALIDATOR = new EmailValidator();

  /**
   * Email address assigned to user who wants to reset password.
   */
  @ApiModelProperty(value = "${swagger.operations.password-reset.attributes.email}")
  String email;

  /**
   * UI address which should be used in the reset link.
   */
  @ApiModelProperty(value = "${swagger.operations.password-reset.attributes.reset-url}")
  String resetUrl;

  /**
   * Private method with validation rules.
   */
  private static final Map<Function<PasswordResetRequest, Boolean>, String> validators =
      MapPreservingOrderBuilder.<Function<PasswordResetRequest, Boolean>, String>builder()
          .put(emailIsNotBlank(), "Email cannot be empty.")
          .put(emailIsValid(), "Email is not valid.")
          .put(resetUrlIsNotBlank(), "ResetUrl cannot be empty.")
          .put(resetUrlIsValid(), "ResetUrl must be a valid URL.")
          .build();

  /**
   * Public method that validates given object and throws exceptions if validation doesn't pass.
   */
  public PasswordResetRequest validate() {
    validators.forEach((validator, message) -> {
      Boolean isValid = validator.apply(this);
      if (!isValid) {
        throw new InvalidRequestPayloadException(message);
      }
    });
    return this;
  }

  /**
   * Returns a lambda that verifies if 'email' is not blank.
   */
  private static Function<PasswordResetRequest, Boolean> emailIsNotBlank() {
    return request -> isNotBlank(request.getEmail());
  }

  /**
   * Returns a lambda that verifies if 'email' is valid Email Address.
   */
  private static Function<PasswordResetRequest, Boolean> emailIsValid() {
    return request -> EMAIL_VALIDATOR.isValid(request.getEmail(), null);
  }

  /**
   * Returns a lambda that verifies if 'resetUrl' is not blank.
   */
  private static Function<PasswordResetRequest, Boolean> resetUrlIsNotBlank() {
    return request -> isNotBlank(request.getResetUrl());
  }

  /**
   * Returns a lambda that verifies if 'resetUrl' is a valid URI.
   */
  private static Function<PasswordResetRequest, Boolean> resetUrlIsValid() {
    return request -> UrlValidator.urlValidator(request.getResetUrl());
  }

  /**
   * Poor man's URL validator helper.
   */
  private static class UrlValidator {

    /**
     * Regex to validate incoming String as URL.
     */
    private static final String URL_REGEX =
        "^((((https?|ftps?|gopher|telnet|nntp)://)|(mailto:|news:))"
            + "(%[0-9A-Fa-f]{2}|[-()_.!~*';/?:@&=+$,A-Za-z0-9])+)"
            + "([).!';/?:,][[:blank:]])?$";

    /**
     * Compiled Regex for optimal performance.
     */
    private static final Pattern URL_PATTERN = Pattern.compile(URL_REGEX);

    /**
     * Checks if incoming String is a valid URL.
     *
     * @param url Incoming String to validate.
     * @return true if String is valid URL, false otherwise.
     */
    public static boolean urlValidator(String url) {
      Matcher matcher = URL_PATTERN.matcher(url);
      return matcher.matches();
    }
  }
}
