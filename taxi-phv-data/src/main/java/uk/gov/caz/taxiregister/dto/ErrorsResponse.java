package uk.gov.caz.taxiregister.dto;

import java.util.Collections;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Value;
import org.springframework.http.HttpStatus;

@Value
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ErrorsResponse {

  private static final ErrorsResponse UNHANDLED_EXCEPTION_RESPONSE = new ErrorsResponse(
      Collections.singletonList(
          ErrorResponse.builder()
              .vrm("")
              .title("Unknown error")
              .detail("Internal server error")
              .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
              .build())
  );

  List<ErrorResponse> errors;

  public static ErrorsResponse singleValidationErrorResponse(String detail) {
    ErrorResponse errorResponse = ErrorResponse.validationErrorResponseWithDetail(detail);
    return new ErrorsResponse(Collections.singletonList(errorResponse));
  }

  /**
   * Creates a unknown error response, i.e. its title is fixed and equal to 'Unknown error',
   * vrm is an empty string, status is set to the parameter and detail is set to the parameter.
   */
  public static ErrorsResponse unknownErrorWithDetailAndStatus(String detail,
      HttpStatus status) {
    ErrorResponse errorResponse = ErrorResponse
        .unknownErrorResponseWithDetailAndStatus(detail, status);
    return new ErrorsResponse(Collections.singletonList(errorResponse));
  }

  public static ErrorsResponse internalError() {
    return UNHANDLED_EXCEPTION_RESPONSE;
  }

  public static ErrorsResponse from(List<ErrorResponse> validationErrors) {
    return new ErrorsResponse(validationErrors);
  }
}
