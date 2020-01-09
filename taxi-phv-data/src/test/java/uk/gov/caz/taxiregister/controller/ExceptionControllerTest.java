package uk.gov.caz.taxiregister.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConversionException;
import uk.gov.caz.taxiregister.dto.ErrorsResponse;

class ExceptionControllerTest {

  @ParameterizedTest
  @MethodSource("inputAndExpectedMessages")
  public void testHandlingOfHttpMessageConversionException(String inputMessage,
      String expectedMessage) {
    // given
    ExceptionController exceptionController = new ExceptionController();
    HttpMessageConversionException httpMessageConversionException = new HttpMessageConversionException(
        inputMessage);

    // when
    ResponseEntity<ErrorsResponse> responseEntity = exceptionController
        .handleMessageConversionException(httpMessageConversionException);

    // then
    assertThat(responseEntity.getStatusCode()).isEqualByComparingTo(HttpStatus.BAD_REQUEST);
    assertThat(responseEntity.getBody().getErrors()).hasSize(1);
    assertThat(responseEntity.getBody().getErrors().get(0).getDetail()).isEqualTo(expectedMessage);
  }

  static Stream<Arguments> inputAndExpectedMessages() {
    return Stream.of(
        Arguments.arguments("Error", "Error"),
        Arguments.arguments("Bare Message: Stack Trace", "Bare Message"),
        Arguments.arguments("", "Cannot process request")
    );
  }
}