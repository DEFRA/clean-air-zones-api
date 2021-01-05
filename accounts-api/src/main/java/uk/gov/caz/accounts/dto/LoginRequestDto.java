package uk.gov.caz.accounts.dto;

import static org.apache.logging.log4j.util.Strings.isNotBlank;

import io.swagger.annotations.ApiModelProperty;
import java.util.Map;
import java.util.function.Function;
import lombok.Builder;
import lombok.Value;
import uk.gov.caz.accounts.controller.exception.InvalidRequestPayloadException;
import uk.gov.caz.accounts.util.MapPreservingOrderBuilder;

/**
 * Class that represents incoming JSON payload for login.
 */
@Value
@Builder
public class LoginRequestDto {

  /**
   * Unique login credential.
   */
  @ApiModelProperty(value = "${swagger.model.descriptions.login.email}")
  String email;

  /**
   * User password used to login to the application.
   */
  @ApiModelProperty(value = "${swagger.model.descriptions.account.password}")
  String password;

  private static final Map<Function<LoginRequestDto, Boolean>, String> validators =
      MapPreservingOrderBuilder.<Function<LoginRequestDto, Boolean>, String>builder()
          .put(loginRequestDto -> isNotBlank(loginRequestDto.email), "Email cannot be blank.")
          .put(loginRequestDto -> isNotBlank(loginRequestDto.password), "Password cannot be blank.")
          .put(loginRequestDto -> loginRequestDto.email.length() < 256, "Email is too long")
          .put(loginRequestDto -> loginRequestDto.password.length() < 256, "Password is too long")
          .build();

  /**
   * Public method that validates given object and throws exceptions if validation doesn't pass.
   */
  public LoginRequestDto validate() {
    validators.forEach((validator, message) -> {
      boolean isValid = validator.apply(this);

      if (!isValid) {
        throw new InvalidRequestPayloadException(message);
      }
    });

    return this;
  }
}