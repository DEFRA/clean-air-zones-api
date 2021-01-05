package uk.gov.caz.accounts.controller;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.caz.accounts.dto.AccountUserResponse;
import uk.gov.caz.accounts.dto.AccountUsersResponse;
import uk.gov.caz.accounts.dto.UpdateUserRequestDto;
import uk.gov.caz.accounts.dto.UserResponse;
import uk.gov.caz.accounts.model.Account;
import uk.gov.caz.accounts.model.Permission;
import uk.gov.caz.accounts.model.User;
import uk.gov.caz.accounts.service.AccountFetcherService;
import uk.gov.caz.accounts.service.UserPermissionsUpdaterService;
import uk.gov.caz.accounts.service.UserRemovalService;
import uk.gov.caz.accounts.service.UserRemovalStatus;
import uk.gov.caz.accounts.service.UserRenameService;
import uk.gov.caz.accounts.service.UserService;

@RestController
@RequiredArgsConstructor
@Slf4j
public class AccountUsersController implements AccountsUsersControllerApiSpec {

  public static final String USERS_PATH = "/v1/accounts/{accountId}/users";

  private final AccountFetcherService accountFetcherService;
  private final UserService userService;
  private final UserRemovalService userRemovalService;
  private final UserPermissionsUpdaterService userPermissionsUpdaterService;
  private final UserRenameService userRenameService;

  @Override
  public ResponseEntity<AccountUsersResponse> getAllUsers(UUID accountId) {
    Optional<Account> accountOptional = accountFetcherService.findById(accountId);
    Boolean accountMultiPayer = accountOptional
        .map(Account::isMultiPayerAccount)
        .orElse(false);
    return accountOptional
        .map(account -> userService.getAllUsersForAccountId(account.getId()))
        .map(this::toAccountUserResponses)
        .map(users -> AccountUsersResponse.builder()
            .users(users)
            .multiPayerAccount(accountMultiPayer)
            .build())
        .map(ResponseEntity::ok)
        .orElseGet(() -> ResponseEntity.notFound().build());
  }

  @Override
  public ResponseEntity<Void> removeUser(UUID accountId, UUID accountUserId) {
    UserRemovalStatus userRemovalStatus = userRemovalService
        .removeStandardUser(accountId, accountUserId);
    return ResponseEntity.status(userRemovalStatus.getHttpStatus()).build();
  }

  @Override
  public ResponseEntity<Void> updateUserData(UUID accountId,
      UUID accountUserId, UpdateUserRequestDto updateUserRequestDto) {
    updateUserRequestDto.validate();

    if (updateUserRequestDto.isChangingPermissions()) {
      userPermissionsUpdaterService.update(accountId, accountUserId,
          toPermissions(updateUserRequestDto));
    }
    if (updateUserRequestDto.isChangingName()) {
      userRenameService.updateUserName(accountId, accountUserId, updateUserRequestDto.getName());
    }
    return ResponseEntity.noContent().build();
  }

  private Set<Permission> toPermissions(UpdateUserRequestDto updateUserRequestDto) {
    return updateUserRequestDto.getPermissions()
        .stream()
        .map(Permission::valueOf)
        .collect(Collectors.toSet());
  }

  @Override
  public ResponseEntity<UserResponse> getUser(UUID accountId, UUID accountUserId) {
    return userService.getUserForAccountId(accountId, accountUserId)
        .map(UserResponse::from)
        .map(ResponseEntity::ok)
        .orElseGet(() -> ResponseEntity.notFound().build());
  }

  private List<AccountUserResponse> toAccountUserResponses(List<User> users) {
    return users.stream()
        .map(AccountUserResponse::from)
        .collect(Collectors.toList());
  }
}
