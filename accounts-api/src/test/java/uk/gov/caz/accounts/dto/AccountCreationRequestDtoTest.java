package uk.gov.caz.accounts.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.caz.accounts.controller.exception.InvalidRequestPayloadException;

class AccountCreationRequestDtoTest {

  @ParameterizedTest
  @MethodSource("accountsTestData")
  public void shouldThrowExceptionForEachBadInput(AccountCreationRequestDto dto) {
    assertThrows(InvalidRequestPayloadException.class, dto::validate);
  }

  @Test
  public void shouldNotThrowAnyExceptionsForValidInput() {
    //given
    AccountCreationRequestDto dto = AccountCreationRequestDto.builder()
        .accountName("valid, account name!")
        .build();

    //then
    assertThat(dto.validate()).isEqualTo(dto);
  }

  static Stream<Arguments> accountsTestData() {
    return Stream.of(
        Arguments.arguments(AccountCreationRequestDto.builder()
            .accountName(null)
            .build()),
        Arguments.arguments(AccountCreationRequestDto.builder()
            .accountName("")
            .build()),
        Arguments.arguments(AccountCreationRequestDto.builder()
            .accountName(IntStream.rangeClosed(0, 181).mapToObj(Integer::toString).collect(
                Collectors.joining("")))
            .build()),
        Arguments.arguments(AccountCreationRequestDto.builder()
            .accountName("  ")
            .build())
    );
  }
}