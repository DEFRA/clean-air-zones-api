package uk.gov.caz.accounts.controller;

import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.caz.accounts.dto.UserDetailsResponse;
import uk.gov.caz.accounts.model.Account;
import uk.gov.caz.accounts.model.UserEntity;
import uk.gov.caz.accounts.service.AccountFetcherService;
import uk.gov.caz.accounts.service.UserService;

@RestController
@RequiredArgsConstructor
@Slf4j
public class UsersController implements UsersControllerApiSpec {

  public static final String USERS_PATH = "/v1/users";

  private final UserService userService;
  private final AccountFetcherService accountFetcherService;

  @Override
  public ResponseEntity<UserDetailsResponse> getUser(UUID accountUserId) {
    Optional<UserEntity> user = userService.findUser(accountUserId);
    if (!user.isPresent()) {
      return ResponseEntity.notFound().build();
    }

    Optional<Account> account = accountFetcherService.findById(user.get().getAccountId());
    if (!account.isPresent()) {
      throw new IllegalStateException("Cannot find an account for an existing user");
    }

    return ResponseEntity.ok(UserDetailsResponse.from(user.get(), account.get()));
  }
}