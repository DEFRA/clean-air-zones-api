package uk.gov.caz.vcc.dto.validation;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import uk.gov.caz.vcc.dto.ErrorResponse;

/**
 * DTO that holds single validation error from {@link uk.gov.caz.vcc.dto.VehicleEntrantDto}.
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class ValidationError {

  private static final String MANDATORY_FIELD_MISSING_ERROR_TITLE = "Mandatory field missing";
  private static final String MANDATORY_FIELD_EMPTY_ERROR_TITLE = "Mandatory field empty";
  private static final String INVALID_FIELD_VALUE = "Invalid field value";

  private final String reason;
  private final String detail;
  private final String vrn;

  /**
   * Method that returns formatter error message.
   */
  public ErrorResponse asErrorResponse() {
    return ErrorResponse.builder()
        .vrn(vrn)
        .title(reason)
        .detail(detail)
        .status(HttpStatus.BAD_REQUEST.value())
        .build();
  }

  /**
   * Factory method that produces Validation error for given VRN.
   */
  static ValidationError missingFieldError(String vrn, String fieldName) {
    return new ValidationError(MANDATORY_FIELD_MISSING_ERROR_TITLE, fieldName, vrn);
  }

  /**
   * Factory method that produces Validation error for given VRN.
   */
  static ValidationError emptyFieldError(String vrn, String fieldName) {
    return new ValidationError(MANDATORY_FIELD_EMPTY_ERROR_TITLE, fieldName, vrn);
  }

  /**
   * Factory method that produces Validation error for given VRN.
   */
  static ValidationError invalidVrnFormat(String vrn) {
    return new ValidationError(INVALID_FIELD_VALUE, "invalid VRN format", vrn);
  }

  /**
   * Factory method that produces Validation error for given VRN.
   */
  static ValidationError invalidTimestampFormat(String vrn, String fieldValue) {
    return new ValidationError(INVALID_FIELD_VALUE, "invalid timestamp format:" + fieldValue, vrn);
  }
}
