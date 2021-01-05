package uk.gov.caz.accounts.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.caz.accounts.controller.exception.InvalidRequestPayloadException;

class PasswordResetRequestTest {

  private static final String VALID_EMAIL = "someone@mail.com";
  private static final String VALID_URL = "https://gov.uk/password";
  private static final String INVALID_URL = "https/gov.uk/password";

  @Test
  public void shouldNotThrowAnyExceptionsForValidInput() {
    // given
    PasswordResetRequest dto = PasswordResetRequest.builder()
        .email("good@email.com")
        .resetUrl("https://gov.uk/password")
        .build();

    // then
    assertThat(dto.validate()).isEqualTo(dto);
  }

  @ParameterizedTest
  @MethodSource("uk.gov.caz.accounts.dto.PasswordResetRequestTest#loginRequestTestDataAndExceptionMessage")
  public void shouldThrowExceptionForEachBadInput(PasswordResetRequest dto, String message) {
    // given
    InvalidRequestPayloadException result = assertThrows(InvalidRequestPayloadException.class,
        dto::validate);

    // then
    assertThat(result.getMessage()).isEqualTo(message);
  }

  static Stream<Arguments> loginRequestTestDataAndExceptionMessage() {
    return Stream.of(
        Arguments.arguments(buildDto(null, VALID_URL),
            "Email cannot be empty."),
        Arguments.arguments(buildDto("", VALID_URL),
            "Email cannot be empty."),
        Arguments.arguments(buildDto(VALID_EMAIL, null),
            "ResetUrl cannot be empty."),
        Arguments.arguments(buildDto(VALID_EMAIL, ""),
            "ResetUrl cannot be empty."),
        Arguments.arguments(buildDto(VALID_EMAIL, INVALID_URL),
            "ResetUrl must be a valid URL.")
    );
  }

  private static PasswordResetRequest buildDto(String email, String resetUrl) {
    return PasswordResetRequest.builder()
        .email(email)
        .resetUrl(resetUrl)
        .build();
  }
}