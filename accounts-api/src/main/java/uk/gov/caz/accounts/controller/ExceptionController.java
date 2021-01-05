package uk.gov.caz.accounts.controller;

import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import uk.gov.caz.GlobalExceptionHandler;
import uk.gov.caz.accounts.controller.exception.ValidationError;
import uk.gov.caz.accounts.controller.exception.VehicleRetrievalDtoValidationException;
import uk.gov.caz.accounts.dto.AccountError;
import uk.gov.caz.accounts.dto.AccountErrorCode;
import uk.gov.caz.accounts.dto.DirectDebitMandatesUpdateErrorResponse;
import uk.gov.caz.accounts.dto.LoginError;
import uk.gov.caz.accounts.dto.LoginErrorCode;
import uk.gov.caz.accounts.dto.ValidationErrorsResponse;
import uk.gov.caz.accounts.repository.exception.InvalidCredentialsException;
import uk.gov.caz.accounts.repository.exception.PendingEmailChangeException;
import uk.gov.caz.accounts.service.exception.AbusiveNameException;
import uk.gov.caz.accounts.service.exception.AccountAlreadyExistsException;
import uk.gov.caz.accounts.service.exception.DirectDebitMandateUpdateException;
import uk.gov.caz.accounts.service.exception.EmailAlreadyVerifiedException;
import uk.gov.caz.accounts.service.exception.ExpiredUserEmailVerificationCodeException;
import uk.gov.caz.accounts.service.exception.InvalidUserEmailVerificationCodeException;
import uk.gov.caz.accounts.service.exception.NotUniqueEmailException;
import uk.gov.caz.accounts.service.exception.OldPasswordWrongException;
import uk.gov.caz.accounts.service.exception.PasswordInvalidException;
import uk.gov.caz.accounts.service.exception.PasswordRecentlyUsedException;

@Slf4j
@RestControllerAdvice
public class ExceptionController extends GlobalExceptionHandler {

  @ExceptionHandler(VehicleRetrievalDtoValidationException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  ValidationErrorsResponse handleVehicleRetrievalDtoValidationException(
      VehicleRetrievalDtoValidationException exception) {
    List<ValidationError> errorsList = exception.getErrorParams()
        .stream()
        .map(ValidationError::from)
        .collect(Collectors.toList());

    log.info("VehicleRetrievalDtoValidationException occurred: {}", errorsList);
    return ValidationErrorsResponse.from(errorsList);
  }

  @ExceptionHandler(AbusiveNameException.class)
  ResponseEntity<AccountError> handleAbusiveAccountNameException(AbusiveNameException exception) {
    log.info("AbusiveNameException occurred", exception);
    return buildAccountErrorResponse("Improper language detected",
        AccountErrorCode.valueOf(exception.getType().name()));
  }

  @ExceptionHandler(NotUniqueEmailException.class)
  ResponseEntity<AccountError> handleNotUniqueEmailException(NotUniqueEmailException exception) {
    log.info("NotUniqueEmailException occurred", exception);
    return buildAccountErrorResponse(exception.getMessage(), AccountErrorCode.EMAIL_NOT_UNIQUE);
  }

  @ExceptionHandler(PasswordInvalidException.class)
  ResponseEntity<AccountError> handleInvalidPasswordException(PasswordInvalidException exception) {
    log.info("InvalidPasswordException occurred", exception);
    return buildAccountErrorResponse(exception.getMessage(), AccountErrorCode.PASSWORD_NOT_VALID);
  }

  @ExceptionHandler(AccountAlreadyExistsException.class)
  ResponseEntity<AccountError> handleAccountAlreadyExistsException(
      AccountAlreadyExistsException exception) {
    log.info("AccountAlreadyExistsException occurred", exception);
    return buildAccountErrorResponse(exception.getMessage(),
        AccountErrorCode.ACCOUNT_NAME_NOT_UNIQUE);
  }

  @ExceptionHandler(InvalidCredentialsException.class)
  ResponseEntity<LoginError> handleInvalidCredentialsException(
      InvalidCredentialsException exception) {

    return buildLoginErrorResponse(exception.getMessage(), LoginErrorCode.INVALID_CREDENTIALS);
  }

  @ExceptionHandler(PendingEmailChangeException.class)
  ResponseEntity<LoginError> handlePendingEmailChangeException(
      PendingEmailChangeException exception) {

    return buildLoginErrorResponse(exception.getMessage(), LoginErrorCode.PENDING_EMAIL_CHANGE);
  }

  @ExceptionHandler(EmailAlreadyVerifiedException.class)
  ResponseEntity<AccountError> handleEmailAlreadyVerifiedException(
      EmailAlreadyVerifiedException exception) {
    return buildAccountErrorResponse(exception.getMessage(),
        AccountErrorCode.EMAIL_ALREADY_VERIFIED);
  }

  @ExceptionHandler(InvalidUserEmailVerificationCodeException.class)
  ResponseEntity<AccountError> handleInvalidUserVerificationTokenException(
      InvalidUserEmailVerificationCodeException exception) {
    return buildAccountErrorResponse(exception.getMessage(),
        AccountErrorCode.INVALID_USER_VERIFICATION_TOKEN);
  }

  @ExceptionHandler(ExpiredUserEmailVerificationCodeException.class)
  ResponseEntity<AccountError> handleExpiredUserEmailVerificationCodeException(
      ExpiredUserEmailVerificationCodeException exception) {
    return buildAccountErrorResponse(exception.getMessage(),
        AccountErrorCode.EXPIRED_USER_VERIFICATION_TOKEN);
  }

  @ExceptionHandler(DirectDebitMandateUpdateException.class)
  ResponseEntity<DirectDebitMandatesUpdateErrorResponse> handleDirectDebitMandatesUpdateException(
      DirectDebitMandateUpdateException exception) {
    log.info("UpdateDirectDebitMandatesException occurred: {}", exception.getMessage());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(DirectDebitMandatesUpdateErrorResponse.from(exception.getErrors()));
  }

  @ExceptionHandler(PasswordRecentlyUsedException.class)
  ResponseEntity<AccountError> handlePasswordRecentlyUsedException(
      PasswordRecentlyUsedException exception) {
    return buildAccountErrorResponse("You have already used that password, "
        + "choose a new one", AccountErrorCode.PASSWORD_RECENTLY_USED);
  }

  @ExceptionHandler(OldPasswordWrongException.class)
  ResponseEntity<AccountError> handleOldPasswordWrongException(
      OldPasswordWrongException exception) {
    return buildAccountErrorResponse("The password you entered is incorrect",
        AccountErrorCode.OLD_PASSWORD_INVALID);
  }

  private ResponseEntity<AccountError> buildAccountErrorResponse(String message,
      AccountErrorCode errorCode) {
    return ResponseEntity.unprocessableEntity().body(
        AccountError.builder()
            .message(message)
            .errorCode(errorCode)
            .build()
    );
  }

  private ResponseEntity<LoginError> buildLoginErrorResponse(String message,
      LoginErrorCode errorCode) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
        LoginError.builder()
            .message(message)
            .errorCode(errorCode)
            .build()
    );
  }
}
