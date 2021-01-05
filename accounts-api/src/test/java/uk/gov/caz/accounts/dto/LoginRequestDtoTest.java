package uk.gov.caz.accounts.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.caz.accounts.controller.exception.InvalidRequestPayloadException;

class LoginRequestDtoTest {

  @ParameterizedTest
  @MethodSource("uk.gov.caz.accounts.util.TestObjectFactory#loginRequestTestDataAndExceptionMessage")
  public void shouldThrowExceptionForEachBadInput(LoginRequestDto dto, String message) {
    // given
    InvalidRequestPayloadException result = assertThrows(InvalidRequestPayloadException.class,
        dto::validate);

    // then
    assertThat(result.getMessage()).isEqualTo(message);
  }

  @Test
  public void shouldNotThrowAnyExceptionsForValidInput() {
    // given
    LoginRequestDto dto = LoginRequestDto.builder()
        .password("valid password")
        .email("test@gov.uk")
        .build();

    // then
    assertThat(dto.validate()).isEqualTo(dto);
  }
}