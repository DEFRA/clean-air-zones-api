package uk.gov.caz.accounts.controller;

import static uk.gov.caz.accounts.controller.AccountUsersController.USERS_PATH;
import static uk.gov.caz.correlationid.Constants.X_CORRELATION_ID_HEADER;

import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import uk.gov.caz.accounts.dto.AccountUsersResponse;
import uk.gov.caz.accounts.dto.UpdateUserRequestDto;
import uk.gov.caz.accounts.dto.UserResponse;

@RequestMapping(path = USERS_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
public interface AccountsUsersControllerApiSpec {

  /**
   * Endpoint specification that returns a list of users assigned to the account.
   *
   * @param accountId UUID representing accountId.
   * @return {@link AccountUsersResponse} users assigned to the account
   */
  @ApiOperation(value =
      "${swagger.operations.users.get-standard-users.description}")
  @ApiResponses({@ApiResponse(code = 503, message = "Service Unavailable Error"),
      @ApiResponse(code = 500, message = "Internal Server Error / No message available"),
      @ApiResponse(code = 404, message = "Account not found"),
      @ApiResponse(code = 200, message = "Users returned successfully")})
  @ApiImplicitParams({
      @ApiImplicitParam(name = X_CORRELATION_ID_HEADER, required = true,
          value = "UUID formatted string to track the request through the enquiries stack",
          paramType = "header"),
      @ApiImplicitParam(name = "accountId", required = true,
          value = "UUID formatted string from Account table"
              + " (must match PK in Account table)",
          paramType = "path")})
  @GetMapping
  ResponseEntity<AccountUsersResponse> getAllUsers(@PathVariable("accountId") UUID accountId);

  /**
   * Endpoint specification that returns a standard user details.
   *
   * @param accountId UUID representing accountId.
   * @param accountUserId UUID representing accountId.
   * @return {@link UserResponse} with user details.
   */
  @ApiOperation(value =
      "${swagger.operations.users.get-standard-user.description}")
  @ApiResponses({@ApiResponse(code = 503, message = "Service Unavailable Error"),
      @ApiResponse(code = 500, message = "Internal Server Error / No message available"),
      @ApiResponse(code = 404, message = "Account not found"),
      @ApiResponse(code = 200, message = "User returned successfully")})
  @ApiImplicitParams({
      @ApiImplicitParam(name = X_CORRELATION_ID_HEADER, required = true,
          value = "UUID formatted string to track the request through the enquiries stack",
          paramType = "header"),
      @ApiImplicitParam(name = "accountId", required = true,
          value = "UUID formatted string from Account table"
              + " (must match PK in Account table)",
          paramType = "path"),
      @ApiImplicitParam(name = "accountUserId", required = true,
          value = "UUID formatted string from AccountUser table"
              + " (must match PK in AccountUser table)",
          paramType = "path")})
  @GetMapping("/{accountUserId}")
  ResponseEntity<UserResponse> getUser(@PathVariable("accountId") UUID accountId,
      @PathVariable("accountUserId") UUID accountUserId);

  /**
   * Endpoint specification that returns result of removing an user.
   *
   * @param accountId string representing accountId.
   * @param accountUserId string representing userId assigned to given accountId.
   */
  @DeleteMapping("/{accountUserId}")
  ResponseEntity removeUser(
      @PathVariable("accountId") UUID accountId,
      @PathVariable("accountUserId") UUID accountUserId
  );

  /**
   * Gives a way to update user's data, including his permissions or name.
   */
  @ApiOperation(value =
      "${swagger.operations.users.manage-userdata.description}")
  @ApiResponses({@ApiResponse(code = 503, message = "Service Unavailable Error"),
      @ApiResponse(code = 500, message = "Internal Server Error / No message available"),
      @ApiResponse(code = 404, message = "Account / user not found"),
      @ApiResponse(code = 204, message = "User's data updated successfully")})
  @ApiImplicitParams({
      @ApiImplicitParam(name = X_CORRELATION_ID_HEADER, required = true,
          value = "UUID formatted string to track the request through the enquiries stack",
          paramType = "header"),
      @ApiImplicitParam(name = "accountId", required = true,
          value = "UUID formatted string from Account table"
              + " (must match PK in Account table)",
          paramType = "path"),
      @ApiImplicitParam(name = "accountUserId", required = true,
          value = "UUID formatted string from AccountUser table"
              + " (must match PK in AccountUser table)",
          paramType = "path")})
  @PatchMapping("/{accountUserId}")
  ResponseEntity<Void> updateUserData(
      @PathVariable("accountId") UUID accountId,
      @PathVariable("accountUserId") UUID accountUserId,
      @RequestBody UpdateUserRequestDto updateUserRequestDto
  );
}
