package uk.gov.caz.accounts.controller;

import static com.google.common.base.MoreObjects.firstNonNull;
import static uk.gov.caz.accounts.util.Strings.mask;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import java.net.URI;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.caz.accounts.controller.exception.EmailNotUniqueException;
import uk.gov.caz.accounts.dto.AccountCreationRequestDto;
import uk.gov.caz.accounts.dto.AccountCreationResponseDto;
import uk.gov.caz.accounts.dto.AccountResponseDto;
import uk.gov.caz.accounts.dto.AccountUpdateRequestDto;
import uk.gov.caz.accounts.dto.AccountVerificationRequestDto;
import uk.gov.caz.accounts.dto.CreateAndInviteUserRequestDto;
import uk.gov.caz.accounts.dto.UserCreationResponseDto;
import uk.gov.caz.accounts.dto.UserForAccountCreationRequestDto;
import uk.gov.caz.accounts.dto.UserValidationRequest;
import uk.gov.caz.accounts.dto.UserVerificationEmailResendRequest;
import uk.gov.caz.accounts.dto.UserVerificationEmailResendResponse;
import uk.gov.caz.accounts.model.Account;
import uk.gov.caz.accounts.model.Permission;
import uk.gov.caz.accounts.model.User;
import uk.gov.caz.accounts.model.UserEntity;
import uk.gov.caz.accounts.service.AccountAdminUserCreatorService;
import uk.gov.caz.accounts.service.AccountCreatorService;
import uk.gov.caz.accounts.service.AccountFetcherService;
import uk.gov.caz.accounts.service.AccountStandardUserCreatorService;
import uk.gov.caz.accounts.service.AccountUpdateService;
import uk.gov.caz.accounts.service.UserService;
import uk.gov.caz.accounts.service.VerificationEmailConfirmationService;
import uk.gov.caz.accounts.service.VerificationEmailResendService;

/**
 * Rest Controller with endpoints related to accounts.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class AccountsController implements AccountsControllerApiSpec {

  @VisibleForTesting
  public static final String ACCOUNTS_PATH = "/v1/accounts";

  @VisibleForTesting
  public static final String RESEND_VERIFICATION_EMAIL_PATH =
      "/{accountId}/users/{accountUserId}/verifications";

  private final AccountCreatorService accountCreatorService;
  private final AccountUpdateService accountUpdateService;
  private final AccountAdminUserCreatorService accountAdminUserCreatorService;
  private final AccountStandardUserCreatorService accountStandardUserCreatorService;
  private final VerificationEmailConfirmationService verificationEmailConfirmationService;
  private final VerificationEmailResendService verificationEmailResendService;
  private final UserService userService;
  private final AccountFetcherService accountFetcherService;

  @Override
  public ResponseEntity<AccountCreationResponseDto> createAccount(
      AccountCreationRequestDto accountCreationRequestDto) {
    accountCreationRequestDto.validate();
    Account account = accountCreatorService.createAccount(
        accountCreationRequestDto.getAccountName());

    return ResponseEntity.status(HttpStatus.CREATED)
        .body(AccountCreationResponseDto.of(account.getId().toString()));
  }

  @Override
  public ResponseEntity<Void> updateAccount(
      UUID accountId, AccountUpdateRequestDto accountUpdateRequestDto) {
    accountUpdateRequestDto.validate();
    accountUpdateService.updateAccountName(accountId, accountUpdateRequestDto.getAccountName());
    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity<Map<String, String>> verifyUserEmail(
      AccountVerificationRequestDto request) {
    request.validate();
    log.info("Attempt to verify a user using token '{}'", mask(request.getToken()));
    verificationEmailConfirmationService.verifyUserEmail(UUID.fromString(request.getToken()));
    log.info("User has been successfully verified");
    return ResponseEntity.status(HttpStatus.OK)
        .body(ImmutableMap.of("message", "Account email verified successfully."));
  }

  @Override
  public ResponseEntity<UserCreationResponseDto> createAdminUserForAccount(UUID accountId,
      UserForAccountCreationRequestDto userForAccountCreationRequestDto) {
    userForAccountCreationRequestDto.validate();

    Pair<UserEntity, Account> userWithAccount = accountAdminUserCreatorService
        .createAdminUserForAccount(
            accountId,
            userForAccountCreationRequestDto.getEmail(),
            userForAccountCreationRequestDto.getPassword(),
            URI.create(userForAccountCreationRequestDto.getVerificationUrl())
        );

    UserEntity createdUser = userWithAccount.getLeft();
    Account matchedAccount = userWithAccount.getRight();

    return ResponseEntity.status(HttpStatus.CREATED)
        .body(UserCreationResponseDto.toDto(createdUser, matchedAccount.getName()));
  }

  @Override
  public ResponseEntity<UserVerificationEmailResendResponse> resendVerificationEmail(
      String accountId, String accountUserId, UserVerificationEmailResendRequest request) {
    request.validate();

    UserEntity userRecipient = verificationEmailResendService.resendVerificationEmail(
        UUID.fromString(accountId),
        UUID.fromString(accountUserId),
        URI.create(request.getVerificationUrl())
    );

    return ResponseEntity.ok(UserVerificationEmailResendResponse.from(userRecipient));
  }

  @Override
  public ResponseEntity<Void> createAndInviteStandardUserForAccount(
      UUID accountId, CreateAndInviteUserRequestDto request) {
    request.validate();

    accountStandardUserCreatorService.createStandardUserForAccount(accountId,
        UUID.fromString(request.getIsAdministeredBy()),
        request.getEmail(),
        request.getName(),
        toPermissions(request),
        URI.create(request.getVerificationUrl())
    );
    return ResponseEntity.status(HttpStatus.CREATED).build();
  }

  @Override
  public ResponseEntity<Void> validateUser(UUID accountId,
      UserValidationRequest userValidationRequest) {
    Optional<User> user = userService.getUserByEmail(userValidationRequest.validate().getEmail());
    if (user.isPresent()) {
      throw new EmailNotUniqueException("Provided email is not unique");
    }

    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
  }

  @Override
  public ResponseEntity<AccountResponseDto> getAccount(UUID accountId) {
    return accountFetcherService.findById(accountId)
        .map(e -> ResponseEntity.ok(new AccountResponseDto(e.getName())))
        .orElse(ResponseEntity.notFound().build());
  }

  private Set<Permission> toPermissions(CreateAndInviteUserRequestDto request) {
    return firstNonNull(request.getPermissions(), Collections.<String>emptySet())
        .stream()
        .map(Permission::valueOf)
        .collect(Collectors.toSet());
  }
}
