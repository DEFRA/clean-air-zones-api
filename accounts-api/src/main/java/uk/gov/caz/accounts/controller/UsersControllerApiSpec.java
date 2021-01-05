package uk.gov.caz.accounts.controller;

import static uk.gov.caz.accounts.controller.UsersController.USERS_PATH;
import static uk.gov.caz.correlationid.Constants.X_CORRELATION_ID_HEADER;

import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import uk.gov.caz.accounts.dto.UserDetailsResponse;
import uk.gov.caz.accounts.dto.UserResponse;

@RequestMapping(path = USERS_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
public interface UsersControllerApiSpec {

  /**
   * Endpoint that returns details of given user.
   *
   * @param accountUserId UUID representing accountId.
   * @return {@link UserResponse} with user details.
   */
  @ApiOperation(value =
      "${swagger.operations.users.get-user.description}")
  @ApiResponses({@ApiResponse(code = 503, message = "Service Unavailable Error"),
      @ApiResponse(code = 500, message = "Internal Server Error / No message available"),
      @ApiResponse(code = 404, message = "Account not found"),
      @ApiResponse(code = 200, message = "User returned successfully")})
  @ApiImplicitParams({
      @ApiImplicitParam(name = X_CORRELATION_ID_HEADER, required = true,
          value = "UUID formatted string to track the request through the enquiries stack",
          paramType = "header"),
      @ApiImplicitParam(name = "accountUserId", required = true,
          value = "UUID formatted string from AccountUser table"
              + " (must match PK in AccountUser table)",
          paramType = "path")})
  @GetMapping("/{accountUserId}")
  ResponseEntity<UserDetailsResponse> getUser(@PathVariable("accountUserId") UUID accountUserId);
}
