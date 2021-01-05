package uk.gov.caz.accounts.controller;

import static uk.gov.caz.accounts.controller.LoginController.LOGIN_PATH;
import static uk.gov.caz.correlationid.Constants.X_CORRELATION_ID_HEADER;

import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import uk.gov.caz.accounts.dto.LoginRequestDto;
import uk.gov.caz.accounts.dto.LoginResponseDto;

@RequestMapping(value = LOGIN_PATH, produces = {
    MediaType.APPLICATION_JSON_VALUE})
public interface LoginControllerApiSpec {

  /**
   * Endpoint specification that handles login operation.
   *
   * @param loginRequestDto object representing single login data.
   * @return {@link LoginResponseDto} representing logged user object.
   */
  @ApiOperation(
      value = "${swagger.operations.login.description}",
      response = LoginResponseDto.class
  )
  @ApiResponses({
      @ApiResponse(code = 503, message = "Service Unavailable Error (AWS Cognito)"),
      @ApiResponse(code = 500, message = "Internal Server Error / No message available"),
      @ApiResponse(code = 422, message = "User not confirmed"),
      @ApiResponse(code = 400, message = "Invalid parameters or missing correlation ID "),
      @ApiResponse(code = 401, message = "User not found or invalid credentials"),
      @ApiResponse(code = 200, message = "Login Successful")
  })
  @ApiImplicitParams({
      @ApiImplicitParam(name = X_CORRELATION_ID_HEADER,
          required = true,
          value = "UUID formatted string to track the request through the enquiries stack",
          paramType = "header"),
  })
  @PostMapping
  ResponseEntity<LoginResponseDto> login(@RequestBody LoginRequestDto loginRequestDto);
}