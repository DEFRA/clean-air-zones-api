package uk.gov.caz.vcc.dto.validation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import uk.gov.caz.vcc.dto.ErrorResponse;

class ValidationErrorTest {

  private static String SOME_VRN = "XXXX1123";

  @Test
  void testMissingFieldErrorMessage() {
    ValidationError validationError = ValidationError.missingFieldError(SOME_VRN, "timestampField");

    assertThat(validationError.asErrorResponse())
        .isEqualTo(ErrorResponse.builder()
            .vrn(SOME_VRN)
            .title("Mandatory field missing")
            .detail("timestampField")
            .status(HttpStatus.BAD_REQUEST.value())
            .build()
        );
  }

  @Test
  void testEmptyFieldErrorMessage() {
    ValidationError validationError = ValidationError.emptyFieldError(SOME_VRN, "timestampField");

    assertThat(validationError.asErrorResponse())
        .isEqualTo(ErrorResponse.builder()
            .vrn(SOME_VRN)
            .title("Mandatory field empty")
            .detail("timestampField")
            .status(HttpStatus.BAD_REQUEST.value())
            .build()
        );
  }

  @Test
  void testInvalidVrnFormatErrorMessage() {
    ValidationError validationError = ValidationError.invalidVrnFormat(SOME_VRN);

    assertThat(validationError.asErrorResponse())
        .isEqualTo(ErrorResponse.builder()
            .vrn(SOME_VRN)
            .title("Invalid field value")
            .detail("VRN should have from 2 to 15 characters")
            .status(HttpStatus.BAD_REQUEST.value())
            .build()
        );
  }

  @Test
  void testInvalidTimestampFormatErrorMessage() {
    ValidationError validationError = ValidationError.invalidTimestampFormat(SOME_VRN, "2011//222//22");

    assertThat(validationError.asErrorResponse())
        .isEqualTo(ErrorResponse.builder()
            .vrn(SOME_VRN)
            .title("Invalid field value")
            .detail("invalid timestamp format:2011//222//22")
            .status(HttpStatus.BAD_REQUEST.value())
            .build()
        );
  }
}