package uk.gov.caz.accounts.controller;

import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.caz.accounts.dto.PasswordResetRequest;
import uk.gov.caz.accounts.dto.SetPasswordRequest;
import uk.gov.caz.accounts.dto.UpdatePasswordRequest;
import uk.gov.caz.accounts.dto.ValidateTokenRequest;
import uk.gov.caz.accounts.model.CodeType;
import uk.gov.caz.accounts.service.PasswordResetService;
import uk.gov.caz.accounts.service.SetPasswordService;
import uk.gov.caz.accounts.service.UserCodeService;
import uk.gov.caz.accounts.service.exception.InvalidAccountUserPasswordResetCodeException;

/**
 * Rest Controller with endpoints related to password operation.
 */
@RestController
@RequiredArgsConstructor
public class PasswordController implements PasswordControllerApiSpec {

  public static final String BASE_PATH = "/v1/auth/password";
  public static final String RESET_PATH = "/reset";
  public static final String VALIDATE_TOKEN_PATH = RESET_PATH + "/validation";
  public static final String SET_PATH = "/set";
  public static final String UPDATE_PATH = "/update";

  private final PasswordResetService passwordResetService;
  private final UserCodeService userCodeService;
  private final SetPasswordService setPasswordService;
  private final UpdatePasswordService updatePasswordService;

  @Override
  public ResponseEntity<Void> reset(PasswordResetRequest passwordResetRequest) {
    passwordResetRequest.validate();

    passwordResetService.generateAndSaveResetToken(passwordResetRequest.getEmail(),
        URI.create(passwordResetRequest.getResetUrl()));
    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity<Void> validateToken(ValidateTokenRequest validateTokenRequest) {
    validateTokenRequest.validate();

    if (!userCodeService.isActive(validateTokenRequest.getToken(), CodeType.PASSWORD_RESET)) {
      throw new InvalidAccountUserPasswordResetCodeException();
    }

    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity<Void> setPassword(SetPasswordRequest setPasswordRequest) {
    setPasswordRequest.validate();

    setPasswordService.process(setPasswordRequest.getToken(), setPasswordRequest.getPassword());

    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity<Void> updatePassword(UpdatePasswordRequest updatePasswordRequest) {
    updatePasswordRequest.validate();

    updatePasswordService.process(updatePasswordRequest.getAccountUserId(),
        updatePasswordRequest.getOldPassword(), updatePasswordRequest.getNewPassword());

    return ResponseEntity.noContent().build();
  }
}
