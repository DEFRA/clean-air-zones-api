package uk.gov.caz.psr.dto;

import java.util.Collections;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Value;
import uk.gov.caz.psr.service.exception.PaymentDoesNotExistException;

/**
 * Value object that represents collection of errors response which are returned to the client upon
 * a call to notify about the error.
 */
@Value
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PaymentStatusErrorsResponse {

  List<PaymentStatusErrorResponse> errors;

  /**
   * Method to return single errors response for provided vrn and provided details.
   *
   * @param vrn Vehicle Registration Number
   * @param detail Error details.
   */
  public static PaymentStatusErrorsResponse singleValidationErrorResponse(String vrn,
      String detail) {
    PaymentStatusErrorResponse errorResponse = PaymentStatusErrorResponse
        .validationErrorResponseWithDetailAndVrn(vrn, detail);
    return new PaymentStatusErrorsResponse(Collections.singletonList(errorResponse));
  }

  /**
   * Method to build a single errors response from exception thrown when vehicle entrant 
   * cannot be found.
   *
   * @param e {@link PaymentDoesNotExistException}
   */  
  public static PaymentStatusErrorsResponse entrantNotFoundErrorResponse(
      PaymentDoesNotExistException e) {
    PaymentStatusErrorResponse errorResponse = PaymentStatusErrorResponse
        .errorResponseFromNonExistentEntrantException(e);    
    return new PaymentStatusErrorsResponse(Collections.singletonList(errorResponse));
  }

  /**
   * Method to return Error Response based on provided validationErrors.
   *
   * @param validationErrors Validation Errors.
   */
  public static PaymentStatusErrorsResponse from(
      List<PaymentStatusErrorResponse> validationErrors) {
    return new PaymentStatusErrorsResponse(validationErrors);
  }
}
