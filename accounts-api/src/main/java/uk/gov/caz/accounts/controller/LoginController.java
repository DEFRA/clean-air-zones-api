package uk.gov.caz.accounts.controller;

import static uk.gov.caz.accounts.util.Strings.mask;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.caz.accounts.dto.LoginRequestDto;
import uk.gov.caz.accounts.dto.LoginResponseDto;
import uk.gov.caz.accounts.model.LoginData;
import uk.gov.caz.accounts.service.LoginService;
import uk.gov.caz.accounts.util.auth.LoginDataToLoginResponseConverter;

/**
 * Rest Controller with endpoints related to login operation.
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class LoginController implements LoginControllerApiSpec {

  public static final String LOGIN_PATH = "/v1/auth/login";

  private final LoginService loginService;

  @Override
  public ResponseEntity<LoginResponseDto> login(LoginRequestDto loginRequest) {
    loginRequest.validate();
    LoginData loginData = loginService.login(loginRequest.getEmail(), loginRequest.getPassword());
    log.info("User '{}' (account id: '{}') successfully logged in", mask(loginRequest.getEmail()),
        loginData.getAccount().getId());
    return ResponseEntity.ok(LoginDataToLoginResponseConverter.from(loginData));
  }
}