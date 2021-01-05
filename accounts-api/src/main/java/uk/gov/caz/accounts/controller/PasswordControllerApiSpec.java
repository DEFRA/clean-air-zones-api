package uk.gov.caz.accounts.controller;

import static uk.gov.caz.correlationid.Constants.X_CORRELATION_ID_HEADER;

import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import uk.gov.caz.accounts.dto.PasswordResetRequest;
import uk.gov.caz.accounts.dto.SetPasswordRequest;
import uk.gov.caz.accounts.dto.UpdatePasswordRequest;
import uk.gov.caz.accounts.dto.ValidateTokenRequest;

@RequestMapping(value = PasswordController.BASE_PATH, produces = {
    MediaType.APPLICATION_JSON_VALUE})
public interface PasswordControllerApiSpec {

  /**
   * Endpoint specification that handles reset password operation.
   *
   * @param passwordResetRequest object containing email for account which want to
   *     reset password.
   */
  @ApiOperation(
      value = "${swagger.operations.password-reset.description}",
      response = String.class
  )
  @ApiResponses({
      @ApiResponse(code = 500, message = "Internal Server Error / No message available"),
      @ApiResponse(code = 400, message = "Invalid parameters or missing correlation ID"),
      @ApiResponse(code = 204, message = "Password reset request received")
  })
  @ApiImplicitParams({
      @ApiImplicitParam(name = X_CORRELATION_ID_HEADER,
          required = true,
          value = "UUID formatted string to track the request through the enquiries stack",
          paramType = "header"),
  })
  @PostMapping(PasswordController.RESET_PATH)
  ResponseEntity<Void> reset(@RequestBody PasswordResetRequest passwordResetRequest);

  /**
   * Endpoint specification that handles validation of password reset token.
   *
   * @param validateTokenRequest object containing email for account which want to
   *     reset password.
   */
  @ApiOperation(
      value = "${swagger.operations.password-validate-token.description}",
      response = String.class
  )
  @ApiResponses({
      @ApiResponse(code = 500, message = "Internal Server Error / No message available"),
      @ApiResponse(code = 400, message = "Token is invalid or expired or missing correlation ID"),
      @ApiResponse(code = 204, message = "Token is valid")
  })
  @ApiImplicitParams({
      @ApiImplicitParam(name = X_CORRELATION_ID_HEADER,
          required = true,
          value = "UUID formatted string to track the request through the enquiries stack",
          paramType = "header"),
  })
  @PostMapping(PasswordController.VALIDATE_TOKEN_PATH)
  ResponseEntity<Void> validateToken(@RequestBody ValidateTokenRequest validateTokenRequest);

  /**
   * Endpoint specification that sets a new password for user.
   */
  @ApiOperation(
      value = "${swagger.operations.set-new-password.description}"
  )
  @ApiResponses({
      @ApiResponse(code = 503, message = "Service Unavailable Error (AWS Cognito)"),
      @ApiResponse(code = 500, message = "Internal Server Error / No message available"),
      @ApiResponse(code = 422, message = "Password update failed."),
      @ApiResponse(code = 400, message = "Invalid parameters or missing correlation ID"),
      @ApiResponse(code = 204, message = "Password successfully updated")
  })
  @ApiImplicitParams({
      @ApiImplicitParam(name = X_CORRELATION_ID_HEADER,
          required = true,
          value = "UUID formatted string to track the request through the enquiries stack",
          paramType = "header"),
  })
  @PutMapping(PasswordController.SET_PATH)
  ResponseEntity<Void> setPassword(@RequestBody SetPasswordRequest setPasswordRequest);

  /**
   * Endpoint serving as a way to change password for an user.
   */
  @ApiOperation(
      value = "${swagger.operations.update-password.description}"
  )
  @ApiResponses({
      @ApiResponse(code = 503, message = "Service Unavailable Error (AWS Cognito)"),
      @ApiResponse(code = 500, message = "Internal Server Error / No message available"),
      @ApiResponse(code = 422, message = "Password update failed, it is invalid or was used "
          + "in the past"),
      @ApiResponse(code = 400, message = "Invalid parameters or missing correlation ID"),
      @ApiResponse(code = 204, message = "Password successfully updated")
  })
  @ApiImplicitParams({
      @ApiImplicitParam(name = X_CORRELATION_ID_HEADER,
          required = true,
          value = "UUID formatted string to track the request through the enquiries stack",
          paramType = "header"),
  })
  @PostMapping(PasswordController.UPDATE_PATH)
  ResponseEntity<Void> updatePassword(@RequestBody UpdatePasswordRequest updatePasswordRequest);
}
