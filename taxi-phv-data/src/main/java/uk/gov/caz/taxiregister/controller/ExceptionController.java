package uk.gov.caz.taxiregister.controller;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

import com.google.common.base.Splitter;
import java.util.List;
import java.util.Optional;
import javax.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import uk.gov.caz.GlobalExceptionHandler;
import uk.gov.caz.taxiregister.controller.exception.InvalidUploaderIdFormatException;
import uk.gov.caz.taxiregister.controller.exception.PayloadValidationException;
import uk.gov.caz.taxiregister.controller.exception.UnableToGetUploaderIdMetadataException;
import uk.gov.caz.taxiregister.dto.ErrorsResponse;
import uk.gov.caz.taxiregister.service.exception.JobNameDuplicateException;

@Slf4j
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ExceptionController extends GlobalExceptionHandler {

  private static final Splitter SPLITTER = Splitter.on(':')
      .trimResults()
      .omitEmptyStrings();

  private static final String VALIDATION_ERROR_TEMPLATE = "Validation error: {}";

  @ExceptionHandler(UnableToGetUploaderIdMetadataException.class)
  ResponseEntity<String> handleUnableToGetUploaderIdMetadataException(Exception e) {
    log.error(e.getMessage());
    return ResponseEntity
        .status(INTERNAL_SERVER_ERROR)
        .body(e.getMessage());
  }

  @ExceptionHandler(MissingRequestHeaderException.class)
  ResponseEntity<String> handleMissingHeaderException(MissingRequestHeaderException e) {
    log.error("Missing request header: ", e);
    return ResponseEntity.status(BAD_REQUEST).body(stripStackTrace(e.getMessage()));
  }

  @ExceptionHandler(JobNameDuplicateException.class)
  ResponseEntity<ErrorsResponse> handleJobNameDuplicateException(
      JobNameDuplicateException e) {
    log.warn("There is already job with given name: ", e);
    return ResponseEntity
        .status(INTERNAL_SERVER_ERROR)
        .body(ErrorsResponse
            .unknownErrorWithDetailAndStatus("There is already job with given name",
                INTERNAL_SERVER_ERROR));
  }

  @ExceptionHandler({PayloadValidationException.class, InvalidUploaderIdFormatException.class,
      ConstraintViolationException.class})
  ResponseEntity<ErrorsResponse> handleValidationException(Exception e) {
    log.info(VALIDATION_ERROR_TEMPLATE, e.getMessage());
    return ResponseEntity.badRequest()
        .body(ErrorsResponse.singleValidationErrorResponse(e.getMessage()));
  }

  @ExceptionHandler({HttpMessageConversionException.class,})
  ResponseEntity<ErrorsResponse> handleMessageConversionException(
      Exception e) {
    log.info(VALIDATION_ERROR_TEMPLATE, e.getMessage());
    return ResponseEntity.badRequest()
        .body(ErrorsResponse.singleValidationErrorResponse(stripStackTrace(e.getMessage())));
  }

  @Override
  protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException e,
      HttpHeaders headers, HttpStatus status, WebRequest request) {
    log.info(VALIDATION_ERROR_TEMPLATE, e.getMessage());
    return ResponseEntity.badRequest()
        .body(ErrorsResponse.singleValidationErrorResponse(stripStackTrace(e.getMessage())));
  }

  /**
   * Given error message tries to strip any additional details added after ":" character, like stack
   * trace.
   *
   * @param errorMessage Input error message, potentially with stack trace.
   * @return Bare exception message without noise like stack trace. If error message is empty
   *     returns fixed "Cannot process request" message.
   */
  private String stripStackTrace(String errorMessage) {
    return extractBareMessage(errorMessage).orElse("Cannot process request");
  }

  /**
   * Given error message tries to split it by ":" character. If input is empty returns
   * Optional.empty. Otherwise returns first character sequence before ":".
   *
   * @param errorMessage Input error message.
   * @return Optional of String. If input is empty returns Optional.empty. Otherwise returns first
   *     character sequence before ":".
   */
  private Optional<String> extractBareMessage(String errorMessage) {
    List<String> errorMessageSections = SPLITTER.splitToList(errorMessage);
    if (errorMessageSections.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(errorMessageSections.get(0));
  }
}