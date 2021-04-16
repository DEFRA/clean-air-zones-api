package uk.gov.caz.whitelist.dto;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Value;
import org.springframework.http.HttpStatus;
import uk.gov.caz.whitelist.model.ValidationError;

@Value
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ErrorsResponse {

  private static final ErrorsResponse UNHANDLED_EXCEPTION_RESPONSE = new ErrorsResponse(
      Collections.singletonList(
          ErrorResponse.builder()
              .vrn("")
              .title("Unknown error")
              .detail("Internal server error")
              .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
              .build())
  );

  List<ErrorResponse> errors;

  public static ErrorsResponse singleValidationErrorResponse(String detail) {
    ErrorResponse errorResponse = ErrorResponse.validationErrorResponse(detail);
    return new ErrorsResponse(Collections.singletonList(errorResponse));
  }

  public static ErrorsResponse internalError() {
    return UNHANDLED_EXCEPTION_RESPONSE;
  }

  /**
   * Cast validation errors into an ErrorsResponse object.
   *
   * @param validationErrors List of validation erros to be cast.
   * @return ErrorsResponse containing the validation errors to be returned.
   */
  public static ErrorsResponse from(List<ValidationError> validationErrors) {
    List<ErrorResponse> errorResponses = validationErrors.stream()
        .map(ValidationError::asErrorResponse)
        .collect(Collectors.toList());
    return new ErrorsResponse(errorResponses);
  }
}
