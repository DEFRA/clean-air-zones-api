package uk.gov.caz.accounts.controller;

import static uk.gov.caz.correlationid.Constants.X_CORRELATION_ID_HEADER;

import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import uk.gov.caz.accounts.dto.ConfirmEmailChangeRequest;
import uk.gov.caz.accounts.dto.ConfirmEmailChangeResponse;
import uk.gov.caz.accounts.dto.InitiateEmailChangeRequest;

@RequestMapping(path = UpdateEmailController.PATH, produces = MediaType.APPLICATION_JSON_VALUE,
    consumes = MediaType.APPLICATION_JSON_VALUE)
public interface UpdateEmailControllerApiSpec {

  /**
   * Endpoint that initiates the process of the change of the email for a user.
   */
  @ApiOperation(value =
      "${swagger.operations.users.init-change-email.description}")
  @ApiResponses({@ApiResponse(code = 503, message = "Service Unavailable Error"),
      @ApiResponse(code = 500, message = "Internal Server Error / No message available"),
      @ApiResponse(code = 200, message = "Process initiated successfully")})
  @ApiImplicitParams({
      @ApiImplicitParam(name = X_CORRELATION_ID_HEADER, required = true,
          value = "UUID formatted string to track the request through the enquiries stack",
          paramType = "header")
  })
  @PutMapping("/change-request")
  ResponseEntity<Void> initiateEmailChange(@RequestBody
      InitiateEmailChangeRequest request);

  /**
   * Endpoint that confirms the email change of the owner user.
   */
  @ApiOperation(value = "${swagger.operations.users.confirm-email-change.description}")
  @ApiResponses({
      @ApiResponse(code = 503, message = "Service Unavailable Error"),
      @ApiResponse(code = 500, message = "Internal Server Error / No message available"),
      @ApiResponse(code = 200, message = "Password change process successfully finalized")
  })
  @ApiImplicitParams({
      @ApiImplicitParam(name = X_CORRELATION_ID_HEADER, required = true,
          value = "UUID formatted string to track the request through the enquiries stack",
          paramType = "header")
  })
  @PutMapping("/change-confirm")
  ResponseEntity<ConfirmEmailChangeResponse> confirmEmailChange(
      @RequestBody ConfirmEmailChangeRequest request);
}
