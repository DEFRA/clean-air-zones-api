package uk.gov.caz.accounts.controller;

import static uk.gov.caz.accounts.controller.AccountsController.ACCOUNTS_PATH;
import static uk.gov.caz.correlationid.Constants.X_CORRELATION_ID_HEADER;

import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import uk.gov.caz.accounts.dto.AccountCreationRequestDto;
import uk.gov.caz.accounts.dto.AccountCreationResponseDto;
import uk.gov.caz.accounts.dto.AccountResponseDto;
import uk.gov.caz.accounts.dto.AccountUpdateRequestDto;
import uk.gov.caz.accounts.dto.AccountVerificationRequestDto;
import uk.gov.caz.accounts.dto.CloseAccountRequestDto;
import uk.gov.caz.accounts.dto.CreateAndInviteUserRequestDto;
import uk.gov.caz.accounts.dto.UserCreationResponseDto;
import uk.gov.caz.accounts.dto.UserForAccountCreationRequestDto;
import uk.gov.caz.accounts.dto.UserValidationRequest;
import uk.gov.caz.accounts.dto.UserVerificationEmailResendRequest;
import uk.gov.caz.accounts.dto.UserVerificationEmailResendResponse;
import uk.gov.caz.accounts.model.User;

@RequestMapping(value = ACCOUNTS_PATH, produces = {
    MediaType.APPLICATION_JSON_VALUE})
public interface AccountsControllerApiSpec {

  /**
   * Endpoint specification that handles accounts creation.
   *
   * @param accountCreationRequestDto object representing single user data.
   * @return {@link AccountCreationResponseDto} representing created user object.
   */
  @ApiOperation(
      value = "${swagger.operations.accounts.create.description}",
      response = AccountCreationResponseDto.class
  )
  @ApiResponses({
      @ApiResponse(code = 503, message = "Service Unavailable Error (AWS Cognito)"),
      @ApiResponse(code = 500, message = "Internal Server Error / No message available"),
      @ApiResponse(code = 422, message = "Admin Creation Failed"),
      @ApiResponse(code = 400, message = "Missing Correlation Id header"),
      @ApiResponse(code = 201, message = "Admin Created")
  })
  @ApiImplicitParams({
      @ApiImplicitParam(name = X_CORRELATION_ID_HEADER,
          required = true,
          value = "UUID formatted string to track the request through the enquiries stack",
          paramType = "header")
  })
  @PostMapping
  ResponseEntity<AccountCreationResponseDto> createAccount(
      @RequestBody AccountCreationRequestDto accountCreationRequestDto);

  /**
   * Endpoint specification that handles updates to the account.
   *
   * @param accountId               Id of the account to update.
   * @param accountUpdateRequestDto object representing account update details.
   */
  @ApiOperation(
      value = "${swagger.operations.accounts.update.description}"
  )
  @ApiResponses({
      @ApiResponse(code = 503, message = "Service Unavailable Error (AWS Cognito)"),
      @ApiResponse(code = 500, message = "Internal Server Error / No message available"),
      @ApiResponse(code = 404, message = "Account Not Found"),
      @ApiResponse(code = 400, message = "Missing Correlation Id header or invalid payload"),
      @ApiResponse(code = 204, message = "Account Updated")
  })
  @ApiImplicitParams({
      @ApiImplicitParam(name = X_CORRELATION_ID_HEADER,
          required = true,
          value = "UUID formatted string to track the request through the enquiries stack",
          paramType = "header")
  })
  @PatchMapping("/{accountId}")
  ResponseEntity<Void> updateAccount(
      @PathVariable("accountId") UUID accountId,
      @RequestBody AccountUpdateRequestDto accountUpdateRequestDto);

  /**
   * Endpoint specification that handles verify accounts.
   *
   * @param request request with verification data.
   * @return {@link String} representing created user object.
   */
  @ApiOperation(
      value = "${swagger.operations.accounts.verify.description}"
  )
  @ApiResponses({
      @ApiResponse(code = 503, message = "Service Unavailable Error (AWS Cognito)"),
      @ApiResponse(code = 500, message = "Internal Server Error / No message available"),
      @ApiResponse(code = 404, message = "Account Not Found"),
      @ApiResponse(code = 400, message = "Email already confirmed"),
      @ApiResponse(code = 200, message = "Account email verified successfully")
  })
  @ApiImplicitParams({
      @ApiImplicitParam(name = X_CORRELATION_ID_HEADER,
          required = true,
          value = "UUID formatted string to track the request through the enquiries stack",
          paramType = "header"),
      @ApiImplicitParam(name = "accountId",
          required = true,
          value = "UUID formatted string from AccountUser table (must match PK in Account table)",
          paramType = "path"),
      @ApiImplicitParam(name = "accountUserId",
          required = true,
          value = "UUID formatted string from AccountUser table",
          paramType = "path")
  })
  @PostMapping("/verify")
  ResponseEntity<Map<String, String>> verifyUserEmail(
      @RequestBody AccountVerificationRequestDto request);

  /**
   * Endpoint specification that handles the {@link User} creation for the existing account.
   *
   * @param userForAccountCreationRequestDto object representing data for the new user
   * @return {@link UserCreationResponseDto} representing created user object.
   */
  @ApiOperation(
      value = "${swagger.operations.accounts.create-user-for-account.description}"
  )
  @ApiResponses({
      @ApiResponse(code = 503, message = "Service Unavailable Error (AWS Cognito)"),
      @ApiResponse(code = 500, message = "Internal Server Error / No message available"),
      @ApiResponse(code = 422, message = "Admin Creation Failed"),
      @ApiResponse(code = 404, message = "Account was not found"),
      @ApiResponse(code = 400, message = "Missing Correlation Id header"),
      @ApiResponse(code = 201, message = "Admin Created")
  })
  @ApiImplicitParams({
      @ApiImplicitParam(name = X_CORRELATION_ID_HEADER,
          required = true,
          value = "UUID formatted string to track the request through the enquiries stack",
          paramType = "header"),
      @ApiImplicitParam(name = "accountId",
          required = true,
          value = "UUID formatted string from Account table",
          paramType = "path")
  })
  @PostMapping("/{accountId}/users")
  ResponseEntity<UserCreationResponseDto> createAdminUserForAccount(
      @PathVariable("accountId") UUID accountId,
      @RequestBody UserForAccountCreationRequestDto userForAccountCreationRequestDto
  );

  @ApiOperation(
      value = "${swagger.operations.accounts.resend-verification-email.description}"
  )
  @ApiResponses({
      @ApiResponse(code = 500, message = "Internal Server Error / No message available"),
      @ApiResponse(code = 422, message = "Email Sending Failed"),
      @ApiResponse(code = 404, message = "Account was not found"),
      @ApiResponse(code = 400, message = "Missing Correlation Id header"),
      @ApiResponse(code = 200, message = "Email was sent")
  })
  @ApiImplicitParams({
      @ApiImplicitParam(name = X_CORRELATION_ID_HEADER,
          required = true,
          value = "UUID formatted string to track the request through the enquiries stack",
          paramType = "header"),
      @ApiImplicitParam(name = "accountId",
          required = true,
          value = "UUID formatted string from Account table",
          paramType = "path"),
      @ApiImplicitParam(name = "accountUserId",
          required = true,
          value = "UUID formatted string from AccountUser table (must match PK from Account table)"
      )
  })
  @PostMapping(AccountsController.RESEND_VERIFICATION_EMAIL_PATH)
  ResponseEntity<UserVerificationEmailResendResponse> resendVerificationEmail(
      @PathVariable String accountId,
      @PathVariable String accountUserId,
      @RequestBody UserVerificationEmailResendRequest request
  );

  @ApiOperation(
      value = "${swagger.operations.accounts.create-and-invite-user-for-account.description}"
  )
  @ApiResponses({
      @ApiResponse(code = 500, message = "Internal Server Error / No message available"),
      @ApiResponse(code = 422, message = "Invitation failed"),
      @ApiResponse(code = 404, message = "Account or inviting user was not found"),
      @ApiResponse(code = 400, message = "Missing Correlation Id header"),
      @ApiResponse(code = 201, message = "User has been invited")
  })
  @ApiImplicitParams({
      @ApiImplicitParam(name = X_CORRELATION_ID_HEADER,
          required = true,
          value = "UUID formatted string to track the request through the enquiries stack",
          paramType = "header"),
      @ApiImplicitParam(name = "accountId",
          required = true,
          value = "Internal identifier of the account",
          paramType = "path")
  })
  @PostMapping("/{accountId}/user-invitations")
  ResponseEntity<Void> createAndInviteStandardUserForAccount(
      @PathVariable("accountId") UUID accountId,
      @RequestBody CreateAndInviteUserRequestDto userForAccountCreationRequestDto
  );

  /**
   * Endpoint that validates email uniqueness.
   *
   * @param userValidationRequest request with data to be validated.
   */
  @ApiResponses({
      @ApiResponse(code = 500, message = "Internal Server Error / No message available"),
      @ApiResponse(code = 204, message = "Email is unique"),
      @ApiResponse(code = 400, message = "Email is not unique"),
  })
  @ApiImplicitParams({
      @ApiImplicitParam(name = X_CORRELATION_ID_HEADER,
          required = true,
          value = "UUID formatted string to track the request through the enquiries stack",
          paramType = "header"),
      @ApiImplicitParam(name = "accountId",
          required = true,
          value = "UUID formatted string from AccountUser table (must match PK in Account table)",
          paramType = "path")
  })
  @PostMapping("/{accountId}/user-validations")
  ResponseEntity<Void> validateUser(
      @PathVariable("accountId") UUID accountId,
      @RequestBody UserValidationRequest userValidationRequest);

  /**
   * Endpoint that returns account name.
   */
  @ApiResponses({
      @ApiResponse(code = 500, message = "Internal Server Error / No message available"),
      @ApiResponse(code = 404, message = "Account for given accountId doesn't exist"),
  })
  @ApiImplicitParams({
      @ApiImplicitParam(name = X_CORRELATION_ID_HEADER,
          required = true,
          value = "UUID formatted string to track the request through the enquiries stack",
          paramType = "header"),
      @ApiImplicitParam(name = "accountId",
          required = true,
          value = "UUID of given Account",
          paramType = "path")
  })
  @GetMapping("/{accountId}")
  ResponseEntity<AccountResponseDto> getAccount(@PathVariable("accountId") UUID accountId);

  /**
   * Endpoint specification that handles closing account.
   *
   * @param accountId Id of the account to close.
   */
  @ApiOperation(value = "${swagger.operations.accounts.inactivate.description}")
  @ApiResponses({
      @ApiResponse(code = 503, message = "Service Unavailable Error (AWS Cognito)"),
      @ApiResponse(code = 500, message = "Internal Server Error / No message available"),
      @ApiResponse(code = 404, message = "Account Not Found"),
      @ApiResponse(code = 400, message = "Missing Correlation Id header or invalid payload"),
      @ApiResponse(code = 204, message = "Account Closed")
  })
  @ApiImplicitParams({
      @ApiImplicitParam(name = X_CORRELATION_ID_HEADER,
          required = true,
          value = "UUID formatted string to track the request through the enquiries stack",
          paramType = "header"),
      @ApiImplicitParam(name = "accountId",
          required = true,
          value = "UUID formatted string from AccountUser table (must match PK in Account table)",
          paramType = "path")
  })
  @PostMapping("/{accountId}/cancellation")
  ResponseEntity<Void> closeAccount(
      @PathVariable("accountId") UUID accountId,
      @RequestBody CloseAccountRequestDto request);
}