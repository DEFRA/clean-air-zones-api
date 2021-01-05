package uk.gov.caz.accounts.controller;

import java.net.URI;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.caz.accounts.dto.ConfirmEmailChangeRequest;
import uk.gov.caz.accounts.dto.ConfirmEmailChangeResponse;
import uk.gov.caz.accounts.dto.InitiateEmailChangeRequest;
import uk.gov.caz.accounts.service.ConfirmEmailChangeService;
import uk.gov.caz.accounts.service.EmailChangeService;

@RestController
@RequiredArgsConstructor
@Slf4j
public class UpdateEmailController implements UpdateEmailControllerApiSpec {

  public static final String PATH = "/v1/auth/email";

  private final EmailChangeService emailChangeService;
  private final ConfirmEmailChangeService confirmEmailChangeService;

  @Override
  public ResponseEntity<Void> initiateEmailChange(InitiateEmailChangeRequest request) {
    request.validate();

    emailChangeService.initiateEmailChange(
        UUID.fromString(request.getAccountUserId()),
        request.getNewEmail(),
        URI.create(request.getConfirmUrl())
    );

    return ResponseEntity.ok().build();
  }

  @Override
  public ResponseEntity<ConfirmEmailChangeResponse> confirmEmailChange(
      ConfirmEmailChangeRequest request) {
    request.validate();

    String confirmedEmail = confirmEmailChangeService.confirmEmailChange(
        UUID.fromString(request.getEmailChangeVerificationToken()),
        request.getPassword()
    );

    return ResponseEntity.ok(ConfirmEmailChangeResponse.from(confirmedEmail));
  }
}
